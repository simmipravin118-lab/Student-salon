package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.data.model.SalonService

@Entity(tableName = "queue_tickets")
data class QueueTicketEntity(
    @PrimaryKey val id: String,
    val queueNumber: Int,
    val customerName: String,
    val customerPhone: String,
    val serviceName: String,
    val statusName: String,
    val queueDate: String,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val isRejoinedPriority: Boolean = false,
    val notes: String = "",
    val creatorUid: String = ""
) {
    fun toDomain(): QueueTicket {
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

    companion object {
        fun fromDomain(ticket: QueueTicket): QueueTicketEntity {
            return QueueTicketEntity(
                id = ticket.id,
                queueNumber = ticket.queueNumber,
                customerName = ticket.customerName,
                customerPhone = ticket.customerPhone,
                serviceName = ticket.service.name,
                statusName = ticket.status.name,
                queueDate = ticket.queueDate,
                createdAt = ticket.createdAt,
                startedAt = ticket.startedAt,
                completedAt = ticket.completedAt,
                isRejoinedPriority = ticket.isRejoinedPriority,
                notes = ticket.notes,
                creatorUid = ticket.creatorUid
            )
        }
    }
}
