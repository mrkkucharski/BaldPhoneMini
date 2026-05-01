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
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.pills.AddPillActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Functional tests for PillsActivity (pill/reminder list screen).
 *
 * The layout ({@code activity_reminders.xml}) contains:
 * <ul>
 *   <li>{@code R.id.bald_title_bar} — title bar at the top</li>
 *   <li>{@code R.id.recycler_view} — pill list inside a ScrollingHelper</li>
 *   <li>{@code R.id.bt_add} — add pill button in the bottom options bar</li>
 *   <li>{@code R.id.bt_time_changer} — time-slot changer in the bottom options bar</li>
 * </ul>
 *
 * Covered scenarios:
 * - Basic screen-open sanity (title bar visible).
 * - RecyclerView is rendered (even when empty).
 * - Both bottom-bar buttons are visible.
 * - Tapping "add pill" launches AddPillActivity without crashing.
 * - Tapping "time changer" does not crash.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PillsActivityFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<PillsActivity> activityRule =
            new ActivityTestRule<>(PillsActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Basic visibility checks
    // -------------------------------------------------------------------------

    /**
     * PillsActivity must display its title bar immediately after opening.
     */
    @Test
    public void pillsScreenOpens() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bald_title_bar)).check(matches(isDisplayed()));
    }

    /**
     * The pill list RecyclerView must be rendered regardless of whether any
     * reminders have been added (the ScrollingHelper shows an empty-state
     * text when the list is empty, but the child RecyclerView itself is still
     * present in the view hierarchy).
     */
    @Test
    public void pillListRecyclerViewIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()));
    }

    /**
     * The "add pill" button in the bottom bar must be visible so the user can
     * reach the AddPillActivity from this screen.
     */
    @Test
    public void addPillButtonIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_add)).check(matches(isDisplayed()));
    }

    /**
     * The time-changer button must be visible so the user can adjust when
     * pill reminders fire.
     */
    @Test
    public void timeChangerButtonIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_time_changer)).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Tapping the "add pill" button must launch AddPillActivity.
     *
     * We register an ActivityMonitor for AddPillActivity before clicking so
     * we can capture and immediately finish the launched activity, keeping the
     * test hermetic.  A 3-second timeout is used to accommodate slower emulators.
     */
    @Test
    public void addPillButtonOpensAddPillScreen() {
        activityRule.launchActivity(new Intent());
        sleep();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor =
                instrumentation.addMonitor(AddPillActivity.class.getName(), null, false);

        onView(withId(R.id.bt_add)).perform(click());

        android.app.Activity launched = monitor.waitForActivityWithTimeout(3_000);
        try {
            org.junit.Assert.assertNotNull(
                    "AddPillActivity was not launched within the timeout", launched);
        } finally {
            if (launched != null) {
                launched.finish();
            }
            instrumentation.removeMonitor(monitor);
        }
    }

    /**
     * Tapping the time-changer button must not crash the app.
     * The button opens PillTimeSetterActivity via a plain startActivity call.
     * We verify no exception propagates back to the test thread by reaching the
     * assertion after the click.
     */
    @Test
    public void timeChangerButtonDoesNotCrash() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_time_changer)).perform(click());
        sleep();

        // Reaching this line means no uncaught exception was thrown.
        // The time-setter activity may be on top; that is expected behaviour.
    }
}
