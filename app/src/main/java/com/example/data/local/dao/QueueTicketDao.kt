package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.QueueTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueTicketDao {

    @Query("SELECT * FROM queue_tickets WHERE queueDate = :todayDate ORDER BY isRejoinedPriority DESC, queueNumber ASC")
    fun getTodayTickets(todayDate: String): Flow<List<QueueTicketEntity>>

    @Query("SELECT * FROM queue_tickets WHERE id = :ticketId LIMIT 1")
    fun getTicketById(ticketId: String): Flow<QueueTicketEntity?>

    @Query("SELECT * FROM queue_tickets WHERE id = :ticketId LIMIT 1")
    suspend fun getTicketDirect(ticketId: String): QueueTicketEntity?

    @Query("SELECT MAX(queueNumber) FROM queue_tickets WHERE queueDate = :todayDate")
    suspend fun getMaxQueueNumber(todayDate: String): Int?

    @Query("SELECT * FROM queue_tickets WHERE queueDate = :todayDate AND statusName = 'SERVING' LIMIT 1")
    fun getServingTicket(todayDate: String): Flow<QueueTicketEntity?>

    @Query("SELECT * FROM queue_tickets WHERE queueDate = :todayDate AND statusName = 'SERVING' LIMIT 1")
    suspend fun getServingTicketDirect(todayDate: String): QueueTicketEntity?

    @Query("SELECT * FROM queue_tickets WHERE queueDate = :todayDate AND statusName = 'WAITING' ORDER BY isRejoinedPriority DESC, queueNumber ASC")
    fun getWaitingTickets(todayDate: String): Flow<List<QueueTicketEntity>>

    @Query("SELECT * FROM queue_tickets WHERE queueDate = :todayDate AND statusName = 'SKIPPED' ORDER BY queueNumber ASC")
    fun getSkippedTickets(todayDate: String): Flow<List<QueueTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: QueueTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<QueueTicketEntity>)

    @Update
    suspend fun updateTicket(ticket: QueueTicketEntity)

    @Query("UPDATE queue_tickets SET statusName = :newStatus WHERE id = :ticketId")
    suspend fun updateStatus(ticketId: String, newStatus: String)

    @Query("UPDATE queue_tickets SET statusName = :newStatus, startedAt = :startedAt WHERE id = :ticketId")
    suspend fun startService(ticketId: String, newStatus: String, startedAt: Long)

    @Query("UPDATE queue_tickets SET statusName = :newStatus, completedAt = :completedAt WHERE id = :ticketId")
    suspend fun completeService(ticketId: String, newStatus: String, completedAt: Long)

    @Query("UPDATE queue_tickets SET statusName = 'WAITING', isRejoinedPriority = 1 WHERE id = :ticketId")
    suspend fun rejoinSkippedTicket(ticketId: String)

    @Query("DELETE FROM queue_tickets WHERE queueDate = :todayDate")
    suspend fun clearTodayQueue(todayDate: String)

    @Query("DELETE FROM queue_tickets")
    suspend fun clearAll()
}
