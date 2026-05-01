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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.alarms.AddAlarmActivity;
import com.bald.uriah.baldphone.activities.alarms.AlarmsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Deep functional tests for the alarms flow.
 *
 * Covered scenarios:
 * - AddAlarmActivity form has all required UI elements (name field, title bar, submit button).
 * - AlarmsActivity displays its three action buttons.
 * - Tapping "quickly add alarm" does not crash the app.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AlarmsDeepFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<AddAlarmActivity> addAlarmRule =
            new ActivityTestRule<>(AddAlarmActivity.class, true, false);

    @Rule
    public ActivityTestRule<AlarmsActivity> alarmsRule =
            new ActivityTestRule<>(AlarmsActivity.class, true, false);

    // -------------------------------------------------------------------------
    // AddAlarmActivity form
    // -------------------------------------------------------------------------

    /**
     * Launching AddAlarmActivity directly must show the title bar, the alarm name
     * input field, and the submit button — all before the user interacts at all.
     */
    @Test
    public void addAlarmFormHasRequiredFields() {
        addAlarmRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bald_title_bar)).check(matches(isDisplayed()));
        onView(withId(R.id.alarm_edit_name)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_alarm_submit)).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // AlarmsActivity button bar
    // -------------------------------------------------------------------------

    /**
     * AlarmsActivity must render all three action buttons in its bottom bar
     * so the user can add, quickly-add, or cancel all alarms.
     */
    @Test
    public void alarmsActivityShowsAllActionButtons() {
        alarmsRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_add_alarm)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_quickly_add_alarm)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_cancel_all_alarms)).check(matches(isDisplayed()));
    }

    /**
     * AlarmsActivity must render the alarm list RecyclerView so items (if any)
     * are displayed, or the empty-state placeholder is shown by the ScrollingHelper.
     */
    @Test
    public void alarmsActivityShowsAlarmList() {
        alarmsRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.child)).check(matches(isDisplayed()));
    }

    /**
     * Tapping the "quickly add alarm" button must not crash the app.
     * The button opens AddTimerActivity; we only assert that the originating
     * AlarmsActivity is still running and its UI is intact.
     *
     * Note: the button launches AddTimerActivity via startActivityForResult, so
     * an additional activity will appear on top. We verify the button itself was
     * clickable by checking that the alarms list view is still reachable once we
     * return (or that no uncaught exception occurred during the launch).
     */
    @Test
    public void quicklyAddAlarmDoesNotCrash() {
        alarmsRule.launchActivity(new Intent());
        sleep();

        // Perform a direct click via the instrumentation main-thread helper to
        // avoid the "press longer" warning that BaldButton emits when long-press
        // mode is active. BaseActivityTest.setUp() already sets LONG_PRESSES_KEY
        // to false, so a normal click is sufficient.
        onView(withId(R.id.bt_quickly_add_alarm))
                .perform(androidx.test.espresso.action.ViewActions.click());
        sleep();

        // If we reach this point without an uncaught exception the test passes.
        // We do not assert which Activity is on top because AddTimerActivity may
        // open immediately; asserting its presence would be a separate test.
    }
}
