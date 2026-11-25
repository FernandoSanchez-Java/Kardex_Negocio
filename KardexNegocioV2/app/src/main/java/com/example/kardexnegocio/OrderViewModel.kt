package com.example.kardexnegocio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrderViewModel : ViewModel() {

    // 1. Menú inicial con datos de prueba (Placeholders genéricos)
    private val _productList = MutableStateFlow<List<Product>>(
        listOf(
            Product(name = "Producto A", price = 50.0, stock = 10, category = "General"),
            Product(name = "Producto B", price = 85.5, stock = 5, category = "General"),
            Product(name = "Producto C", price = 30.0, stock = 20, category = "Bebidas"),
            Product(name = "Producto D", price = 120.0, stock = 8, category = "Especial")
        )
    )
    val productList: StateFlow<List<Product>> = _productList.asStateFlow()

    // 2. Lista global de órdenes activas (Sincronización instantánea Admin <-> Cocina)
    private val _orderList = MutableStateFlow<List<Order>>(emptyList())
    val orderList: StateFlow<List<Order>> = _orderList.asStateFlow()

    private var nextOrderNumber = 1

    // 3. Carrito temporal para armado de pedido en Admin
    private val _cartItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val cartItems: StateFlow<List<OrderItem>> = _cartItems.asStateFlow()

    fun addToCart(product: Product) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        val productInStock = _productList.value.find { it.id == product.id } ?: return
        
        val currentQty = if (index != -1) currentCart[index].quantity else 0
        if (currentQty + 1 <= productInStock.stock) {
            if (index != -1) {
                currentCart[index] = currentCart[index].copy(quantity = currentQty + 1)
            } else {
                currentCart.add(OrderItem(product = product, quantity = 1))
            }
            _cartItems.value = currentCart
        }
    }

    fun removeFromCart(product: Product) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
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

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // 4. Crear una orden (Ejecutado por el Admin)
    fun createOrder(itemsToOrder: List<OrderItem>, orderType: OrderType, paymentMethod: PaymentMethod): Boolean {
        if (itemsToOrder.isEmpty()) return false

        val currentProducts = _productList.value.toMutableList()

        // Validar stock disponible (Kárdex)
        for (orderItem in itemsToOrder) {
            val productInStock = currentProducts.find { it.id == orderItem.product.id }
            if (productInStock == null || productInStock.stock < orderItem.quantity) {
                return false
            }
        }

        // Descontar stock en tiempo real
        val calculatedTotal = itemsToOrder.sumOf { it.product.price * it.quantity }

        for (orderItem in itemsToOrder) {
            val index = currentProducts.indexOfFirst { it.id == orderItem.product.id }
            if (index != -1) {
                currentProducts[index] = currentProducts[index].copy(
                    stock = currentProducts[index].stock - orderItem.quantity
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
    }

    // 5. Configurar / Editar pedido en caso de equivocación
    fun updateOrderDetails(orderNumber: Int, newOrderType: OrderType, newPaymentMethod: PaymentMethod) {
        _orderList.value = _orderList.value.map { order ->
            if (order.orderNumber == orderNumber) {
                order.copy(orderType = newOrderType, paymentMethod = newPaymentMethod)
            } else {
                order
            }
        }
    }

    // 6. Cancelar pedido (revierte stock al kárdex)
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

        // Eliminar orden de la lista
        _orderList.value = _orderList.value.filter { it.orderNumber != orderNumber }
    }
}