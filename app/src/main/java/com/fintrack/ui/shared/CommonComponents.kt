package com.fintrack.ui.shared
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────── Currency formatter (INR) ───────────
val inrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}

fun formatAmount(amount: Double): String = inrFormatter.format(amount)

fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))

fun formatDateShort(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM"))

// ─────────── Month name helper ───────────
val MONTHS = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
fun monthName(m: Int) = MONTHS.getOrElse(m - 1) { "?" }

// ─────────── Section Summary Card (Home screen) ───────────
@Composable
fun SummaryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = Primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = OnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = OnSurfaceVariant
            )
        }
    }
}

// ─────────── Amount display ───────────
@Composable
fun AmountText(
    amount: Double,
    isPositive: Boolean = amount >= 0,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = formatAmount(amount),
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = if (isPositive) CreditGreen else DebitRed
        )
    )
}

// ─────────── Status Chip ───────────
@Composable
fun StatusChip(
    isPaid: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bg = if (isPaid) PrimaryContainer else ErrorContainer
    val fg = if (isPaid) PaidGreen else UnpaidRed
    val label = if (isPaid) "Paid" else "Unpaid"
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = if (onClick != null) {
            modifier.clip(RoundedCornerShape(50)).clickable { onClick() }
        } else {
            modifier
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isPaid) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (isPaid) "Click to mark Unpaid" else "Click to mark Paid",
                    tint = fg,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// ─────────── Payment Mode Chip ───────────
@Composable
fun ModeChip(mode: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceContainer
    ) {
        Text(
            text = mode,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─────────── Empty State ───────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    }
}

// ─────────── Amount Input Field ───────────
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Amount",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        prefix = { Text("₹", color = OnSurfaceVariant) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
        isError = isError,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Outline,
            focusedLabelColor = Primary,
            cursorColor = Primary,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            unfocusedLabelColor = OnSurfaceVariant,
            errorBorderColor = Error
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ─────────── Date Field (tappable, shows DatePickerDialog) ───────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    label: String = "Date",
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date?.let { formatDate(it) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", tint = Primary)
            }
        },
        modifier = modifier
            .fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Outline,
            focusedLabelColor = Primary,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            unfocusedLabelColor = OnSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    )

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now())
                .toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(LocalDate.ofEpochDay(millis / 86_400_000L))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

// ─────────── Section Header ───────────
@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

// ─────────── FinTrack top-level FAB ───────────
@Composable
fun FinFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Primary,
        contentColor = OnPrimary,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add"
        )
    }
}
