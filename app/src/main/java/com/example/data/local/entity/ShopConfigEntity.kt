package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ShopConfig

@Entity(tableName = "shop_config")
data class ShopConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isOpen: Boolean = true,
    val shopName: String = "Student Salon",
    val location: String = "Telo, Chandrapura, Bokaro, Jharkhand",
    val openingHours: String = "08:00 AM - 09:00 PM",
    val contactPhone: String = "+91 91234 56789",
    val announcement: String = "Welcome to Student Salon! Digital queue is live.",
    val ownerPin: String = "1234",
    val ownerUid: String = ""
) {
    fun toDomain(): ShopConfig {
        return ShopConfig(
            id = id,
            isOpen = isOpen,
            shopName = shopName,
            location = location,
            openingHours = openingHours,
            contactPhone = contactPhone,
            announcement = announcement,
            ownerPin = ownerPin,
            ownerUid = ownerUid
        )
    }

    companion object {
        fun fromDomain(config: ShopConfig): ShopConfigEntity {
            return ShopConfigEntity(
                id = config.id,
                isOpen = config.isOpen,
                shopName = config.shopName,
                location = config.location,
                openingHours = config.openingHours,
                contactPhone = config.contactPhone,
                announcement = config.announcement,
                ownerPin = config.ownerPin,
                ownerUid = config.ownerUid
            )
        }
    }
}
