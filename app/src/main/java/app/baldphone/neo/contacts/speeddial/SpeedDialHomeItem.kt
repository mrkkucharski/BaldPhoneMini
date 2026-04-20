package app.baldphone.neo.contacts.speeddial

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper
import com.bald.uriah.baldphone.utils.S
import com.bald.uriah.baldphone.views.HomeScreenAppView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class SpeedDialHomeItem(val entry: SpeedDialEntry) : HomeScreenPinHelper.HomeScreenPinnable {
    override fun applyToHomeScreenAppView(view: HomeScreenAppView) {
        view.setText(entry.displayNameSnapshot)
        view.setSpeedDialCall(entry.phoneNumber)
        if (S.isValidContextForGlide(view.iv_icon.context)) {
            Glide.with(view.iv_icon)
                .load(entry.photoUriSnapshot)
                .apply(RequestOptions().error(R.drawable.face_on_button))
                .into(view.iv_icon)
        }
    }
}
