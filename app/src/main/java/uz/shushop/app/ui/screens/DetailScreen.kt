package uz.shushop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.data.ProductDetail
import uz.shushop.app.data.ShoeWithVariants
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.AppTextField
import uz.shushop.app.ui.components.ColorDot
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.HeartButton
import uz.shushop.app.ui.components.OutlineButton
import uz.shushop.app.ui.components.PriceText
import uz.shushop.app.ui.components.PrimaryButton
import uz.shushop.app.ui.components.QtyStepper
import uz.shushop.app.ui.components.ShoePicture
import uz.shushop.app.ui.components.Stars
import uz.shushop.app.ui.components.Tag
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.formatDate
import uz.shushop.app.util.formatSom
import uz.shushop.app.util.pairs

/** One shoe: picture in the chosen colour, sizes, quantity, "Add to Cart", reviews. */
@Composable
fun DetailScreen(vm: ShopViewModel, shoeId: Long, onBack: () -> Unit, onOpenCart: () -> Unit) {
    val item by remember(shoeId) { vm.shoe(shoeId) }.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(shoeId) { vm.openShoe(shoeId) }

    val current = item
    if (current == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BackButton(onBack, Modifier.padding(16.dp))
            EmptyState(Icons.Filled.Star, "This shoe isn't available", "It may have been removed from the shop.", actionLabel = "Back to the shop", onAction = onBack)
        }
        return
    }
    DetailContent(vm, current, onBack, onOpenCart)
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Palette.Paper.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(vm: ShopViewModel, item: ShoeWithVariants, onBack: () -> Unit, onOpenCart: () -> Unit) {
    val shoe = item.shoe
    val user by vm.user.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val alerts by vm.alertIds.collectAsStateWithLifecycle()
    val cartCount by vm.cartCount.collectAsStateWithLifecycle()
    val working by vm.working.collectAsStateWithLifecycle()
    val live by vm.reviews.collectAsStateWithLifecycle()

    val colors = remember(item) { item.variants.map { it.colorName to it.colorHex }.distinct() }
    val sizes = remember(item) { item.variants.map { it.size }.distinct().sorted() }
    fun variantOf(size: Int?, color: String?) = if (size == null || color == null) null else item.variants.firstOrNull { it.size == size && it.colorName == color }
    fun colorHasStock(color: String) = item.variants.any { it.colorName == color && it.stock > 0 }

    var color by rememberSaveable(shoe.id) { mutableStateOf(colors.firstOrNull { colorHasStock(it.first) }?.first ?: colors.firstOrNull()?.first) }
    var size by rememberSaveable(shoe.id) {
        val preferred = user?.shoeSizeEu            // start on the customer's own size when it is available
        mutableStateOf(if (preferred != null && (variantOf(preferred, color)?.stock ?: 0) > 0) preferred else null)
    }
    var qty by rememberSaveable(shoe.id) { mutableStateOf(1) }

    val variant = variantOf(size, color)
    val maxQty = minOf(variant?.stock ?: 1, meta.maxQtyPerItem).coerceAtLeast(1)
    val colorHex = colors.firstOrNull { it.first == color }?.second
    val isFavourite = shoe.id in favourites
    val hasAlert = shoe.id in alerts

    Column(Modifier.fillMaxSize().background(Palette.Paper)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Palette.Mist),
            ) {
                ShoePicture(shoe.artStyle, shoe.imageUrl, colorHex, shoe.id, Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 20.dp))
                BackButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
                HeartButton(isFavourite, { vm.toggleFavourite(shoe.id) }, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp))
                if (shoe.onSale) Tag("Save ${shoe.discountPercent}%", Modifier.align(Alignment.BottomStart).padding(18.dp), Palette.Sale)
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(shoe.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${shoe.brand}, ${shoe.type.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
                    Spacer(Modifier.height(2.dp))
                    PriceText(shoe.price, shoe.oldPrice, big = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stars(shoe.avgRating)
                        Text(
                            if (shoe.reviewCount == 0) "No reviews yet" else "${"%.1f".format(shoe.avgRating)} (${shoe.reviewCount})",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Slate,
                        )
                    }
                }

                if (shoe.description.isNotBlank()) Text(shoe.description, style = MaterialTheme.typography.bodyLarge, color = Palette.Slate)

                // ---- colour
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Colour", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Text(color ?: "", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colors.forEach { (name, hex) ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .clickable(role = Role.RadioButton) {
                                        color = name
                                        if (size != null && (variantOf(size, name)?.stock ?: 0) <= 0) size = null
                                        qty = 1
                                    }
                                    .padding(2.dp)
                                    .semantics { contentDescription = if (colorHasStock(name)) name else "$name, sold out" },
                            ) { ColorDot(hex, size = 30.dp, selected = name == color) }
                        }
                    }
                }

                // ---- size
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Size", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sizes.forEach { s ->
                            val inStock = (variantOf(s, color)?.stock ?: 0) > 0
                            SizeCircle(s, selected = s == size, enabled = inStock) { size = s; qty = minOf(qty, variantOf(s, color)?.stock ?: 1).coerceAtLeast(1) }
                        }
                    }
                    val left = variant?.stock ?: 0
                    if (variant != null && left in 1..3) Text("Only ${pairs(left)} left in this size", color = Palette.Sale, style = MaterialTheme.typography.bodySmall)
                    if (color != null && !colorHasStock(color!!)) Text("This colour is sold out for now.", color = Palette.Slate, style = MaterialTheme.typography.bodySmall)
                }

                // ---- quantity
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    QtyStepper(qty, { qty = it }, min = 1, max = maxQty)
                }

                HorizontalDivider(color = Palette.Fog)
                ReviewsSection(vm, shoe.id, live)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ---- the action bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Paper)
                .border(1.dp, Palette.Fog.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Palette.Fog, CircleShape)
                    .clickable(role = Role.Button, onClick = onOpenCart)
                    .semantics { contentDescription = if (cartCount > 0) "Cart, $cartCount items" else "Cart" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(24.dp))
                if (cartCount > 0) {
                    Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(10.dp).clip(CircleShape).background(Palette.Blue))
                }
            }
            Box(Modifier.weight(1f)) {
                val soldOut = !item.inStock || (variant != null && variant.stock <= 0)
                when {
                    soldOut -> PrimaryButton(
                        if (hasAlert) "We'll tell you when it's back" else "Tell me when it's back",
                        { vm.toggleAlert(shoe.id) },
                        icon = Icons.Filled.Notifications,
                    )
                    variant == null -> PrimaryButton("Choose a size", {}, enabled = false)
                    else -> PrimaryButton(
                        "Add to Cart, ${formatSom(shoe.price * qty)}",
                        { vm.addToCart(variant.id, qty) { vm.say("${shoe.name} added to your cart"); qty = 1 } },
                        loading = working,
                    )
                }
            }
        }
    }
}

