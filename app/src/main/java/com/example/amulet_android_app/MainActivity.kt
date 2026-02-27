package com.example.amulet_android_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.amulet.core.foreground.AmuletForegroundService
import com.example.amulet.shared.domain.practices.PracticeSessionManager
import com.example.amulet_android_app.presentation.AmuletApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var practiceSessionManager: PracticeSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmuletApp()
        }

        restoreForegroundServiceIfNeeded()
    }

    private fun restoreForegroundServiceIfNeeded() {
        lifecycleScope.launch {
            val session = practiceSessionManager.activeSession.firstOrNull()
            if (session != null) {
                val intent = Intent(this@MainActivity, AmuletForegroundService::class.java)
                startForegroundService(intent)
            }
        }
    }
}