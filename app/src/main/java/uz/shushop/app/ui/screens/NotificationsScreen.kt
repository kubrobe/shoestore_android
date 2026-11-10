package uz.shushop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import uz.shushop.app.data.NotificationItem
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.BottomBarSpace
import uz.shushop.app.ui.components.Chip
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.ShoePicture
import uz.shushop.app.ui.components.displayColorHex
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.timeAgo

private val KIND_CHIPS = listOf("all" to "All", "price_drop" to "Price Drops", "restock" to "Restocks", "order" to "Orders")

/** The dark notification list: price drops, restocks, new drops and order updates. A blue dot marks the unread ones. */
@Composable
fun NotificationsScreen(vm: ShopViewModel, onOpenShoe: (Long) -> Unit, onOpenOrder: (Long) -> Unit) {
    val all by vm.notifications.collectAsStateWithLifecycle()
    val shoes by vm.shoes.collectAsStateWithLifecycle()
    val unread by vm.unreadCount.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf("all") }

    val shown = if (kind == "all") all else all.filter { it.kind == kind }

    Column(Modifier.fillMaxSize().background(Palette.Night).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, color = Palette.Paper, modifier = Modifier.weight(1f))
            if (unread > 0) TextButton(onClick = vm::markAllRead) { Text("Mark all read", color = Palette.NightMuted) }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(KIND_CHIPS) { (key, label) -> Chip(label, kind == key, { kind = key }, dark = true) }
        }

        if (shown.isEmpty()) {
            EmptyState(
                Icons.Filled.Notifications,
                if (all.isEmpty()) "You're all caught up" else "Nothing here",
                if (all.isEmpty()) "Price drops on saved shoes, restocks and order updates will show up here." else "Pick another tab to see the rest.",
                dark = true,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = BottomBarSpace),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shown, key = { it.id }) { n ->
                    val shoe = n.shoeId?.let { id -> shoes.firstOrNull { it.shoe.id == id } }
                    NotificationRow(n, shoe?.let { Triple(it.shoe.artStyle, it.displayColorHex(), it.shoe.id) }) {
                        vm.openNotification(n)
                        when {
                            shoe != null -> onOpenShoe(shoe.shoe.id)
                            n.orderId != null -> onOpenOrder(n.orderId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationItem, shoe: Triple<String, String?, Long>?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.NightCard)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = (if (n.isRead) "" else "Unread. ") + n.title + ". " + n.body }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Palette.NightLine), contentAlignment = Alignment.Center) {
            if (shoe != null) {
                ShoePicture(shoe.first, null, shoe.second, shoe.third, Modifier.fillMaxSize().padding(3.dp))
            } else {
                androidx.compose.material3.Icon(Icons.Filled.Notifications, contentDescription = null, tint = Palette.NightMuted, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(n.title, style = MaterialTheme.typography.titleMedium, color = Palette.Paper, maxLines = 1)
            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = Palette.NightMuted, maxLines = 2)
            Text(timeAgo(n.createdAt), style = MaterialTheme.typography.bodySmall, color = Palette.NightMuted.copy(alpha = 0.7f))
        }
        if (!n.isRead) Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.Blue))
    }
}
