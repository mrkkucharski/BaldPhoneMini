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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.activities.ContactsActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.AddContactActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContactsActivityFunctionalTest extends BaseActivityTest {
    @Rule
    public ActivityTestRule<ContactsActivity> activityRule =
            new ActivityTestRule<>(ContactsActivity.class, true, false);

    @Test
    public void searchForMissingContactShowsEmptyState() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.search_edit_text))
                .perform(replaceText("zzzz functional missing contact"), closeSoftKeyboard());

        waitUntilDisplayed(R.id.empty_state_text);
        onView(withId(R.id.empty_state_text)).check(matches(withText(R.string.no_contacts_found)));
    }

    @Test
    public void favoritesButtonTogglesFilterState() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.button_favorites)).perform(click());
        sleep();

        onView(withId(R.id.button_favorites)).check(matches(isChecked()));
    }

    @Test
    public void addContactButtonOpensAddContactScreen() {
        Instrumentation.ActivityMonitor monitor =
                getInstrumentation().addMonitor(AddContactActivity.class.getName(), null, false);
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.button_add_contact)).perform(click());

        Activity launched = getInstrumentation().waitForMonitorWithTimeout(monitor, 3000);
        assertNotNull(launched);
        launched.finish();
        getInstrumentation().removeMonitor(monitor);
    }

    private void waitUntilDisplayed(int viewId) {
        AssertionError lastError = null;
        for (int i = 0; i < 30; i++) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()));
                return;
            } catch (AssertionError error) {
                lastError = error;
                sleep();
            }
        }
        throw lastError;
    }
}
