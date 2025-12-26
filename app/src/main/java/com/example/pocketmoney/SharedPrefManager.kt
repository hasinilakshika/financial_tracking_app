package com.example.pocketmoney

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPrefManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PocketMoneyPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TRANSACTIONS_KEY = "transactions"
        private const val BUDGET_KEY = "budget"
        private const val CURRENCY_KEY = "currency"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LOGGED_IN_USER = "logged_in_user"
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(transactions)
        editor.putString(TRANSACTIONS_KEY, json)
        editor.apply()
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(TRANSACTIONS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions[index] = updatedTransaction
            val editor = sharedPreferences.edit()
            val json = gson.toJson(transactions)
            editor.putString(TRANSACTIONS_KEY, json)
            editor.apply()
        }
    }

    fun deleteTransaction(transactionId: String) {
        val transactions = getTransactions().toMutableList()
        transactions.removeAll { it.id == transactionId }
        val editor = sharedPreferences.edit()
        val json = gson.toJson(transactions)
        editor.putString(TRANSACTIONS_KEY, json)
        editor.apply()
    }

    fun clearTransactions() {
        val editor = sharedPreferences.edit()
        editor.remove(TRANSACTIONS_KEY)
        editor.apply()
    }

    fun restoreTransactions(transactions: List<Transaction>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(transactions)
        editor.putString(TRANSACTIONS_KEY, json)
        editor.apply()
    }

    fun saveBudget(budget: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat(BUDGET_KEY, budget)
        editor.apply()
    }

    fun getBudget(): Float {
        return sharedPreferences.getFloat(BUDGET_KEY, 0f)
    }

    fun saveCurrency(currency: String) {
        val editor = sharedPreferences.edit()
        editor.putString(CURRENCY_KEY, currency)
        editor.apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(CURRENCY_KEY, "$") ?: "$"
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, completed)
        editor.apply()
    }

    fun setLoggedInUser(username: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_LOGGED_IN_USER, username)
        editor.apply()
    }

    fun getLoggedInUser(): String? {
        return sharedPreferences.getString(KEY_LOGGED_IN_USER, null)
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInUser() != null
    }

    fun logout() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_LOGGED_IN_USER)
        editor.apply()
    }
}