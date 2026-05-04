package com.donyaep.netflow

import android.app.Application
import com.donyaep.netflow.data.AppContainer
import com.donyaep.netflow.data.DefaultAppContainer

class NetFlowApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}