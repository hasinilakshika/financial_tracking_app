package com.example.pocketmoney

object CategoryUtils {
    val EXPENSE_CATEGORIES = listOf(
        "Food", "Transport", "Bills", "Entertainment", "Others"
    )

    val INCOME_CATEGORIES = listOf(
        "Salary", "Business", "Investment", "Others"
    )

    fun getCategoriesForType(type: String): List<String> {
        return when (type) {
            "Expense" -> EXPENSE_CATEGORIES
            "Income" -> INCOME_CATEGORIES
            else -> emptyList()
        }
    }

    fun getCategoryImage(type: String, category: String): Int {
        return when (type) {
            "Expense" -> when (category) {
                "Food" -> R.drawable.ic_food
                "Transport" -> R.drawable.ic_transport
                "Bills" -> R.drawable.ic_bills
                "Entertainment" -> R.drawable.ic_entertainment
                "Others" -> R.drawable.ic_other_expense
                else -> R.drawable.ic_other_expense
            }
            "Income" -> when (category) {
                "Salary" -> R.drawable.ic_salary
                "Business" -> R.drawable.ic_business
                "Investment" -> R.drawable.ic_investment
                "Others" -> R.drawable.ic_other_income
                else -> R.drawable.ic_other_income
            }
            else -> R.drawable.ic_other_expense
        }
    }
}