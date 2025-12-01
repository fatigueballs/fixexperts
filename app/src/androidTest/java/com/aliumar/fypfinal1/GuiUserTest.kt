package com.aliumar.fypfinal1

import androidx.test.espresso.Espresso.onView //view
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
// import androidx.test.espresso.matcher.ViewMatchers.isDisplayed // display match
import androidx.test.espresso.action.ViewActions.*
// import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*

// import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)

// test scope
class GUIUserTest{

    @get: Rule // which activity to use
    var activityRule: ActivityScenarioRule<LoginActivity> =
        ActivityScenarioRule(LoginActivity::class.java)

/**
    @Before
    fun clearPersistentSession() {

    }
 */
    @Test // test step, for this specific test case
    fun userHomePage() {

        // --- Test Case: TC_LOG_001 (User Login Successfully) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("alifhakim.anuar@student.gmi.edu.my"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("123456"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        Thread.sleep(1000)

        onView(withId(R.id.grid_plumbing))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.grid_aircond))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.grid_electrical))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.grid_gas))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.grid_appliance))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.grid_view_all))
            .perform(click())

        onView(withId(R.id.btnCleaning))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.backButton))
            .perform(click())

        onView(withId(R.id.btnLogout))
            .perform(click())
    }
}