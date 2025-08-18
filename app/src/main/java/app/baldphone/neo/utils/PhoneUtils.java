package app.baldphone.neo.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import java.util.Locale;

public class PhoneUtils {

    /**
     * Attempts to determine the user's current country region.
     *
     * <p>It checks for the region in the following order:
     *
     * <ol>
     *   <li>Network country ISO from TelephonyManager.
     *   <li>SIM country ISO from TelephonyManager.
     *   <li>Device's primary locale.
     * </ol>
     *
     * @param context The application context.
     * @return A two-letter uppercase country code (ISO 3166-1 alpha-2), e.g., "US".
     */
    @NonNull
    public static String getDeviceRegion(@NonNull Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            String region = tm.getNetworkCountryIso();
            if (region != null && !region.isEmpty()) {
                return region.toUpperCase(Locale.US);
            }

            // Fallback to SIM country ISO
            region = tm.getSimCountryIso();
            if (region != null && !region.isEmpty()) {
                return region.toUpperCase(Locale.US);
            }
        }

        // 2. Fallback to device locale
        Locale primaryLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            primaryLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            primaryLocale = context.getResources().getConfiguration().locale;
        }

        if (primaryLocale != null) {
            String country = primaryLocale.getCountry();
            if (!country.isEmpty()) {
                return country.toUpperCase(Locale.US);
            }
        }

        // 3. Final fallback to a default region (e.g., "US")
        return "US";
    }
}
