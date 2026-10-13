package it.gymlog.app

import android.app.Application
import it.gymlog.app.timer.RestTimerService

class GymLogApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RestTimerService.createChannels(this)
    }
}
