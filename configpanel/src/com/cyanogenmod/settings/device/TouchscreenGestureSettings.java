/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import cyanogenmod.providers.CMSettings;
import com.cyanogenmod.settings.device.utils.Constants;

public class TouchscreenGestureSettings extends PreferenceActivity
        implements OnPreferenceChangeListener {
    private static final String KEY_HAPTIC_FEEDBACK = "touchscreen_gesture_haptic_feedback";

    private SwitchPreference mHapticFeedback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.touchscreen_panel);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mHapticFeedback = (SwitchPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Remove the padding around the listview
        getListView().setPadding(0, 0, 0, 0);

        mHapticFeedback.setChecked(CMSettings.System.getInt(getContentResolver(),
                CMSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (KEY_HAPTIC_FEEDBACK.equals(key)) {
            final boolean value = (Boolean) newValue;
            CMSettings.System.putInt(getContentResolver(),
                    CMSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, value ? 1 : 0);
            return true;
        }

        Constants.setGestureEnabled((Boolean) newValue, preference.getKey());
        return true;
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Initialize node preferences
        for (String pref : Constants.sGesturePrefKeys) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            b.setOnPreferenceChangeListener(this);
            b.setChecked(Constants.getGestureEnabled(pref));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
