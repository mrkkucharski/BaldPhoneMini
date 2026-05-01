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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.activities.ContactsActivity;

import com.bald.uriah.baldphone.R;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContactsDeepFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<ContactsActivity> activityRule =
            new ActivityTestRule<>(ContactsActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Polls until the view with {@code viewId} is displayed, up to 30 iterations.
     * Throws the last {@link AssertionError} if the condition is never met.
     */
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

    /**
     * Polls until the view with {@code viewId} is NOT displayed, up to 30 iterations.
     * Throws the last {@link AssertionError} if the condition is never met.
     */
    private void waitUntilNotDisplayed(int viewId) {
        AssertionError lastError = null;
        for (int i = 0; i < 30; i++) {
            try {
                onView(withId(viewId)).check(matches(Matchers.not(isDisplayed())));
                return;
            } catch (AssertionError error) {
                lastError = error;
                sleep();
            }
        }
        throw lastError;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Types a search query that matches nothing, waits for the empty-state view to appear,
     * then clears the query and verifies the empty-state view disappears (i.e. the normal
     * contacts list is restored).  On an emulator with no contacts seeded the list will be
     * empty to begin with; in that case we at minimum assert the search field is empty after
     * clearing.
     */
    @Test
    public void clearingSearchQueryRestoresContactList() {
        activityRule.launchActivity(new Intent());
        sleep();

        // Type a query that will not match any real contact.
        onView(withId(R.id.search_edit_text))
                .perform(replaceText("zzzz no match unique string"), closeSoftKeyboard());

        // Wait for the empty-state label to become visible.
        waitUntilDisplayed(R.id.empty_state_text);

        // Clear the search field.
        onView(withId(R.id.search_edit_text))
                .perform(replaceText(""), closeSoftKeyboard());

        // The empty-state label should disappear once the query is cleared.
        // If there are no contacts at all the recycler is also empty, but the
        // empty-state label is only shown during an active (non-empty) search
        // that yields no results — clearing the query should still hide it.
        waitUntilNotDisplayed(R.id.empty_state_text);
    }

    /**
     * Verifies that the search field is present and visible when ContactsActivity is launched.
     */
    @Test
    public void contactsScreenHasSearchField() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.search_edit_text)).check(matches(isDisplayed()));
    }
}
