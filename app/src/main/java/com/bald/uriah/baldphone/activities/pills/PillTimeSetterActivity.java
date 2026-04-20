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

package com.bald.uriah.baldphone.activities.pills;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.databases.reminders.ReminderScheduler;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.PillTimeSlots;
import com.bald.uriah.baldphone.views.BaldNumberChooser;
import com.bald.uriah.baldphone.views.BaldTimeSlotSelection;

public class PillTimeSetterActivity extends BaldActivity {
    private BaldNumberChooser hour, minute;
    private BaldTimeSlotSelection timeSlotSelection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_time_setter);
        hour = findViewById(R.id.chooser_hours);
        minute = findViewById(R.id.chooser_minutes);
        timeSlotSelection = findViewById(R.id.multiple_selection);
        timeSlotSelection.setSlotLabels(PillTimeSlots.getSelectionLabels(this));
        applyChoosers();

        timeSlotSelection.setOnItemClickListener(i -> applyChoosers());

        final View.OnClickListener onClickListener =
                v -> {
                    BPrefs.setHourAndMinute(
                            this,
                            timeSlotSelection.getSelection(),
                            hour.getNumber(),
                            minute.getNumber());
                    timeSlotSelection.setSlotLabels(PillTimeSlots.getSelectionLabels(this));
                };
        hour.setOnClickListener(onClickListener);
        minute.setOnClickListener(onClickListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        ReminderScheduler.reStartReminders(this);
    }

    private void applyChoosers() {
        hour.setNumber(BPrefs.getHour(timeSlotSelection.getSelection(), this));
        minute.setNumber(BPrefs.getMinute(timeSlotSelection.getSelection(), this));
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_SYSTEM_ALERT_WINDOW;
    }
}
