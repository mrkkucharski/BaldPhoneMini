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

public final class PillTimeSlotDefaults {
    public static final int TIME_MORNING = 0;
    public static final int TIME_AFTERNOON = 1;
    public static final int TIME_EVENING = 2;
    public static final int TIME_EXTRA_1 = 3;
    public static final int TIME_EXTRA_2 = 4;
    public static final int TIME_EXTRA_3 = 5;
    public static final int PILL_TIME_SLOT_COUNT = 6;

    private static final int[] DEFAULT_HOURS = new int[]{7, 12, 19, 10, 16, 22};
    private static final int[] DEFAULT_MINUTES = new int[]{30, 30, 30, 0, 0, 0};

    private PillTimeSlotDefaults() {
    }

    public static int getDefaultHour(int slot) {
        return DEFAULT_HOURS[slot];
    }

    public static int getDefaultMinute(int slot) {
        return DEFAULT_MINUTES[slot];
    }
}
