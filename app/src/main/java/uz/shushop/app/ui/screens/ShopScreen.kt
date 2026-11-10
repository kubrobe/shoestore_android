package uz.shushop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.data.ShoeFilter
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.AppIcons
import uz.shushop.app.ui.components.BottomBarSpace
import uz.shushop.app.ui.components.Chip
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.InfoStrip
import uz.shushop.app.ui.components.OutlineButton
import uz.shushop.app.ui.components.PrimaryButton
import uz.shushop.app.ui.components.ShoeCard
import uz.shushop.app.ui.theme.Palette

/** The shop: search, filter, category chips and the 2-column grid of shoes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(vm: ShopViewModel, onOpenShoe: (Long) -> Unit, onOpenCart: () -> Unit) {
    val shown by vm.shown.collectAsStateWithLifecycle()
    val all by vm.shoes.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val cartCount by vm.cartCount.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Shop", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            CartButton(cartCount, onOpenCart)
        }
        if (offline) InfoStrip("You're offline. Showing the shoes saved on this phone.", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchBox(filter.query, vm::setQuery, Modifier.weight(1f))
            FilterButton(filter.activeCount) { showFilters = true }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShoeFilter.CHIPS) { (key, label) -> Chip(label, filter.category == key, { vm.setCategory(key) }) }
        }
        Spacer(Modifier.height(14.dp))

        PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (shown.isEmpty()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (all.isEmpty()) {
                        EmptyState(
                            Icons.Filled.Search, "No shoes to show yet",
                            if (offline) "Connect to the internet and pull down to load the shop." else "Pull down to load the shop.",
                            actionLabel = "Try again", onAction = vm::refresh,
                        )
                    } else {
                        EmptyState(
                            Icons.Filled.Search, "No shoes match",
                            "Try a different word, size or category.",
                            actionLabel = "Clear filters", onAction = { vm.setFilter(ShoeFilter()) },
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = BottomBarSpace),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = { it.shoe.id }) { item ->
                        ShoeCard(item, item.shoe.id in favourites, onClick = { onOpenShoe(item.shoe.id) }, onHeart = { vm.toggleFavourite(item.shoe.id) })
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(vm, onClose = { showFilters = false })
    }
}

@Composable
private fun CartButton(count: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (count > 0) "Cart, $count items" else "Cart" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(26.dp))
        if (count > 0) {
            Text(
                if (count > 9) "9+" else count.toString(),
                color = Palette.Paper,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 1.dp)
                    .clip(CircleShape)
                    .background(Palette.Ink)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Palette.Mist)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Palette.Ash, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Search shoes", color = Palette.Ash, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close, contentDescription = "Clear search", tint = Palette.Slate,
                modifier = Modifier.size(20.dp).clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Palette.Ink)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (activeCount > 0) "Filters, $activeCount on" else "Filters" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Filter, contentDescription = null, tint = Palette.Paper, modifier = Modifier.size(24.dp))
        if (activeCount > 0) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).clip(CircleShape).background(Palette.Blue))
        }
    }
}

/** Sort, size and brand. The button shows how many shoes the choices give before you close the sheet. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(vm: ShopViewModel, onClose: () -> Unit) {
    val current by vm.filter.collectAsStateWithLifecycle()
    val all by vm.shoes.collectAsStateWithLifecycle()
    val brands by vm.brands.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(current) }
    val sizes = remember(all) { all.flatMap { it.variants }.filter { it.stock > 0 }.map { it.size }.distinct().sorted() }
    val count = remember(draft, all) { draft.apply(all).size }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Palette.Paper) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Filter and sort", style = MaterialTheme.typography.titleLarge)

            Text("Sort by", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShoeFilter.SORTS.forEach { (key, label) -> Chip(label, draft.sort == key, { draft = draft.copy(sort = key) }) }
            }

            Text("Size in stock", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Any", draft.size == null, { draft = draft.copy(size = null) })
                sizes.forEach { s -> Chip(s.toString(), draft.size == s, { draft = draft.copy(size = s) }) }
            }

            if (brands.isNotEmpty()) {
                Text("Brand", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("All", draft.brand == null, { draft = draft.copy(brand = null) })
                    brands.forEach { b -> Chip(b, draft.brand == b, { draft = draft.copy(brand = b) }) }
                }
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(if (count == 1) "Show 1 shoe" else "Show $count shoes", { vm.setFilter(draft); onClose() })
            OutlineButton("Reset", { draft = draft.copy(size = null, brand = null, sort = ShoeFilter.POPULAR) })
        }
    }
}

/** The shoes you saved with the heart. */
@Composable
fun WishlistScreen(vm: ShopViewModel, onOpenShoe: (Long) -> Unit, onBrowse: () -> Unit) {
    val items by vm.wishlist.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Wishlist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
        Text(
            if (items.isEmpty()) "Shoes you save show up here." else if (items.size == 1) "1 saved pair" else "${items.size} saved pairs",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Slate,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp),
        )
        if (items.isEmpty()) {
            EmptyState(
                Icons.Filled.FavoriteBorder, "Nothing saved yet",
                "Tap the heart on a shoe and it will wait for you here, with a heads-up when its price drops.",
                actionLabel = "Browse shoes", onAction = onBrowse,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = BottomBarSpace),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.shoe.id }) { item ->
                    ShoeCard(item, item.shoe.id in favourites, onClick = { onOpenShoe(item.shoe.id) }, onHeart = { vm.toggleFavourite(item.shoe.id) })
                }
            }
        }
    }
}
