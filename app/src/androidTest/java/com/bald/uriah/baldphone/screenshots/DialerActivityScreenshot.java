/*
 * Copyright 2019 Uriah Shaul Mandel
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

package com.bald.uriah.baldphone.screenshots;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import app.baldphone.neo.activities.DialerActivity;

import com.bald.uriah.baldphone.R;

import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DialerActivityScreenshot extends BaseScreenshotTakerTest<DialerActivity> {

    public void test() {
        mActivityTestRule.launchActivity(new Intent());
        getInstrumentation().waitForIdleSync();
        onView(withId(R.id.b_0)).perform(click());
        onView(withId(R.id.b_5)).perform(click());
        onView(withId(R.id.b_9)).perform(click());
        onView(withId(R.id.b_4)).perform(click());
        onView(withId(R.id.b_5)).perform(click());
        onView(withId(R.id.b_0)).perform(click());
        onView(withId(R.id.b_8)).perform(click());
        onView(withId(R.id.b_1)).perform(click());
        onView(withId(R.id.b_6)).perform(click());
        onView(withId(R.id.b_0)).perform(click());
        getInstrumentation().waitForIdleSync();

    }

    @Override
    protected Class<DialerActivity> activity() {
        return DialerActivity.class;
    }
}
