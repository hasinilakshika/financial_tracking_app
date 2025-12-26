package com.example.pocketmoney

sealed class TransactionListItem {
    data class DateHeader(val date: String, val totalAmount: Double) : TransactionListItem()
    data class TransactionItem(val transaction: Transaction) : TransactionListItem()
}