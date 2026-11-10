package uz.shushop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uz.shushop.app.data.ShoeWithVariants
import uz.shushop.app.ui.theme.Palette

/** The colour a shoe is shown in on a card: the first colour that still has pairs, else the first colour. */
fun ShoeWithVariants.displayColorHex(): String? =
    (variants.firstOrNull { it.stock > 0 } ?: variants.firstOrNull())?.colorHex

/** The round white heart on a shoe picture. */
@Composable
fun HeartButton(isFavourite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Palette.Paper.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (isFavourite) "Remove from wishlist" else "Save to wishlist" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = if (isFavourite) Palette.Sale else Palette.Ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A small black label on a picture: "-19%" or "Sold out". */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Palette.Ink) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        color = Palette.Paper,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** One shoe in the 2-column grid (shop and wishlist). */
@Composable
fun ShoeCard(item: ShoeWithVariants, isFavourite: Boolean, onClick: () -> Unit, onHeart: () -> Unit, modifier: Modifier = Modifier) {
    val shoe = item.shoe
    Column(modifier.clickable(role = Role.Button, onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.Mist),
        ) {
            ShoePicture(shoe.artStyle, shoe.imageUrl, item.displayColorHex(), shoe.id, Modifier.fillMaxSize().padding(8.dp))
            when {
                !item.inStock -> Tag("Sold out", Modifier.align(Alignment.TopStart).padding(10.dp), Palette.Slate)
                shoe.onSale -> Tag("-${shoe.discountPercent}%", Modifier.align(Alignment.TopStart).padding(10.dp), Palette.Sale)
            }
            HeartButton(isFavourite, onHeart, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(shoe.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text("${shoe.brand}, ${shoe.type.lowercase()}", style = MaterialTheme.typography.bodySmall, color = Palette.Ash, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        PriceText(shoe.price, shoe.oldPrice)
    }
}

class NavTab(val route: String, val label: String, val icon: ImageVector)

/**
 * The black pill that floats at the bottom: Home, Notifications, Wishlist, Profile.
 * The open tab is a white circle; [badges] puts a blue dot on a tab (unread notifications).
 */
@Composable
fun PillBottomBar(tabs: List<NavTab>, selectedRoute: String?, onSelect: (String) -> Unit, badges: Set<String>, dark: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(if (dark) Palette.NightCard else Palette.Ink)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == selectedRoute
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(role = Role.Tab) { onSelect(tab.route) }
                        .semantics { contentDescription = tab.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(if (selected) Palette.Paper else Palette.Ink.copy(alpha = 0f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = if (selected) Palette.Ink else Palette.Paper.copy(alpha = 0.78f), modifier = Modifier.size(24.dp))
                    }
                    if (tab.route in badges) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .padding(start = 26.dp, bottom = 26.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Palette.Blue),
                        )
                    }
                }
            }
        }
    }
}
