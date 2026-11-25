package com.example.kardexnegocio

import androidx.annotation.DrawableRes
import java.util.UUID

// 1. Modelo de producto (con soporte opcional para recurso drawable)
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,         // Ej. "Clásica", "Combo Solitario"
    val price: Double,        // Precio en MXN
    var stock: Int,           // Control de inventario en tiempo real
    val category: String,     // Categoría ("Hamburguesas", "Papas", "Malteadas", "Bebidas", "Combos")
    @field:DrawableRes val imageResId: Int? = null,
)

// 2. Ítem individual dentro de una orden o comanda
data class OrderItem(
    val product: Product,
    val quantity: Int,
    val notes: String = "",    // Modificadores/notas opcionales (ej. "Sin cebolla", "Extra salsa")
)

// 3. Estructura principal de la Orden / Comanda digital
data class Order(
    val orderNumber: Int,              // Consecutivo (1, 2, 3...)
    val items: List<OrderItem>,        // Lista de productos solicitados
    val orderType: OrderType,          // Para Comer Aquí o Para Llevar
    var status: OrderStatus,           // PENDING (Cocina), READY (Listo), COMPLETED (Entregado)
    val paymentMethod: PaymentMethod,  // Efectivo, Transferencia, Terminal
    val totalAmount: Double,
    val timestamp: Long = System.currentTimeMillis(),
)

// 4. Modalidad de consumo
enum class OrderType {
    DINE_IN,    // Para Comer Aquí
    TAKE_AWAY   // Para Llevar
}

// 5. Estatus de la orden para la sincronización Admin-Cocina
enum class OrderStatus {
    PENDING,    // Visible únicamente en la pantalla de la cocina
    READY,      // Avisado por cocina, listo para entregar
    COMPLETED   // Cerrado y archivado por el administrador
}

// 6. Métodos de pago obligatorios
enum class PaymentMethod {
    CASH,       // Efectivo (con cálculo de cambio)
    TRANSFER,   // Transferencia manual
    TERMINAL    // Tarjeta / Terminal bancaria
}