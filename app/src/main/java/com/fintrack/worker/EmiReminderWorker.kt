package com.fintrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.fintrack.data.repository.BankAccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class EmiReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val bankAccountRepository: BankAccountRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val dayOfMonth = today.dayOfMonth

        val emis = bankAccountRepository.getAllActiveEmis()

        emis.forEach { emi ->
            if (!emi.isReminderEnabled || emi.isPaid) return@forEach
            val daysUntilDue = emi.dueDayOfMonth - dayOfMonth
            // Handle month wrap (e.g. today is day 28, due is day 3 next month)
            val adjustedDays = if (daysUntilDue < 0) {
                // Calculate days in current month remaining + due day
                val daysInMonth = today.lengthOfMonth()
                daysInMonth - dayOfMonth + emi.dueDayOfMonth
            } else daysUntilDue

            if (adjustedDays in 0..emi.reminderDaysBefore) {
                NotificationHelper.showEmiNotification(
                    context = applicationContext,
                    notifId = emi.id.toInt(),
                    title = "EMI Due Soon!",
                    body = "${emi.label} — ₹${emi.amount} due on day ${emi.dueDayOfMonth}"
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "EmiReminderDailyCheck"

        fun schedule(context: Context) {
            // Calculate delay so the first run is at 9:00 AM local time
            val now = java.time.LocalTime.now()
            val targetHour = 9
            val delayMinutes = if (now.hour < targetHour) {
                // Still before 9 AM today
                (targetHour - now.hour) * 60L - now.minute
            } else {
                // Already past 9 AM — schedule for tomorrow 9 AM
                (24 - now.hour + targetHour) * 60L - now.minute
            }

            val request = PeriodicWorkRequestBuilder<EmiReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
