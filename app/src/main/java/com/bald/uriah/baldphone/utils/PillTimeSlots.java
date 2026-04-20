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

package com.bald.uriah.baldphone.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bald.uriah.baldphone.databases.reminders.Reminder;

import java.util.Locale;

public final class PillTimeSlots {

    private PillTimeSlots() {
    }

    @NonNull
    public static CharSequence[] getSelectionLabels(@NonNull Context context) {
        CharSequence[] labels = new CharSequence[Reminder.PILL_TIME_SLOT_COUNT];
        for (int i = 0; i < labels.length; i++)
            labels[i] = getSelectionLabel(context, i);
        return labels;
    }

    @NonNull
    public static String getSelectionLabel(@NonNull Context context, @Reminder.Time int slot) {
        return getSlotLabel(context, slot) + "\n" + getTimeLabel(context, slot);
    }

    @NonNull
    public static String getListLabel(@NonNull Context context, @Reminder.Time int slot) {
        return getTimeLabel(context, slot) + "\n" + getSlotLabel(context, slot);
    }

    @NonNull
    public static String getSlotLabel(@NonNull Context context, @Reminder.Time int slot) {
        return context.getString(Reminder.getTimeNameRes(slot));
    }

    @NonNull
    public static String getTimeLabel(@NonNull Context context, @Reminder.Time int slot) {
        return formatTime(BPrefs.getHour(slot, context), BPrefs.getMinute(slot, context));
    }

    public static int compareByResolvedTime(@NonNull Context context, @NonNull Reminder first, @NonNull Reminder second) {
        return compareResolvedTime(
                BPrefs.getHour(first.getStartingTime(), context),
                BPrefs.getMinute(first.getStartingTime(), context),
                first.getTextualContent(),
                BPrefs.getHour(second.getStartingTime(), context),
                BPrefs.getMinute(second.getStartingTime(), context),
                second.getTextualContent()
        );
    }

    @NonNull
    public static String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public static int compareResolvedTime(
            int firstHour,
            int firstMinute,
            String firstTextualContent,
            int secondHour,
            int secondMinute,
            String secondTextualContent
    ) {
        int firstMinutes = minutesFromMidnight(firstHour, firstMinute);
        int secondMinutes = minutesFromMidnight(secondHour, secondMinute);
        if (firstMinutes != secondMinutes)
            return firstMinutes - secondMinutes;

        String firstName = firstTextualContent == null ? "" : firstTextualContent;
        String secondName = secondTextualContent == null ? "" : secondTextualContent;
        return firstName.compareToIgnoreCase(secondName);
    }

    private static int minutesFromMidnight(int hour, int minute) {
        return hour * 60 + minute;
    }
}
