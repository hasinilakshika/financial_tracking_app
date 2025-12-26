package com.example.pocketmoney

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNextArrow: ImageButton
    private lateinit var btnGetStarted: Button
    private lateinit var adapter: OnboardingPagerAdapter
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        sharedPrefManager = SharedPrefManager(this)

        // Check if onboarding has already been completed
        if (sharedPrefManager.isOnboardingCompleted()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewPager = findViewById(R.id.viewPagerOnboarding)
        btnNextArrow = findViewById(R.id.btnNextArrow)
        btnGetStarted = findViewById(R.id.btnGetStarted)

        // Set up ViewPager2 with fragments
        val fragments = listOf(
            OnboardingFragment.newInstance(R.layout.onboarding_screen_1),
            OnboardingFragment.newInstance(R.layout.onboarding_screen_2),
            OnboardingFragment.newInstance(R.layout.onboarding_screen_3)
        )
        adapter = OnboardingPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        // Dynamically adjust margins for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(viewPager) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarHeight = systemBars.bottom

            // Adjust margins for btnGetStarted
            btnGetStarted.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = 16 + navigationBarHeight
            }

            // Adjust margins for btnNextArrow
            btnNextArrow.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = 120 + navigationBarHeight
            }

            insets
        }

        // Update button visibility based on the current page
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == fragments.size - 1) {
                    btnNextArrow.visibility = View.GONE
                    btnGetStarted.visibility = View.VISIBLE
                } else {
                    btnNextArrow.visibility = View.VISIBLE
                    btnGetStarted.visibility = View.GONE
                }
            }
        })

        btnNextArrow.setOnClickListener {
            val nextItem = viewPager.currentItem + 1
            if (nextItem < fragments.size) {
                viewPager.currentItem = nextItem
            }
        }

        btnGetStarted.setOnClickListener {
            // Save budget and currency from the second screen
            val secondFragment = adapter.createFragment(1) as OnboardingFragment
            val (budget, currency) = secondFragment.getBudgetData()
            if (budget != null && budget > 0) {
                sharedPrefManager.saveBudget(budget)
                sharedPrefManager.saveCurrency(currency)
            } else {
                // Default budget if not set
                sharedPrefManager.saveBudget(1000f)
                sharedPrefManager.saveCurrency("$")
            }
            sharedPrefManager.setOnboardingCompleted(true)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}