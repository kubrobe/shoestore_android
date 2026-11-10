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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.data.Address
import uz.shushop.app.data.AddressInput
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.AppTextField
import uz.shushop.app.ui.components.BottomBarSpace
import uz.shushop.app.ui.components.EmptyState
import uz.shushop.app.ui.components.PrimaryButton
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.Validators
import uz.shushop.app.util.initials

/** Who you are, your numbers, and the account settings. */
@Composable
fun ProfileScreen(vm: ShopViewModel, onOpenOrders: () -> Unit, onOpenAddresses: () -> Unit) {
    val user by vm.user.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    val saved by vm.favouriteIds.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf("") }          // "", "profile", "password", "server", "logout"

    val u = user
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(Palette.Ink), contentAlignment = Alignment.Center) {
                Text(initials(u?.fullName ?: ""), color = Palette.Paper, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(u?.fullName ?: "", style = MaterialTheme.typography.headlineSmall)
            Text(u?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Palette.Slate)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Orders", orders.size.toString(), Modifier.weight(1f))
            Stat("Saved pairs", saved.size.toString(), Modifier.weight(1f))
            Stat("Shoe size", u?.shoeSizeEu?.toString() ?: "-", Modifier.weight(1f))
        }

        Text("My Account", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, Palette.Fog, RoundedCornerShape(20.dp))) {
            AccountRow(Icons.Filled.ShoppingCart, "My orders", onOpenOrders)
            AccountRow(Icons.Filled.LocationOn, "Addresses", onOpenAddresses)
            AccountRow(Icons.Filled.Edit, "Edit profile") { dialog = "profile" }
            AccountRow(Icons.Filled.Lock, "Change password") { dialog = "password" }
            AccountRow(Icons.Filled.Settings, "Server") { dialog = "server" }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Palette.Sale.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .clickable(role = Role.Button) { dialog = "logout" }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Palette.Sale)
            Text("Log out", color = Palette.Sale, style = MaterialTheme.typography.labelLarge)
        }
    }

    when (dialog) {
        "profile" -> if (u != null) EditProfileDialog(vm, u.fullName, u.phone, u.shoeSizeEu) { dialog = "" }
        "password" -> PasswordDialog(vm) { dialog = "" }
        "server" -> ServerDialog(vm) { dialog = "" }
        "logout" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text("Log out?") },
            text = { Text("Your cart and orders stay safe on the server. Log in again to see them.") },
            confirmButton = { TextButton(onClick = { dialog = ""; vm.logout() }) { Text("Log out", color = Palette.Sale) } },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Stay") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Palette.Mist).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Slate)
    }
}

@Composable
private fun AccountRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Slate)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Ash)
    }
}

