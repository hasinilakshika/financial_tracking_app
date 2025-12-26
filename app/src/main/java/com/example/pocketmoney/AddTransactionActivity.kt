package com.example.pocketmoney

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var spCurrency: Spinner
    private lateinit var etAmount: EditText
    private lateinit var spCategory: Spinner
    private lateinit var etNote: EditText
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnDeleteTransaction: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPrefManager: SharedPrefManager
    private var selectedType = "Expense"
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var transaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        sharedPrefManager = SharedPrefManager(this)

        // Check if user is logged in
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        btnExpense = findViewById(R.id.btnExpense)
        btnIncome = findViewById(R.id.btnIncome)
        spCurrency = findViewById(R.id.spCurrency)
        etAmount = findViewById(R.id.etAmount)
        spCategory = findViewById(R.id.spCategory)
        etNote = findViewById(R.id.etNote)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        btnDeleteTransaction = findViewById(R.id.btnDeleteTransaction)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up currency spinner
        val currencies = listOf("Rs", "$", "€", "£", "₹")
        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCurrency.adapter = currencyAdapter
        spCurrency.setSelection(currencies.indexOf(sharedPrefManager.getCurrency()))

        // Set up category spinner based on type
        updateCategorySpinner(selectedType)

        // Check if editing an existing transaction
        transaction = intent.getSerializableExtra("transaction") as? Transaction
        if (transaction != null) {
            selectedType = transaction!!.type
            updateTabSelection(if (selectedType == "Expense") btnExpense else btnIncome, if (selectedType == "Expense") btnIncome else btnExpense)
            etAmount.setText(transaction!!.amount.toString())
            spCategory.setSelection((spCategory.adapter as ArrayAdapter<String>).getPosition(transaction!!.category))
            etNote.setText(transaction!!.note)
            tvDate.text = transaction!!.date
            tvTime.text = transaction!!.time
            selectedDate = transaction!!.date
            selectedTime = transaction!!.time
            btnDeleteTransaction.visibility = View.VISIBLE
        } else {
            btnDeleteTransaction.visibility = View.GONE
            selectedType = intent.getStringExtra("type") ?: "Expense"
            updateTabSelection(if (selectedType == "Expense") btnExpense else btnIncome, if (selectedType == "Expense") btnIncome else btnExpense)
            updateCategorySpinner(selectedType)
            setCurrentDateTime()
        }

        btnExpense.setOnClickListener {
            selectedType = "Expense"
            updateTabSelection(btnExpense, btnIncome)
            updateCategorySpinner(selectedType)
        }

        btnIncome.setOnClickListener {
            selectedType = "Income"
            updateTabSelection(btnIncome, btnExpense)
            updateCategorySpinner(selectedType)
        }

        tvDate.setOnClickListener {
            showDatePicker()
        }

        tvTime.setOnClickListener {
            showTimePicker()
        }

        btnDeleteTransaction.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Delete Transaction?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    transaction?.let {
                        sharedPrefManager.deleteTransaction(it.id)
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.black))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.purple_500))
            }

            dialog.show()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString()
            val note = etNote.text.toString().trim()
            val category = spCategory.selectedItem.toString()

            if (amountText.isEmpty()) {
                etAmount.error = "Amount is required"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                etAmount.error = "Please enter a valid amount"
                return@setOnClickListener
            }

            if (selectedDate == null || selectedTime == null) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the category icon using CategoryUtils
            val categoryIcon = CategoryUtils.getCategoryImage(selectedType, category)

            if (transaction != null) {
                // Update existing transaction
                val updatedTransaction = Transaction(
                    id = transaction!!.id,
                    type = selectedType,
                    amount = amount,
                    category = category,
                    date = selectedDate!!,
                    time = selectedTime!!,
                    categoryIcon = categoryIcon, // Added categoryIcon
                    note = note
                )
                sharedPrefManager.updateTransaction(updatedTransaction)
                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
            } else {
                // Add new transaction
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = selectedType,
                    amount = amount,
                    category = category,
                    date = selectedDate!!,
                    time = selectedTime!!,
                    categoryIcon = categoryIcon, // Added categoryIcon
                    note = note
                )
                sharedPrefManager.saveTransaction(newTransaction)
                Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

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
    }

    private fun updateTabSelection(selected: Button, unselected: Button) {
        selected.setBackgroundResource(R.drawable.tab_selected_background)
        selected.setTextColor(resources.getColor(android.R.color.white))
        unselected.setBackgroundResource(android.R.color.transparent)
        unselected.setTextColor(resources.getColor(R.color.primary_blue))
    }

    private fun updateCategorySpinner(type: String) {
        val categories = if (type == "Expense") {
            listOf("Food", "Transport", "Bills", "Entertainment", "Others")
        } else {
            listOf("Salary", "Business", "Investment", "Others")
        }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter
    }

    private fun setCurrentDateTime() {
        val calendar = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        selectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        tvDate.text = selectedDate
        tvTime.text = selectedTime
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                tvDate.text = selectedDate
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                tvTime.text = selectedTime
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}