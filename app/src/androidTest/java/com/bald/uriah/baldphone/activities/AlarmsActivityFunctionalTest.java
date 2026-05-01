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
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;
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

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AlarmsActivityFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<AlarmsActivity> activityRule =
            new ActivityTestRule<>(AlarmsActivity.class, true, false);

    @Test
    public void alarmsScreenOpens() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.bt_add_alarm)).check(matches(isDisplayed()));
    }

    @Test
    public void quicklyAddAlarmButtonIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.bt_quickly_add_alarm)).check(matches(isDisplayed()));
    }

    @Test
    public void cancelAllAlarmsButtonIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.bt_cancel_all_alarms)).check(matches(isDisplayed()));
    }

    @Test
    public void addAlarmButtonOpensAddAlarmScreen() {
        Instrumentation.ActivityMonitor monitor =
                getInstrumentation().addMonitor(AddAlarmActivity.class.getName(), null, false);
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_add_alarm)).check(matches(isDisplayed())).perform(click());

        Activity launched = getInstrumentation().waitForMonitorWithTimeout(monitor, 3000);
        assertNotNull("AddAlarmActivity did not launch", launched);
        launched.finish();
        getInstrumentation().removeMonitor(monitor);
        sleep();
    }
}
