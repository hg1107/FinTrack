package com.fintrack.ui.kitty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.ui.shared.*
import com.fintrack.ui.theme.*

/** Shows all monthly payment records for a single kitty member. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPaymentHistorySheet(
    member: KittyMember,
    payments: List<KittyPayment>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariant,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Payment History",
                    style = MaterialTheme.typography.headlineMedium, color = OnSurface
                )
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                // Summary chips
                val paidCount = payments.count { it.isPaid }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = PrimaryContainer) {
                        Text(
                            "$paidCount paid",
                            style = MaterialTheme.typography.labelSmall, color = PaidGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    val unpaidCount = payments.count { !it.isPaid }
                    if (unpaidCount > 0) {
                        Surface(shape = RoundedCornerShape(50), color = ErrorContainer) {
                            Text(
                                "$unpaidCount unpaid",
                                style = MaterialTheme.typography.labelSmall, color = UnpaidRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider(color = Outline.copy(alpha = 0.4f))

            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No payment records yet", color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payments.sortedByDescending { it.year * 100 + it.month }) { payment ->
                        PaymentHistoryRow(payment)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaymentHistoryRow(payment: KittyPayment) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (payment.isPaid) Surface else SurfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (payment.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (payment.isPaid) PaidGreen else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${monthName(payment.month)} ${payment.year}",
                    style = MaterialTheme.typography.titleSmall, color = OnSurface
                )
                if (payment.isPaid && payment.datePaid != null) {
                    Text(
                        "Paid on ${formatDate(payment.datePaid)}",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                    )
                }
                if (payment.paymentMode != null) {
                    Text(
                        "via ${payment.paymentMode.name}" +
                                if (payment.onlineAccountName != null) " (${payment.onlineAccountName})" else "",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                    )
                }
                if (!payment.note.isNullOrBlank()) {
                    Text(
                        payment.note,
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                    )
                }
            }
            if (payment.isPaid) {
                Text(
                    formatAmount(payment.amountPaid),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PaidGreen
                )
            } else {
                Text("Unpaid", style = MaterialTheme.typography.bodySmall, color = UnpaidRed)
            }
        }
    }
}
