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

    // Catálogo completo de 24 productos del negocio de hamburguesas
    private val defaultProducts = listOf(
        // Hamburguesas (6)
        Product(id = "h1", name = "Clásica", price = 99.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.clasica),
        Product(id = "h2", name = "Doble Tentación", price = 119.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.doble_tentacion),
        Product(id = "h3", name = "BBQ Especial", price = 139.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.bbq_especial),
        Product(id = "h4", name = "Fuego Mexicano", price = 135.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.fuego_mexicano),
        Product(id = "h5", name = "Mamona", price = 149.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.mamona),
        Product(id = "h6", name = "Artic Monki", price = 159.0, stock = 15, category = "Hamburguesas", imageResId = R.drawable.artic_monki),

        // Papas (3)
        Product(id = "p1", name = "Papas Clásicas", price = 45.0, stock = 15, category = "Papas", imageResId = R.drawable.papas_clasicas),
        Product(id = "p2", name = "Papas Cheddar", price = 69.0, stock = 15, category = "Papas", imageResId = R.drawable.papas_cheddar),
        Product(id = "p3", name = "Papas Bacon", price = 79.0, stock = 15, category = "Papas", imageResId = R.drawable.papas_bacon),

        // Malteadas (3)
        Product(id = "m1", name = "Malteada de Fresa", price = 69.0, stock = 15, category = "Malteadas", imageResId = R.drawable.malteada_fresa),
        Product(id = "m2", name = "Malteada de Chocolate", price = 69.0, stock = 15, category = "Malteadas", imageResId = R.drawable.malteada_chocolate),
        Product(id = "m3", name = "Malteada de Vainilla", price = 69.0, stock = 15, category = "Malteadas", imageResId = R.drawable.maldeada_vainilla),

        // Bebidas (6)
        Product(id = "b1", name = "Coca-Cola Original 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.coca_cola_original_600_ml),
        Product(id = "b2", name = "Coca-Cola Sin Azúcar 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.coca_cola_sin_azucar_600_ml),
        Product(id = "b3", name = "Pepsi 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.pepsi_600_ml),
        Product(id = "b4", name = "Sprite 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.sprite_600_ml),
        Product(id = "b5", name = "Fanta Naranja 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.fanta_naranja_600_ml),
        Product(id = "b6", name = "Sidral Mundet 600 ml", price = 35.0, stock = 15, category = "Bebidas", imageResId = R.drawable.sidral_mundet_600_ml),

        // Combos (6)
        Product(id = "c1", name = "Combo Solitario", price = 199.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_solitario),
        Product(id = "c2", name = "Combo Duo", price = 359.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_duo),
        Product(id = "c3", name = "Combo x3", price = 599.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_x3),
        Product(id = "c4", name = "Combo Cuadrilla", price = 819.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_cuadrilla),
        Product(id = "c5", name = "Combo Amigos", price = 999.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_amigos),
        Product(id = "c6", name = "Combo Familiar", price = 1299.0, stock = 15, category = "Combos", imageResId = R.drawable.combo_familiar)
    )

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList.asStateFlow()

    // Lista global de órdenes activas (Sincronización instantánea Admin <-> Cocina)
    private val _orderList = MutableStateFlow<List<Order>>(emptyList())
    val orderList: StateFlow<List<Order>> = _orderList.asStateFlow()

    private var nextOrderNumber = 1

    // Carrito temporal para armado de pedido en Admin
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

    // --- AJUSTE MANUAL DE STOCK (KÁRDEX MANUAL) ---
    fun updateProductStock(productId: String, delta: Int) {
        val currentProducts = _productList.value.toMutableList()
        val index = currentProducts.indexOfFirst { it.id == productId }
        if (index != -1) {
            val currentStock = currentProducts[index].stock
            val newStock = (currentStock + delta).coerceAtLeast(0)
            if (newStock != currentStock) {
                currentProducts[index] = currentProducts[index].copy(stock = newStock)
                _productList.value = currentProducts

                // Ajustar el carrito si la cantidad total agregada supera el nuevo stock
                val currentCart = _cartItems.value.toMutableList()
                val totalInCart = currentCart.filter { it.product.id == productId }.sumOf { it.quantity }
                if (totalInCart > newStock) {
                    var excess = totalInCart - newStock
                    for (i in currentCart.indices.reversed()) {
                        if (currentCart[i].product.id == productId) {
                            if (currentCart[i].quantity <= excess) {
                                excess -= currentCart[i].quantity
                                currentCart.removeAt(i)
                            } else {
                                currentCart[i] = currentCart[i].copy(quantity = currentCart[i].quantity - excess)
                                excess = 0
                            }
                            if (excess == 0) break
                        }
                    }
                    _cartItems.value = currentCart
                }

                saveDataToPrefs()
            }
        }
    }

    fun increaseProductStock(productId: String) {
        updateProductStock(productId, 1)
    }

    fun decreaseProductStock(productId: String) {
        updateProductStock(productId, -1)
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
                product.imageResId?.let { put("imageResId", it) }
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
                            item.product.imageResId?.let { put("imageResId", it) }
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
                    val prodId = obj.getString("id")
                    val prodName = obj.getString("name")
                    val defaultProd = defaultProducts.find { it.id == prodId || it.name.equals(prodName, ignoreCase = true) }
                    val imageResId = if (obj.has("imageResId") && obj.getInt("imageResId") != 0) {
                        obj.getInt("imageResId")
                    } else {
                        defaultProd?.imageResId
                    }
                    loadedProducts.add(
                        Product(
                            id = prodId,
                            name = defaultProd?.name ?: prodName,
                            price = defaultProd?.price ?: obj.getDouble("price"),
                            stock = obj.getInt("stock"),
                            category = defaultProd?.category ?: obj.getString("category"),
                            imageResId = imageResId
                        )
                    )
                }

                val hasOldGenericProducts = loadedProducts.any { it.category == "General" } || loadedProducts.size < 10
                if (hasOldGenericProducts) {
                    _productList.value = defaultProducts
                } else {
                    val mergedList = defaultProducts.map { def ->
                        loadedProducts.find { it.id == def.id || it.name.equals(def.name, ignoreCase = true) }?.copy(imageResId = def.imageResId) ?: def
                    }
                    _productList.value = mergedList
                }
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
                        val prodId = prodObj.getString("id")
                        val prodName = prodObj.getString("name")
                        val defaultProd = defaultProducts.find { it.id == prodId || it.name.equals(prodName, ignoreCase = true) }
                        val imageResId = if (prodObj.has("imageResId") && prodObj.getInt("imageResId") != 0) {
                            prodObj.getInt("imageResId")
                        } else {
                            defaultProd?.imageResId
                        }
                        val product = Product(
                            id = prodId,
                            name = defaultProd?.name ?: prodName,
                            price = defaultProd?.price ?: prodObj.getDouble("price"),
                            stock = prodObj.getInt("stock"),
                            category = defaultProd?.category ?: prodObj.getString("category"),
                            imageResId = imageResId
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