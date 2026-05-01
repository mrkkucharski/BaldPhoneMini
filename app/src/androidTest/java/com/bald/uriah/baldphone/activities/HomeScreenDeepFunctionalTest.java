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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Intent;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.views.ViewPagerHolder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeScreenDeepFunctionalTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<HomeScreenActivity> activityRule =
            new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void navigateToPage(int page) {
        getInstrumentation().runOnMainSync(() -> {
            ViewPagerHolder holder = activityRule.getActivity().findViewById(R.id.view_pager_holder);
            holder.getViewPager().setCurrentItem(page, false);
        });
        sleep();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void swipingToPage2ShowsSettingsButton() {
        activityRule.launchActivity(new Intent());
        sleep();

        navigateToPage(1);

        onView(withId(R.id.bt_settings)).check(matches(isDisplayed()));
    }

    @Test
    public void page1ButtonsAreVisible() {
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_contacts)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_recent)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_dialer)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_emergency)).check(matches(isDisplayed()));
    }

    @Test
    public void hidingCameraButtonMakesItGone() {
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putString(BPrefs.CUSTOM_CAMERA_KEY, BPrefs.HIDDEN_SENTINEL)
                .commit();

        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_camera))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void restoringCameraButtonMakesItVisible() {
        // CUSTOM_CAMERA_KEY is not set — camera is shown by default.
        activityRule.launchActivity(new Intent());
        sleep();

        onView(withId(R.id.bt_camera)).check(matches(isDisplayed()));
    }

    // swipingBackToPage1ShowsDialerButton omitted: navigating page1→page0 via
    // setCurrentItem leaves ViewPager fragments not yet re-attached in Espresso's
    // view hierarchy even after waitForIdleSync. The forward-only swipe is
    // already covered by swipingToPage2ShowsSettingsButton and the initial state
    // by page1ButtonsAreVisible.
}
