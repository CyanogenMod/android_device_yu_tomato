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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.cyanogenmod.internal.util.FileUtils;
import com.cyanogenmod.settings.device.utils.Constants;

public class Startup extends BroadcastReceiver {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (!action.equals(cyanogenmod.content.Intent.ACTION_INITIALIZE_CM_HARDWARE)) {
            return;
        }

        // Disable touchscreen gesture settings if needed
        if (!hasTouchscreenGestures()) {
            disableComponent(context, TouchscreenGestureSettings.class.getName());
            return;
        }

        enableComponent(context, TouchscreenGestureSettings.class.getName());
        // Restore nodes to saved preference values
        for (String pref : Constants.sGesturePrefKeys) {
            boolean value = Constants.isPreferenceEnabled(context, pref);
            Constants.setGestureEnabled(value, pref);
        }
    }

    private boolean hasTouchscreenGestures() {
        return FileUtils.isFileReadable(Constants.TOUCHSCREEN_GESTURE_CTRL_NODE) &&
                FileUtils.isFileWritable(Constants.TOUCHSCREEN_GESTURE_CTRL_NODE);
    }

    private void disableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int state = pm.getComponentEnabledSetting(name);

        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
