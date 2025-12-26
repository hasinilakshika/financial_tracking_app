package com.example.pocketmoney

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class BudgetSettingsActivity : AppCompatActivity() {

    private lateinit var etBudget: EditText
    private lateinit var spCurrency: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnClearData: Button
    private lateinit var btnExportData: Button
    private lateinit var btnRestoreData: Button
    private lateinit var btnLogout: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_settings)

        sharedPrefManager = SharedPrefManager(this)

        // Check if user is logged in
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        etBudget = findViewById(R.id.etBudget)
        spCurrency = findViewById(R.id.spCurrency)
        btnSave = findViewById(R.id.btnSave)
        btnClearData = findViewById(R.id.btnClearData)
        btnExportData = findViewById(R.id.btnExportData)
        btnRestoreData = findViewById(R.id.btnRestoreData)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val currencies = listOf("Rs", "$", "€", "£", "₹")
        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCurrency.adapter = currencyAdapter
        spCurrency.setSelection(currencies.indexOf(sharedPrefManager.getCurrency()))

        val currentBudget = sharedPrefManager.getBudget()
        etBudget.setText(if (currentBudget > 0) currentBudget.toString() else "")

        btnSave.setOnClickListener {
            val budgetText = etBudget.text.toString()
            if (budgetText.isEmpty()) {
                etBudget.error = "Budget cannot be empty"
                return@setOnClickListener
            }

            val budget = budgetText.toFloatOrNull()
            if (budget == null) {
                etBudget.error = "Please enter a valid budget"
                return@setOnClickListener
            }

            sharedPrefManager.saveBudget(budget)
            sharedPrefManager.saveCurrency(spCurrency.selectedItem.toString())
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        btnClearData.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Clear All Transactions?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    sharedPrefManager.clearTransactions()
                    Toast.makeText(this, "Transactions cleared", Toast.LENGTH_SHORT).show()
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

        btnExportData.setOnClickListener {
            try {
                val transactions = sharedPrefManager.getTransactions()
                if (transactions.isEmpty()) {
                    Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val gson = Gson()
                val json = gson.toJson(transactions)
                val fileName = "PocketMoney_Export_${System.currentTimeMillis()}.json"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(json)
                Toast.makeText(this, "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error exporting data", Toast.LENGTH_LONG).show()
            }
        }

        btnRestoreData.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(Intent.createChooser(intent, "Select PocketMoney Export File"), 101)
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening file picker", Toast.LENGTH_LONG).show()
            }
        }

        btnLogout.setOnClickListener {
            sharedPrefManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        bottomNavigationView.selectedItemId = R.id.nav_settings
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
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader().use { it?.readText() }
                    inputStream?.close()

                    if (json.isNullOrEmpty()) {
                        Toast.makeText(this, "Selected file is empty", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val gson = Gson()
                    val type = object : TypeToken<List<Transaction>>() {}.type
                    val transactions: List<Transaction> = gson.fromJson(json, type)
                    sharedPrefManager.restoreTransactions(transactions)
                    Toast.makeText(this, "Restored ${transactions.size} transactions", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error restoring data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!sharedPrefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            val currentBudget = sharedPrefManager.getBudget()
            etBudget.setText(if (currentBudget > 0) currentBudget.toString() else "")
            spCurrency.setSelection(
                (spCurrency.adapter as ArrayAdapter<String>).getPosition(sharedPrefManager.getCurrency())
            )
        }
    }
}