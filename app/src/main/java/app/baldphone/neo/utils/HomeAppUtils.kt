package app.baldphone.neo.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.bald.uriah.baldphone.activities.FakeLauncherActivity

object HomeAppUtils {

    private val TAG = HomeAppUtils::class.java.simpleName

    const val REQUEST_ROLE_HOME = 1001

    enum class DefaultLauncherRequestAction {
        NONE,
        REQUEST_HOME_ROLE,
        OPEN_LEGACY_CHOOSER
    }

    /**
     * Requests that this app be set as the default home/launcher app.
     * On Android 10+ uses RoleManager; on older versions uses the FakeLauncherActivity chooser trick.
     */
    @JvmStatic
    fun requestDefaultLauncher(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            when (
                getDefaultLauncherRequestAction(
                    isAtLeastAndroidQ = true,
                    isRoleManagerAvailable = roleManager != null,
                    isHomeRoleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
                )
            ) {
                DefaultLauncherRequestAction.NONE -> return
                DefaultLauncherRequestAction.REQUEST_HOME_ROLE -> {
                    activity.startActivityForResult(
                        roleManager!!.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_ROLE_HOME
                    )
                    return
                }
                DefaultLauncherRequestAction.OPEN_LEGACY_CHOOSER -> Unit
            }
        }
        FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(activity)
    }

    fun getDefaultLauncherRequestAction(
        isAtLeastAndroidQ: Boolean,
        isRoleManagerAvailable: Boolean,
        isHomeRoleHeld: Boolean
    ): DefaultLauncherRequestAction {
        if (!isAtLeastAndroidQ) return DefaultLauncherRequestAction.OPEN_LEGACY_CHOOSER
        if (!isRoleManagerAvailable) return DefaultLauncherRequestAction.OPEN_LEGACY_CHOOSER
        if (isHomeRoleHeld) return DefaultLauncherRequestAction.NONE
        return DefaultLauncherRequestAction.REQUEST_HOME_ROLE
    }

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
