package com.bald.uriah.baldphone.utils;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helpers for safely reading values from a {@link Cursor}.
 *
 * <p>ContactsContract and MediaStore columns are routinely nullable even when the row
 * exists, and {@code getColumnIndex} returns {@code -1} when the column was not in the
 * projection. Treat every read as nullable and decide a fallback at the call site.
 */
public final class CursorUtils {

    private CursorUtils() {
    }

    /**
     * Reads a string column by name, returning {@code null} if the column is absent
     * from the cursor or the stored value is NULL.
     */
    @Nullable
    public static String getStringOrNull(@NonNull Cursor cursor, @NonNull String columnName) {
        final int idx = cursor.getColumnIndex(columnName);
        if (idx < 0) return null;
        return cursor.getString(idx);
    }

    /**
     * Reads a string column by name, returning {@code fallback} when the column is
     * absent or the stored value is NULL.
     */
    @NonNull
    public static String getStringOr(@NonNull Cursor cursor, @NonNull String columnName, @NonNull String fallback) {
        final String value = getStringOrNull(cursor, columnName);
        return value != null ? value : fallback;
    }

    /**
     * Reads an int column by name, returning {@code fallback} when the column is
     * absent from the cursor.
     */
    public static int getIntOr(@NonNull Cursor cursor, @NonNull String columnName, int fallback) {
        final int idx = cursor.getColumnIndex(columnName);
        if (idx < 0) return fallback;
        return cursor.getInt(idx);
    }
}
