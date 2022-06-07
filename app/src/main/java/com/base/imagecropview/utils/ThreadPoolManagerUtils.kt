package com.base.imagecropview.utils

import com.base.imagecropview.utils.ThreadPoolManagerUtils
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.jvm.Synchronized

/**
 *
 * 线程池管理类
 */
class ThreadPoolManagerUtils private constructor() {
    // 线程池的对象
    private var executor: ThreadPoolExecutor? = null

    /**
     * 使用线程池，线程池中线程的创建完全是由线程池自己来维护的，我们不需要创建任何的线程
     *
     * @param runnable 在线程池里面运行的线程
     */
    fun execute(runnable: Runnable?) {
        if (executor == null) {
            executor = ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
                SynchronousQueue(), Executors.defaultThreadFactory(),
                ThreadPoolExecutor.AbortPolicy()
            )
        }
        executor!!.execute(runnable) // 添加任务
    }

    /**
     * 移除指定的线程
     *
     * @param runnable 指定的线程
     */
    fun cancel(runnable: Runnable?) {
        if (runnable != null) {
            executor!!.queue.remove(runnable) // 移除任务
        }
    }

    companion object {
        private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

        // 核心线程数
        private val CORE_POOL_SIZE = CPU_COUNT + 1

        // 线程池最大线程数
        private val MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1

        // 非核心线程闲置时超时0s
        private const val KEEP_ALIVE = 1L

        // 确保该类只有一个实例对象
        private var sInstance: ThreadPoolManagerUtils? = null

        @get:Synchronized
        val instance: ThreadPoolManagerUtils?
            get() {
                if (sInstance == null) {
                    sInstance = ThreadPoolManagerUtils()
                }
                return sInstance
            }
    }
}