package com.aliumar.fypfinal1

import androidx.test.espresso.Espresso.onView //view
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed // display match
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*

import org.junit.Before
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
class SuccessTest {

    @get: Rule // which activity to use
    var activityRule: ActivityScenarioRule<LoginActivity> =
        ActivityScenarioRule(LoginActivity::class.java)

    @Before
    fun clearPersistentSession() {

    }

    @Test // test step, for this specific test case
    fun testSuccessfulUserLogin() {

        // --- Test Case: TC_LOG_001 (User Login Successfully) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("testuser@gmail.com"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("1234"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.etSearch))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnLogout))
            .perform(click())
    }

    @Test // test step, for this specific test case
    fun testSuccessfulRMLogin() {

        // --- Test Case: TC_LOG_002 (Repair Man Login Successfully) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("testrp@gmail.com"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("1234"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.tvWelcomeGreeting))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnLogout))
            .perform(click())
    }

    @Test // test step, for this specific test case
    fun testSuccessfulAdminLogin() {

        // --- Test Case: TC_LOG_003 (Admin Login Successfully) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("admin@fixexperts.com"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("admin123"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.tvAdminTitle))
            .check(matches(isDisplayed()))

        onView(withId(R.id.backButton))
            .perform(click())
    }
}