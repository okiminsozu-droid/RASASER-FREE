package com.rasaser.free.icerikureticisi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val requestedCreator = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(247, 241, 231)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        requestedCreator.longValue = intent?.getLongExtra("creator_id", -1L) ?: -1L
        setContent { CreatorHubApp(initialCreatorId = requestedCreator.longValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedCreator.longValue = intent.getLongExtra("creator_id", -1L)
    }
}
