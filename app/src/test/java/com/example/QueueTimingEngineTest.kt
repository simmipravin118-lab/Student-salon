package com.example

import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.data.model.SalonService
import com.example.data.repository.SalonRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueTimingEngineTest {

    private val repository = SalonRepository(
        queueTicketDao = FakeQueueTicketDao(),
        shopConfigDao = FakeShopConfigDao(),
        context = FakeContext()
    )

    @Test
    fun testServingCustomerRemainingTime_BasedOnExactElapsedTime() {
        val now = 1000000000000L
        // Service is Haircut (20 min = 1,200,000 ms). Started 7 min 25 sec ago (445,000 ms ago).
        val startedAt = now - (7 * 60 * 1000L + 25 * 1000L)
        val servingTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Serving Customer",
            service = SalonService.HAIRCUT,
            status = QueueStatus.SERVING,
            queueDate = "2026-08-24",
            createdAt = startedAt - 60000L,
            startedAt = startedAt
        )

        val info = repository.calculateTicketInfo(servingTicket, listOf(servingTicket), now)

        // Remaining should be 20m - 7m 25s = 12m 35s (755,000 ms)
        assertEquals(12, info.remainingServingMinutes)
        assertEquals(35, info.remainingServingSeconds)
        assertEquals(755000L, info.remainingServingMillis)
        assertEquals("12 min 35 sec", info.remainingServingFormatted)
        assertEquals("NOW (In Chair)", info.estimatedTurnTimeFormatted)
    }

    @Test
    fun testNextWaitingCustomer_IncludesServingCustomerRemainingSeconds() {
        val now = 1000000000000L
        // Serving customer has 12m 35s remaining
        val startedAt = now - (7 * 60 * 1000L + 25 * 1000L) // 20m haircut started 7m25s ago
        val servingTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Serving Customer",
            service = SalonService.HAIRCUT,
            status = QueueStatus.SERVING,
            queueDate = "2026-08-24",
            createdAt = startedAt - 60000L,
            startedAt = startedAt
        )

        // Next customer waiting
        val nextWaitingTicket = QueueTicket(
            id = "t2",
            queueNumber = 2,
            customerName = "Next Waiting",
            service = SalonService.HAIR_BEARD, // 35 min service
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now - 50000L
        )

        val allTickets = listOf(servingTicket, nextWaitingTicket)
        val info = repository.calculateTicketInfo(nextWaitingTicket, allTickets, now)

        // Estimated wait for next waiting must include the 12m 35s of the serving customer, not the full 20m
        assertEquals(1, info.customersAhead)
        assertEquals(12, info.estimatedWaitingMinutes)
        assertEquals(35, info.estimatedWaitingSeconds)
        assertEquals(755000L, info.estimatedWaitingMillis)
        assertEquals("12 min 35 sec", info.estimatedWaitingFormatted)
    }

    @Test
    fun testMultipleWaitingCustomers_AddDurationsInQueueOrder() {
        val now = 1000000000000L
        val startedAt = now - (10 * 60 * 1000L) // 10 min elapsed out of 20 min -> 10m remaining
        val servingTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Serving Customer",
            service = SalonService.HAIRCUT, // 20 min
            status = QueueStatus.SERVING,
            queueDate = "2026-08-24",
            createdAt = startedAt,
            startedAt = startedAt
        )

        val waiting1 = QueueTicket(
            id = "t2",
            queueNumber = 2,
            customerName = "Waiting 1",
            service = SalonService.HAIR_BEARD, // 35 min
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now
        )

        val waiting2 = QueueTicket(
            id = "t3",
            queueNumber = 3,
            customerName = "Waiting 2",
            service = SalonService.HAIRCUT, // 20 min
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now
        )

        val allTickets = listOf(servingTicket, waiting1, waiting2)

        val info1 = repository.calculateTicketInfo(waiting1, allTickets, now)
        assertEquals(1, info1.customersAhead)
        assertEquals(10, info1.estimatedWaitingMinutes)
        assertEquals(0, info1.estimatedWaitingSeconds)

        val info2 = repository.calculateTicketInfo(waiting2, allTickets, now)
        // Waiting 2 has 2 customers ahead (serving + waiting1), wait = 10m (serving) + 35m (waiting1) = 45m
        assertEquals(2, info2.customersAhead)
        assertEquals(45, info2.estimatedWaitingMinutes)
        assertEquals(0, info2.estimatedWaitingSeconds)
    }

    @Test
    fun testEmptyChair_ZeroWaitingAhead_EstimatedWaitIsZero() {
        val now = 1000000000000L
        val ticket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "First Waiting",
            service = SalonService.HAIRCUT,
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now
        )

        val info = repository.calculateTicketInfo(ticket, listOf(ticket), now)
        assertEquals(0, info.customersAhead)
        assertEquals(0, info.estimatedWaitingMinutes)
        assertEquals(0, info.estimatedWaitingSeconds)
        assertEquals("0 min", info.estimatedWaitingFormatted)
    }

    @Test
    fun testEarlyCompletion_ImmediatelyRecalculatesNextCustomerWait() {
        val now = 1000000000000L
        val completedTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Completed Early",
            service = SalonService.HAIRCUT,
            status = QueueStatus.COMPLETED,
            queueDate = "2026-08-24",
            createdAt = now - 600000L,
            startedAt = now - 300000L,
            completedAt = now // completed after 5 mins instead of 20 mins
        )

        val nextTicket = QueueTicket(
            id = "t2",
            queueNumber = 2,
            customerName = "Next Customer",
            service = SalonService.HAIRCUT,
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now - 100000L
        )

        val allTickets = listOf(completedTicket, nextTicket)
        val info = repository.calculateTicketInfo(nextTicket, allTickets, now)

        // Completed ticket must not contribute to wait time, next customer is now immediate
        assertEquals(0, info.customersAhead)
        assertEquals(0, info.estimatedWaitingMinutes)
        assertEquals(0, info.estimatedWaitingSeconds)
        assertEquals("0 min", info.estimatedWaitingFormatted)
    }

    @Test
    fun testSkippedAndCancelledTickets_DoNotContributeToWaitTime() {
        val now = 1000000000000L
        val skippedTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Skipped",
            service = SalonService.HAIR_BEARD_FACIAL, // 45 min
            status = QueueStatus.SKIPPED,
            queueDate = "2026-08-24",
            createdAt = now - 500000L
        )

        val cancelledTicket = QueueTicket(
            id = "t2",
            queueNumber = 2,
            customerName = "Cancelled",
            service = SalonService.HAIR_BEARD_FACIAL, // 45 min
            status = QueueStatus.CANCELLED,
            queueDate = "2026-08-24",
            createdAt = now - 400000L
        )

        val waitingTicket = QueueTicket(
            id = "t3",
            queueNumber = 3,
            customerName = "Active Waiting",
            service = SalonService.HAIRCUT,
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now - 200000L
        )

        val allTickets = listOf(skippedTicket, cancelledTicket, waitingTicket)
        val info = repository.calculateTicketInfo(waitingTicket, allTickets, now)

        assertEquals(0, info.customersAhead)
        assertEquals(0, info.estimatedWaitingMinutes)
        assertEquals(0, info.estimatedWaitingSeconds)
    }

    @Test
    fun testRejoinCustomerPriority_HandledWithoutInterruptingServingCustomer() {
        val now = 1000000000000L
        val startedAt = now - (5 * 60 * 1000L) // 15 min remaining out of 20 min
        val servingTicket = QueueTicket(
            id = "t1",
            queueNumber = 1,
            customerName = "Serving",
            service = SalonService.HAIRCUT,
            status = QueueStatus.SERVING,
            queueDate = "2026-08-24",
            createdAt = startedAt,
            startedAt = startedAt
        )

        val normalWaiting = QueueTicket(
            id = "t2",
            queueNumber = 2,
            customerName = "Normal Waiting",
            service = SalonService.HAIRCUT, // 20 min
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now - 100000L,
            isRejoinedPriority = false
        )

        val rejoinedTicket = QueueTicket(
            id = "t3",
            queueNumber = 3,
            customerName = "Rejoined Customer",
            service = SalonService.HAIR_BEARD, // 35 min
            status = QueueStatus.WAITING,
            queueDate = "2026-08-24",
            createdAt = now - 200000L,
            isRejoinedPriority = true
        )

        val allTickets = listOf(servingTicket, normalWaiting, rejoinedTicket)

        // Rejoined customer gets priority over normal waiting
        val rejoinedInfo = repository.calculateTicketInfo(rejoinedTicket, allTickets, now)
        assertEquals(1, rejoinedInfo.customersAhead) // only serving is ahead
        assertEquals(15, rejoinedInfo.estimatedWaitingMinutes)

        // Normal waiting is pushed behind rejoined customer
        val normalInfo = repository.calculateTicketInfo(normalWaiting, allTickets, now)
        assertEquals(2, normalInfo.customersAhead) // serving + rejoined are ahead
        assertEquals(15 + 35, normalInfo.estimatedWaitingMinutes) // 15m serving + 35m rejoined = 50m
    }
}

