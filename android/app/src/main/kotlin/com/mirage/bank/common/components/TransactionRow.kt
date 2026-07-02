package com.mirage.bank.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mirage.bank.core.network.dto.TransactionItem
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.common.util.formatIsoTimestamp

/** Reused by personal/guardian/admin transaction lists so direction-coloring stays consistent everywhere. */
@Composable
fun TransactionRow(transaction: TransactionItem, currency: String = "EUR") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(transaction.description, style = MaterialTheme.typography.bodyLarge)
            Text(formatIsoTimestamp(transaction.timestamp), style = MaterialTheme.typography.bodySmall)
        }
        val isCredit = transaction.direction == "credit"
        val amountText = (if (isCredit) "+" else "-") + formatCents(transaction.amountCents, currency)
        Text(
            amountText,
            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
