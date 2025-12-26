package com.example.pocketmoney

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var btnAddTransaction: Button
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var rvTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var prefManager: SharedPrefManager
    private var transactions = mutableListOf<Transaction>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadAndDisplayData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        prefManager = SharedPrefManager(this)

        // Check if user is logged in
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        NotificationHelper.createNotificationChannel(this)

        btnAddTransaction = findViewById(R.id.btnAddTransaction)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpenses = findViewById(R.id.tvExpenses)
        tvBalance = findViewById(R.id.tvBalance)
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
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

        btnAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("type", "Expense")
            startActivity(intent)
        }

        // Enable haptic feedback for the BottomNavigationView
        bottomNavigationView.isHapticFeedbackEnabled = true

        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                loadAndDisplayData()
            }
        } else {
            loadAndDisplayData()
        }
    }

    private fun loadAndDisplayData() {
        transactions = prefManager.getTransactions().toMutableList()
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = income - expense
        var budget = prefManager.getBudget()

        // Debug logging to check values
        Log.d("MainActivity", "Budget: $budget, Expense: $expense, Income: $income, Balance: $balance")

        // Ensure budget is positive; set a default if 0
        if (budget <= 0) {
            budget = 1000f // Default budget if not set
            Log.d("MainActivity", "Budget was 0 or negative, setting default to $budget")
        }

        tvBalance.text = "${prefManager.getCurrency()}${String.format("%.2f", balance)}"
        tvIncome.text = "${prefManager.getCurrency()}${String.format("%.2f", income)}"
        tvExpenses.text = "${prefManager.getCurrency()}${String.format("%.2f", expense)}"

        // Calculate remaining budget and update text
        val remainingBudget = budget - expense.toFloat()
        tvBudgetRemaining.text = "Remaining: ${prefManager.getCurrency()}${String.format("%.2f", remainingBudget)}"

        // Update progress bar
        budgetProgressBar.max = budget.toInt()
        budgetProgressBar.progress = expense.toInt()
        budgetProgressBar.invalidate() // Force UI refresh

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val expensePercentage = if (budget > 0) (expense.toFloat() / budget) * 100 else 0f
            Log.d("MainActivity", "Expense Percentage: $expensePercentage")
            if (expensePercentage >= 90 && expensePercentage < 100) {
                NotificationHelper.showBudgetAlert(
                    this,
                    "Budget Warning",
                    "You're approaching your budget limit! (${expensePercentage.toInt()}% used)",
                    1
                )
            } else if (expensePercentage >= 100) {
                NotificationHelper.showBudgetAlert(
                    this,
                    "Budget Exceeded",
                    "You've exceeded your budget! (${expensePercentage.toInt()}% used)",
                    2
                )
            }
        }

        updateTransactionList()
    }

    private fun updateTransactionList() {
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
        if (prefManager.isLoggedIn()) {
            loadAndDisplayData()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}