package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.dao.QueueTicketDao
import com.example.data.local.dao.ShopConfigDao
import com.example.data.local.entity.QueueTicketEntity
import com.example.data.local.entity.ShopConfigEntity
import com.example.data.model.CustomerTicketInfo
import com.example.data.model.DailySummary
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.data.model.SalonService
import com.example.data.model.ShopConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class SalonRepository(
    private val queueTicketDao: QueueTicketDao,
    private val shopConfigDao: ShopConfigDao,
    context: Context,
    firestoreInstance: FirebaseFirestore? = null,
    authInstance: FirebaseAuth? = null
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("student_salon_prefs", Context.MODE_PRIVATE)

    private fun logWarn(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg")
        }
    }

    private val firestore: FirebaseFirestore? = firestoreInstance ?: try {
        FirebaseApp.initializeApp(context)
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        logWarn(TAG, "Firestore initialization fallback: ${e.message}")
        null
    }

    private val auth: FirebaseAuth? = authInstance ?: try {
        FirebaseAuth.getInstance().apply {
            if (currentUser == null) {
                signInAnonymously().addOnFailureListener {
                    logWarn(TAG, "Anonymous sign-in note: ${it.message}")
                }
            }
        }
    } catch (e: Throwable) {
        logWarn(TAG, "Auth initialization fallback: ${e.message}")
        null
    }

    suspend fun ensureAnonymousAuth(): String? {
        val fbAuth = auth ?: return null
        return try {
            val user = fbAuth.currentUser
            if (user != null) {
                user.uid
            } else {
                val result = fbAuth.signInAnonymously().awaitTask()
                result.user?.uid
            }
        } catch (e: Exception) {
            logWarn(TAG, "ensureAnonymousAuth fallback: ${e.message}")
            fbAuth.currentUser?.uid
        }
    }

    private val _syncStatus = kotlinx.coroutines.flow.MutableStateFlow(
        com.example.data.model.SyncStatus(
            isCloudConnected = firestore != null,
            isUsingLocalCache = firestore == null,
            statusMessage = if (firestore != null) "Live Cloud Synced" else "Offline Mode • Local Room Cache Active"
        )
    )
    val syncStatus: kotlinx.coroutines.flow.StateFlow<com.example.data.model.SyncStatus> = _syncStatus

    fun getCurrentUserUid(): String? {
        return auth?.currentUser?.uid
    }

    fun isOwnerAuthenticated(): Boolean {
        return prefs.getBoolean(KEY_OWNER_AUTH, false)
    }

    fun setOwnerAuthenticated(authenticated: Boolean) {
        prefs.edit().putBoolean(KEY_OWNER_AUTH, authenticated).apply()
    }

    private val salonDocRef by lazy {
        firestore?.collection(COLLECTION_SALONS)?.document(DEFAULT_SALON_ID)
    }

    companion object {
        private const val TAG = "SalonRepository"
        private const val COLLECTION_SALONS = "salons"
        private const val COLLECTION_TICKETS = "tickets"
        private const val DEFAULT_SALON_ID = "student_salon_telo"

        private const val KEY_MY_TICKET_ID = "my_ticket_id"
        private const val KEY_MY_TICKET_IDS = "my_ticket_ids"
        private const val KEY_OWNER_AUTH = "owner_authenticated"
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getShopConfig(): Flow<ShopConfig> {
        val remoteDoc = salonDocRef
        if (remoteDoc == null) {
            return shopConfigDao.getShopConfig().map { entity ->
                entity?.toDomain() ?: ShopConfig()
            }
        }

        return callbackFlow {
            val initialLocal = shopConfigDao.getShopConfigDirect()?.toDomain() ?: ShopConfig()
            trySend(initialLocal)

            val listener: ListenerRegistration = remoteDoc.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    logWarn(TAG, "Shop config snapshot error / not exists: ${error?.message}")
                    _syncStatus.value = com.example.data.model.SyncStatus(
                        isCloudConnected = false,
                        isUsingLocalCache = true,
                        statusMessage = "Offline Mode • Showing Local Cache"
                    )
                    return@addSnapshotListener
                }

                val isCache = snapshot.metadata.isFromCache
                _syncStatus.value = com.example.data.model.SyncStatus(
                    isCloudConnected = !isCache,
                    isUsingLocalCache = isCache,
                    statusMessage = if (!isCache) "Live Cloud Synced" else "Offline Mode • Showing Local Cache"
                )

                val config = snapshotToShopConfig(snapshot)
                trySend(config)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        shopConfigDao.insertOrUpdate(ShopConfigEntity.fromDomain(config))
                    } catch (e: Exception) {
                        logWarn(TAG, "Local cache sync error: ${e.message}")
                    }
                }
            }

            awaitClose { listener.remove() }
        }
    }

    suspend fun ensureDefaultShopConfig() {
        val existing = shopConfigDao.getShopConfigDirect()
        if (existing == null) {
            shopConfigDao.insertOrUpdate(ShopConfigEntity.fromDomain(ShopConfig()))
        }

        salonDocRef?.let { docRef ->
            try {
                val snapshot = docRef.get().awaitTask()
                if (!snapshot.exists()) {
                    val defaultConfig = ShopConfig()
                    val data = hashMapOf(
                        "shopName" to defaultConfig.shopName,
                        "location" to defaultConfig.location,
                        "openingHours" to defaultConfig.openingHours,
                        "contactPhone" to defaultConfig.contactPhone,
                        "announcement" to defaultConfig.announcement,
                        "isOpen" to defaultConfig.isOpen,
                        "ownerPin" to defaultConfig.ownerPin,
                        "dailyCounterDate" to getTodayDateString(),
                        "currentMaxQueueNumber" to 0L
                    )
                    docRef.set(data, SetOptions.merge()).awaitTask()
                }
            } catch (e: Exception) {
                logWarn(TAG, "ensureDefaultShopConfig cloud check fallback: ${e.message}")
            }
        }
    }

    fun getTodayTickets(): Flow<List<QueueTicket>> {
        val today = getTodayDateString()
        val ticketsCollection = salonDocRef?.collection(COLLECTION_TICKETS)

        if (ticketsCollection == null) {
            return queueTicketDao.getTodayTickets(today).map { list ->
                list.map { it.toDomain() }
            }
        }

        return callbackFlow {
            val listener: ListenerRegistration = ticketsCollection
                .whereEqualTo("queueDate", today)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        logWarn(TAG, "Tickets snapshot error: ${error?.message}")
                        _syncStatus.value = com.example.data.model.SyncStatus(
                            isCloudConnected = false,
                            isUsingLocalCache = true,
                            statusMessage = "Offline Mode • Showing Local Cache"
                        )
                        return@addSnapshotListener
                    }

                    val isCache = snapshot.metadata.isFromCache
                    _syncStatus.value = com.example.data.model.SyncStatus(
                        isCloudConnected = !isCache,
                        isUsingLocalCache = isCache,
                        statusMessage = if (!isCache) "Live Cloud Synced" else "Offline Mode • Showing Local Cache"
                    )

                    val tickets = snapshot.documents.mapNotNull { doc ->
                        docToQueueTicket(doc)
                    }.sortedWith(
                        compareByDescending<QueueTicket> { it.isRejoinedPriority }
                            .thenBy { it.queueNumber }
                    )

                    trySend(tickets)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val entities = tickets.map { QueueTicketEntity.fromDomain(it) }
                            queueTicketDao.clearTodayQueue(today)
                            if (entities.isNotEmpty()) {
                                queueTicketDao.insertAll(entities)
                            }
                        } catch (e: Exception) {
                            logWarn(TAG, "Sync to local Room error: ${e.message}")
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    fun getWaitingTickets(): Flow<List<QueueTicket>> {
        return getTodayTickets().map { list ->
            list.filter { it.status == QueueStatus.WAITING }
                .sortedWith(
                    compareByDescending<QueueTicket> { it.isRejoinedPriority }
                        .thenBy { it.queueNumber }
                )
        }
    }

    fun getSkippedTickets(): Flow<List<QueueTicket>> {
        return getTodayTickets().map { list ->
            list.filter { it.status == QueueStatus.SKIPPED }.sortedBy { it.queueNumber }
        }
    }

    fun getServingTicket(): Flow<QueueTicket?> {
        return getTodayTickets().map { list ->
            list.firstOrNull { it.status == QueueStatus.SERVING }
        }
    }

    fun getMySavedTicketId(): String? {
        return prefs.getString(KEY_MY_TICKET_ID, null)
    }

    fun getMySavedTicketIds(): Set<String> {
        val set = prefs.getStringSet(KEY_MY_TICKET_IDS, emptySet())?.toSet() ?: emptySet()
        val single = prefs.getString(KEY_MY_TICKET_ID, null)
        return if (!single.isNullOrBlank()) set + single else set
    }

    fun addMyTicketId(ticketId: String) {
        val currentSet = prefs.getStringSet(KEY_MY_TICKET_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(ticketId)
        prefs.edit()
            .putStringSet(KEY_MY_TICKET_IDS, currentSet)
            .putString(KEY_MY_TICKET_ID, ticketId)
            .apply()
    }

    fun removeMyTicketId(ticketId: String) {
        val currentSet = prefs.getStringSet(KEY_MY_TICKET_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(ticketId)
        val editor = prefs.edit().putStringSet(KEY_MY_TICKET_IDS, currentSet)
        if (prefs.getString(KEY_MY_TICKET_ID, null) == ticketId) {
            editor.putString(KEY_MY_TICKET_ID, currentSet.lastOrNull())
        }
        editor.apply()
    }

    fun saveMyTicketId(ticketId: String?) {
        if (ticketId != null) {
            addMyTicketId(ticketId)
        } else {
            clearAllMySavedTickets()
        }
    }

    fun clearAllMySavedTickets() {
        prefs.edit()
            .remove(KEY_MY_TICKET_IDS)
            .remove(KEY_MY_TICKET_ID)
            .apply()
    }

    fun getMyTicketInfo(currentTimeMillis: Long): Flow<CustomerTicketInfo?> {
        return combine(
            getTodayTickets(),
            getShopConfig()
        ) { tickets, _ ->
            val savedId = getMySavedTicketId() ?: return@combine null
            val myTicket = tickets.find { it.id == savedId } ?: return@combine null
            calculateTicketInfo(myTicket, tickets, currentTimeMillis)
        }
    }

    fun calculateTicketInfo(
        ticket: QueueTicket,
        allTodayTickets: List<QueueTicket>,
        now: Long
    ): CustomerTicketInfo {
        if (ticket.status == QueueStatus.SERVING) {
            val elapsedMillis = if (ticket.startedAt != null) {
                max(0L, now - ticket.startedAt)
            } else 0L
            val totalDurationMillis = ticket.service.durationMinutes * 60_000L
            val remainingMillis = max(0L, totalDurationMillis - elapsedMillis)
            val remainingTotalSeconds = remainingMillis / 1000L
            val remainingMinutes = (remainingTotalSeconds / 60L).toInt()
            val remainingSeconds = (remainingTotalSeconds % 60L).toInt()
            val formatted = formatMinutesSeconds(remainingMinutes, remainingSeconds)

            return CustomerTicketInfo(
                ticket = ticket,
                customersAhead = 0,
                estimatedWaitingMinutes = 0,
                estimatedTurnTimeFormatted = "NOW (In Chair)",
                remainingServingMinutes = remainingMinutes,
                estimatedWaitingSeconds = 0,
                estimatedWaitingMillis = 0L,
                estimatedWaitingFormatted = "0 min",
                remainingServingSeconds = remainingSeconds,
                remainingServingMillis = remainingMillis,
                remainingServingFormatted = formatted
            )
        }

        if (ticket.status == QueueStatus.COMPLETED || ticket.status == QueueStatus.CANCELLED || ticket.status == QueueStatus.SKIPPED) {
            return CustomerTicketInfo(
                ticket = ticket,
                customersAhead = 0,
                estimatedWaitingMinutes = 0,
                estimatedTurnTimeFormatted = ticket.status.displayName,
                remainingServingMinutes = 0,
                estimatedWaitingSeconds = 0,
                estimatedWaitingMillis = 0L,
                estimatedWaitingFormatted = "0 min",
                remainingServingSeconds = 0,
                remainingServingMillis = 0L,
                remainingServingFormatted = "0 min"
            )
        }

        val serving = allTodayTickets.firstOrNull { it.status == QueueStatus.SERVING }
        val waitingQueue = allTodayTickets
            .filter { it.status == QueueStatus.WAITING }
            .sortedWith(
                compareByDescending<QueueTicket> { it.isRejoinedPriority }
                    .thenBy { it.queueNumber }
            )

        val servingRemainingMillis = if (serving != null && serving.startedAt != null) {
            val totalServingDurationMillis = serving.service.durationMinutes * 60_000L
            val elapsed = max(0L, now - serving.startedAt)
            max(0L, totalServingDurationMillis - elapsed)
        } else if (serving != null) {
            serving.service.durationMinutes * 60_000L
        } else {
            0L
        }

        val servingCustomerCount = if (serving != null) 1 else 0

        val myIndexInWaiting = waitingQueue.indexOfFirst { it.id == ticket.id }
        val customersAhead: Int
        val totalEstimatedWaitingMillis: Long

        if (myIndexInWaiting == -1) {
            customersAhead = servingCustomerCount
            totalEstimatedWaitingMillis = servingRemainingMillis
        } else {
            val waitingAhead = waitingQueue.take(myIndexInWaiting)
            customersAhead = servingCustomerCount + waitingAhead.size
            val waitingAheadMillis = waitingAhead.sumOf { it.service.durationMinutes * 60_000L }
            totalEstimatedWaitingMillis = servingRemainingMillis + waitingAheadMillis
        }

        val totalWaitSeconds = totalEstimatedWaitingMillis / 1000L
        val waitMinutes = (totalWaitSeconds / 60L).toInt()
        val waitSeconds = (totalWaitSeconds % 60L).toInt()
        val formattedWait = formatMinutesSeconds(waitMinutes, waitSeconds)

        val estimatedTurnTimestamp = now + totalEstimatedWaitingMillis
        val estimatedTurnTimeFormatted = if (customersAhead == 0 && totalEstimatedWaitingMillis == 0L) {
            formatTurnTime(now)
        } else {
            formatTurnTime(estimatedTurnTimestamp)
        }

        val servingSecTotal = servingRemainingMillis / 1000L
        val servingMinutes = (servingSecTotal / 60L).toInt()
        val servingSeconds = (servingSecTotal % 60L).toInt()

        return CustomerTicketInfo(
            ticket = ticket,
            customersAhead = customersAhead,
            estimatedWaitingMinutes = waitMinutes,
            estimatedTurnTimeFormatted = estimatedTurnTimeFormatted,
            remainingServingMinutes = servingMinutes,
            estimatedWaitingSeconds = waitSeconds,
            estimatedWaitingMillis = totalEstimatedWaitingMillis,
            estimatedWaitingFormatted = formattedWait,
            remainingServingSeconds = servingSeconds,
            remainingServingMillis = servingRemainingMillis,
            remainingServingFormatted = formatMinutesSeconds(servingMinutes, servingSeconds)
        )
    }

    private fun formatMinutesSeconds(minutes: Int, seconds: Int): String {
        return when {
            minutes > 0 && seconds > 0 -> "${minutes} min ${seconds} sec"
            minutes > 0 -> "${minutes} min"
            seconds > 0 -> "${seconds} sec"
            else -> "0 min"
        }
    }

    private fun formatTurnTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    suspend fun joinQueue(
        customerName: String,
        customerPhone: String,
        service: SalonService,
        notes: String = ""
    ): QueueTicket = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val ticketId = UUID.randomUUID().toString()
        val creatorUid = ensureAnonymousAuth() ?: ""

        val remoteDoc = salonDocRef
        val fs = firestore

        if (fs != null && remoteDoc != null) {
            try {
                val assignedQueueNumber = fs.runTransaction { transaction ->
                    val salonSnapshot = transaction.get(remoteDoc)
                    val counterDate = salonSnapshot.getString("dailyCounterDate") ?: today
                    val lastNumber = if (counterDate == today) {
                        salonSnapshot.getLong("currentMaxQueueNumber") ?: 0L
                    } else {
                        0L
                    }

                    val nextQueueNumber = (lastNumber + 1).toInt()
                    val ticketDocRef = remoteDoc.collection(COLLECTION_TICKETS).document(ticketId)

                    transaction.update(
                        remoteDoc,
                        mapOf(
                            "dailyCounterDate" to today,
                            "currentMaxQueueNumber" to nextQueueNumber.toLong()
                        )
                    )

                    val ticketMap = hashMapOf(
                        "id" to ticketId,
                        "queueNumber" to nextQueueNumber.toLong(),
                        "customerName" to customerName.trim(),
                        "customerPhone" to customerPhone.trim(),
                        "serviceName" to service.name,
                        "statusName" to QueueStatus.WAITING.name,
                        "queueDate" to today,
                        "createdAt" to System.currentTimeMillis(),
                        "startedAt" to null,
                        "completedAt" to null,
                        "isRejoinedPriority" to false,
                        "notes" to notes.trim(),
                        "creatorUid" to creatorUid
                    )
                    transaction.set(ticketDocRef, ticketMap)

                    nextQueueNumber
                }.awaitTask()

                val newTicket = QueueTicket(
                    id = ticketId,
                    queueNumber = assignedQueueNumber,
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim(),
                    service = service,
                    status = QueueStatus.WAITING,
                    queueDate = today,
                    createdAt = System.currentTimeMillis(),
                    notes = notes.trim(),
                    creatorUid = creatorUid
                )

                queueTicketDao.insertTicket(QueueTicketEntity.fromDomain(newTicket))
                saveMyTicketId(newTicket.id)
                return@withContext newTicket
            } catch (e: Exception) {
                logWarn(TAG, "Firestore transaction joinQueue failed, falling back to local: ${e.message}")
            }
        }

        val maxNumber = queueTicketDao.getMaxQueueNumber(today) ?: 0
        val newQueueNumber = maxNumber + 1
        val newTicket = QueueTicket(
            id = ticketId,
            queueNumber = newQueueNumber,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            service = service,
            status = QueueStatus.WAITING,
            queueDate = today,
            createdAt = System.currentTimeMillis(),
            notes = notes.trim(),
            creatorUid = creatorUid
        )
        queueTicketDao.insertTicket(QueueTicketEntity.fromDomain(newTicket))
        saveMyTicketId(newTicket.id)
        newTicket
    }

    suspend fun cancelTicket(ticketId: String) = withContext(Dispatchers.IO) {
        salonDocRef?.collection(COLLECTION_TICKETS)?.document(ticketId)?.update(
            "statusName", QueueStatus.CANCELLED.name
        )
        queueTicketDao.updateStatus(ticketId, QueueStatus.CANCELLED.name)
    }

    suspend fun clearMySavedTicket() {
        saveMyTicketId(null)
    }

    suspend fun startCustomer(ticketId: String) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val fs = firestore
        val remoteDoc = salonDocRef

        if (fs != null && remoteDoc != null) {
            try {
                val ticketsQuery = remoteDoc.collection(COLLECTION_TICKETS)
                    .whereEqualTo("queueDate", today)
                    .whereEqualTo("statusName", QueueStatus.SERVING.name)
                    .get()
                    .awaitTask()

                val batch = fs.batch()
                val now = System.currentTimeMillis()

                for (doc in ticketsQuery.documents) {
                    if (doc.id != ticketId) {
                        batch.update(doc.reference, mapOf(
                            "statusName" to QueueStatus.COMPLETED.name,
                            "completedAt" to now
                        ))
                    }
                }

                val targetDoc = remoteDoc.collection(COLLECTION_TICKETS).document(ticketId)
                batch.update(targetDoc, mapOf(
                    "statusName" to QueueStatus.SERVING.name,
                    "startedAt" to now
                ))

                batch.commit().awaitTask()
            } catch (e: Exception) {
                logWarn(TAG, "startCustomer remote write fallback: ${e.message}")
            }
        }

        val currentServing = queueTicketDao.getServingTicketDirect(today)
        if (currentServing != null && currentServing.id != ticketId) {
            queueTicketDao.completeService(
                currentServing.id,
                QueueStatus.COMPLETED.name,
                System.currentTimeMillis()
            )
        }
        queueTicketDao.startService(
            ticketId,
            QueueStatus.SERVING.name,
            System.currentTimeMillis()
        )
    }

    suspend fun completeService(ticketId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        salonDocRef?.collection(COLLECTION_TICKETS)?.document(ticketId)?.update(
            mapOf(
                "statusName" to QueueStatus.COMPLETED.name,
                "completedAt" to now
            )
        )
        queueTicketDao.completeService(
            ticketId,
            QueueStatus.COMPLETED.name,
            now
        )
    }

    suspend fun skipCustomer(ticketId: String) = withContext(Dispatchers.IO) {
        salonDocRef?.collection(COLLECTION_TICKETS)?.document(ticketId)?.update(
            "statusName", QueueStatus.SKIPPED.name
        )
        queueTicketDao.updateStatus(ticketId, QueueStatus.SKIPPED.name)
    }

    suspend fun rejoinCustomer(ticketId: String) = withContext(Dispatchers.IO) {
        salonDocRef?.collection(COLLECTION_TICKETS)?.document(ticketId)?.update(
            mapOf(
                "statusName" to QueueStatus.WAITING.name,
                "isRejoinedPriority" to true
            )
        )
        queueTicketDao.rejoinSkippedTicket(ticketId)
    }

    suspend fun setShopOpen(isOpen: Boolean) = withContext(Dispatchers.IO) {
        salonDocRef?.update("isOpen", isOpen)
        shopConfigDao.updateShopStatus(isOpen)
    }

    suspend fun updateAnnouncement(text: String) = withContext(Dispatchers.IO) {
        salonDocRef?.update("announcement", text)
        shopConfigDao.updateAnnouncement(text)
    }

    suspend fun clearTodayQueue() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val fs = firestore
        val remoteDoc = salonDocRef

        if (fs != null && remoteDoc != null) {
            try {
                val query = remoteDoc.collection(COLLECTION_TICKETS)
                    .whereEqualTo("queueDate", today)
                    .get()
                    .awaitTask()

                val batch = fs.batch()
                for (doc in query.documents) {
                    batch.delete(doc.reference)
                }
                batch.update(remoteDoc, "currentMaxQueueNumber", 0L)
                batch.commit().awaitTask()
            } catch (e: Exception) {
                logWarn(TAG, "clearTodayQueue remote delete fallback: ${e.message}")
            }
        }

        queueTicketDao.clearTodayQueue(today)
        saveMyTicketId(null)
    }

    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        val sampleTickets = listOf(
            QueueTicket(
                id = "sample_1",
                queueNumber = 1,
                customerName = "Yunus Ansari",
                customerPhone = "9876543210",
                service = SalonService.HAIRCUT,
                status = QueueStatus.SERVING,
                queueDate = today,
                createdAt = now - (12 * 60 * 1000L),
                startedAt = now - (8 * 60 * 1000L)
            ),
            QueueTicket(
                id = "sample_2",
                queueNumber = 2,
                customerName = "Rahul Verma",
                customerPhone = "9876501234",
                service = SalonService.HAIR_BEARD,
                status = QueueStatus.WAITING,
                queueDate = today,
                createdAt = now - (10 * 60 * 1000L)
            ),
            QueueTicket(
                id = "sample_3",
                queueNumber = 3,
                customerName = "Aman Kumar",
                customerPhone = "9876512345",
                service = SalonService.HAIRCUT,
                status = QueueStatus.WAITING,
                queueDate = today,
                createdAt = now - (6 * 60 * 1000L)
            ),
            QueueTicket(
                id = "sample_4",
                queueNumber = 4,
                customerName = "Arjun Singh",
                customerPhone = "9876523456",
                service = SalonService.HAIR_BEARD_FACIAL,
                status = QueueStatus.WAITING,
                queueDate = today,
                createdAt = now - (3 * 60 * 1000L)
            ),
            QueueTicket(
                id = "sample_5",
                queueNumber = 5,
                customerName = "Vikram Sharma",
                customerPhone = "9876534567",
                service = SalonService.HAIRCUT,
                status = QueueStatus.SKIPPED,
                queueDate = today,
                createdAt = now - (15 * 60 * 1000L)
            )
        )

        val fs = firestore
        val remoteDoc = salonDocRef
        if (fs != null && remoteDoc != null) {
            try {
                val batch = fs.batch()
                for (ticket in sampleTickets) {
                    val docRef = remoteDoc.collection(COLLECTION_TICKETS).document(ticket.id)
                    val map = hashMapOf(
                        "id" to ticket.id,
                        "queueNumber" to ticket.queueNumber.toLong(),
                        "customerName" to ticket.customerName,
                        "customerPhone" to ticket.customerPhone,
                        "serviceName" to ticket.service.name,
                        "statusName" to ticket.status.name,
                        "queueDate" to ticket.queueDate,
                        "createdAt" to ticket.createdAt,
                        "startedAt" to ticket.startedAt,
                        "completedAt" to ticket.completedAt,
                        "isRejoinedPriority" to ticket.isRejoinedPriority,
                        "notes" to ticket.notes
                    )
                    batch.set(docRef, map, SetOptions.merge())
                }
                batch.update(remoteDoc, "currentMaxQueueNumber", 5L)
                batch.commit().awaitTask()
            } catch (e: Exception) {
                logWarn(TAG, "seedSampleData remote batch fallback: ${e.message}")
            }
        }

        queueTicketDao.insertAll(sampleTickets.map { QueueTicketEntity.fromDomain(it) })
    }

    fun calculateDailySummary(tickets: List<QueueTicket>): DailySummary {
        val total = tickets.size
        val waiting = tickets.count { it.status == QueueStatus.WAITING }
        val serving = tickets.count { it.status == QueueStatus.SERVING }
        val completed = tickets.count { it.status == QueueStatus.COMPLETED }
        val skipped = tickets.count { it.status == QueueStatus.SKIPPED }
        val cancelled = tickets.count { it.status == QueueStatus.CANCELLED }
        return DailySummary(
            totalCustomers = total,
            waitingCount = waiting,
            servingCount = serving,
            completedCount = completed,
            skippedCount = skipped,
            cancelledCount = cancelled
        )
    }

    private fun snapshotToShopConfig(doc: DocumentSnapshot): ShopConfig {
        return ShopConfig(
            id = 1,
            isOpen = doc.getBoolean("isOpen") ?: true,
            shopName = doc.getString("shopName") ?: "Student Salon",
            location = doc.getString("location") ?: "Telo, Chandrapura, Bokaro, Jharkhand",
            openingHours = doc.getString("openingHours") ?: "08:00 AM - 09:00 PM",
            contactPhone = doc.getString("contactPhone") ?: "+91 91234 56789",
            announcement = doc.getString("announcement") ?: "Welcome to Student Salon! Digital queue is active.",
            ownerPin = doc.getString("ownerPin") ?: "1234",
            ownerUid = doc.getString("ownerUid") ?: ""
        )
    }

    private fun docToQueueTicket(doc: DocumentSnapshot): QueueTicket? {
        val id = doc.getString("id") ?: doc.id
        val queueNumber = doc.getLong("queueNumber")?.toInt() ?: return null
        val customerName = doc.getString("customerName") ?: return null
        val customerPhone = doc.getString("customerPhone") ?: ""
        val serviceName = doc.getString("serviceName") ?: SalonService.HAIRCUT.name
        val statusName = doc.getString("statusName") ?: QueueStatus.WAITING.name
        val queueDate = doc.getString("queueDate") ?: getTodayDateString()
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        val startedAt = doc.getLong("startedAt")
        val completedAt = doc.getLong("completedAt")
        val isRejoinedPriority = doc.getBoolean("isRejoinedPriority") ?: false
        val notes = doc.getString("notes") ?: ""
        val creatorUid = doc.getString("creatorUid") ?: ""

        return QueueTicket(
            id = id,
            queueNumber = queueNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            service = SalonService.fromString(serviceName),
            status = QueueStatus.fromString(statusName),
            queueDate = queueDate,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            isRejoinedPriority = isRejoinedPriority,
            notes = notes,
            creatorUid = creatorUid
        )
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result ->
                if (cont.isActive) cont.resumeWith(Result.success(result))
            }
            addOnFailureListener { exception ->
                if (cont.isActive) cont.resumeWith(Result.failure(exception))
            }
            addOnCanceledListener {
                if (cont.isActive) cont.cancel()
            }
        }
}
