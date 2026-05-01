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

import app.baldphone.neo.activities.ContactsActivity;
import app.baldphone.neo.activities.DialerActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.views.ViewPagerHolder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeScreenFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<HomeScreenActivity> activityRule =
            new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    @Test
    public void firstPageButtonsOpenCoreCommunicationScreens() {
        activityRule.launchActivity(new Intent());
        sleep();

        launchAndFinish(R.id.bt_dialer, DialerActivity.class);
        launchAndFinish(R.id.bt_contacts, ContactsActivity.class);
        launchAndFinish(R.id.bt_recent, RecentActivity.class);
        launchAndFinish(R.id.bt_emergency, SOSActivity.class);
    }

    @Test
    public void secondPageSettingsButtonOpensSettingsScreen() {
        activityRule.launchActivity(new Intent());
        sleep();

        getInstrumentation().runOnMainSync(() -> {
            ViewPagerHolder holder = activityRule.getActivity().findViewById(R.id.view_pager_holder);
            holder.getViewPager().setCurrentItem(1, false);
        });
        sleep();

        launchAndFinish(R.id.bt_settings, SettingsActivity.class);
    }

    private void launchAndFinish(int sourceViewId, Class<? extends Activity> activityClass) {
        Instrumentation.ActivityMonitor monitor =
                getInstrumentation().addMonitor(activityClass.getName(), null, false);

        onView(withId(sourceViewId)).check(matches(isDisplayed())).perform(click());

        Activity launched = getInstrumentation().waitForMonitorWithTimeout(monitor, 3000);
        assertNotNull(launched);
        launched.finish();
        getInstrumentation().removeMonitor(monitor);
        sleep();
    }
}
