package app.baldphone.neo.utils

import app.baldphone.neo.utils.HomeAppUtils.DefaultLauncherRequestAction.NONE
import app.baldphone.neo.utils.HomeAppUtils.DefaultLauncherRequestAction.OPEN_LEGACY_CHOOSER
import app.baldphone.neo.utils.HomeAppUtils.DefaultLauncherRequestAction.REQUEST_HOME_ROLE
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAppUtilsTest {
    @Test
    fun defaultLauncherRequestActionUsesLegacyChooserBeforeAndroidQ() {
        assertEquals(
            OPEN_LEGACY_CHOOSER,
            HomeAppUtils.getDefaultLauncherRequestAction(
                isAtLeastAndroidQ = false,
                isRoleManagerAvailable = false,
                isHomeRoleHeld = false
            )
        )
    }

    @Test
    fun defaultLauncherRequestActionUsesLegacyChooserWhenRoleManagerIsUnavailable() {
        assertEquals(
            OPEN_LEGACY_CHOOSER,
            HomeAppUtils.getDefaultLauncherRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = false,
                isHomeRoleHeld = false
            )
        )
    }

    @Test
    fun defaultLauncherRequestActionDoesNothingWhenHomeRoleIsAlreadyHeld() {
        assertEquals(
            NONE,
            HomeAppUtils.getDefaultLauncherRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = true,
                isHomeRoleHeld = true
            )
        )
    }

    @Test
    fun defaultLauncherRequestActionRequestsHomeRoleOnAndroidQAndLater() {
        assertEquals(
            REQUEST_HOME_ROLE,
            HomeAppUtils.getDefaultLauncherRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = true,
                isHomeRoleHeld = false
            )
        )
    }
}
