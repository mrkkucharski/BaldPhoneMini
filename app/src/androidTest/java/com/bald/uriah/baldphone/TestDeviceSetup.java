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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TestDeviceSetup {
    private static final String ROLE_SMS = "android.app.role.SMS";
    private static boolean deviceConfigured = false;

    private TestDeviceSetup() {
    }

    public static synchronized void ensureReady() {
        if (deviceConfigured) {
            return;
        }

        Context context = getInstrumentation().getTargetContext();
        grantRuntimePermissions(context);
        allowSpecialAccesses(context);
        enableNotificationListener(context);
        ensureDefaultSmsApp(context);

        deviceConfigured = true;
    }

    public static synchronized void ensureDefaultSmsApp() {
        ensureReady();
    }

    private static void grantRuntimePermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        String[] permissions = new String[]{
                "android.permission.CALL_PHONE",
                "android.permission.CAMERA",
                "android.permission.READ_CALL_LOG",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_PHONE_STATE",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.RECEIVE_WAP_PUSH"
        };

        for (String permission : permissions) {
            shell("pm grant " + context.getPackageName() + " " + permission);
        }
    }

    private static void allowSpecialAccesses(Context context) {
        String packageName = context.getPackageName();
        shell("appops set " + packageName + " SYSTEM_ALERT_WINDOW allow");
        shell("appops set " + packageName + " WRITE_SETTINGS allow");
        shell("appops set " + packageName + " SCHEDULE_EXACT_ALARM allow");
    }

    private static void enableNotificationListener(Context context) {
        ComponentName listener = new ComponentName(
                context.getPackageName(),
                "com.bald.uriah.baldphone.services.NotificationListenerService"
        );
        String listeners = shell("settings get secure enabled_notification_listeners").trim();
        String listenerValue = listener.flattenToString();

        if (listeners.equals("null") || listeners.isEmpty()) {
            shell("settings put secure enabled_notification_listeners " + listenerValue);
            return;
        }

        if (!listeners.contains(listenerValue)) {
            shell("settings put secure enabled_notification_listeners " + listeners + ":" + listenerValue);
        }
    }

    private static void ensureDefaultSmsApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String holders = shell("cmd role get-role-holders " + ROLE_SMS);
            if (!holders.contains(context.getPackageName())) {
                shell("cmd role set-bypassing-role-qualification true");
                shell("cmd role add-role-holder " + ROLE_SMS + " " + context.getPackageName() + " 0");
                holders = shell("cmd role get-role-holders " + ROLE_SMS);
                if (!holders.contains(context.getPackageName())) {
                    throw new AssertionError("Could not set default SMS app. Current holders: " + holders);
                }
            }
        }
    }

    private static String shell(String command) {
        ParcelFileDescriptor descriptor =
                getInstrumentation().getUiAutomation().executeShellCommand(command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ParcelFileDescriptor.AutoCloseInputStream(descriptor),
                StandardCharsets.UTF_8
        ))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString();
        } catch (IOException ignored) {
            return "";
        }
    }
}
