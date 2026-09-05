package com.example.data

import kotlinx.coroutines.flow.Flow

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Double get() = product.price * quantity
    val subtotalCost: Double get() = product.costPrice * quantity
}

class SalesRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()

    suspend fun addProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun updateStock(productId: Long, newStock: Int) {
        productDao.updateStock(productId, newStock)
    }

    suspend fun completeSale(
        cartItems: List<CartItem>,
        paymentMethod: String,
        customerName: String?,
        notes: String?
    ): Long {
        if (cartItems.isEmpty()) return -1L

        val totalAmount = cartItems.sumOf { it.subtotal }
        val totalCost = cartItems.sumOf { it.subtotalCost }
        val totalProfit = totalAmount - totalCost
        val totalItemsCount = cartItems.sumOf { it.quantity }

        val saleEntity = SaleEntity(
            totalAmount = totalAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            itemCount = totalItemsCount,
            paymentMethod = paymentMethod,
            customerName = customerName?.takeIf { it.isNotBlank() },
            notes = notes?.takeIf { it.isNotBlank() }
        )

        val saleId = saleDao.insertSale(saleEntity)

        val saleItemEntities = cartItems.map { cartItem ->
            SaleItemEntity(
                saleId = saleId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                unitPrice = cartItem.product.price,
                costPrice = cartItem.product.costPrice,
                quantity = cartItem.quantity
            )
        }
        saleDao.insertSaleItems(saleItemEntities)

        // Decrease stock for each sold product
        cartItems.forEach { item ->
            productDao.decreaseStock(item.product.id, item.quantity)
        }

        return saleId
    }

    suspend fun deleteSale(saleWithItems: SaleWithItems) {
        // Restore stock
        saleWithItems.items.forEach { item ->
            productDao.updateStock(item.productId, item.quantity) // or add back
        }
        saleDao.deleteSaleItemsBySaleId(saleWithItems.sale.id)
        saleDao.deleteSale(saleWithItems.sale)
    }

    suspend fun initializeDefaultDataIfEmpty() {
        if (productDao.countProducts() == 0) {
            val sampleProducts = listOf(
                ProductEntity(
                    name = "T-shirt Coton Bio",
                    description = "100% coton biologique peigné, coupe unisexe confortable.",
                    price = 24.90,
                    costPrice = 11.50,
                    stock = 25,
                    category = "Vêtements",
                    colorHex = 0xFF3B82F6,
                    iconKey = "checkroom"
                ),
                ProductEntity(
                    name = "Casque Audio Bluetooth",
                    description = "Réduction de bruit active, autonomie 30h, son haute fidélité.",
                    price = 79.99,
                    costPrice = 42.00,
                    stock = 12,
                    category = "Électronique",
                    colorHex = 0xFF8B5CF6,
                    iconKey = "devices"
                ),
                ProductEntity(
                    name = "Café Artisanal Moulu 250g",
                    description = "Torréfaction lente aux notes de chocolat noir et noisettes.",
                    price = 8.50,
                    costPrice = 3.80,
                    stock = 40,
                    category = "Alimentation",
                    colorHex = 0xFFD97706,
                    iconKey = "restaurant"
                ),
                ProductEntity(
                    name = "Bougie Végétale Parfumée",
                    description = "Cire de soja naturelle parfum vanille & ambre doux, 45h de brûle.",
                    price = 16.00,
                    costPrice = 6.20,
                    stock = 18,
                    category = "Maison",
                    colorHex = 0xFFEC4899,
                    iconKey = "home"
                ),
                ProductEntity(
                    name = "Gourde Inox Isotherme 750ml",
                    description = "Maintient au frais 24h et au chaud 12h, double paroi étanche.",
                    price = 22.50,
                    costPrice = 9.00,
                    stock = 15,
                    category = "Maison",
                    colorHex = 0xFF10B981,
                    iconKey = "water_drop"
                ),
                ProductEntity(
                    name = "Montre Connectée Sport",
                    description = "Cardiofréquencemètre, GPS intégré et étanche 50m.",
                    price = 99.00,
                    costPrice = 52.00,
                    stock = 7,
                    category = "Électronique",
                    colorHex = 0xFF6366F1,
                    iconKey = "watch"
                )
            )
            productDao.insertProducts(sampleProducts)

            // Also add a couple of historical sales to show stats immediately
            val initialSale1 = SaleEntity(
                timestamp = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
                totalAmount = 57.40,
                totalCost = 26.80,
                totalProfit = 30.60,
                itemCount = 3,
                paymentMethod = "Carte",
                customerName = "Sophie Bernard",
                notes = "Première commande en magasin"
            )
            val saleId1 = saleDao.insertSale(initialSale1)
            saleDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId1,
                        productId = 1,
                        productName = "T-shirt Coton Bio",
                        unitPrice = 24.90,
                        costPrice = 11.50,
                        quantity = 2
                    ),
                    SaleItemEntity(
                        saleId = saleId1,
                        productId = 3,
                        productName = "Café Artisanal Moulu 250g",
                        unitPrice = 7.60,
                        costPrice = 3.80,
                        quantity = 1
                    )
                )
            )

            val initialSale2 = SaleEntity(
                timestamp = System.currentTimeMillis() - 3600000L * 5, // 5 hours ago
                totalAmount = 79.99,
                totalCost = 42.00,
                totalProfit = 37.99,
                itemCount = 1,
                paymentMethod = "Espèces",
                customerName = "Marc Dupont",
                notes = "Paiement en liquide reçu"
            )
            val saleId2 = saleDao.insertSale(initialSale2)
            saleDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId2,
                        productId = 2,
                        productName = "Casque Audio Bluetooth",
                        unitPrice = 79.99,
                        costPrice = 42.00,
                        quantity = 1
                    )
                )
            )
        }
    }
}
