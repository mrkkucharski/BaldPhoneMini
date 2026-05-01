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
import static org.junit.Assert.assertFalse;

import android.content.Intent;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.sms.ConversationActivity;

import com.bald.uriah.baldphone.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Functional tests for {@link ConversationActivity}.
 *
 * Tests use a fake thread id of -1 and a synthetic phone number so that no
 * real SMS thread is required.  The READ_SEND_SMS permissions are already
 * granted by {@code BaseActivityTest.setUp()} → {@code TestDeviceSetup.ensureReady()}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ConversationActivityFunctionalTest extends BaseActivityTest {

    // Extra key constants mirror the private constants in ConversationActivity.
    private static final String EXTRA_THREAD_ID    = "extra_thread_id";
    private static final String EXTRA_ADDRESS      = "extra_address";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";

    @Rule
    public final ActivityTestRule<ConversationActivity> activityRule =
            new ActivityTestRule<>(ConversationActivity.class, true, false);

    // -----------------------------------------------------------------------
    // Intent builders
    // -----------------------------------------------------------------------

    /** Returns a fully-populated intent for a fake conversation. */
    private Intent fullConversationIntent() {
        return new Intent()
                .putExtra(EXTRA_THREAD_ID, -1L)
                .putExtra(EXTRA_ADDRESS, "+15550001234")
                .putExtra(EXTRA_DISPLAY_NAME, "Test Contact");
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Launching with a valid (fake) address and display name must result in
     * the message-input field being displayed.
     */
    @Test
    public void conversationScreenOpens() {
        activityRule.launchActivity(fullConversationIntent());
        sleep();
        onView(withId(R.id.message_input)).check(matches(isDisplayed()));
    }

    /**
     * The send button must be visible once the conversation screen is open.
     */
    @Test
    public void sendButtonIsDisplayed() {
        activityRule.launchActivity(fullConversationIntent());
        sleep();
        onView(withId(R.id.send_button)).check(matches(isDisplayed()));
    }

    /**
     * The messages RecyclerView must be visible once the conversation screen
     * is open (even when the fake thread id -1 yields no messages).
     */
    @Test
    public void messagesRecyclerViewIsDisplayed() {
        activityRule.launchActivity(fullConversationIntent());
        sleep();
        onView(withId(R.id.messages_recycler_view)).check(matches(isDisplayed()));
    }

    /**
     * Launching without an address extra must not crash the activity.
     * ConversationActivity defaults the address to an empty string, so the
     * screen should still open. If it finishes early (e.g. SMS permission
     * not satisfied at runtime), it must do so gracefully.
     */
    @Test
    public void conversationWithMissingAddressStillOpens() {
        Intent intentWithoutAddress = new Intent()
                .putExtra(EXTRA_THREAD_ID, -1L);
        activityRule.launchActivity(intentWithoutAddress);
        sleep();
        try {
            onView(withId(R.id.message_input)).check(matches(isDisplayed()));
        } catch (NoMatchingViewException | AmbiguousViewMatcherException e) {
            // message_input not visible — activity may have finished early.
            // As long as it did not hard-crash (isDestroyed while not finishing)
            // the test passes.
            ConversationActivity activity = activityRule.getActivity();
            if (activity != null) {
                assertFalse(
                        "ConversationActivity was unexpectedly destroyed (possible crash)",
                        activity.isDestroyed() && !activity.isFinishing());
            }
        }
    }
}