// Minimal test fakes to instantiate SalonRepository for testing calculateTicketInfo
private class FakeQueueTicketDao : com.example.data.local.dao.QueueTicketDao {
    override fun getTodayTickets(todayDate: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.QueueTicketEntity>())
    override fun getTicketById(ticketId: String) = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun getTicketDirect(ticketId: String) = null
    override suspend fun getMaxQueueNumber(todayDate: String) = 0
    override fun getServingTicket(todayDate: String) = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun getServingTicketDirect(todayDate: String) = null
    override fun getWaitingTickets(todayDate: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.QueueTicketEntity>())
    override fun getSkippedTickets(todayDate: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.QueueTicketEntity>())
    override suspend fun insertTicket(ticket: com.example.data.local.entity.QueueTicketEntity) {}
    override suspend fun insertAll(tickets: List<com.example.data.local.entity.QueueTicketEntity>) {}
    override suspend fun updateTicket(ticket: com.example.data.local.entity.QueueTicketEntity) {}
    override suspend fun updateStatus(ticketId: String, newStatus: String) {}
    override suspend fun startService(ticketId: String, newStatus: String, startedAt: Long) {}
    override suspend fun completeService(ticketId: String, newStatus: String, completedAt: Long) {}
    override suspend fun rejoinSkippedTicket(ticketId: String) {}
    override suspend fun clearTodayQueue(todayDate: String) {}
    override suspend fun clearAll() {}
}

