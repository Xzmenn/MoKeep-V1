package com.example.domain

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.AccountEntity
import com.example.data.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId

object CsvAndBackupEngine {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val csvHeader = "tanggal,jenis,jumlah,akun,akun_tujuan,kategori,subkategori,catatan,tag"

    fun templateCsv(): String {
        return "$csvHeader\n2026-01-15,pengeluaran,50000,Kas,,Makan & Minum,Kopi,Kopi pagi,rutin|kopi\n" +
            "2026-01-25,pemasukan,10000000,BCA,,Gaji,,Gaji bulanan,rutin\n" +
            "2026-01-26,transfer,500000,BCA,GoPay,Transfer,,Isi saldo GoPay,\n"
    }

    fun exportTransactionsCsv(transactions: List<TransactionDetail>): String {
        val sb = StringBuilder()
        sb.append(csvHeader).append("\n")
        for (item in transactions) {
            val tx = item.transaction
            val date = DateFormatters.formatIsoDate(tx.date)
            val jenis = when (tx.type) {
                TransactionType.INCOME -> "pemasukan"
                TransactionType.EXPENSE -> "pengeluaran"
                TransactionType.TRANSFER -> "transfer"
            }
            val amount = tx.amount.toLong().toString()
            val akun = escapeCsv(item.account.name)
            val akunTujuan = escapeCsv(item.toAccount?.name.orEmpty())
            val kategori = escapeCsv(item.category?.name.orEmpty())
            val subKategori = escapeCsv(item.subCategory?.name.orEmpty())
            val catatan = escapeCsv(tx.note.orEmpty())
            val tag = escapeCsv(item.tags.joinToString("|") { it.name })
            sb.append("$date,$jenis,$amount,$akun,$akunTujuan,$kategori,$subKategori,$catatan,$tag\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun parseCsv(content: String, accounts: List<AccountEntity>): ImportPreviewResult {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportPreviewResult(
                total = 0,
                validRows = emptyList(),
                importedCount = null,
                failed = 1,
                errors = listOf(ImportRowError(1, "File CSV kosong"))
            )
        }
        val headers = splitCsvLine(lines.first()).map { it.trim().lowercase() }
        val expected = listOf("tanggal", "jenis", "jumlah", "akun", "akun_tujuan", "kategori", "subkategori", "catatan", "tag")
        if (headers != expected) {
            return ImportPreviewResult(
                total = 0,
                validRows = emptyList(),
                importedCount = null,
                failed = 1,
                errors = listOf(
                    ImportRowError(
                        1,
                        "Header CSV harus: tanggal,jenis,jumlah,akun,akun_tujuan,kategori,subkategori,catatan,tag"
                    )
                )
            )
        }

        val accountMap = accounts.associateBy { it.name.trim().lowercase() }
        val valid = mutableListOf<ParsedImportRow>()
        val errors = mutableListOf<ImportRowError>()

        lines.drop(1).forEachIndexed { idx, line ->
            val rowNum = idx + 2
            val cols = splitCsvLine(line)
            fun col(i: Int) = cols.getOrNull(i)?.trim().orEmpty()

            val tanggal = col(0)
            val jenis = col(1).lowercase()
            val jumlahRaw = col(2).replace(",", ".")
            val akunName = col(3)
            val akunTujuanName = col(4)
            val kategori = col(5)
            val subKategori = col(6)
            val catatan = col(7)
            val tagRaw = col(8)

            val dateMillis = parseImportDate(tanggal)
            if (dateMillis == null) {
                errors.add(ImportRowError(rowNum, "Tanggal harus YYYY-MM-DD atau DD/MM/YYYY"))
                return@forEachIndexed
            }

            val txType = when (jenis) {
                "pemasukan", "income" -> TransactionType.INCOME
                "pengeluaran", "expense" -> TransactionType.EXPENSE
                "transfer" -> TransactionType.TRANSFER
                else -> null
            }
            if (txType == null) {
                errors.add(ImportRowError(rowNum, "Jenis harus pemasukan, pengeluaran, atau transfer"))
                return@forEachIndexed
            }

            val amount = jumlahRaw.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                errors.add(ImportRowError(rowNum, "Jumlah harus lebih besar dari nol"))
                return@forEachIndexed
            }

            val account = accountMap[akunName.lowercase()]
            if (account == null) {
                errors.add(ImportRowError(rowNum, "Akun \"$akunName\" tidak ditemukan"))
                return@forEachIndexed
            }

            val toAccount = if (txType == TransactionType.TRANSFER) {
                accountMap[akunTujuanName.lowercase()]
            } else null

            if (txType == TransactionType.TRANSFER && toAccount == null) {
                errors.add(ImportRowError(rowNum, "Akun tujuan \"$akunTujuanName\" tidak ditemukan"))
                return@forEachIndexed
            }
            if (txType == TransactionType.TRANSFER && toAccount?.id == account.id) {
                errors.add(ImportRowError(rowNum, "Akun tujuan transfer harus berbeda dari akun asal"))
                return@forEachIndexed
            }
            if (txType != TransactionType.TRANSFER && kategori.isBlank()) {
                errors.add(ImportRowError(rowNum, "Kategori wajib diisi"))
                return@forEachIndexed
            }

            val tags = tagRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            valid.add(
                ParsedImportRow(
                    row = rowNum,
                    type = txType,
                    amount = amount,
                    date = dateMillis,
                    accountId = account.id,
                    toAccountId = toAccount?.id,
                    categoryName = kategori,
                    subCategoryName = subKategori,
                    note = catatan,
                    tags = tags
                )
            )
        }

