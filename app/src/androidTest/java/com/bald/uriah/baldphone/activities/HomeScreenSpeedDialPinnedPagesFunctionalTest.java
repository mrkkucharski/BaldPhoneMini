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
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.contacts.speeddial.SpeedDialEntry;
import app.baldphone.neo.contacts.speeddial.SpeedDialRepository;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.databases.apps.App;
import com.bald.uriah.baldphone.databases.apps.AppsDatabase;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseDao;
import com.bald.uriah.baldphone.databases.apps.AppsDatabaseHelper;
import com.bald.uriah.baldphone.views.ViewPagerHolder;
import com.bald.uriah.baldphone.views.home.HomeViewFactory;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeScreenSpeedDialPinnedPagesFunctionalTest extends BaseActivityTest {
    private static final String SPEED_DIAL_PREFS_NAME = "speed_dial_prefs";
    private static final String SPEED_DIAL_NAME = "Regression Speed Dial";
    private static final String SPEED_DIAL_PHONE = "+15550001";
    private static final int PINNED_APP_COUNT_FOR_TWO_PAGES = HomeViewFactory.AMOUNT_PER_PAGE + 1;

    private Long seededRawContactId;

    @Rule
    public ActivityTestRule<HomeScreenActivity> activityRule =
            new ActivityTestRule<>(HomeScreenActivity.class, true, false);

    @After
    public void cleanupSeededData() {
        clearSpeedDialEntries();
        deleteSeededContact();
        clearPinnedApps();
    }

    @Test
    public void speedDialStillVisibleAfterNavigatingTwoPinnedPagesRightAndBack() {
        seedPinnedAppsForTwoAdditionalPages();
        seedSpeedDialEntry();

        activityRule.launchActivity(new Intent());
        waitForTwoPinnedPages();

        assertSpeedDialVisible();

        HomeScreenActivity activity = activityRule.getActivity();
        int homePage = activity.baldPagerAdapter.startingPage;
        int secondPinnedPage = homePage + 2;

        navigateToPage(secondPinnedPage);
        assertEquals(secondPinnedPage, currentPage());
        onView(allOf(withId(R.id.a00), isDisplayed())).check(matches(isDisplayed()));

        pressBack();
        sleep();

        assertEquals(homePage, currentPage());
        assertSpeedDialVisible();
    }

    private void seedPinnedAppsForTwoAdditionalPages() {
        Context context = getInstrumentation().getTargetContext();
        AppsDatabaseHelper.updateDB(context);

        AppsDatabaseDao dao = AppsDatabase.getInstance(context).appsDatabaseDao();
        clearPinnedApps(dao);

        List<App> apps = dao.getAllOrderedByABC();
        assertTrue(
                "Expected at least " + PINNED_APP_COUNT_FOR_TWO_PAGES
                        + " launchable apps to create a second pinned page",
                apps.size() >= PINNED_APP_COUNT_FOR_TWO_PAGES);

        for (int i = 0; i < PINNED_APP_COUNT_FOR_TWO_PAGES; i++) {
            dao.update(apps.get(i).getId(), true);
        }
    }

    private void seedSpeedDialEntry() {
        clearSpeedDialEntries();
        String lookupKey = insertSpeedDialContact();
        new SpeedDialRepository(getInstrumentation().getTargetContext())
                .add(new SpeedDialEntry(
                        lookupKey,
                        SPEED_DIAL_PHONE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                        null,
                        SPEED_DIAL_NAME,
                        null,
                        1L
                ));
    }

    private String insertSpeedDialContact() {
        ContentResolver resolver = getInstrumentation().getTargetContext().getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        SPEED_DIAL_NAME)
                .build());
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, SPEED_DIAL_PHONE)
                .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            ContentProviderResult[] results =
                    resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            Uri rawContactUri = results[0].uri;
            assertTrue("Raw contact insert did not return a URI", rawContactUri != null);
            seededRawContactId = ContentUris.parseId(rawContactUri);
        } catch (RemoteException | android.content.OperationApplicationException e) {
            throw new AssertionError("Failed to insert temporary speed dial contact", e);
        }

        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            String lookupKey = lookupKeyForRawContact(resolver, seededRawContactId);
            if (lookupKey != null) {
                return lookupKey;
            }
            sleep();
        }

        throw new AssertionError("Timed out waiting for temporary contact lookup key");
    }

    private String lookupKeyForRawContact(ContentResolver resolver, long rawContactId) {
        try (Cursor cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.RAW_CONTACT_ID + "=?",
                new String[]{String.valueOf(rawContactId)},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    private void deleteSeededContact() {
        Long rawContactId = seededRawContactId;
        if (rawContactId == null) return;

        getInstrumentation()
                .getTargetContext()
                .getContentResolver()
                .delete(
                        ContentUris.withAppendedId(
                                ContactsContract.RawContacts.CONTENT_URI,
                                rawContactId),
                        null,
                        null);
        seededRawContactId = null;
    }

    private void waitForTwoPinnedPages() {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            HomeScreenActivity activity = activityRule.getActivity();
            if (activity != null
                    && activity.finishedUpdatingApps
                    && activity.baldPagerAdapter != null
                    && activity.baldPagerAdapter.getCount() > activity.baldPagerAdapter.startingPage + 2) {
                getInstrumentation().waitForIdleSync();
                return;
            }
            sleep();
        }

        HomeScreenActivity activity = activityRule.getActivity();
        int count = activity == null || activity.baldPagerAdapter == null
                ? -1
                : activity.baldPagerAdapter.getCount();
        int startingPage = activity == null || activity.baldPagerAdapter == null
                ? -1
                : activity.baldPagerAdapter.startingPage;
        throw new AssertionError(
                "Timed out waiting for two pinned pages. count=" + count
                        + ", startingPage=" + startingPage);
    }

    private void navigateToPage(int page) {
        getInstrumentation().runOnMainSync(() -> {
            ViewPagerHolder holder = activityRule.getActivity().findViewById(R.id.view_pager_holder);
            holder.getViewPager().setCurrentItem(page, false);
        });
        sleep();
    }

    private int currentPage() {
        final int[] page = new int[1];
        getInstrumentation().runOnMainSync(() -> {
            ViewPagerHolder holder = activityRule.getActivity().findViewById(R.id.view_pager_holder);
            page[0] = holder.getViewPager().getCurrentItem();
        });
        return page[0];
    }

    private void assertSpeedDialVisible() {
        onView(withId(R.id.row_speed_dial))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.sd_slot_0))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(allOf(withId(R.id.et_name), isDescendantOfA(withId(R.id.sd_slot_0))))
                .check(matches(withText(SPEED_DIAL_NAME)));
    }

    private void clearSpeedDialEntries() {
        getInstrumentation()
                .getTargetContext()
                .getSharedPreferences(SPEED_DIAL_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private void clearPinnedApps() {
        clearPinnedApps(AppsDatabase
                .getInstance(getInstrumentation().getTargetContext())
                .appsDatabaseDao());
    }

    private void clearPinnedApps(AppsDatabaseDao dao) {
        for (App app : dao.getAll()) {
            dao.update(app.getId(), false);
        }
    }
}
