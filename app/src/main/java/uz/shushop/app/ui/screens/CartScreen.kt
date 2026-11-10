package uz.shushop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.data.Address
import uz.shushop.app.data.CartItem
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.AppTextField
import uz.shushop.app.ui.components.ColorDot
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.InfoStrip
import uz.shushop.app.ui.components.Panel
import uz.shushop.app.ui.components.PrimaryButton
import uz.shushop.app.ui.components.QtyStepper
import uz.shushop.app.ui.components.ShoePicture
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.OrderStatus
import uz.shushop.app.util.Pricing
import uz.shushop.app.util.formatSom
import uz.shushop.app.util.pairs

/** Cart and checkout on one screen: items, delivery address, payment, promo code, cost, "Confirm Order". */
@Composable
fun CartScreen(vm: ShopViewModel, onBack: () -> Unit, onOpenShoe: (Long) -> Unit, onPlaced: (Long) -> Unit, onBrowse: () -> Unit) {
    val items by vm.cart.collectAsStateWithLifecycle()
    val addresses by vm.addresses.collectAsStateWithLifecycle()
    val price by vm.price.collectAsStateWithLifecycle()
    val coupon by vm.coupon.collectAsStateWithLifecycle()
    val couponError by vm.couponError.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val working by vm.working.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()

    var addressId by rememberSaveable { mutableStateOf<Long?>(null) }
    var payment by rememberSaveable { mutableStateOf("cash") }
    var promo by rememberSaveable { mutableStateOf("") }
    var showAddress by rememberSaveable { mutableStateOf(false) }

    val address = addresses.firstOrNull { it.id == addressId } ?: addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    val hasProblem = items.any { !it.available }
    val canOrder = items.isNotEmpty() && !hasProblem && address != null && !offline

    Column(Modifier.fillMaxSize().background(Palette.Paper)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text("Checkout", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 6.dp))
        }

        if (items.isEmpty()) {
            EmptyState(Icons.Filled.ShoppingCart, "Your cart is empty", "Pick a size on any shoe and add it here.", actionLabel = "Browse shoes", onAction = onBrowse)
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (offline) InfoStrip("You're offline. Connect to the internet to place your order.")

            items.forEach { line ->
                CartLine(line, meta.maxQtyPerItem, onOpen = { onOpenShoe(line.shoeId) }, onQty = { vm.setCartQty(line.variantId, it) }, onRemove = { vm.removeFromCart(line.variantId) })
                HorizontalDivider(color = Palette.Fog)
            }

            // ---- delivery address
            Text("Delivery address", style = MaterialTheme.typography.titleMedium)
            if (address == null) {
                Panel {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add the address we should deliver to.", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
                        PrimaryButton("Add address", { showAddress = true })
                    }
                }
            } else {
                addresses.forEach { a ->
                    AddressChoice(a, selected = a.id == address.id, onClick = { addressId = a.id })
                }
                Text(
                    "Add another address",
                    modifier = Modifier.clickable(role = Role.Button) { showAddress = true }.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge, color = Palette.Slate,
                )
            }

            // ---- payment
            Text("Payment", style = MaterialTheme.typography.titleMedium)
            meta.paymentMethods.forEach { method ->
                ChoiceRow(OrderStatus.paymentLabel(method), selected = payment == method, onClick = { payment = method })
            }

            // ---- promo code
            Text("Promo code", style = MaterialTheme.typography.titleMedium)
            if (coupon != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Mist).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Good, modifier = Modifier.size(20.dp))
                    Text(
                        "${coupon?.couponCode} takes ${coupon?.couponPercent}% off",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                    Text("Remove", modifier = Modifier.clickable(role = Role.Button) { vm.clearCoupon(); promo = "" }.padding(6.dp), style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { AppTextField(promo, { promo = it.uppercase() }, "Code", error = couponError, placeholder = "WELCOME10") }
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Palette.Ink)
                            .clickable(enabled = promo.isNotBlank(), role = Role.Button) { vm.applyCoupon(promo) }
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Apply", color = Palette.Paper, style = MaterialTheme.typography.labelLarge) }
                }
            }

            // ---- cost
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostRow("Subtotal", formatSom(price.subtotal))
                    if (price.discount > 0) CostRow("Discount", "-" + formatSom(price.discount), color = Palette.Good)
                    CostRow("Delivery", if (price.shipping == 0L) "Free" else formatSom(price.shipping))
                    HorizontalDivider(color = Palette.Fog)
                    CostRow("Total", formatSom(price.total), bold = true)
                    val more = Pricing.toFreeShipping(price.subtotal, price.discount, meta.freeShippingFrom)
                    if (price.shipping > 0 && more > 0) {
                        Text("Add ${formatSom(more)} more for free delivery.", style = MaterialTheme.typography.bodySmall, color = Palette.Slate)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(
            Modifier.fillMaxWidth().border(1.dp, Palette.Fog.copy(alpha = 0.6f)).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hasProblem) Text("Remove the sold-out items to continue.", color = Palette.Sale, style = MaterialTheme.typography.bodySmall)
            PrimaryButton(
                "Confirm Order, ${formatSom(price.total)}",
                { address?.let { a -> vm.placeOrder(a.id, payment, onPlaced) } },
                enabled = canOrder,
                loading = working,
            )
        }
    }

    if (showAddress) AddressDialog(vm, existing = null, onClose = { showAddress = false })
}

@Composable
private fun CartLine(line: CartItem, maxPerItem: Int, onOpen: () -> Unit, onQty: (Int) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Mist).clickable(role = Role.Button, onClick = onOpen),
        ) {
            ShoePicture(line.artStyle, line.imageUrl, line.colorHex, line.shoeId, Modifier.fillMaxSize().padding(4.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(line.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDot(line.colorHex, size = 12.dp)
                Text("${line.colorName}, size ${line.size}", style = MaterialTheme.typography.bodySmall, color = Palette.Slate, maxLines = 1)
            }
            Text(formatSom(line.unitPrice), style = MaterialTheme.typography.bodyMedium)
            if (!line.available) {
                Text(
                    if (line.stock <= 0) "Sold out" else "Only ${pairs(line.stock)} left",
                    color = Palette.Sale, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QtyStepper(line.qty, onQty, compact = true, max = minOf(maxPerItem, if (line.stock > 0) line.stock else line.qty))
            Icon(
                Icons.Filled.Delete, contentDescription = "Remove ${line.name} from cart", tint = Palette.Ash,
                modifier = Modifier.size(24.dp).clickable(role = Role.Button, onClick = onRemove),
            )
        }
    }
}

@Composable
private fun AddressChoice(a: Address, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.Ink else Palette.Fog, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (selected) Palette.Ink else Palette.Ash)
        Column(Modifier.weight(1f)) {
            Text(a.label + if (a.isDefault) " (default)" else "", style = MaterialTheme.typography.labelLarge)
            Text("${a.line}, ${a.city}", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
            Text(a.phone, style = MaterialTheme.typography.bodySmall, color = Palette.Ash)
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.Ink else Palette.Fog, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CostRow(label: String, value: String, bold: Boolean = false, color: androidx.compose.ui.graphics.Color = Palette.Ink) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = if (bold) Palette.Ink else Palette.Slate, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = color)
    }
}
