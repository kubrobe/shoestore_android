package uz.shushop.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.shushop.app.ui.theme.Palette
import uz.shushop.app.util.formatSom

// Small building blocks shared by all screens.

/** The big black pill button ("Add to Cart", "Confirm Order"). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    val active = enabled && !loading
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) Palette.Ink else Palette.Fog)
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Palette.Paper, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Palette.Paper, modifier = Modifier.size(20.dp))
                Text(text, color = if (enabled) Palette.Paper else Palette.Ash, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** A white pill with a thin outline: the quiet button next to a primary one. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val color = if (danger) Palette.Sale else Palette.Ink
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(BorderStroke(1.dp, if (danger) Palette.Sale.copy(alpha = 0.5f) else Palette.Fog), RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) color else Palette.Ash, style = MaterialTheme.typography.labelLarge)
    }
}

/** A filter chip: "All Shoes", "Men", "Price Drops"... */
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val bg = when {
        selected && dark -> Palette.Paper
        selected -> Palette.Ink
        dark -> Palette.NightCard
        else -> Palette.Mist
    }
    val fg = when {
        selected && dark -> Palette.Ink
        selected -> Palette.Paper
        dark -> Palette.NightMuted
        else -> Palette.Slate
    }
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** - 1 +  with the number between. [max] is the most the stock (or the shop's limit) allows. */
@Composable
fun QtyStepper(qty: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 1, max: Int = 10, compact: Boolean = false) {
    val size = if (compact) 32.dp else 40.dp
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
        StepButton(AppIcons.Remove, "One less", enabled = qty > min, size = size) { onChange(qty - 1) }
        Text(qty.toString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(if (compact) 20.dp else 26.dp))
        StepButton(Icons.Filled.Add, "One more", enabled = qty < max, size = size) { onChange(qty + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, if (enabled) Palette.Ink else Palette.Fog, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Palette.Ink else Palette.Ash, modifier = Modifier.size(18.dp))
    }
}

/** A text field with the app's rounded look, an error line and optional password mode. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    password: Boolean = false,
    singleLine: Boolean = true,
    onDone: (() -> Unit)? = null,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Palette.Ash) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Ink,
            unfocusedBorderColor = Palette.Fog,
            focusedLabelColor = Palette.Ink,
            cursorColor = Palette.Ink,
            focusedContainerColor = Palette.Paper,
            unfocusedContainerColor = Palette.Paper,
            errorBorderColor = Palette.Sale,
        ),
    )
}

/** "720 000 so'm" with the old price crossed out when the shoe is on sale. */
@Composable
fun PriceText(price: Long, oldPrice: Long?, modifier: Modifier = Modifier, big: Boolean = false, dark: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            formatSom(price),
            style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = if (dark) Palette.Paper else Palette.Ink,
            maxLines = 1,
        )
        if (oldPrice != null && oldPrice > price) {
            Text(
                formatSom(oldPrice),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Ash,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                modifier = Modifier.padding(bottom = if (big) 3.dp else 1.dp),
            )
        }
    }
}

/** Five stars, filled up to the rating. */
@Composable
fun Stars(rating: Double, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (rating >= i - 0.25) Color(0xFFF2B33D) else Palette.Fog,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** A round colour dot (colour picker and cart lines). */
@Composable
fun ColorDot(hex: String, modifier: Modifier = Modifier, size: Dp = 22.dp, selected: Boolean = false) {
    val color = Color(uz.shushop.app.util.ShoeColors.parse(hex) or 0xFF000000.toInt())
    Box(
        modifier
            .size(size + if (selected) 8.dp else 0.dp)
            .then(if (selected) Modifier.border(2.dp, Palette.Ink, CircleShape) else Modifier)
            .padding(if (selected) 4.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Palette.Fog, CircleShape),
    )
}

/** Big friendly message for an empty list or a screen that needs something first. */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null, dark: Boolean = false) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (dark) Palette.NightCard else Palette.Mist),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (dark) Palette.NightMuted else Palette.Slate, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = if (dark) Palette.Paper else Palette.Ink, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (dark) Palette.NightMuted else Palette.Slate, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            PrimaryButton(actionLabel, onAction, Modifier.width(220.dp))
        }
    }
}

/** A grey strip under the title: "You're offline. Showing your saved shoes." */
@Composable
fun InfoStrip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Mist)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.Slate,
    )
}

/** A heading above a group of rows. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp, bottom = 4.dp), fontWeight = FontWeight.SemiBold)
}

/** Space the floating bottom bar needs so the last list item is not hidden behind it. */
val BottomBarSpace = 104.dp

/** A light grey block with rounded corners, used for the cost breakdown and address cards. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mist)
            .padding(18.dp),
    ) { content() }
}
