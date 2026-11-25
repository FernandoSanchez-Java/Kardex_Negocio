package com.example.kardexnegocio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kardexnegocio.ui.theme.KardexNegocioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KardexNegocioTheme {
                val viewModel: OrderViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: OrderViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text("Panel Admin (V3)")
            }
            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text("Pantalla Cocina")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> AdminView(viewModel)
            1 -> KitchenView(viewModel)
        }
    }
}

@Composable
fun AdminView(viewModel: OrderViewModel) {
    val products by viewModel.productList.collectAsState()
    val orders by viewModel.orderList.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val activeOrders = orders.filter { it.status != OrderStatus.COMPLETED }
    val completedOrders = orders.filter { it.status == OrderStatus.COMPLETED }

    var isCompletedExpanded by remember { mutableStateOf(false) }
    var isCorteCajaExpanded by remember { mutableStateOf(true) }

    // Estados de diálogos
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showNewShiftDialog by remember { mutableStateOf(false) }
    var orderToConfigure by remember { mutableStateOf<Order?>(null) }
    var editingNoteIndex by remember { mutableStateOf<Int?>(null) }

    val cartTotal = cartItems.sumOf { it.product.price * it.quantity }

    // Cálculos para Corte de Caja (solo órdenes COMPLETED)
    val totalIncome = completedOrders.sumOf { it.totalAmount }
    val cashTotal = completedOrders.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    val transferTotal = completedOrders.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.totalAmount }
    val terminalTotal = completedOrders.filter { it.paymentMethod == PaymentMethod.TERMINAL }.sumOf { it.totalAmount }

    val dineInCount = completedOrders.count { it.orderType == OrderType.DINE_IN }
    val takeAwayCount = completedOrders.count { it.orderType == OrderType.TAKE_AWAY }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- SECCIÓN 0: CORTE DE CAJA DEL TURNO ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📊 Corte de Caja del Turno",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = { isCorteCajaExpanded = !isCorteCajaExpanded }) {
                            Text(
                                if (isCorteCajaExpanded) "▲ Ocultar" else "▼ Ver Resumen",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isCorteCajaExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ingreso Total: \$${totalIncome}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("💵 Efectivo: \$${cashTotal}", fontSize = 12.sp)
                                Text("💸 Transferencia: \$${transferTotal}", fontSize = 12.sp)
                                Text("💳 Tarjeta/Terminal: \$${terminalTotal}", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Tickets Cerrados: ${completedOrders.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("• Comer Aquí: $dineInCount", fontSize = 11.sp, color = Color.DarkGray)
                                Text("• Para Llevar: $takeAwayCount", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showNewShiftDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("🔄 Iniciar Nuevo Turno / Limpiar Historial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 1: MENÚ Y VENTAS ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Menú Genérico (Kárdex Automático)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(products, key = { it.id }) { product ->
            val itemsInCartForProd = cartItems.filter { it.product.id == product.id }
            val cartQty = itemsInCartForProd.sumOf { it.quantity }
            val availableStock = product.stock - cartQty

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text("Precio: \$${product.price}  |  Stock: ${availableStock}", fontSize = 12.sp, color = Color.DarkGray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (cartQty > 0) {
                            OutlinedButton(
                                onClick = { viewModel.removeFromCart(product) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$cartQty",
                                modifier = Modifier.padding(horizontal = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.addToCart(product) },
                            enabled = availableStock > 0,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(if (availableStock > 0) "+ Agregar" else "Agotado")
                        }
                    }
                }
            }
        }

        // --- CARRITO DE PEDIDO EN CURSO ---
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pedido Actual (${cartItems.sumOf { it.quantity }} items)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Total: \$${cartTotal}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        cartItems.forEachIndexed { index, item ->
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${item.quantity}x ${item.product.name} - \$${item.product.price * item.quantity}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    TextButton(
                                        onClick = { editingNoteIndex = index },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (item.notes.isBlank()) "📝 Agregar Nota" else "✏️ Editar Nota", fontSize = 11.sp)
                                    }
                                }
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        "   └ Nota: ${item.notes}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD84315)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Text("Vaciar", color = Color.Red)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showCheckoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Procesar Pago y Enviar")
                            }
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 2: ÓRDENES ACTIVAS ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Órdenes Activas (${activeOrders.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (activeOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "No hay órdenes activas en este momento",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(activeOrders, key = { it.orderNumber }) { order ->
                val typeText = if (order.orderType == OrderType.DINE_IN) "Comer Aquí" else "Para Llevar"
                val paymentText = when (order.paymentMethod) {
                    PaymentMethod.CASH -> "💵 Efectivo"
                    PaymentMethod.TRANSFER -> "💸 Transferencia"
                    PaymentMethod.TERMINAL -> "💳 Tarjeta / Terminal"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                color = Color(0xFF1976D2),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "EN COCINA",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total: \$${order.totalAmount} | Tipo: $typeText", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Método de Pago: $paymentText", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(4.dp))
                        order.items.forEach { item ->
                            Column {
                                Text("• ${item.quantity}x ${item.product.name}", fontSize = 11.sp, color = Color.DarkGray)
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        "   └ 📝 ${item.notes}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD84315)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { orderToConfigure = order },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⚙️ Configurar", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateOrderStatus(order.orderNumber, OrderStatus.COMPLETED)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Finalizar ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 3: ÓRDENES FINALIZADAS (DESPLEGABLE / COLLAPSIBLE) ---
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                onClick = { isCompletedExpanded = !isCompletedExpanded },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Órdenes Finalizadas (${completedOrders.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (isCompletedExpanded) "▲ Ocultar" else "▼ Desplegar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (isCompletedExpanded) {
            if (completedOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Text(
                            "No hay órdenes finalizadas aún",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(completedOrders, key = { it.orderNumber }) { order ->
                    val typeText = if (order.orderType == OrderType.DINE_IN) "Comer Aquí" else "Para Llevar"
                    val paymentText = when (order.paymentMethod) {
                        PaymentMethod.CASH -> "💵 Efectivo"
                        PaymentMethod.TRANSFER -> "💸 Transferencia"
                        PaymentMethod.TERMINAL -> "💳 Tarjeta / Terminal"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        "FINALIZADO ✓",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Total: \$${order.totalAmount} | Tipo: $typeText", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Text("Pago: $paymentText", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            order.items.forEach { item ->
                                Column {
                                    Text("• ${item.quantity}x ${item.product.name}", fontSize = 11.sp, color = Color(0xFF388E3C))
                                    if (item.notes.isNotBlank()) {
                                        Text("   └ 📝 ${item.notes}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO PARA INICIAR NUEVO TURNO / LIMPIAR HISTORIAL
    if (showNewShiftDialog) {
        AlertDialog(
            onDismissRequest = { showNewShiftDialog = false },
            title = { Text("¿Iniciar Nuevo Turno?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Esto archivará/limpiará el historial de órdenes finalizadas y reiniciará el corte de caja a \$0.0.\n\nEl stock del kárdex y las órdenes activas en curso NO se verán afectados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedOrders()
                        showNewShiftDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sí, Iniciar Nuevo Turno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewShiftDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO PARA EDITAR NOTA EN EL CARRITO
    editingNoteIndex?.let { index ->
        val currentNote = if (index in cartItems.indices) cartItems[index].notes else ""
        val item = if (index in cartItems.indices) cartItems[index] else null

        if (item != null) {
            ItemNoteDialog(
                productName = item.product.name,
                initialNote = currentNote,
                onDismiss = { editingNoteIndex = null },
                onSave = { newNote ->
                    viewModel.updateCartItemNotes(index, newNote)
                    editingNoteIndex = null
                }
            )
        }
    }

    // DIÁLOGO DE CHECKOUT / PROCESAR PAGO AL CREAR ORDEN
    if (showCheckoutDialog) {
        CheckoutDialog(
            totalAmount = cartTotal,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { selectedOrderType, selectedPaymentMethod ->
                viewModel.createOrder(cartItems, selectedOrderType, selectedPaymentMethod)
                showCheckoutDialog = false
            }
        )
    }

    // DIÁLOGO DE CONFIGURAR / EDITAR PEDIDO EXISTENTE
    orderToConfigure?.let { order ->
        ConfigureOrderDialog(
            order = order,
            onDismiss = { orderToConfigure = null },
            onSave = { updatedType, updatedPayment ->
                viewModel.updateOrderDetails(order.orderNumber, updatedType, updatedPayment)
                orderToConfigure = null
            },
            onCancelOrder = {
                viewModel.cancelOrder(order.orderNumber)
                orderToConfigure = null
            }
        )
    }
}

// DIÁLOGO: Agregar/Editar Nota de Ítem en Carrito
@Composable
fun ItemNoteDialog(
    productName: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota para $productName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Escribe aclaraciones para la cocina (ej. Sin cebolla, Extra salsa):", fontSize = 13.sp)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota o Modificador") },
                    placeholder = { Text("Ej. Sin jitomate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(noteText.trim()) }) {
                Text("Guardar Nota")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIÁLOGO: Selección de método de pago y modalidad antes de enviar
@Composable
fun CheckoutDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (OrderType, PaymentMethod) -> Unit
) {
    var selectedType by remember { mutableStateOf(OrderType.DINE_IN) }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Procesar Pago y Confirmar", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Total a Pagar: \$${totalAmount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Text("Modalidad de Consumo:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == OrderType.DINE_IN,
                        onClick = { selectedType = OrderType.DINE_IN },
                        label = { Text("Comer Aquí") }
                    )
                    FilterChip(
                        selected = selectedType == OrderType.TAKE_AWAY,
                        onClick = { selectedType = OrderType.TAKE_AWAY },
                        label = { Text("Para Llevar") }
                    )
                }

                Text("Método de Pago:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPayment == PaymentMethod.CASH,
                            onClick = { selectedPayment = PaymentMethod.CASH }
                        )
                        Text("💵 Efectivo")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPayment == PaymentMethod.TRANSFER,
                            onClick = { selectedPayment = PaymentMethod.TRANSFER }
                        )
                        Text("💸 Transferencia")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPayment == PaymentMethod.TERMINAL,
                            onClick = { selectedPayment = PaymentMethod.TERMINAL }
                        )
                        Text("💳 Tarjeta / Terminal")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType, selectedPayment) }) {
                Text("Enviar a Cocina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIÁLOGO: Configurar/Editar pedido en caso de equivocación
@Composable
fun ConfigureOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (OrderType, PaymentMethod) -> Unit,
    onCancelOrder: () -> Unit
) {
    var selectedType by remember { mutableStateOf(order.orderType) }
    var selectedPayment by remember { mutableStateOf(order.paymentMethod) }
    var showConfirmCancel by remember { mutableStateOf(false) }

    if (showConfirmCancel) {
        AlertDialog(
            onDismissRequest = { showConfirmCancel = false },
            title = { Text("¿Cancelar Orden #${order.orderNumber}?") },
            text = { Text("Esta acción eliminará el pedido y devolverá los productos al stock del kárdex.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCancel = false
                        onCancelOrder()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Sí, Cancelar Pedido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCancel = false }) {
                    Text("Volver")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Configurar Orden #${order.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modalidad de Consumo:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == OrderType.DINE_IN,
                            onClick = { selectedType = OrderType.DINE_IN },
                            label = { Text("Comer Aquí") }
                        )
                        FilterChip(
                            selected = selectedType == OrderType.TAKE_AWAY,
                            onClick = { selectedType = OrderType.TAKE_AWAY },
                            label = { Text("Para Llevar") }
                        )
                    }

                    Text("Método de Pago:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPayment == PaymentMethod.CASH,
                                onClick = { selectedPayment = PaymentMethod.CASH }
                            )
                            Text("💵 Efectivo")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPayment == PaymentMethod.TRANSFER,
                                onClick = { selectedPayment = PaymentMethod.TRANSFER }
                            )
                            Text("💸 Transferencia")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPayment == PaymentMethod.TERMINAL,
                                onClick = { selectedPayment = PaymentMethod.TERMINAL }
                            )
                            Text("💳 Tarjeta / Terminal")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = { showConfirmCancel = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚠️ Anular / Cancelar Pedido")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(selectedType, selectedPayment) }) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun KitchenView(viewModel: OrderViewModel) {
    val orders by viewModel.orderList.collectAsState()
    val pendingOrders = orders.filter { it.status == OrderStatus.PENDING }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Comandas en Cocina (Solo Lectura)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay pedidos pendientes en la cocina 👨‍🍳", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pendingOrders, key = { it.orderNumber }) { order ->
                    val typeText = if (order.orderType == OrderType.DINE_IN) "Comer Aquí" else "Para Llevar"

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ORDEN #${order.orderNumber}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Surface(
                                    color = Color(0xFFE65100),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = typeText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFFFCC80))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("PRODUCTOS A PREPARAR:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            order.items.forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        "• ${item.quantity}x ${item.product.name}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            "   📌 NOTA: ${item.notes}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFD84315)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}