package com.ninetymax.app

import android.app.Application
import com.ninetymax.app.data.Repository
import com.ninetymax.app.notify.Reminders

class MaxApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(filesDir)
        Reminders.createChannel(this)
        Reminders.scheduleAll(this, repository.state.value.reminders)
    }
}
