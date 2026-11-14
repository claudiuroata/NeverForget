package com.neverforget.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.neverforget.app.ui.screens.HomeScreen
import com.neverforget.app.ui.theme.NeverForgetTheme
import com.neverforget.app.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        // Check and request notification permission on Android 13+
        requestNotificationPermission()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            NeverForgetTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onRequestNotificationPermission = { requestNotificationPermission() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh date in case app is resumed on a new calendar day
        viewModel.refreshDate()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val openPillsTab = intent.getBooleanExtra("open_pills_tab", false)
        val pillId = intent.getLongExtra("pill_id", -1L)
        val pillTimeSlot = intent.getStringExtra("pill_time_slot") ?: ""
        val taskId = intent.getLongExtra("task_id", -1L)
        val openPendingTab = intent.getBooleanExtra("open_pending_tab", false)

        if (pillId != -1L) {
            viewModel.openPillDetail(pillId, pillTimeSlot)
        } else if (openPillsTab) {
            viewModel.setWorkspace(com.neverforget.app.viewmodel.AppWorkspace.PILLS)
            viewModel.setPillFilter(com.neverforget.app.viewmodel.PillFilter.TODAY_DOSES)
            viewModel.dismissPillDetail()
        } else if (taskId != -1L) {
            viewModel.setWorkspace(com.neverforget.app.viewmodel.AppWorkspace.TASKS)
            viewModel.openTaskDetail(taskId)
        } else if (openPendingTab) {
            viewModel.setWorkspace(com.neverforget.app.viewmodel.AppWorkspace.TASKS)
            viewModel.setFilter(com.neverforget.app.viewmodel.TaskFilter.TODAY_PENDING)
            viewModel.dismissTaskDetail()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
