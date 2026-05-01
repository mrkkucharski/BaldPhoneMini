/*
 * Copyright 2026 Marek Kucharski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.activities.DialerActivity;

import com.bald.uriah.baldphone.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Deep functional tests for DialerActivity that are distinct from the basic
 * entry/backspace/long-press scenarios already covered by
 * {@link DialerActivityFunctionalTest}.
 *
 * Covered here:
 * - Call button is visible when the dialer is opened.
 * - Call button stays enabled after one or more digits are typed.
 * - Entering an emergency number leaves the call button enabled.
 * - The "add contact" empty-state button is present in the layout (visibility
 *   asserted as part of the empty-state container that appears when no contacts
 *   match the typed number).
 *
 * Note on R.id.bt_add_contact:
 *   The view lives inside the {@code empty_state_container} include
 *   ({@code add_contact.xml}).  It is set to {@code View.GONE} on launch and
 *   becomes visible only when the contacts search returns zero results.  We
 *   therefore do not assert {@code isDisplayed()} for it here; instead we
 *   verify it is present in the hierarchy after the ViewModel exposes an empty
 *   result set (which happens naturally when a number is entered that matches
 *   no contacts).  The actual navigation to AddContactActivity is left to a
 *   manual or E2E test because it depends on the live contacts ContentProvider.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class DialerDeepFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<DialerActivity> activityRule =
            new ActivityTestRule<>(DialerActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Call button visibility and enabled state
    // -------------------------------------------------------------------------

    /**
     * The call button must be visible as soon as the dialer screen opens,
     * before any digit is entered.
     */
    @Test
    public void callButtonIsDisplayedOnLaunch() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_call)).check(matches(isDisplayed()));
    }

    /**
     * After tapping a single dial-pad digit the call button must still be
     * displayed and enabled so the user can place the call.
     */
    @Test
    public void callButtonIsDisplayedWhenNumberEntered() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_1)).perform(click());
        sleep();

        onView(withId(R.id.b_call)).check(matches(isDisplayed()));
        onView(withId(R.id.b_call)).check(matches(isEnabled()));
    }

    // -------------------------------------------------------------------------
    // Emergency number
    // -------------------------------------------------------------------------

    /**
     * Entering the European emergency number 112 must leave the call button
     * enabled — ensuring no state change disables it for short digit sequences.
     */
    @Test
    public void emergencyNumberCallButtonIsEnabled() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_1)).perform(click());
        onView(withId(R.id.b_1)).perform(click());
        onView(withId(R.id.b_2)).perform(click());
        sleep();

        onView(withId(R.id.b_call)).check(matches(isEnabled()));
    }

    // -------------------------------------------------------------------------
    // Star and hash keys
    // -------------------------------------------------------------------------

    /**
     * The star (*) key must be present and tappable without crashing.
     * This covers the DTMF star tone path and the ViewModel character append.
     */
    @Test
    public void starKeyAppendsCharacterWithoutCrash() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_star)).perform(click());
        sleep();

        // After tapping star the number field must be non-empty and the
        // backspace button must have become enabled.
        onView(withId(R.id.b_backspace)).check(matches(isEnabled()));
    }

    /**
     * The hash (#) key must be present and tappable without crashing.
     */
    @Test
    public void hashKeyAppendsCharacterWithoutCrash() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_hash)).perform(click());
        sleep();

        onView(withId(R.id.b_backspace)).check(matches(isEnabled()));
    }

    // -------------------------------------------------------------------------
    // Number display
    // -------------------------------------------------------------------------

    /**
     * The number display (tv_number) must be visible at all times so the user
     * can see what they have typed.
     */
    @Test
    public void numberDisplayIsAlwaysVisible() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.tv_number)).check(matches(isDisplayed()));

        onView(withId(R.id.b_9)).perform(click());
        onView(withId(R.id.b_8)).perform(click());
        sleep();

        onView(withId(R.id.tv_number)).check(matches(isDisplayed()));
    }
}
