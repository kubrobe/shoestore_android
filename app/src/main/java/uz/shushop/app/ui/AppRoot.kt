package uz.shushop.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.shushop.app.ui.components.NavTab
import uz.shushop.app.ui.components.PillBottomBar
import uz.shushop.app.ui.screens.AddressesScreen
import uz.shushop.app.ui.screens.AuthScreen
import uz.shushop.app.ui.screens.CartScreen
import uz.shushop.app.ui.screens.DetailScreen
import uz.shushop.app.ui.screens.NotificationsScreen
import uz.shushop.app.ui.screens.OrderDetailScreen
import uz.shushop.app.ui.screens.OrdersScreen
import uz.shushop.app.ui.screens.ProfileScreen
import uz.shushop.app.ui.screens.ShopScreen
import uz.shushop.app.ui.screens.WishlistScreen
import uz.shushop.app.ui.theme.Palette

/** Every screen of the app and its address. */
object Routes {
    const val SHOP = "shop"
    const val NOTIFICATIONS = "notifications"
    const val WISHLIST = "wishlist"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{id}"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val ORDER = "order/{id}"
    const val ADDRESSES = "addresses"

    fun detail(id: Long) = "detail/$id"
    fun order(id: Long) = "order/$id"
}

/**
 * Not logged in -> the login screen. Logged in -> the shop with its bottom bar.
 * Because the login state is read from the saved token, reopening the app keeps you logged in (session restore),
 * and logging out - or a login that expires - brings you straight back to the login screen.
 */
@Composable
fun AppRoot(vm: ShopViewModel) {
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    if (loggedIn) {
        // MainHost leaves the composition on logout, so a new login always starts with fresh navigation on the shop
        MainHost(vm, snackbar)
    } else {
        StatusBarIcons(darkIcons = true)
        Scaffold(containerColor = Palette.Paper, snackbarHost = { SnackbarHost(snackbar) }) { _ ->
            Box(Modifier.fillMaxSize()) { AuthScreen(vm) }
        }
    }
}

@Composable
private fun MainHost(vm: ShopViewModel, snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val unread by vm.unreadCount.collectAsStateWithLifecycle()

    val tabs = remember {
        listOf(
            NavTab(Routes.SHOP, "Home", Icons.Filled.Home),
            NavTab(Routes.NOTIFICATIONS, "Notifications", Icons.Filled.Notifications),
            NavTab(Routes.WISHLIST, "Wishlist", Icons.Filled.Favorite),
            NavTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
        )
    }
    val showBar = tabs.any { it.route == route }
    val dark = route == Routes.NOTIFICATIONS
    StatusBarIcons(darkIcons = !dark)

    fun openTab(target: String) {
        nav.navigate(target) {
            popUpTo(Routes.SHOP) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = if (dark) Palette.Night else Palette.Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                PillBottomBar(tabs, route, ::openTab, badges = if (unread > 0) setOf(Routes.NOTIFICATIONS) else emptySet(), dark = dark)
            }
        },
    ) { _ ->
        // the screens leave room for the floating bar themselves, so the content is allowed to slide under it
        NavHost(nav, startDestination = Routes.SHOP, modifier = Modifier.fillMaxSize()) {
            composable(Routes.SHOP) {
                ShopScreen(vm, onOpenShoe = { nav.navigate(Routes.detail(it)) }, onOpenCart = { nav.navigate(Routes.CART) })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(vm, onOpenShoe = { nav.navigate(Routes.detail(it)) }, onOpenOrder = { nav.navigate(Routes.order(it)) })
            }
            composable(Routes.WISHLIST) {
                WishlistScreen(vm, onOpenShoe = { nav.navigate(Routes.detail(it)) }, onBrowse = { openTab(Routes.SHOP) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(vm, onOpenOrders = { nav.navigate(Routes.ORDERS) }, onOpenAddresses = { nav.navigate(Routes.ADDRESSES) })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                DetailScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() }, onOpenCart = { nav.navigate(Routes.CART) })
            }
            composable(Routes.CART) {
                CartScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onOpenShoe = { nav.navigate(Routes.detail(it)) },
                    onPlaced = { id -> nav.navigate(Routes.order(id)) { popUpTo(Routes.CART) { inclusive = true } } },
                    onBrowse = { nav.popBackStack(Routes.SHOP, false) },
                )
            }
            composable(Routes.ORDERS) {
                OrdersScreen(vm, onBack = { nav.popBackStack() }, onOpenOrder = { nav.navigate(Routes.order(it)) }, onBrowse = { nav.popBackStack(Routes.SHOP, false) })
            }
            composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                OrderDetailScreen(vm, e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() }, onOpenShoe = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.ADDRESSES) {
                AddressesScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Dark clock and battery icons on light screens, white ones on the dark notification screen. */
@Composable
private fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
