package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType(val label: String) {
    CASH("Kas"),
    BANK("Bank"),
    EWALLET("E-Wallet"),
    CREDIT_CARD("Kartu Kredit"),
    INVESTMENT("Investasi")
}

@Serializable
enum class CategoryType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

@Serializable
enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

@Serializable
enum class RecurrenceFrequency(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan")
}

enum class PeriodPreset(val label: String) {
    TODAY("Hari ini"),
    WEEK("Minggu ini"),
    MONTH("Bulan ini"),
    YEAR("Tahun ini"),
    CUSTOM("Rentang khusus")
}

enum class BudgetStatus(val label: String) {
    SAFE("Aman"),
    WARNING("Peringatan"),
    EXCEEDED("Melebihi anggaran")
}

enum class ThemeMode(val key: String, val label: String) {
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap"),
    SYSTEM("system", "Sesuai perangkat");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}
