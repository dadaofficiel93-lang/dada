package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CartItem
import com.example.data.ProductEntity
import com.example.data.SaleWithItems
import com.example.data.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab {
    VENTE,     // Caisse / Nouvelle Vente
    PRODUITS,  // Gestion du catalogue & stock
    HISTORIQUE,// Ventes réalisées & statistiques
    VITRINE    // Aperçu catalogue partageable
}

enum class DateFilter(val label: String) {
    TODAY("Aujourd'hui"),
    WEEK("7 derniers jours"),
    MONTH("Ce mois"),
    ALL("Tout l'historique")
}

data class SalesAnalytics(
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalOrders: Int = 0,
    val totalItemsSold: Int = 0,
    val averageTicket: Double = 0.0,
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0,
    val mobileRevenue: Double = 0.0
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SalesRepository(db.productDao(), db.saleDao())
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }

    // Tab navigation
    private val _currentTab = MutableStateFlow(AppTab.VENTE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || product.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = allProducts.combine(_selectedCategory) { products, _ ->
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart / Point of Sale
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Double> = _cart.combine(allProducts) { cartItems, _ ->
        cartItems.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cart.combine(allProducts) { cartItems, _ ->
        cartItems.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) return
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            if (existing.quantity < product.stock) {
                current[existingIndex] = existing.copy(quantity = existing.quantity + 1)
            }
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val maxStock = item.product.stock
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else if (newQty <= maxStock) {
                current[index] = item.copy(quantity = newQty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Sales & History
    val allSales: StateFlow<List<SaleWithItems>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    val filteredSales: StateFlow<List<SaleWithItems>> = combine(allSales, _dateFilter) { sales, filter ->
        val now = Calendar.getInstance()
        sales.filter { saleWithItems ->
            val saleCal = Calendar.getInstance().apply {
                timeInMillis = saleWithItems.sale.timestamp
            }
            when (filter) {
                DateFilter.TODAY -> {
                    saleCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            saleCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                DateFilter.WEEK -> {
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }
                    saleCal.after(sevenDaysAgo)
                }
                DateFilter.MONTH -> {
                    saleCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            saleCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                DateFilter.ALL -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics
    val analytics: StateFlow<SalesAnalytics> = filteredSales.combine(allProducts) { sales, _ ->
        val totalRevenue = sales.sumOf { it.sale.totalAmount }
        val totalProfit = sales.sumOf { it.sale.totalProfit }
        val totalOrders = sales.size
        val totalItemsSold = sales.sumOf { it.sale.itemCount }
        val avgTicket = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

        val cash = sales.filter { it.sale.paymentMethod.equals("Espèces", ignoreCase = true) }
            .sumOf { it.sale.totalAmount }
        val card = sales.filter { it.sale.paymentMethod.equals("Carte", ignoreCase = true) }
            .sumOf { it.sale.totalAmount }
        val mobile = sales.filter {
            it.sale.paymentMethod.contains("Mobile", ignoreCase = true) ||
                    it.sale.paymentMethod.contains("Virement", ignoreCase = true)
        }.sumOf { it.sale.totalAmount }

        SalesAnalytics(
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            totalOrders = totalOrders,
            totalItemsSold = totalItemsSold,
            averageTicket = avgTicket,
            cashRevenue = cash,
            cardRevenue = card,
            mobileRevenue = mobile
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesAnalytics())

    // Last completed sale dialog
    private val _lastSaleReceipt = MutableStateFlow<SaleWithItems?>(null)
    val lastSaleReceipt: StateFlow<SaleWithItems?> = _lastSaleReceipt.asStateFlow()

    fun dismissReceipt() {
        _lastSaleReceipt.value = null
    }

    fun completeSale(
        paymentMethod: String,
        customerName: String?,
        notes: String?,
        onSuccess: () -> Unit = {}
    ) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val saleId = repository.completeSale(cartItems, paymentMethod, customerName, notes)
            if (saleId > 0) {
                // Prepare receipt preview
                val receipt = SaleWithItems(
                    sale = com.example.data.SaleEntity(
                        id = saleId,
                        timestamp = System.currentTimeMillis(),
                        totalAmount = cartItems.sumOf { it.subtotal },
                        totalCost = cartItems.sumOf { it.subtotalCost },
                        totalProfit = cartItems.sumOf { it.subtotal } - cartItems.sumOf { it.subtotalCost },
                        itemCount = cartItems.sumOf { it.quantity },
                        paymentMethod = paymentMethod,
                        customerName = customerName?.takeIf { it.isNotBlank() },
                        notes = notes?.takeIf { it.isNotBlank() }
                    ),
                    items = cartItems.map {
                        com.example.data.SaleItemEntity(
                            saleId = saleId,
                            productId = it.product.id,
                            productName = it.product.name,
                            unitPrice = it.product.price,
                            costPrice = it.product.costPrice,
                            quantity = it.quantity
                        )
                    }
                )
                _lastSaleReceipt.value = receipt
                _cart.value = emptyList()
                onSuccess()
            }
        }
    }

    // Product CRUD operations
    fun saveProduct(
        id: Long = 0,
        name: String,
        description: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        category: String,
        colorHex: Long,
        iconKey: String
    ) {
        viewModelScope.launch {
            val entity = ProductEntity(
                id = id,
                name = name.trim(),
                description = description.trim(),
                price = price,
                costPrice = costPrice,
                stock = stock,
                category = category.trim().ifBlank { "Général" },
                colorHex = colorHex,
                iconKey = iconKey
            )
            if (id == 0L) {
                repository.addProduct(entity)
            } else {
                repository.updateProduct(entity)
            }
        }
    }

    fun updateStock(productId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(productId, newStock.coerceAtLeast(0))
        }
    }

    fun quickAdjustStock(productId: Long, delta: Int) {
        val current = allProducts.value.find { it.id == productId } ?: return
        val updated = (current.stock + delta).coerceAtLeast(0)
        updateStock(productId, updated)
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            // also remove from cart if present
            _cart.value = _cart.value.filter { it.product.id != product.id }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }
}

class SalesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            return SalesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
