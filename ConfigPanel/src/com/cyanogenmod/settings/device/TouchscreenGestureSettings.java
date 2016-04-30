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
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.cyanogenmod.settings.device.Utils;
import cyanogenmod.providers.CMSettings;

public class TouchscreenGestureSettings extends PreferenceActivity {

    private SwitchPreference mHapticFeedback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.touchscreen_panel);

        // Initialize node preferences
        for (String pref : Utils.ALL_GESTURE_KEYS) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            b.setOnPreferenceChangeListener(mGesturePrefListener);
            b.setChecked(Utils.getGestureEnabled(pref));
        }

        mHapticFeedback = (SwitchPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setOnPreferenceChangeListener(mHapticPrefListener);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Remove the padding around the listview
        ((ViewGroup) getListView().getParent()).setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHapticFeedback.setChecked(CMSettings.System.getInt(getContentResolver(),
                CMSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Preference.OnPreferenceChangeListener mHapticPrefListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            CMSettings.System.putInt(
                    getContentResolver(),
                    CMSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK,
                    (Boolean) newValue ? 1 : 0
            );
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener mGesturePrefListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Utils.setGestureEnabled((Boolean) newValue, preference.getKey());
            return true;
        }
    };
}
