package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.WordRepository
import com.example.ui.HomeScreen
import com.example.ui.VocabViewModel
import com.example.ui.VocabViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize daily goal notification channel (Android 13+ compatibility)
        NotificationHelper.createNotificationChannel(this)
        
        enableEdgeToEdge()

        // Build data layers
        val database = AppDatabase.getDatabase(applicationContext)
        val wordDao = database.wordDao()
        val repository = WordRepository(wordDao, applicationContext)

        setContent {
            val viewModel: VocabViewModel = viewModel(
                factory = VocabViewModelFactory(repository)
            )

            val stats by viewModel.userStats.collectAsState()
            
            // Check if user has explicit dark mode preference; fallback to system default
            val darkModeActive = stats?.darkModePreferred ?: isSystemInDarkTheme()

            MyApplicationTheme(
                darkTheme = darkModeActive,
                dynamicColor = false // Force custom color palette coherence
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
