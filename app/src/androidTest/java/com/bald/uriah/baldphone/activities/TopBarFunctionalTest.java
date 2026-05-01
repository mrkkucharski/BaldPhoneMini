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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// NOTE: BPrefs.TOP_BAR_COMPACT_KEY (boolean) does not exist in BPrefs.java.
// The equivalent functionality is controlled by BPrefs.HOME_TOP_BAR_CONTROLS_KEY (int),
// where HOME_TOP_BAR_CONTROLS_FULL = 0 means full mode and HOME_TOP_BAR_CONTROLS_SIMPLE = 1
// means simplified mode (flash + sound hidden). Tests that reference compact/full mode below
// use the actual HOME_TOP_BAR_CONTROLS_KEY constant accordingly.

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TopBarFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<HomeScreenActivity> activityRule =
            new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Basic visibility tests
    // -------------------------------------------------------------------------

    @Test
    public void topBarIsDisplayedOnHomeScreen() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.top_bar)).check(matches(isDisplayed()));
    }

    @Test
    public void batteryIndicatorIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.battery)).check(matches(isDisplayed()));
    }

    @Test
    public void flashlightButtonIsDisplayedInFullMode() {
        // TODO: spec requested BPrefs.TOP_BAR_COMPACT_KEY (boolean) = false for full mode,
        //       but that key does not exist. Using BPrefs.HOME_TOP_BAR_CONTROLS_KEY (int)
        //       with HOME_TOP_BAR_CONTROLS_FULL = 0 instead.
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putInt(BPrefs.HOME_TOP_BAR_CONTROLS_KEY, BPrefs.HOME_TOP_BAR_CONTROLS_FULL)
                .commit();
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.flash)).check(matches(isDisplayed()));
    }

    @Test
    public void soundButtonIsDisplayedInFullMode() {
        // TODO: spec requested BPrefs.TOP_BAR_COMPACT_KEY (boolean) = false for full mode,
        //       but that key does not exist. Using BPrefs.HOME_TOP_BAR_CONTROLS_KEY (int)
        //       with HOME_TOP_BAR_CONTROLS_FULL = 0 instead.
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putInt(BPrefs.HOME_TOP_BAR_CONTROLS_KEY, BPrefs.HOME_TOP_BAR_CONTROLS_FULL)
                .commit();
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.sound)).check(matches(isDisplayed()));
    }

    @Test
    public void notificationsBellIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.notifications)).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // Navigation test
    // -------------------------------------------------------------------------

    @Test
    public void tappingNotificationsBellOpensNotificationsScreen() {
        Instrumentation.ActivityMonitor monitor =
                getInstrumentation().addMonitor(NotificationsActivity.class.getName(), null, false);

        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.notifications)).perform(click());

        Activity launched = getInstrumentation().waitForMonitorWithTimeout(monitor, 3000);
        assertNotNull(launched);
        launched.finish();
        getInstrumentation().removeMonitor(monitor);
    }

    // -------------------------------------------------------------------------
    // Simplified / full mode tests
    //
    // TODO: spec requested BPrefs.TOP_BAR_COMPACT_KEY (boolean), which does not exist
    //       in BPrefs.java. The actual key is BPrefs.HOME_TOP_BAR_CONTROLS_KEY (int).
    //       Tests below use the real key. If BPrefs.TOP_BAR_COMPACT_KEY is ever added,
    //       update these tests to use it.
    // -------------------------------------------------------------------------

    @Test
    public void simplifiedTopBarShowsOnlyBatteryAndNotifications() {
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putInt(BPrefs.HOME_TOP_BAR_CONTROLS_KEY, BPrefs.HOME_TOP_BAR_CONTROLS_SIMPLE)
                .commit();
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.battery)).check(matches(isDisplayed()));
        onView(withId(R.id.notifications)).check(matches(isDisplayed()));
        onView(withId(R.id.flash)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.sound)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    @Test
    public void fullTopBarShowsAllFourControls() {
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putInt(BPrefs.HOME_TOP_BAR_CONTROLS_KEY, BPrefs.HOME_TOP_BAR_CONTROLS_FULL)
                .commit();
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.battery)).check(matches(isDisplayed()));
        onView(withId(R.id.flash)).check(matches(isDisplayed()));
        onView(withId(R.id.sound)).check(matches(isDisplayed()));
        onView(withId(R.id.notifications)).check(matches(isDisplayed()));
    }
}
