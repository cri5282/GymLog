package it.gymlog.app.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogSyncSchedulerTest {
    @Test fun schedule_enqueuesOnePeriodicJobWithNetworkConstraint() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        CatalogSyncScheduler.schedule(context)
        CatalogSyncScheduler.schedule(context)

        val jobs = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.UNIQUE_NAME).get()
        assertEquals(1, jobs.size)
        assertEquals(WorkInfo.State.ENQUEUED, jobs.single().state)
        assertEquals(NetworkType.CONNECTED, jobs.single().constraints.requiredNetworkType)
    }
}