private class FakeShopConfigDao : com.example.data.local.dao.ShopConfigDao {
    override fun getShopConfig() = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun getShopConfigDirect() = null
    override suspend fun insertOrUpdate(config: com.example.data.local.entity.ShopConfigEntity) {}
    override suspend fun updateShopStatus(isOpen: Boolean) {}
    override suspend fun updateAnnouncement(announcement: String) {}
}

private class FakeContext : android.content.ContextWrapper(null) {
    private val memoryPrefs = FakeSharedPreferences()
    override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences = memoryPrefs
    override fun getApplicationContext(): android.content.Context = this
}

private class FakeSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    override fun getAll(): Map<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = (map[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = EditorImpl()
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : android.content.SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        override fun putString(key: String?, value: String?) = apply { temp[key ?: ""] = value }
        override fun putStringSet(key: String?, values: Set<String>?) = apply { temp[key ?: ""] = values }
        override fun putInt(key: String?, value: Int) = apply { temp[key ?: ""] = value }
        override fun putLong(key: String?, value: Long) = apply { temp[key ?: ""] = value }
        override fun putFloat(key: String?, value: Float) = apply { temp[key ?: ""] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { temp[key ?: ""] = value }
        override fun remove(key: String?) = apply { temp.remove(key ?: ""); map.remove(key ?: "") }
        override fun clear() = apply { temp.clear(); map.clear() }
        override fun commit(): Boolean { map.putAll(temp); return true }
        override fun apply() { map.putAll(temp) }
    }
}