        return ImportPreviewResult(
            total = lines.size - 1,
            validRows = valid,
            importedCount = null,
            failed = errors.size,
            errors = errors
        )
    }

    private fun parseImportDate(value: String): Long? {
        val iso = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(value)
        val local = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").matchEntire(value)
        return try {
            val ld = when {
                iso != null -> LocalDate.of(
                    iso.groupValues[1].toInt(),
                    iso.groupValues[2].toInt(),
                    iso.groupValues[3].toInt()
                )
                local != null -> LocalDate.of(
                    local.groupValues[3].toInt(),
                    local.groupValues[2].toInt(),
                    local.groupValues[1].toInt()
                )
                else -> return null
            }
            ld.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun encodeBackup(payload: BackupPayload): String = json.encodeToString(payload)

    fun decodeBackup(rawJson: String): BackupPayload = json.decodeFromString(rawJson)

    fun generateReportPdfBytes(
        transactions: List<TransactionDetail>,
        fromMillis: Long?,
        toMillis: Long?,
        currency: String,
        locale: String
    ): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F766E")
            textSize = 18f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 44f
        canvas.drawText("Laporan Keuangan MoKeep", 36f, y, titlePaint)
        y += 20f
        val periodText = if (fromMillis != null && toMillis != null) {
            "Periode: ${DateFormatters.formatIsoDate(fromMillis)} - ${DateFormatters.formatIsoDate(toMillis)}"
        } else {
            "Periode: Semua periode"
        }
        canvas.drawText(periodText, 36f, y, bodyPaint)
        y += 24f

        val income = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        val expense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        canvas.drawText("Ringkasan", 36f, y, headingPaint)
        y += 16f
        canvas.drawText("Pemasukan: ${CurrencyFormatter.format(income, currency, locale)}", 36f, y, bodyPaint)
        y += 14f
        canvas.drawText("Pengeluaran: ${CurrencyFormatter.format(expense, currency, locale)}", 36f, y, bodyPaint)
        y += 14f
        canvas.drawText("Arus kas bersih: ${CurrencyFormatter.format(income - expense, currency, locale)}", 36f, y, bodyPaint)
        y += 14f
        canvas.drawText("Jumlah transaksi: ${transactions.size}", 36f, y, bodyPaint)
        y += 22f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 18f

        canvas.drawText("Daftar Transaksi", 36f, y, headingPaint)
        y += 16f
        canvas.drawText("Tanggal", 36f, y, headingPaint.apply { textSize = 10f })
        canvas.drawText("Jenis", 115f, y, headingPaint)
        canvas.drawText("Kategori", 195f, y, headingPaint)
        canvas.drawText("Akun", 325f, y, headingPaint)
        canvas.drawText("Jumlah", 455f, y, headingPaint)
        y += 10f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 14f

        for (item in transactions) {
            if (y > pageHeight - 48) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
            val tx = item.transaction
            canvas.drawText(DateFormatters.formatIsoDate(tx.date), 36f, y, bodyPaint)
            canvas.drawText(tx.type.label, 115f, y, bodyPaint)
            canvas.drawText((item.category?.name ?: "-").take(20), 195f, y, bodyPaint)
            canvas.drawText(item.account.name.take(18), 325f, y, bodyPaint)
            canvas.drawText(CurrencyFormatter.format(tx.amount, currency, locale), 455f, y, bodyPaint)
            y += 15f
        }

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
