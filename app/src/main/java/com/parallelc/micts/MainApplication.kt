package com.parallelc.micts

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import com.parallelc.micts.warmup.WarmupHooks

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { WarmupHooks.registerScreenOn(this) }
        runCatching { WarmupHooks.requestShizukuPermission() }
    }

    val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(SettingsViewModel::class.java)
    }
}