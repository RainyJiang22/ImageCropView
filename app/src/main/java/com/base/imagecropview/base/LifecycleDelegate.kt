package com.base.imagecropview.base

import android.app.Activity
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import java.util.*

class LifecycleDelegate(private val lifecycleOwner: LifecycleOwner) : LifecycleObserver {

    private val mRunnables: Queue<Runnable> = LinkedList()

    fun scheduleTaskAtStarted(runnable: Runnable) {
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            assertMainThread()
            mRunnables.add(runnable)
            considerExecute()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onStateChange() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            mRunnables.clear()
            lifecycle.removeObserver(this)
        } else {
            considerExecute()
        }
    }

    private var executing = false

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private fun considerExecute() {
        if (isAtLeastStarted && !executing) {
            executing = true
            var runnable = mRunnables.poll()
            while (runnable != null) {
                runnable.run()
                runnable = mRunnables.poll()
            }
            executing = false
        }
    }

    private val isAtLeastStarted: Boolean
        get() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    private fun assertMainThread() {
        check(isMainThread) { "you should perform the task at main thread." }
    }

    companion object {
        private val isMainThread: Boolean
            get() = Looper.getMainLooper().thread === Thread.currentThread()
    }
}

