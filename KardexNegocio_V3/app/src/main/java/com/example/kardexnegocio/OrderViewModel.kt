package com.example.kardexnegocio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("kardex_prefs", Context.MODE_PRIVATE)

    // 1. Menú inicial con datos de prueba (Placeholders genéricos)
    private val defaultProducts = listOf(
        Product(id = "p1", name = "Producto A", price = 50.0, stock = 10, category = "General"),
        Product(id = "p2", name = "Producto B", price = 85.5, stock = 5, category = "General"),
        Product(id = "p3", name = "Producto C", price = 30.0, stock = 20, category = "Bebidas"),
        Product(id = "p4", name = "Producto D", price = 120.0, stock = 8, category = "Especial")
    )

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList.asStateFlow()

    // 2. Lista global de órdenes activas (Sincronización instantánea Admin <-> Cocina)
    private val _orderList = MutableStateFlow<List<Order>>(emptyList())
    val orderList: StateFlow<List<Order>> = _orderList.asStateFlow()

    private var nextOrderNumber = 1

    // 3. Carrito temporal para armado de pedido en Admin
    private val _cartItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val cartItems: StateFlow<List<OrderItem>> = _cartItems.asStateFlow()

    init {
        loadDataFromPrefs()
    }

    // --- MANEJO DE CARRITO CON NOTAS ---
    fun addToCart(product: Product, notes: String = "") {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id && it.notes == notes }
        val productInStock = _productList.value.find { it.id == product.id } ?: return

        val currentQtyForProduct = currentCart.filter { it.product.id == product.id }.sumOf { it.quantity }
        if (currentQtyForProduct + 1 <= productInStock.stock) {
            if (index != -1) {
                currentCart[index] = currentCart[index].copy(quantity = currentCart[index].quantity + 1)
            } else {
                currentCart.add(OrderItem(product = product, quantity = 1, notes = notes))
            }
            _cartItems.value = currentCart
        }
    }

    fun updateCartItemNotes(indexInCart: Int, newNotes: String) {
        val currentCart = _cartItems.value.toMutableList()
        if (indexInCart in currentCart.indices) {
            currentCart[indexInCart] = currentCart[indexInCart].copy(notes = newNotes)
            _cartItems.value = currentCart
        }
    }

    fun removeFromCart(product: Product) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfLast { it.product.id == product.id }
        if (index != -1) {
            val currentQty = currentCart[index].quantity
            if (currentQty > 1) {
                currentCart[index] = currentCart[index].copy(quantity = currentQty - 1)
            } else {
                currentCart.removeAt(index)
            }
            _cartItems.value = currentCart
        }
    }

    fun removeCartItemAt(index: Int) {
        val currentCart = _cartItems.value.toMutableList()
        if (index in currentCart.indices) {
            currentCart.removeAt(index)
            _cartItems.value = currentCart
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // --- CREACIÓN Y EDICIÓN DE ÓRDENES ---
    fun createOrder(itemsToOrder: List<OrderItem>, orderType: OrderType, paymentMethod: PaymentMethod): Boolean {
        if (itemsToOrder.isEmpty()) return false

        val currentProducts = _productList.value.toMutableList()

        // Validar stock disponible por producto
        val totalRequestedByProduct = itemsToOrder.groupBy { it.product.id }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }

        for ((productId, totalQty) in totalRequestedByProduct) {
            val productInStock = currentProducts.find { it.id == productId }
            if (productInStock == null || productInStock.stock < totalQty) {
                return false
            }
        }

        // Descontar stock en tiempo real
        val calculatedTotal = itemsToOrder.sumOf { it.product.price * it.quantity }

        for ((productId, totalQty) in totalRequestedByProduct) {
            val index = currentProducts.indexOfFirst { it.id == productId }
            if (index != -1) {
                currentProducts[index] = currentProducts[index].copy(
                    stock = currentProducts[index].stock - totalQty
                )
            }
        }
        _productList.value = currentProducts

        // Crear ticket para la cocina
        val newOrder = Order(
            orderNumber = nextOrderNumber++,
            items = itemsToOrder,
            orderType = orderType,
            status = OrderStatus.PENDING,
            paymentMethod = paymentMethod,
            totalAmount = calculatedTotal
        )

        _orderList.value = _orderList.value + newOrder
        clearCart()
        saveDataToPrefs()
        return true
    }

    fun updateOrderStatus(orderNumber: Int, newStatus: OrderStatus) {
        _orderList.value = _orderList.value.map { order ->
            if (order.orderNumber == orderNumber) {
                order.copy(status = newStatus)
            } else {
                order
            }
        }
        saveDataToPrefs()
    }

    fun updateOrderDetails(orderNumber: Int, newOrderType: OrderType, newPaymentMethod: PaymentMethod) {
        _orderList.value = _orderList.value.map { order ->
            if (order.orderNumber == orderNumber) {
                order.copy(orderType = newOrderType, paymentMethod = newPaymentMethod)
            } else {
                order
            }
        }
        saveDataToPrefs()
    }

    fun cancelOrder(orderNumber: Int) {
        val orderToCancel = _orderList.value.find { it.orderNumber == orderNumber } ?: return

        // Devolver stock al kárdex
        val currentProducts = _productList.value.toMutableList()
        for (orderItem in orderToCancel.items) {
            val index = currentProducts.indexOfFirst { it.id == orderItem.product.id }
            if (index != -1) {
                currentProducts[index] = currentProducts[index].copy(
                    stock = currentProducts[index].stock + orderItem.quantity
                )
            }
        }
        _productList.value = currentProducts
        _orderList.value = _orderList.value.filter { it.orderNumber != orderNumber }
        saveDataToPrefs()
    }

    // --- CORTE DE CAJA / NUEVO TURNO ---
    fun clearCompletedOrders() {
        _orderList.value = _orderList.value.filter { it.status != OrderStatus.COMPLETED }
        saveDataToPrefs()
    }

    // --- PERSISTENCIA LOCAL (SharedPreferences + JSON) ---
    private fun saveDataToPrefs() {
        val editor = sharedPreferences.edit()

        // Guardar productos
        val productsArray = JSONArray()
        _productList.value.forEach { product ->
            val json = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("stock", product.stock)
                put("category", product.category)
            }
            productsArray.put(json)
        }
        editor.putString("product_list_json", productsArray.toString())

        // Guardar órdenes
        val ordersArray = JSONArray()
        _orderList.value.forEach { order ->
            val orderJson = JSONObject().apply {
                put("orderNumber", order.orderNumber)
                put("orderType", order.orderType.name)
                put("status", order.status.name)
                put("paymentMethod", order.paymentMethod.name)
                put("totalAmount", order.totalAmount)
                put("timestamp", order.timestamp)

                val itemsArray = JSONArray()
                order.items.forEach { item ->
                    val itemJson = JSONObject().apply {
                        put("product", JSONObject().apply {
                            put("id", item.product.id)
                            put("name", item.product.name)
                            put("price", item.product.price)
                            put("stock", item.product.stock)
                            put("category", item.product.category)
                        })
                        put("quantity", item.quantity)
                        put("notes", item.notes)
                    }
                    itemsArray.put(itemJson)
                }
                put("items", itemsArray)
            }
            ordersArray.put(orderJson)
        }
        editor.putString("order_list_json", ordersArray.toString())
        editor.putInt("next_order_number", nextOrderNumber)
        editor.apply()
    }

    private fun loadDataFromPrefs() {
        try {
            // Cargar productos
            val productsStr = sharedPreferences.getString("product_list_json", null)
            if (!productsStr.isNullOrEmpty()) {
                val array = JSONArray(productsStr)
                val loadedProducts = mutableListOf<Product>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedProducts.add(
                        Product(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            stock = obj.getInt("stock"),
                            category = obj.getString("category")
                        )
                    )
                }
                _productList.value = loadedProducts
            } else {
                _productList.value = defaultProducts
            }

            // Cargar órdenes
            val ordersStr = sharedPreferences.getString("order_list_json", null)
            if (!ordersStr.isNullOrEmpty()) {
                val array = JSONArray(ordersStr)
                val loadedOrders = mutableListOf<Order>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsArray = obj.getJSONArray("items")
                    val itemsList = mutableListOf<OrderItem>()
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        val prodObj = itemObj.getJSONObject("product")
                        val product = Product(
                            id = prodObj.getString("id"),
                            name = prodObj.getString("name"),
                            price = prodObj.getDouble("price"),
                            stock = prodObj.getInt("stock"),
                            category = prodObj.getString("category")
                        )
                        itemsList.add(
                            OrderItem(
                                product = product,
                                quantity = itemObj.getInt("quantity"),
                                notes = itemObj.optString("notes", "")
                            )
                        )
                    }
                    loadedOrders.add(
                        Order(
                            orderNumber = obj.getInt("orderNumber"),
                            items = itemsList,
                            orderType = OrderType.valueOf(obj.getString("orderType")),
                            status = OrderStatus.valueOf(obj.getString("status")),
                            paymentMethod = PaymentMethod.valueOf(obj.getString("paymentMethod")),
                            totalAmount = obj.getDouble("totalAmount"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                _orderList.value = loadedOrders
            }

            // Cargar consecutivo
            val maxExistingNumber = _orderList.value.maxOfOrNull { it.orderNumber } ?: 0
            nextOrderNumber = sharedPreferences.getInt("next_order_number", maxExistingNumber + 1)
            if (nextOrderNumber <= maxExistingNumber) {
                nextOrderNumber = maxExistingNumber + 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _productList.value = defaultProducts
        }
    }
}