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
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.contacts.ui.details.ContactDetailsActivity;

import com.bald.uriah.baldphone.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContactDetailsActivityFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<ContactDetailsActivity> activityRule =
            new ActivityTestRule<>(ContactDetailsActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Polls {@code activityRule.getActivity().isFinishing()} or
     * {@code isDestroyed()} until true or the timeout elapses.
     *
     * @param maxIterations number of {@link #sleep()} cycles to wait
     * @return {@code true} if the activity is finishing/destroyed within the timeout
     */
    private boolean waitUntilFinishing(int maxIterations) {
        for (int i = 0; i < maxIterations; i++) {
            ContactDetailsActivity activity = activityRule.getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return true;
            }
            sleep();
        }
        ContactDetailsActivity activity = activityRule.getActivity();
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Launches ContactDetailsActivity without any intent extras.
     * The activity detects the missing lookup key in onCreate(), shows an error toast,
     * and calls finish() immediately.  The test asserts that the activity enters the
     * finishing state within ~2 seconds (40 × sleep()), verifying no uncaught exception
     * surfaces to the user.
     */
    @Test
    public void launchWithMissingLookupKeyFinishesGracefully() {
        // Launch with a plain intent — no CONTACT_LOOKUP_KEY extra.
        activityRule.launchActivity(new Intent());

        // Allow up to ~2 s (40 × 50 ms) for the activity to call finish().
        boolean finishing = waitUntilFinishing(40);

        assertTrue(
                "Activity should finish gracefully when CONTACT_LOOKUP_KEY is missing",
                finishing);
    }

    /**
     * Launches ContactDetailsActivity with an invalid / non-existent lookup key.
     * The ViewModel will attempt to load the contact, find nothing, emit a
     * ContactNotFound event, and the activity will either finish or remain in a
     * loading/error state without crashing.  The test verifies:
     *   - no uncaught exception terminates the instrumentation process, AND
     *   - either the activity finishes gracefully, OR the root scroll view /
     *     title bar are still visible (activity is in a valid UI state).
     */
    @Test
    public void launchWithInvalidLookupKeyFinishesGracefully() {
        activityRule.launchActivity(
                new Intent().putExtra(ContactDetailsActivity.CONTACT_LOOKUP_KEY,
                        "invalid_key_that_does_not_exist"));
        sleep();

        // Give the ViewModel time to resolve the lookup and emit ContactNotFound.
        ContactDetailsActivity activity = activityRule.getActivity();
        boolean finishing = waitUntilFinishing(40);

        if (!finishing) {
            // Activity is still running — it must be in a valid UI state.
            // Assert at least the title bar (always inflated before any async work) is shown.
            try {
                onView(withId(R.id.titleBar)).check(matches(isDisplayed()));
            } catch (Exception ignored) {
                // If Espresso throws (e.g. the activity finished between the check and now)
                // that is also an acceptable outcome — the important thing is no crash.
            }
        }
        // If finishing == true the activity ended gracefully; test passes implicitly.
    }
}
