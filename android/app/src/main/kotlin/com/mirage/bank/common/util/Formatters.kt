package com.mirage.bank.common.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cents -> "€12.34". Currency is hardcoded EUR server-side, so no currency-symbol lookup needed. */
fun formatCents(amountCents: Long, currency: String = "EUR"): String {
    val symbol = if (currency == "EUR") "€" else "$currency "
    val sign = if (amountCents < 0) "-" else ""
    val whole = Math.abs(amountCents) / 100
    val fraction = Math.abs(amountCents) % 100
    return "%s%s%,d.%02d".format(sign, symbol, whole, fraction)
}

private val displayFormatter = DateTimeFormatter
    .ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

/** Parses the backend's ISO 8601 UTC timestamps (e.g. "2026-07-02T14:29:00+00:00") for display. */
fun formatIsoTimestamp(iso: String): String {
    return try {
        displayFormatter.format(Instant.parse(iso))
    } catch (e: Exception) {
        iso
    }
}
