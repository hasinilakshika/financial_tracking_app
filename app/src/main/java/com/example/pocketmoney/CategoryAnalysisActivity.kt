package com.example.pocketmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView

class CategoryAnalysisActivity : AppCompatActivity() {

    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var pieChart: PieChart
    private lateinit var legendLayout: LinearLayout
    private lateinit var tvHighlightedCategory: TextView
    private lateinit var tvHighlightedAmount: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPrefManager: SharedPrefManager
    private var selectedType: String = "Expense"

    private val categoryColors = mapOf(
        "Food" to Color.parseColor("#6b21ff"), // Primary blue
        "Transport" to Color.parseColor("#3668ff"), // Light blue
        "Bills" to Color.parseColor("#ff34c6"), // Medium blue
        "Entertainment" to Color.parseColor("#ff3e4a"), // Soft blue
        "Others" to Color.parseColor("#30e8fa"), // Dark blue
        "Salary" to Color.parseColor("#4CAF50"), // Green
        "Business" to Color.parseColor("#fa9130"), // Blue (distinct shade for income)
        "Investment" to Color.parseColor("#f4f825") // Amber
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_analysis)

        sharedPrefManager = SharedPrefManager(this)

        // Check if user is logged in
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        btnExpense = findViewById(R.id.btnExpense)
        btnIncome = findViewById(R.id.btnIncome)
        pieChart = findViewById(R.id.pieChart)
        legendLayout = findViewById(R.id.legendLayout)
        tvHighlightedCategory = findViewById(R.id.tvHighlightedCategory)
        tvHighlightedAmount = findViewById(R.id.tvHighlightedAmount)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        setupPieChart()

        updateStats()

        btnExpense.setOnClickListener {
            selectedType = "Expense"
            updateTabSelection(btnExpense, btnIncome)
            updateStats()
        }

        btnIncome.setOnClickListener {
            selectedType = "Income"
            updateTabSelection(btnIncome, btnExpense)
            updateStats()
        }

        bottomNavigationView.selectedItemId = R.id.nav_stats
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_stats -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, BudgetSettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 50f
        pieChart.transparentCircleRadius = 55f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.setDrawCenterText(false)
    }

    private fun updateTabSelection(selected: Button, unselected: Button) {
        selected.setBackgroundResource(R.drawable.tab_selected_background)
        selected.setTextColor(resources.getColor(android.R.color.white))
        unselected.setBackgroundResource(android.R.color.transparent)
        unselected.setTextColor(resources.getColor(R.color.primary_blue))
    }

    private fun updateStats() {
        val transactions = sharedPrefManager.getTransactions().filter { it.type == selectedType }
        Log.d("CategoryAnalysis", "Transactions for $selectedType: ${transactions.size}")

        if (transactions.isEmpty()) {
            pieChart.data = null
            pieChart.invalidate()
            legendLayout.removeAllViews()
            tvHighlightedCategory.text = ""
            tvHighlightedAmount.text = ""
            return
        }

        val categoryTotals = transactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        Log.d("CategoryAnalysis", "Category Totals: $categoryTotals")

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        categoryTotals.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
            colors.add(categoryColors[category] ?: Color.GRAY)
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = colors
        dataSet.setDrawValues(false)
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data
        pieChart.invalidate()

        updateLegend(categoryTotals)
        highlightLargestCategory(categoryTotals)
    }

    private fun updateLegend(categoryTotals: Map<String, Double>) {
        legendLayout.removeAllViews()

        val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }

        sortedCategories.chunked(4).forEach { rowCategories ->
            val rowLayout = LinearLayout(this)
            rowLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rowLayout.orientation = LinearLayout.HORIZONTAL

            for ((category, _) in rowCategories) {
                val legendView = layoutInflater.inflate(R.layout.legend_item, rowLayout, false)
                val legendColor: View = legendView.findViewById(R.id.legendColor)
                val legendName: TextView = legendView.findViewById(R.id.legendName)

                legendColor.setBackgroundColor(categoryColors[category] ?: Color.GRAY)
                legendName.text = category

                rowLayout.addView(legendView)
            }

            legendLayout.addView(rowLayout)
        }
    }

    private fun highlightLargestCategory(categoryTotals: Map<String, Double>) {
        if (categoryTotals.isEmpty()) {
            tvHighlightedCategory.text = ""
            tvHighlightedAmount.text = ""
            return
        }

        val largestCategory = categoryTotals.entries.maxByOrNull { it.value }
        val category = largestCategory?.key ?: return
        val amount = largestCategory.value

        tvHighlightedCategory.text = category
        tvHighlightedAmount.text = "${sharedPrefManager.getCurrency()}${String.format("%.2f", amount)}"
    }

    override fun onResume() {
        super.onResume()
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            updateStats()
        }
    }
}