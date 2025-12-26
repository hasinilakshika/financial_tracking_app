package com.example.pocketmoney

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_RES_ID = "layout_res_id"

        fun newInstance(layoutResId: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES_ID, layoutResId)
            fragment.arguments = args
            return fragment
        }
    }

    private var selectedCurrency: String = "Rs" // Default currency

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutResId = arguments?.getInt(ARG_LAYOUT_RES_ID) ?: 0
        val view = inflater.inflate(layoutResId, container, false)

        // If this is the second onboarding screen (set budget), initialize the currency selector
        if (layoutResId == R.layout.onboarding_screen_2) {
            val tvCurrency: TextView = view.findViewById(R.id.tvCurrency)
            val currencies = listOf("Rs", "$", "€", "£", "₹")

            // Set initial currency
            tvCurrency.text = selectedCurrency

            // Set up click listener to show dropdown
            tvCurrency.setOnClickListener { v ->
                val popupMenu = PopupMenu(requireContext(), v)
                currencies.forEachIndexed { index, currency ->
                    popupMenu.menu.add(0, index, 0, currency)
                }
                popupMenu.setOnMenuItemClickListener { item ->
                    selectedCurrency = currencies[item.itemId]
                    tvCurrency.text = selectedCurrency
                    Log.d("OnboardingFragment", "PopupMenu selected: currency=$selectedCurrency")
                    true
                }
                popupMenu.show()
            }
        }

        return view
    }

    // Method to get budget data from the second onboarding screen
    fun getBudgetData(): Pair<Float?, String> {
        val view = view ?: return Pair(null, "$")
        val etBudget: EditText = view.findViewById(R.id.etBudget)
        val budgetText = etBudget.text.toString()
        val budget = budgetText.toFloatOrNull()
        Log.d("OnboardingFragment", "getBudgetData: selected currency=$selectedCurrency")
        return Pair(budget, selectedCurrency)
    }
}