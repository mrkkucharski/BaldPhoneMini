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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.sms.MessagesActivity;

import com.bald.uriah.baldphone.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MessagesActivityFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<MessagesActivity> activityRule =
            new ActivityTestRule<>(MessagesActivity.class, true, false);

    @Test
    public void messagesInboxOpens() {
        activityRule.launchActivity(new Intent());
        sleep();
        // Both views are inflated simultaneously; only one is visible at a time.
        // Match the displayed one to avoid AmbiguousViewMatcherException.
        onView(anyOf(
                allOf(withId(R.id.threads_recycler_view), isDisplayed()),
                allOf(withId(R.id.empty_state_text), isDisplayed())))
                .check(matches(isDisplayed()));
    }

    @Test
    public void newMessageButtonIsDisplayed() {
        activityRule.launchActivity(new Intent());
        sleep();
        onView(withId(R.id.button_new_message)).check(matches(isDisplayed()));
    }
}
