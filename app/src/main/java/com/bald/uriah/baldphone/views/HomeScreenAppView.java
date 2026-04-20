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

package com.bald.uriah.baldphone.views;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import app.baldphone.neo.calls.CallManager;
import app.baldphone.neo.contacts.ui.details.ContactDetailsActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.S;

public class HomeScreenAppView {
    public final ImageView iv_icon;
    private final BaldLinearLayoutButton child;
    private final TextView tv_name;
    private final ImageView iv_speed_dial_badge;
    private final TextView tv_call_label;

    public HomeScreenAppView(BaldLinearLayoutButton child) {
        this.child = child;
        tv_name = child.findViewById(R.id.et_name);
        iv_icon = child.findViewById(R.id.iv_icon);
        iv_speed_dial_badge = child.findViewById(R.id.iv_speed_dial_badge);
        tv_call_label = child.findViewById(R.id.tv_call_label);
    }

    public void setText(@StringRes int resId) {
        tv_name.setText(resId);
    }

    public void setText(CharSequence charSequence) {
        tv_name.setText(charSequence);
    }

    public void setIntent(final ComponentName componentName) {
        resetSpeedDialState();
        child.setOnClickListener(v -> S.startComponentName(v.getContext(), componentName));
    }

    public void setIntent(final String contactLookupKey) {
        resetSpeedDialState();
        child.setOnClickListener(v -> v.getContext().startActivity(new Intent(v.getContext(), ContactDetailsActivity.class).putExtra(ContactDetailsActivity.CONTACT_LOOKUP_KEY, contactLookupKey)));
    }

    public void setSpeedDialCall(final String phoneNumber) {
        child.setBackgroundResource(R.drawable.style_for_buttons_speed_dial);
        iv_speed_dial_badge.setVisibility(View.VISIBLE);
        tv_call_label.setVisibility(View.GONE);
        child.setContentDescription(child.getContext().getString(R.string.call_label) + " " + tv_name.getText());
        child.setOnClickListener(v -> CallManager.INSTANCE.call(v.getContext(), phoneNumber, false));
    }

    private void resetSpeedDialState() {
        child.setBackgroundResource(R.drawable.style_for_buttons);
        iv_speed_dial_badge.setVisibility(View.GONE);
        tv_call_label.setVisibility(View.GONE);
        child.setContentDescription(tv_name.getText());
    }

    public void setVisibility(int visibility) {
        child.setVisibility(visibility);
    }
}
