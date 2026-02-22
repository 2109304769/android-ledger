package com.androidledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.androidledger.ui.navigation.AppNavigation
import com.androidledger.ui.theme.LedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startRoute = getStartRoute(intent)

        setContent {
            LedgerTheme {
                AppNavigation(startRoute = startRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun getStartRoute(intent: Intent?): String {
        val data = intent?.data
        return if (data?.scheme == "ledger" && data.host == "quick_entry") {
            "quick_entry"
        } else {
            "dashboard"
        }
    }
}
