package app.baldphone.neo.sms

import app.baldphone.neo.sms.SmsDefaultAppSyncer.DefaultSmsRequestAction.NONE
import app.baldphone.neo.sms.SmsDefaultAppSyncer.DefaultSmsRequestAction.REQUEST_LEGACY_CHANGE_DEFAULT
import app.baldphone.neo.sms.SmsDefaultAppSyncer.DefaultSmsRequestAction.REQUEST_ROLE
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsDefaultAppSyncerTest {
    @Test
    fun defaultSmsRequestActionUsesLegacyChangeDefaultBeforeAndroidQ() {
        assertEquals(
            REQUEST_LEGACY_CHANGE_DEFAULT,
            SmsDefaultAppSyncer.getDefaultSmsRequestAction(
                isAtLeastAndroidQ = false,
                isRoleManagerAvailable = false,
                isSmsRoleAvailable = false,
                isSmsRoleHeld = false
            )
        )
    }

    @Test
    fun defaultSmsRequestActionDoesNothingWhenRoleManagerIsUnavailableOnAndroidQAndLater() {
        assertEquals(
            NONE,
            SmsDefaultAppSyncer.getDefaultSmsRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = false,
                isSmsRoleAvailable = false,
                isSmsRoleHeld = false
            )
        )
    }

    @Test
    fun defaultSmsRequestActionDoesNothingWhenSmsRoleIsUnavailable() {
        assertEquals(
            NONE,
            SmsDefaultAppSyncer.getDefaultSmsRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = true,
                isSmsRoleAvailable = false,
                isSmsRoleHeld = false
            )
        )
    }

    @Test
    fun defaultSmsRequestActionDoesNothingWhenSmsRoleIsAlreadyHeld() {
        assertEquals(
            NONE,
            SmsDefaultAppSyncer.getDefaultSmsRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = true,
                isSmsRoleAvailable = true,
                isSmsRoleHeld = true
            )
        )
    }

    @Test
    fun defaultSmsRequestActionRequestsSmsRoleOnAndroidQAndLater() {
        assertEquals(
            REQUEST_ROLE,
            SmsDefaultAppSyncer.getDefaultSmsRequestAction(
                isAtLeastAndroidQ = true,
                isRoleManagerAvailable = true,
                isSmsRoleAvailable = true,
                isSmsRoleHeld = false
            )
        )
    }
}
