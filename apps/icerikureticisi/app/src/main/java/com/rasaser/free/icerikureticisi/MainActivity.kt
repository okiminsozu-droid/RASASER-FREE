package com.rasaser.free.icerikureticisi

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Never accepts external/untrusted intents to bypass the PIN.
 * The initial locked state is computed from private PIN storage.
 */
class MainActivity : FragmentActivity() {
    private val requestedCreator = mutableLongStateOf(-1L)
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(247, 241, 231)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        requestedCreator.longValue = intent?.getLongExtra("creator_id", -1L) ?: -1L
        unlocked = !AppLock.enabled(this)

        setContent {
            if (!unlocked) {
                MaterialTheme(colorScheme = lightColorScheme(
                    background = Color(0xFFF7F1E7),
                    onBackground = Color(0xFF262521)
                )) {
                    UnlockScreen(this, onUnlock = { unlocked = true })
                }
            } else {
                CreatorHubApp(initialCreatorId = requestedCreator.longValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedCreator.longValue = intent.getLongExtra("creator_id", -1L)
    }
}
