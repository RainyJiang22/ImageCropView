package com.base.imagecropview.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * @author jiangshiyu
 * @date 2022/6/8
 */
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sContext = this
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var sContext: Context? = null

        @JvmStatic
        fun getApplication(): Context? {
            return sContext
        }
    }
}