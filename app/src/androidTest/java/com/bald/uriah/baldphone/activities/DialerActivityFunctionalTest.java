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
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.activities.DialerActivity;

import com.bald.uriah.baldphone.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DialerActivityFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<DialerActivity> activityRule =
            new ActivityTestRule<>(DialerActivity.class, true, false);

    @Test
    public void dialPadFormatsBackspacesAndClearsEnteredNumber() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.tv_number)).check(matches(withText("")));
        onView(withId(R.id.b_backspace)).check(matches(not(isEnabled())));

        onView(withId(R.id.b_1)).perform(click());
        onView(withId(R.id.b_2)).perform(click());
        onView(withId(R.id.b_3)).perform(click());

        onView(withId(R.id.tv_number)).check(matches(withDigits("123")));
        onView(withId(R.id.b_backspace)).check(matches(isEnabled()));

        onView(withId(R.id.b_backspace)).perform(click());
        onView(withId(R.id.tv_number)).check(matches(withDigits("12")));

        onView(withId(R.id.b_backspace)).perform(longClick());
        onView(withId(R.id.tv_number)).check(matches(withText("")));
        onView(withId(R.id.b_backspace)).check(matches(not(isEnabled())));
    }

    @Test
    public void longPressZeroEntersPlusPrefix() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.b_0)).perform(longClick());

        onView(withId(R.id.tv_number)).check(matches(withText("+")));
        onView(withId(R.id.b_call)).check(matches(isDisplayed()));
    }

    private Matcher<View> withDigits(String digits) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("TextView containing digits ").appendValue(digits);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof TextView)) {
                    return false;
                }
                String visibleText = ((TextView) view).getText().toString();
                return visibleText.replaceAll("\\D", "").equals(digits);
            }
        };
    }
}
