package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val price: Double,
    val costPrice: Double = 0.0,
    val stock: Int = 0,
    val category: String = "Général",
    val barcode: String = "",
    val colorHex: Long = 0xFF4338CA,
    val iconKey: String = "tag",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val itemCount: Int,
    val paymentMethod: String = "Espèces", // Espèces, Carte, Mobile Money, Virement
    val customerName: String? = null,
    val notes: String? = null
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val costPrice: Double,
    val quantity: Int
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)
