package com.rasaser.free.rasaserwidgets

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import android.content.Intent

/** User-visible local tool screen: never turns the camera on without a user tap. */
class ActionActivity : Activity() {
    companion object {
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_ACTION = "widget_action"
        private const val CAMERA_REQUEST = 123
    }
    private fun wt(key: String) = WidgetText.t(this, key)
    private var camera: CameraManager? = null
    private var torchId: String? = null
    private var torchOn = false
    private lateinit var status: TextView
    private var callback: CameraManager.TorchCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        if (id < 0) { finish(); return }
        val data = WidgetStore.load(this, id)
        val root = UiKit.page(this)
        UiKit.label(this, root, "VARYOK", 30f, UiKit.GOLD, true)
        if (intent.getStringExtra(EXTRA_ACTION) == "note") {
            UiKit.label(this, root, wt("personal_note"), 20f, UiKit.WHITE, true)
            val panel = UiKit.panel(this, root)
            UiKit.label(this, panel, data.target, 20f, UiKit.WHITE)
            UiKit.button(this, root, wt("copy_note")) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("RaSaSeR notu", data.target))
                Toast.makeText(this, wt("copied"), Toast.LENGTH_SHORT).show()
            }
        } else {
            UiKit.label(this, root, wt("torch_title"), 20f, UiKit.WHITE, true)
            status = UiKit.label(this, root, wt("not_ready"), 17f, UiKit.MUTED)
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            camera = cm
            torchId = cm.cameraIdList.firstOrNull { cameraId ->
                val c = cm.getCameraCharacteristics(cameraId)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            if (torchId == null) status.text = wt("no_torch")
            else {
                val observer = object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (cameraId == torchId) runOnUiThread {
                            torchOn = enabled
                            status.text = if (enabled) wt("torch_on") else wt("torch_off")
                        }
                    }
                    override fun onTorchModeUnavailable(cameraId: String) {
                        if (cameraId == torchId) runOnUiThread { status.text = wt("torch_unavailable") }
                    }
                }
                callback = observer
                cm.registerTorchCallback(observer, null)
                UiKit.button(this, root, wt("torch_toggle")) { requestToggle() }
            }
        }
        UiKit.button(this, root, wt("close"), false) { finish() }
    }
    private fun requestToggle() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
        } else toggleTorch()
    }
    private fun toggleTorch() {
        try {
            camera?.setTorchMode(torchId ?: return, !torchOn)
        } catch (_: Exception) {
            status.text = wt("torch_failed")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toggleTorch()
            else status.text = wt("torch_permission")
        }
    }
    override fun onDestroy() {
        callback?.let { camera?.unregisterTorchCallback(it) }
        super.onDestroy()
    }
}
