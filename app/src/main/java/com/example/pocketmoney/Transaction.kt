package com.example.pocketmoney

import java.io.Serializable

data class Transaction(
    val id: String,
    val type: String, // "Expense" or "Income"
    val amount: Double,
    val category: String,
    val date: String, // Formatted as "MMMM, dd yyyy"
    val time: String, // Formatted as "HH:mm"
    val categoryIcon: Int,
    val note: String? = null
) : Serializable