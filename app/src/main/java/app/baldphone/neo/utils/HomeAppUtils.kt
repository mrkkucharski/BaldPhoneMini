package app.baldphone.neo.utils

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object HomeAppUtils {

    private val TAG = HomeAppUtils::class.java.simpleName

    /**
     * Checks if the application is the current default launcher.
     */
    @JvmStatic
    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
            Log.e(TAG, "RoleManager not available! Falling back to legacy check.")
        }

        val packageManager = context.packageManager
        val thisPackageName = context.packageName
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val resolveInfo =
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo?.activityInfo?.packageName == thisPackageName) {
            Log.d(TAG, "Our app is default launcher (system-resolved)")
            return true
        }

        Log.w(TAG, "Cannot determine if our app is default launcher, returning false.")
        return false
    }
}
