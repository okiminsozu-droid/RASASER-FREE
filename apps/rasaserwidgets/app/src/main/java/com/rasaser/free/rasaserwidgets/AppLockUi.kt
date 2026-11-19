package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.util.concurrent.Executor

/**
 * One-time offline lock enrollment. The OS handles fingerprints; VARYOK never
 * sees, receives, stores or exports biometric templates.
 */
object AppLockUi {
    private fun t(activity: Activity, key: String) = AppLockText.t(activity, key)
    private fun executor(activity: Activity) = Executor { task -> activity.runOnUiThread(task) }

    fun setup(activity: Activity, done: () -> Unit) {
        if (AppSecurity.isEnabled(activity)) return
        AlertDialog.Builder(activity)
            .setTitle(t(activity, "choose"))
            .setItems(arrayOf(t(activity, "pin"), t(activity, "biometric"))) { _, choice ->
                if (choice == 0) createPin(activity, done)
                else setupBiometric(activity, done)
            }
            .setNegativeButton(t(activity, "cancel"), null)
            .show()
    }

    private fun input(activity: Activity, hint: String) = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        isSingleLine = true
        this.hint = hint
        maxLines = 1
        setPadding(20, 16, 20, 16)
    }

    private fun createPin(activity: Activity, done: () -> Unit) {
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 10, 26, 10)
        }
        val first = input(activity, t(activity, "enter"))
        val second = input(activity, t(activity, "confirm"))
        layout.addView(first)
        layout.addView(second)
        val dialog = AlertDialog.Builder(activity).setTitle(t(activity, "pin"))
            .setView(layout)
            .setPositiveButton(t(activity, "create"), null)
            .setNegativeButton(t(activity, "cancel"), null)
            .create()
        dialog.setOnShowListener {
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            save.setOnClickListener {
                val pin = first.text.toString()
                val confirm = second.text.toString()
                if (pin.length !in 4..12 || pin.any { it !in '0'..'9' }) {
                    first.error = t(activity, "pinRule")
                } else if (pin != confirm) {
                    second.error = t(activity, "notMatch")
                } else {
                    save.isEnabled = false
                    Thread {
                        val ok = AppSecurity.enablePin(activity, pin)
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            save.isEnabled = true
                            if (ok) {
                                dialog.dismiss()
                                done()
                            } else Toast.makeText(activity, t(activity, "enableError"),
                                Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                }
            }
        }
        dialog.show()
    }

    private fun setupBiometric(activity: Activity, done: () -> Unit) {
        if (Build.VERSION.SDK_INT < 28) {
            unavailable(activity)
            return
        }
        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(t(activity, "biometricTitle"))
                .setSubtitle(t(activity, "biometricSubtitle"))
                .setNegativeButton(t(activity, "cancel"), executor(activity)) { _, _ -> }
                .build()
            prompt.authenticate(CancellationSignal(), executor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (AppSecurity.enableBiometric(activity)) {
                            Toast.makeText(activity, t(activity, "fingerprintReady"), Toast.LENGTH_SHORT).show()
                            done()
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON)
                            unavailable(activity)
                    }
                })
        } catch (_: Exception) { unavailable(activity) }
    }

    fun unlock(activity: Activity, success: () -> Unit, cancel: () -> Unit,
               useDeviceCredential: () -> Unit) {
        when (AppSecurity.mode(activity)) {
            AppSecurity.MODE_PIN -> unlockPin(activity, success, cancel)
            AppSecurity.MODE_BIOMETRIC -> unlockBiometric(activity, success, cancel, useDeviceCredential)
            else -> success()
        }
    }

    private fun unlockPin(activity: Activity, success: () -> Unit, cancel: () -> Unit) {
        val field = input(activity, t(activity, "enter"))
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
            addView(field)
        }
        val dialog = AlertDialog.Builder(activity).setTitle(t(activity, "locked"))
            .setView(layout)
            .setPositiveButton(t(activity, "unlock"), null)
            .setNegativeButton(t(activity, "cancel")) { _, _ -> cancel() }
            .setCancelable(false)
            .create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val submitted = field.text.toString()
                field.text.clear()
                button.isEnabled = false
                Thread {
                    val okay = AppSecurity.checkPin(activity, submitted)
                    activity.runOnUiThread {
                        if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                        button.isEnabled = true
                        if (okay) {
                            dialog.dismiss()
                            success()
                        } else field.error = t(activity, "wrongPin")
                    }
                }.start()
            }
        }
        dialog.show()
    }

    private fun unlockBiometric(activity: Activity, success: () -> Unit, cancel: () -> Unit,
                                useDeviceCredential: () -> Unit) {
        if (Build.VERSION.SDK_INT < 28) {
            useDeviceCredential()
            return
        }
        var fallback = false
        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(t(activity, "locked"))
                .setSubtitle(t(activity, "biometricSubtitle"))
                .setNegativeButton(t(activity, "deviceCredential"), executor(activity)) { _, _ ->
                    fallback = true
                    useDeviceCredential()
                }
                .build()
            prompt.authenticate(CancellationSignal(), executor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        success()
                    }
                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        if (!fallback) {
                            // The system may return this when fingerprints are removed or locked.
                            // Preserve recovery through the phone's device credential.
                            useDeviceCredential()
                        }
                    }
                })
        } catch (_: Exception) { useDeviceCredential() }
    }

    private fun unavailable(activity: Activity) {
        AlertDialog.Builder(activity).setMessage(t(activity, "notAvailable"))
            .setPositiveButton(t(activity, "cancel"), null).show()
    }

    fun deviceCredential(activity: Activity): Intent? = try {
        val manager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!manager.isDeviceSecure) null
        else manager.createConfirmDeviceCredentialIntent(
            t(activity, "locked"), t(activity, "deviceCredential"))
    } catch (_: Exception) { null }
}
