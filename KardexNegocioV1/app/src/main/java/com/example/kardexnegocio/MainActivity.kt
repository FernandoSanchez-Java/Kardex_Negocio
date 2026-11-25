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
                Text("Panel Admin")
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Menú Genérico (Kárdex Automático)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text("Precio: \$${product.price} | Stock: ${product.stock}", fontSize = 12.sp, color = Color.DarkGray)
                        }

                        Button(onClick = {
                            val orderItem = OrderItem(product = product, quantity = 1)
                            viewModel.createOrder(
                                itemsToOrder = listOf(orderItem),
                                orderType = OrderType.DINE_IN,
                                paymentMethod = PaymentMethod.CASH
                            )
                        }, enabled = product.stock > 0) {
                            Text(if (product.stock > 0) "Vender (1)" else "Agotado")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Órdenes Activas (${orders.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(orders) { order ->
                val typeText = if (order.orderType == OrderType.DINE_IN) "Comer Aquí" else "Para Llevar"
                val paymentText = when (order.paymentMethod) {
                    PaymentMethod.CASH -> "Efectivo"
                    PaymentMethod.TRANSFER -> "Transferencia"
                    PaymentMethod.TERMINAL -> "Terminal / Tarjeta"
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Orden #${order.orderNumber} - Pendiente", fontWeight = FontWeight.Bold)
                        Text("Total: \$${order.totalAmount} | Tipo: $typeText | Pago: $paymentText", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun KitchenView(viewModel: OrderViewModel) {
    val orders by viewModel.orderList.collectAsState()
    val pendingOrders = orders.filter { it.status == OrderStatus.PENDING }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Comandas en Cocina (En tiempo real)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay pedidos pendientes en la cocina 👨‍🍳", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pendingOrders) { order ->
                    val typeText = if (order.orderType == OrderType.DINE_IN) "Comer Aquí" else "Para Llevar"

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ORDEN #${order.orderNumber}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("Modalidad: $typeText", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Productos:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            order.items.forEach { item ->
                                Text("- ${item.quantity}x ${item.product.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}