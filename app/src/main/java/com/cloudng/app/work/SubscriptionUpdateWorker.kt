package com.cloudng.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cloudng.app.data.model.UpdateInterval
import com.cloudng.app.data.repository.SubscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SubscriptionUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SubscriptionUpdate"
        const val KEY_SUB_ID = "subscription_id"

        fun scheduleAll(workManager: WorkManager, subId: String, interval: UpdateInterval) {
            val periodHours = when (interval) {
                UpdateInterval.MANUAL -> return
                UpdateInterval.HOURLY -> 1L
                UpdateInterval.EVERY_6H -> 6L
                UpdateInterval.DAILY -> 24L
                UpdateInterval.WEEKLY -> 168L
            }
            val request = PeriodicWorkRequestBuilder<SubscriptionUpdateWorker>(
                periodHours, TimeUnit.HOURS
            )
                .setInputData(workDataOf(KEY_SUB_ID to subId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("$TAG:$subId")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "$TAG:$subId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(workManager: WorkManager, subId: String) {
            workManager.cancelUniqueWork("$TAG:$subId")
        }
    }

    override suspend fun doWork(): Result {
        val subId = inputData.getString(KEY_SUB_ID) ?: return Result.failure()
        val subscription = subscriptionRepository.getById(subId) ?: return Result.failure()
        val result = subscriptionRepository.refresh(subscription)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
