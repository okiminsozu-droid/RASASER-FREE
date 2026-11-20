package com.rasaser.free.trendnabiz

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NewsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val fresh = NewsStore.sync(applicationContext)
        fresh.take(20).forEach { NewsNotifications.publish(applicationContext, it) }
        LatestNewsWidget.refresh(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<NewsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "trendnabiz_news_check_v1",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
