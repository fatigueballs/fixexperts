package com.aliumar.fypfinal1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class UserMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        val fab: FloatingActionButton = findViewById(R.id.fab_book)

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Make the center item (placeholder) unclickable
        bottomNavView.menu.findItem(R.id.nav_placeholder).isEnabled = false

        // Handle BottomNavigationView item clicks
        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_activity -> {
                    loadFragment(ActivityFragment())
                    true
                }
                else -> false
            }
        }

        // Handle central "BOOK" button click
        fab.setOnClickListener {
            // This launches the existing categories page as you requested
            val intent = Intent(this, UserServiceSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}