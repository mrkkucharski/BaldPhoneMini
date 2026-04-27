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

package com.bald.uriah.baldphone;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.viewpager.widget.ViewPager;

import app.baldphone.neo.contacts.speeddial.SpeedDialEntry;
import app.baldphone.neo.contacts.speeddial.SpeedDialRepository;

import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.screenshots.BaseScreenshotTakerTest;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.views.ViewPagerHolder;
import com.bald.uriah.baldphone.views.home.HomePage1;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SpeedDialHomeRowTest extends BaseScreenshotTakerTest<HomeScreenActivity> {
    private static final String PREFS_NAME = "speed_dial_prefs";

    @Override
    @Test
    public void actualTest() {
        test();
    }

    @Override
    public void test() {
        clearSpeedDialEntries();
        BPrefs.get(getInstrumentation().getTargetContext())
                .edit()
                .putString(BPrefs.CUSTOM_MESSAGES_KEY, "test/custom.messages")
                .commit();
        mActivityTestRule.launchActivity(new Intent());
        getInstrumentation().waitForIdleSync();

        assertNoEntriesState();

        setSpeedDialEntries(1);
        refreshSpeedDialOnUiThread();
        getInstrumentation().waitForIdleSync();
        assertOneEntryState();

        setSpeedDialEntries(3);
        refreshSpeedDialOnUiThread();
        getInstrumentation().waitForIdleSync();
        assertThreeEntriesState();

        setSpeedDialEntries(4);
        refreshSpeedDialOnUiThread();
        getInstrumentation().waitForIdleSync();
        assertThreeEntriesState();
    }

    @Override
    protected void cleanupAfterTest() {
        clearSpeedDialEntries();
    }

    @Override
    protected Class<HomeScreenActivity> activity() {
        return HomeScreenActivity.class;
    }

    private void assertNoEntriesState() {
        onView(withId(R.id.row_speed_dial))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        assertLayoutWeights(2.2f, 3.0f);
    }

    private void assertOneEntryState() {
        onView(withId(R.id.row_speed_dial))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.sd_slot_0))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.sd_slot_1))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.sd_slot_2))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(allOf(withId(R.id.et_name), isDescendantOfA(withId(R.id.sd_slot_0))))
                .check(matches(withText("Contact 1")));
        assertLayoutWeights(1.4f, 3.8f);
    }

    private void assertThreeEntriesState() {
        onView(withId(R.id.row_speed_dial))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        for (int i = 0; i < 3; i++) {
            int slotId = i == 0 ? R.id.sd_slot_0 : i == 1 ? R.id.sd_slot_1 : R.id.sd_slot_2;
            onView(withId(slotId))
                    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(allOf(withId(R.id.et_name), isDescendantOfA(withId(slotId))))
                    .check(matches(withText("Contact " + (i + 1))));
        }
        assertLayoutWeights(1.4f, 3.8f);
    }

    private void setSpeedDialEntries(int count) {
        clearSpeedDialEntries();
        SpeedDialRepository repository = new SpeedDialRepository(getInstrumentation().getTargetContext());
        for (int i = 1; i <= count; i++) {
            repository.add(new SpeedDialEntry(
                    "lookup-" + i,
                    "+1555000" + i,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    null,
                    "Contact " + i,
                    null
            ));
        }
    }

    private void clearSpeedDialEntries() {
        getInstrumentation()
                .getTargetContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private void refreshSpeedDialOnUiThread() {
        HomeScreenActivity activity = mActivityTestRule.getActivity();
        getInstrumentation().runOnMainSync(() -> {
            ViewPagerHolder holder = activity.findViewById(R.id.view_pager_holder);
            ViewPager viewPager = holder.getViewPager();
            for (int i = 0; i < viewPager.getChildCount(); i++) {
                View page = viewPager.getChildAt(i);
                if (page instanceof HomePage1) {
                    ((HomePage1) page).refreshSpeedDial();
                }
            }
        });
    }

    private void assertLayoutWeights(float notificationWeight, float buttonsWeight) {
        HomeScreenActivity activity = mActivityTestRule.getActivity();
        LinearLayout.LayoutParams notificationParams =
                (LinearLayout.LayoutParams) activity.findViewById(R.id.notification_area_container).getLayoutParams();
        LinearLayout.LayoutParams buttonsParams =
                (LinearLayout.LayoutParams) activity.findViewById(R.id.buttons_area).getLayoutParams();
        assertEquals(notificationWeight, notificationParams.weight, 0.01f);
        assertEquals(buttonsWeight, buttonsParams.weight, 0.01f);
    }
}
