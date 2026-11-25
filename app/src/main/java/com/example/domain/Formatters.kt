package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

object CurrencyFormatter {
    fun format(
        amount: Double,
        currency: String = "IDR",
        localeTag: String = "id-ID"
    ): String {
        val locale = Locale.forLanguageTag(localeTag)
        val nf = NumberFormat.getCurrencyInstance(locale)
        try {
            nf.currency = java.util.Currency.getInstance(currency)
        } catch (_: Exception) {
            nf.currency = java.util.Currency.getInstance("IDR")
        }
        val fractionDigits = if (currency == "IDR") 0 else 2
        nf.maximumFractionDigits = fractionDigits
        nf.minimumFractionDigits = fractionDigits
        return nf.format(amount)
    }

    fun formatCompact(amount: Double, localeTag: String = "id-ID"): String {
        val absVal = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000 -> String.format(Locale.forLanguageTag(localeTag), "%s%.1fM", sign, absVal / 1_000_000_000.0)
            absVal >= 1_000_000 -> String.format(Locale.forLanguageTag(localeTag), "%s%.1f jt", sign, absVal / 1_000_000.0)
            absVal >= 1_000 -> String.format(Locale.forLanguageTag(localeTag), "%s%.0f rb", sign, absVal / 1_000.0)
            else -> String.format(Locale.forLanguageTag(localeTag), "%s%.0f", sign, absVal)
        }
    }

    fun parseMoney(raw: String): Double {
        val cleaned = raw.replace(Regex("[^\\d-]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

object DateFormatters {
    private val zoneId: ZoneId get() = ZoneId.systemDefault()
    private val idLocale = Locale.forLanguageTag("id-ID")
    private val fullFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", idLocale)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM yyyy", idLocale)
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val monthYearFmt = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)

    fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(fullFmt)

    fun formatShortDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(shortFmt)

    fun formatIsoDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(isoFmt)

    fun formatMonthYear(year: Int, month: Int): String =
        java.time.LocalDate.of(year, month, 1).format(monthYearFmt)
}

object PasswordHasher {
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val digest = sha256(salt, password)
        val b64Salt = Base64.getEncoder().encodeToString(salt)
        val b64Hash = Base64.getEncoder().encodeToString(digest)
        return "$b64Salt:$b64Hash"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return try {
            val salt = Base64.getDecoder().decode(parts[0])
            val expected = Base64.getDecoder().decode(parts[1])
            val actual = sha256(salt, password)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        }
    }

    private fun sha256(salt: ByteArray, password: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        var bytes = md.digest(password.toByteArray(Charsets.UTF_8))
        repeat(1000) {
            md.reset()
            md.update(salt)
            bytes = md.digest(bytes)
        }
        return bytes
    }
}
