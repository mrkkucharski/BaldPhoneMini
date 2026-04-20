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

package com.bald.uriah.baldphone.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.databases.reminders.Reminder;

public class BaldTimeSlotSelection extends LinearLayout {
    private static final int SLOTS_PER_ROW = 3;

    private BaldMultipleSelection topRow;
    private BaldMultipleSelection bottomRow;
    private int selection = Reminder.TIME_MORNING;
    private OnItemClickListener onItemClickListener = whichItem -> {
    };

    public BaldTimeSlotSelection(Context context) {
        super(context);
        init();
    }

    public BaldTimeSlotSelection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaldTimeSlotSelection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    public void setSlotLabels(CharSequence[] labels) {
        if (labels.length != Reminder.PILL_TIME_SLOT_COUNT)
            throw new IllegalArgumentException("Expected " + Reminder.PILL_TIME_SLOT_COUNT + " pill time slot labels");

        removeAllViews();
        topRow = createRow();
        bottomRow = createRow();

        for (int i = 0; i < SLOTS_PER_ROW; i++)
            topRow.addSelection(labels[i]);
        for (int i = SLOTS_PER_ROW; i < Reminder.PILL_TIME_SLOT_COUNT; i++)
            bottomRow.addSelection(labels[i]);

        topRow.setOnItemClickListener(whichItem -> {
            bottomRow.clearSelection();
            selection = whichItem;
            onItemClickListener.onItemClick(selection);
        });
        bottomRow.setOnItemClickListener(whichItem -> {
            topRow.clearSelection();
            selection = SLOTS_PER_ROW + whichItem;
            onItemClickListener.onItemClick(selection);
        });

        setSelection(selection);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Reminder.Time
    public int getSelection() {
        return selection;
    }

    public void setSelection(@Reminder.Time int selection) {
        this.selection = selection;
        if (topRow == null || bottomRow == null)
            return;

        if (selection < SLOTS_PER_ROW) {
            topRow.setSelection(selection);
            bottomRow.clearSelection();
        } else {
            bottomRow.setSelection(selection - SLOTS_PER_ROW);
            topRow.clearSelection();
        }
    }

    private BaldMultipleSelection createRow() {
        BaldMultipleSelection row = new BaldMultipleSelection(getContext());
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        layoutParams.setMargins(0, margin, 0, margin);
        layoutParams.weight = 1f;
        row.setLayoutParams(layoutParams);
        addView(row);
        return row;
    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(int whichItem);
    }
}