@Composable
private fun EditProfileDialog(vm: ShopViewModel, name0: String, phone0: String, size0: Int?, onClose: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(name0) }
    var phone by rememberSaveable { mutableStateOf(phone0) }
    var size by rememberSaveable { mutableStateOf(size0?.toString() ?: "") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val sizeNumber = size.trim().toIntOrNull()
    val nameOk = Validators.isValidName(name)
    val phoneOk = phone.isBlank() || Validators.isValidPhone(phone)
    val sizeOk = size.isBlank() || (sizeNumber != null && sizeNumber in 16..50)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppTextField(name, { name = it }, "Full name", error = if (tried && !nameOk) "Enter your full name" else null)
                AppTextField(phone, { phone = it }, "Phone", keyboardType = KeyboardType.Phone, error = if (tried && !phoneOk) "Enter a phone number, like +998 90 123 45 67" else null)
                AppTextField(size, { size = it.filter(Char::isDigit).take(2) }, "Shoe size (EU)", keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, error = if (tried && !sizeOk) "Use a size between 16 and 50" else null)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (nameOk && phoneOk && sizeOk) vm.updateProfile(name, phone, sizeNumber, onClose)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

@Composable
private fun PasswordDialog(vm: ShopViewModel, onClose: () -> Unit) {
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppTextField(current, { current = it; serverError = null }, "Current password", password = true, error = serverError)
                AppTextField(next, { next = it }, "New password", password = true, error = if (tried && !Validators.isValidPassword(next)) "Use at least 8 characters" else null)
                AppTextField(again, { again = it }, "New password again", password = true, imeAction = ImeAction.Done, error = if (tried && again != next) "The two passwords don't match" else null)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (current.isNotEmpty() && Validators.isValidPassword(next) && again == next) vm.changePassword(current, next, onClose) { serverError = it }
                else if (current.isEmpty()) serverError = "Enter your current password"
            }) { Text("Change") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

/** Your saved delivery addresses. */
@Composable
fun AddressesScreen(vm: ShopViewModel, onBack: () -> Unit) {
    val addresses by vm.addresses.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Address?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Address?>(null) }

    Column(Modifier.fillMaxSize().background(Palette.Paper)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text("Addresses", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 6.dp).weight(1f))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (addresses.isEmpty()) {
                EmptyState(Icons.Filled.LocationOn, "No addresses yet", "Add where we should deliver your shoes.")
            }
            addresses.forEach { a ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Mist).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.label + if (a.isDefault) " (default)" else "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Edit, contentDescription = "Edit ${a.label}", tint = Palette.Slate,
                            modifier = Modifier.size(40.dp).clickable(role = Role.Button) { editing = a }.padding(9.dp),
                        )
                        Icon(
                            Icons.Filled.Delete, contentDescription = "Delete ${a.label}", tint = Palette.Slate,
                            modifier = Modifier.size(40.dp).clickable(role = Role.Button) { deleting = a }.padding(9.dp),
                        )
                    }
                    Text("${a.line}, ${a.city}", style = MaterialTheme.typography.bodyMedium)
                    Text(a.phone, style = MaterialTheme.typography.bodySmall, color = Palette.Slate)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("Add address", { adding = true }, icon = Icons.Filled.Add, enabled = addresses.size < 5)
        }
    }

    if (adding) AddressDialog(vm, existing = null) { adding = false }
    editing?.let { AddressDialog(vm, existing = it) { editing = null } }
    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this address?") },
            text = { Text("${a.label}: ${a.line}, ${a.city}") },
            confirmButton = { TextButton(onClick = { deleting = null; vm.deleteAddress(a.id) }) { Text("Delete", color = Palette.Sale) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
        )
    }
}

/** Add a new address (existing = null) or change one. */
@Composable
fun AddressDialog(vm: ShopViewModel, existing: Address?, onClose: () -> Unit) {
    var label by rememberSaveable { mutableStateOf(existing?.label ?: "Home") }
    var line by rememberSaveable { mutableStateOf(existing?.line ?: "") }
    var city by rememberSaveable { mutableStateOf(existing?.city ?: "Tashkent") }
    var phone by rememberSaveable { mutableStateOf(existing?.phone ?: vm.user.value?.phone ?: "") }
    var isDefault by rememberSaveable { mutableStateOf(existing?.isDefault ?: false) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }

    val labelOk = label.trim().length in 1..30
    val lineOk = Validators.isValidAddressLine(line)
    val cityOk = city.trim().length in 2..60
    val phoneOk = Validators.isValidPhone(phone)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) "New address" else "Edit address") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppTextField(label, { label = it }, "Name (Home, Work...)", error = if (tried && !labelOk) "Give it a short name" else null)
                AppTextField(line, { line = it; serverError = null }, "Street and house", error = if (tried && !lineOk) "Enter the street and house number" else null)
                AppTextField(city, { city = it }, "City", error = if (tried && !cityOk) "Enter the city" else null)
                AppTextField(phone, { phone = it }, "Phone", keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, error = if (tried && !phoneOk) "Enter a phone number, like +998 90 123 45 67" else null)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use as my default address", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isDefault, onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.Ink, checkedThumbColor = Palette.Paper),
                    )
                }
                serverError?.let { Text(it, color = Palette.Sale, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (labelOk && lineOk && cityOk && phoneOk) {
                    vm.saveAddress(existing?.id, AddressInput(label, line, city, phone, isDefault), onDone = onClose, onError = { serverError = it })
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
