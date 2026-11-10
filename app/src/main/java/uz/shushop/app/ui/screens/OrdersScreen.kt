package uz.shushop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.data.OrderItem
import uz.shushop.app.data.OrderWithItems
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.ColorDot
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.OutlineButton
import uz.shushop.app.ui.components.Panel
import uz.shushop.app.ui.components.ShoePicture
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.OrderStatus
import uz.shushop.app.util.formatDateTime
import uz.shushop.app.util.formatSom

/** A small coloured label for an order's status. */
@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.NEW -> Color(0xFFE6EEFF) to Palette.Blue
        OrderStatus.PROCESSING -> Color(0xFFFFF1D6) to Color(0xFF9A6A00)
        OrderStatus.SHIPPED -> Palette.Ink to Palette.Paper
        OrderStatus.DELIVERED -> Color(0xFFDDF3E6) to Palette.Good
        else -> Palette.Mist to Palette.Slate
    }
    Text(
        OrderStatus.label(status),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 11.dp, vertical = 5.dp),
        color = fg, style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Order history: running orders first, then the finished ones. */
@Composable
fun OrdersScreen(vm: ShopViewModel, onBack: () -> Unit, onOpenOrder: (Long) -> Unit, onBrowse: () -> Unit) {
    val orders by vm.orders.collectAsStateWithLifecycle()
    val active = orders.filter { OrderStatus.isActive(it.order.status) }
    val past = orders.filter { !OrderStatus.isActive(it.order.status) }

    Column(Modifier.fillMaxSize().background(Palette.Paper)) {
        TopBar("My orders", onBack)
        if (orders.isEmpty()) {
            EmptyState(Icons.Filled.ShoppingCart, "No orders yet", "When you place an order it shows up here, and you can follow it until it arrives.", actionLabel = "Browse shoes", onAction = onBrowse)
            return@Column
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            if (active.isNotEmpty()) {
                item { Text("On the way to you", style = MaterialTheme.typography.titleMedium) }
                items(active, key = { it.order.id }) { OrderCard(it) { onOpenOrder(it.order.id) } }
            }
            if (past.isNotEmpty()) {
                item { Text("Earlier orders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                items(past, key = { it.order.id }) { OrderCard(it) { onOpenOrder(it.order.id) } }
            }
        }
    }
}

@Composable
private fun OrderCard(o: OrderWithItems, onClick: () -> Unit) {
    val first = o.items.firstOrNull()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mist)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Paper)) {
            if (first != null) ShoePicture(first.artStyle, null, first.colorHex, first.shoeId ?: first.id, Modifier.fillMaxSize().padding(4.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Order #${o.order.id}", style = MaterialTheme.typography.titleMedium)
                StatusPill(o.order.status)
            }
            Text(
                when {
                    first == null -> ""
                    o.items.size == 1 -> first.name
                    else -> "${first.name} and ${o.items.size - 1} more"
                },
                style = MaterialTheme.typography.bodyMedium, color = Palette.Slate, maxLines = 1,
            )
            Text("${formatDateTime(o.order.createdAt)}, ${formatSom(o.order.total)}", style = MaterialTheme.typography.bodySmall, color = Palette.Ash)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Ash)
    }
}

/** One order: where it is now, what is in it, what it cost, where it goes. */
@Composable
fun OrderDetailScreen(vm: ShopViewModel, orderId: Long, onBack: () -> Unit, onOpenShoe: (Long) -> Unit) {
    val data by remember(orderId) { vm.order(orderId) }.collectAsStateWithLifecycle(initialValue = null)
    val history by vm.history.collectAsStateWithLifecycle()
    val working by vm.working.collectAsStateWithLifecycle()
    LaunchedEffect(orderId) { vm.openOrder(orderId) }
    var askCancel by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Palette.Paper)) {
        TopBar("Order #$orderId", onBack)
        val o = data
        if (o == null) {
            EmptyState(Icons.Filled.ShoppingCart, "Order not found", "It may belong to another account.", actionLabel = "Back", onAction = onBack)
            return@Column
        }
        val order = o.order
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(order.status)
                Text("Placed ${formatDateTime(order.createdAt)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
            }

            ProgressSteps(order.status)

            if (history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    history.forEach { step ->
                        Text("${OrderStatus.label(step.status)}, ${formatDateTime(step.at)}", style = MaterialTheme.typography.bodySmall, color = Palette.Slate)
                    }
                }
            }

            HorizontalDivider(color = Palette.Fog)
            Text("Items", style = MaterialTheme.typography.titleMedium)
            o.items.forEach { OrderLine(it, onOpen = { id -> onOpenShoe(id) }) }

            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostRow("Subtotal", formatSom(order.subtotal))
                    if (order.discount > 0) CostRow("Discount${order.couponCode?.let { " ($it)" } ?: ""}", "-" + formatSom(order.discount), color = Palette.Good)
                    CostRow("Delivery", if (order.shippingFee == 0L) "Free" else formatSom(order.shippingFee))
                    HorizontalDivider(color = Palette.Fog)
                    CostRow("Total", formatSom(order.total), bold = true)
                }
            }

            Text("Delivery", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${order.addressLine}, ${order.city}", style = MaterialTheme.typography.bodyLarge)
                Text(order.phone, style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
                Text("Payment: ${OrderStatus.paymentLabel(order.paymentMethod)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
            }

            if (OrderStatus.canCancel(order.status)) {
                OutlineButton("Cancel order", { askCancel = true }, enabled = !working, danger = true)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (askCancel) {
        AlertDialog(
            onDismissRequest = { askCancel = false },
            title = { Text("Cancel this order?") },
            text = { Text("The pairs go back to the shop. You can order them again any time they are in stock.") },
            confirmButton = { TextButton(onClick = { askCancel = false; vm.cancelOrder(orderId) }) { Text("Cancel order", color = Palette.Sale) } },
            dismissButton = { TextButton(onClick = { askCancel = false }) { Text("Keep it") } },
        )
    }
}

@Composable
private fun OrderLine(item: OrderItem, onOpen: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = item.shoeId != null, role = Role.Button) { item.shoeId?.let(onOpen) },
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Mist)) {
            ShoePicture(item.artStyle, null, item.colorHex, item.shoeId ?: item.id, Modifier.fillMaxSize().padding(3.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDot(item.colorHex, size = 12.dp)
                Text("${item.colorName}, size ${item.size}", style = MaterialTheme.typography.bodySmall, color = Palette.Slate)
            }
            Text("${item.qty} x ${formatSom(item.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = Palette.Ash)
        }
        Text(formatSom(item.lineTotal), style = MaterialTheme.typography.labelLarge)
    }
}

/** New - Packing - On the way - Delivered, with the reached steps filled in. */
@Composable
private fun ProgressSteps(status: String) {
    if (status == OrderStatus.CANCELLED) {
        Text("This order was cancelled.", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
        return
    }
    val reached = OrderStatus.step(status)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        OrderStatus.PATH.forEachIndexed { i, s ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (i <= reached) Palette.Ink else Palette.Mist),
                    contentAlignment = Alignment.Center,
                ) {
                    if (i <= reached) Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Paper, modifier = Modifier.size(16.dp))
                }
                Text(
                    OrderStatus.label(s), textAlign = TextAlign.Center, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (i <= reached) Palette.Ink else Palette.Ash,
                )
            }
        }
    }
}
