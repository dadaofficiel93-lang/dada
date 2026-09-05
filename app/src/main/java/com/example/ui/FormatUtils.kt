package com.example.ui

import android.content.Context
import android.content.Intent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    const val SELLER_PHONE = "07 12 30 85"
    const val SELLER_EMAIL = "dadaofficiel93@gmail.com"

    private val euroFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun formatPrice(amount: Double): String {
        return try {
            euroFormat.format(amount)
        } catch (e: Exception) {
            String.format(Locale.FRANCE, "%.2f €", amount)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.FRANCE)
        return sdf.format(Date(timestamp))
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Partager via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
