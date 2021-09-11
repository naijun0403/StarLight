package com.mooner.starlight.plugincore.project

import com.mooner.starlight.plugincore.logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object JobLocker {

    /*
     * lock -> 프로세스 실행 -> release 체크 -> locked 시 release 까지 기다림
     *
     */

    private data class LockedJob(
        val job: Job,
        val listener: (Throwable?) -> Unit,
        var releaseCounter: Short = 0
    )

    private val runningJobs: ConcurrentMap<String, LockedJob> = ConcurrentHashMap()

    fun registerJob(
        key: String = Thread.currentThread().name,
        job: Job,
        onRelease: (Throwable?) -> Unit
    ): String {

        val data = LockedJob(
            job = job,
            listener = onRelease
        )
        if (!runningJobs.containsKey(key)) {
            runningJobs[key] = data
        }
        Logger.i("JobLocker", "Registered job $key")
        job.invokeOnCompletion {
            if (it != null) {
                Logger.i("JobLocker", "Released job $key with exception")
                runningJobs.remove(key)
                onRelease(it)
            } else {
                Logger.i("JobLocker", "Released job $key")
                if (data.releaseCounter <= 0) {
                    runningJobs.remove(key)
                    onRelease(null)
                } else {
                    Logger.i("JobLocker", "Postponed release of job $key")
                }
            }
        }
        return key
    }

    fun requestLock(key: String = Thread.currentThread().name) {
        if (runningJobs.containsKey(key)) {
            runningJobs[key]!!.releaseCounter++
            Logger.i("JobLocker", "Locked job $key")
        } else {
            Logger.i("JobLocker", "Failed to lock job: job $key is not registered or already released")
        }
    }

    fun requestRelease(key: String = Thread.currentThread().name) {
        if (runningJobs.containsKey(key)) {
            var isReleased = false
            runningJobs[key]!!.also {
                it.releaseCounter--
                if (it.releaseCounter <= 0) {
                    it.listener(null)
                    isReleased = true
                }
            }
            if (isReleased) {
                runningJobs.remove(key)
                Logger.i("JobLocker", "Released job $key")
            }
        } else {
            Logger.i("JobLocker", "Failed to release job: job $key is not registered or already released")
        }
    }

    fun forceRelease(key: String = Thread.currentThread().name) {
        if (runningJobs.containsKey(key)) {
            runningJobs[key]!!.also {
                if (it.job.isActive) {
                    it.job.cancel("Job force released")
                }
                it.listener(ForceReleasedException("Job force released"))
            }
            runningJobs.remove(key)
            Logger.w("JobLocker", "Force released job $key, this might not be a normal behavior")
        }
    }

    /*
    fun addOnReleaseListener(key: String, listener: (Throwable?) -> Unit) {
        if (runningJobs.containsKey(key)) {
            onReleaseListeners[key]!! += listener
        } else {
            Logger.w("ThreadLocker", "Could not find job $key")
        }
    }
     */
}