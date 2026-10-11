package dev.seirei.ruins

import android.app.Application

/**
 * 依存の組み立て場所。DIは手動で、全員がここの [container] から取る（ADR D-006）。
 * Widget の Receiver・BroadcastReceiver・Worker も `applicationContext as SeireiApplication` 経由で取る。
 */
class SeireiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
