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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RecentCallsFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<RecentActivity> activityRule =
            new ActivityTestRule<>(RecentActivity.class, true, false);

    @Test
    public void recentCallsScreenOpens() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()));
    }

    @Test
    public void recentCallsTitleBarIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.bald_title_bar)).check(matches(isDisplayed()));
    }
}
