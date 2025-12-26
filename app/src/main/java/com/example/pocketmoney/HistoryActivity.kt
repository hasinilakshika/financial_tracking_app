package com.example.pocketmoney

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var prefManager: SharedPrefManager
    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_history)

        prefManager = SharedPrefManager(this)

        // Check if user is logged in
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        rvTransactions = findViewById(R.id.rvTransactions)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.isNestedScrollingEnabled = true
        transactionAdapter = TransactionAdapter(emptyList(), prefManager.getCurrency()) { transaction: Transaction ->
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("transaction", transaction)
            startActivity(intent)
        }
        rvTransactions.adapter = transactionAdapter

        // Enable haptic feedback for the BottomNavigationView
        bottomNavigationView.isHapticFeedbackEnabled = true

        bottomNavigationView.selectedItemId = R.id.nav_history
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_history -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, BudgetSettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        loadAndDisplayData()
    }

    private fun loadAndDisplayData() {
        transactions = prefManager.getTransactions().toMutableList()

        val groupedItems = mutableListOf<TransactionListItem>()
        val transactionsByDate = transactions.groupBy { it.date }

        transactionsByDate.forEach { (date, transactionsForDate) ->
            val totalAmount = transactionsForDate.sumOf { transaction ->
                if (transaction.type == "Income") transaction.amount else -transaction.amount
            }
            groupedItems.add(TransactionListItem.DateHeader(date, totalAmount))
            transactionsForDate.forEach { transaction ->
                groupedItems.add(TransactionListItem.TransactionItem(transaction))
            }
        }

        transactionAdapter.updateItems(groupedItems)

        val tvNoTransactions: TextView = findViewById(R.id.tvNoTransactions)
        if (groupedItems.isEmpty()) {
            rvTransactions.visibility = View.GONE
            tvNoTransactions.visibility = View.VISIBLE
        } else {
            rvTransactions.visibility = View.VISIBLE
            tvNoTransactions.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            loadAndDisplayData()
        }
    }
}