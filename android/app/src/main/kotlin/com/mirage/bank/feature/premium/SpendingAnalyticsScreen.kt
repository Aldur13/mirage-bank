package com.mirage.bank.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirage.bank.common.util.GenericViewModelFactory
import com.mirage.bank.common.util.formatCents
import com.mirage.bank.core.di.LocalAppContainer
import com.mirage.bank.core.network.dto.CategorySpend
import com.mirage.bank.core.network.dto.MonthSpend

@Composable
fun SpendingAnalyticsScreen() {
    val container = LocalAppContainer.current
    val viewModel: SpendingAnalyticsViewModel = viewModel(
        factory = GenericViewModelFactory { SpendingAnalyticsViewModel(PremiumRepository(container.apiService)) },
    )
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.analytics != null -> {
                val analytics = state.analytics!!
                val maxCategoryTotal = analytics.byCategory.maxOfOrNull { it.totalCents } ?: 1L
                val maxMonthTotal = analytics.byMonth.maxOfOrNull { it.totalCents } ?: 1L

                LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    item {
                        Text("Spending analytics", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${formatCents(analytics.totalSpentCents)} across ${analytics.totalTransactions} transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        )
                        Text("By category", style = MaterialTheme.typography.titleMedium)
                    }
                    items(analytics.byCategory) { spend ->
                        CategoryRow(spend, maxCategoryTotal)
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("By month", style = MaterialTheme.typography.titleMedium)
                    }
                    items(analytics.byMonth) { spend ->
                        MonthRow(spend, maxMonthTotal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(spend: CategorySpend, maxTotal: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(spend.category.replaceFirstChar { it.uppercase() })
            Text("${formatCents(spend.totalCents)} · ${spend.transactionCount} txns")
        }
        LinearProgressIndicator(
            progress = (spend.totalCents.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun MonthRow(spend: MonthSpend, maxTotal: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(spend.month)
            Text("${formatCents(spend.totalCents)} · ${spend.transactionCount} txns")
        }
        LinearProgressIndicator(
            progress = (spend.totalCents.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
