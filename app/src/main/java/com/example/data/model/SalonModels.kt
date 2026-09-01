package com.example.data.model

enum class SalonService(
    val title: String,
    val durationMinutes: Int,
    val description: String,
    val iconEmoji: String
) {
    HAIRCUT(
        title = "Haircut",
        durationMinutes = 20,
        description = "Standard & modern haircut with styling",
        iconEmoji = "💇"
    ),
    HAIR_BEARD(
        title = "Hair + Beard",
        durationMinutes = 35,
        description = "Haircut & beard trimming, shaping & line-up",
        iconEmoji = "💇‍♂️🧔"
    ),
    HAIR_BEARD_FACIAL(
        title = "Hair + Beard + Facial",
        durationMinutes = 45,
        description = "Full grooming package with refreshing face cleanse & massage",
        iconEmoji = "✨"
    );

    companion object {
        fun fromString(name: String?): SalonService {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) }
                ?: HAIRCUT
        }
    }
}

enum class QueueStatus(
    val displayName: String,
    val emoji: String
) {
    WAITING("Waiting", "🟡"),
    SERVING("Serving", "🟢"),
    COMPLETED("Completed", "✅"),
    SKIPPED("Skipped", "⏭️"),
    CANCELLED("Cancelled", "❌"),
    REJOINED("Rejoined", "↩️");

    companion object {
        fun fromString(name: String?): QueueStatus {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WAITING
        }
    }
}

data class QueueTicket(
    val id: String,
    val queueNumber: Int,
    val customerName: String,
    val customerPhone: String = "",
    val service: SalonService,
    val status: QueueStatus,
    val queueDate: String,
    val createdAt: Long,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val isRejoinedPriority: Boolean = false,
    val notes: String = "",
    val creatorUid: String = ""
)

data class ShopConfig(
    val id: Int = 1,
    val isOpen: Boolean = true,
    val shopName: String = "Student Salon",
    val location: String = "Telo, Chandrapura, Bokaro, Jharkhand",
    val openingHours: String = "08:00 AM - 09:00 PM",
    val contactPhone: String = "+91 91234 56789",
    val announcement: String = "Welcome to Student Salon! Digital queue is active.",
    val ownerPin: String = "1234",
    val ownerUid: String = ""
)

data class CustomerTicketInfo(
    val ticket: QueueTicket,
    val customersAhead: Int,
    val estimatedWaitingMinutes: Int,
    val estimatedTurnTimeFormatted: String,
    val remainingServingMinutes: Int,
    val estimatedWaitingSeconds: Int = 0,
    val estimatedWaitingMillis: Long = 0L,
    val estimatedWaitingFormatted: String = "",
    val remainingServingSeconds: Int = 0,
    val remainingServingMillis: Long = 0L,
    val remainingServingFormatted: String = ""
)

data class DailySummary(
    val totalCustomers: Int = 0,
    val waitingCount: Int = 0,
    val servingCount: Int = 0,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val cancelledCount: Int = 0
)

data class SyncStatus(
    val isCloudConnected: Boolean = true,
    val isUsingLocalCache: Boolean = false,
    val statusMessage: String = "Live Cloud Synced"
)
