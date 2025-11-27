package com.aliumar.fypfinal1

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class UnsuccessfulTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<LoginActivity> =
        ActivityScenarioRule(LoginActivity::class.java)


    @Test
    fun testEmptyField() {

        // --- Test Case: TC_LOG_004 (Unsuccessful Login Empty field) ---

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.tvTitle))
            .check(matches(isDisplayed()))

    }

    @Test
    fun testInvalidLoginEmail() {

        // --- Test Case: TC_LOG_005 (Unsuccessful Login, Invalid Email) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("InvalidEmail"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("1234"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.tvTitle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testInvalidLoginPassword() {

        // --- Test Case: TC_LOG_006 (Unsuccessful Login, Invalid password) ---

        onView(withId(R.id.loginEmail))
            .perform(typeText("testuser@gmail.com"), closeSoftKeyboard())

        onView(withId(R.id.loginPassword))
            .perform(typeText("invalidPassword"), closeSoftKeyboard())

        onView(withId(R.id.buttonLogin))
            .perform(click())

        onView(withId(R.id.tvTitle))
            .check(matches(isDisplayed()))
    }
}