@Composable
private fun SizeCircle(size: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Ink else Palette.Paper)
            .border(1.dp, if (selected) Palette.Ink else Palette.Fog, CircleShape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Size $size" + if (enabled) "" else ", not available" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            size.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> Palette.Paper
                enabled -> Palette.Ink
                else -> Palette.Ash
            },
            textDecoration = if (enabled) null else TextDecoration.LineThrough,
        )
    }
}

@Composable
private fun ReviewsSection(vm: ShopViewModel, shoeId: Long, live: ProductDetail?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (live != null && live.reviews.isNotEmpty()) "Reviews (${live.reviews.size})" else "Reviews",
            style = MaterialTheme.typography.titleMedium,
        )
        when {
            live == null -> Text("Reviews load when you're online.", style = MaterialTheme.typography.bodyMedium, color = Palette.Ash)
            live.reviews.isEmpty() && !live.canReview ->
                Text("No reviews yet. You can review a shoe after an order with it is delivered.", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
        }
        if (live != null) {
            if (live.canReview) ReviewForm(vm, shoeId, live)
            live.reviews.forEach { r ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Mist).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (r.mine) "${r.author} (you)" else r.author, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text(formatDate(r.createdAt), style = MaterialTheme.typography.bodySmall, color = Palette.Ash)
                    }
                    Stars(r.rating.toDouble(), size = 14.dp)
                    if (r.comment.isNotBlank()) Text(r.comment, style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
                }
            }
        }
    }
}

@Composable
private fun ReviewForm(vm: ShopViewModel, shoeId: Long, live: ProductDetail) {
    val mine = live.reviews.firstOrNull { it.mine }
    var rating by remember(shoeId, mine?.id) { mutableIntStateOf(mine?.rating ?: 0) }
    var comment by remember(shoeId, mine?.id) { mutableStateOf(mine?.comment ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val working by vm.working.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Palette.Fog, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (mine != null) "Edit your review" else "Rate this shoe", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..5) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "$i star" + if (i > 1) "s" else "",
                    tint = if (i <= rating) Color(0xFFF2B33D) else Palette.Fog,
                    modifier = Modifier.size(34.dp).clickable(role = Role.Button) { rating = i; error = null },
                )
            }
        }
        AppTextField(comment, { comment = it.take(500) }, "How do they fit and feel?", singleLine = false)
        error?.let { Text(it, color = Palette.Sale, style = MaterialTheme.typography.bodySmall) }
        OutlineButton(
            if (mine != null) "Save review" else "Post review",
            { if (rating == 0) error = "Pick a star rating first." else vm.postReview(shoeId, rating, comment, onDone = {}, onError = { error = it }) },
            enabled = !working,
        )
    }
}
