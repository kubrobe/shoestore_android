package uz.shushop.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.components.AppTextField
import uz.shushop.app.ui.components.PrimaryButton
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.Validators

/** Login and register in one screen (the link under the button switches between them). */
@Composable
fun AuthScreen(vm: ShopViewModel) {
    var register by rememberSaveable { mutableStateOf(false) }
    var showServer by rememberSaveable { mutableStateOf(false) }
    val working by vm.working.collectAsStateWithLifecycle()
    val serverError by vm.authError.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = if (submitted && register && !Validators.isValidName(name)) "Enter your full name" else null
    val emailError = if (submitted && !Validators.isValidEmail(email)) "Enter a valid email, like name@mail.com" else null
    val phoneError = if (submitted && register && !Validators.isValidPhone(phone)) "Enter a phone number, like +998 90 123 45 67" else null
    val passwordError = when {
        !submitted -> null
        register && !Validators.isValidPassword(password) -> "Use at least 8 characters"
        !register && password.isEmpty() -> "Enter your password"
        else -> null
    }

    fun submit() {
        submitted = true
        val ok = Validators.isValidEmail(email) &&
            (if (register) Validators.isValidName(name) && Validators.isValidPhone(phone) && Validators.isValidPassword(password) else password.isNotEmpty())
        if (!ok) return
        if (register) vm.register(name, email, phone, password) else vm.login(email, password)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Shushop", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (register) "Create your account to order shoes and follow your parcels." else "Log in to see your cart, wishlist and orders.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.Slate,
        )
        Spacer(Modifier.height(10.dp))

        if (register) AppTextField(name, { name = it; vm.clearAuthError() }, "Full name", error = nameError)
        AppTextField(email, { email = it; vm.clearAuthError() }, "Email", error = emailError, keyboardType = KeyboardType.Email)
        if (register) AppTextField(phone, { phone = it; vm.clearAuthError() }, "Phone", error = phoneError, keyboardType = KeyboardType.Phone)
        AppTextField(
            password, { password = it; vm.clearAuthError() }, "Password", error = passwordError, password = true,
            imeAction = ImeAction.Done, onDone = ::submit,
        )

        serverError?.let { Text(it, color = Palette.Sale, style = MaterialTheme.typography.bodyMedium) }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(if (register) "Create account" else "Log in", ::submit, loading = working)

        Text(
            if (register) "Already have an account? Log in" else "New here? Create an account",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { register = !register; submitted = false; vm.clearAuthError() }
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )

        if (!register) {
            Text(
                "Demo account: alex@demo.uz / demo12345 (tap to fill)",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { email = "alex@demo.uz"; password = "demo12345"; vm.clearAuthError() }
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Ash,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { showServer = true }) {
                Text("Server: ${vm.serverUrl.removePrefix("https://").removePrefix("http://")}", color = Palette.Ash, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showServer) ServerDialog(vm, onClose = { showServer = false })
}

/** Type the address of the shop server (for example when the app is tested against a computer on the same Wi-Fi). */
@Composable
fun ServerDialog(vm: ShopViewModel, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(vm.serverUrl) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Shop server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The app talks to the same server as the website. Change this only if you run your own copy.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Slate,
                )
                AppTextField(text, { text = it; error = null }, "Address", error = error, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (vm.changeServer(text)) onClose() else error = "Enter a web address, like https://shushop.onrender.com" }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (vm.hasCustomServer) TextButton(onClick = { vm.resetServer(); onClose() }) { Text("Use default") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        },
    )
}
