package com.bald.uriah.baldphone.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bald.uriah.baldphone.databases.alarms.AlarmScheduler;
import com.bald.uriah.baldphone.databases.reminders.ReminderScheduler;

/**
 * Reschedules all alarms and reminders after device reboot.
 * AlarmManager alarms are cleared on reboot; this receiver restores them.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        ReminderScheduler.reStartReminders(context);
        AlarmScheduler.reStartAlarms(context);
    }
}
