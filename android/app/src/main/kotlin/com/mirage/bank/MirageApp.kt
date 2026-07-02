package com.mirage.bank

import android.app.Application
import com.mirage.bank.core.di.AppContainer

class MirageApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
