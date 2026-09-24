package com.example

import android.app.Application
import com.example.data.SettingsManager
import com.example.data.local.DeskFlowDatabase
import com.example.server.DeskFlowHttpServer

class DeskFlowApp : Application() {
    lateinit var database: DeskFlowDatabase
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var httpServer: DeskFlowHttpServer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = DeskFlowDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)
        httpServer = DeskFlowHttpServer(this, settingsManager)
    }

    companion object {
        lateinit var instance: DeskFlowApp
            private set
    }
}
