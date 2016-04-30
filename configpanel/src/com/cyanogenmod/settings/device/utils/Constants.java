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

package com.cyanogenmod.settings.device.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.cyanogenmod.internal.util.FileUtils;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    // Preference keys
    private static final String TOUCHSCREEN_CAMERA_GESTURE_KEY = "touchscreen_gesture_camera";
    private static final String TOUCHSCREEN_MUSIC_GESTURE_KEY = "touchscreen_gesture_music";
    private static final String TOUCHSCREEN_FLASHLIGHT_GESTURE_KEY =
            "touchscreen_gesture_flashlight";

    // Gesture node
    public static final String TOUCHSCREEN_GESTURE_CTRL_NODE =
            "/sys/devices/virtual/touchscreen/touchscreen_dev/gesture_ctrl";

    // Gesture node default values
    private static final boolean TOUCHSCREEN_CAMERA_DEFAULT = true;
    private static final boolean TOUCHSCREEN_MUSIC_DEFAULT = true;
    private static final boolean TOUCHSCREEN_FLASHLIGHT_DEFAULT = true;

    // Holds <preference_key> -> <default_values> mapping
    private static final Map<String, Object> sNodeDefaultMap = new HashMap<>();

    public static final String[] sGesturePrefKeys = {
        TOUCHSCREEN_CAMERA_GESTURE_KEY,
        TOUCHSCREEN_MUSIC_GESTURE_KEY,
        TOUCHSCREEN_FLASHLIGHT_GESTURE_KEY
    };

    static {
        sNodeDefaultMap.put(TOUCHSCREEN_CAMERA_GESTURE_KEY, TOUCHSCREEN_CAMERA_DEFAULT);
        sNodeDefaultMap.put(TOUCHSCREEN_MUSIC_GESTURE_KEY, TOUCHSCREEN_MUSIC_DEFAULT);
        sNodeDefaultMap.put(TOUCHSCREEN_FLASHLIGHT_GESTURE_KEY, TOUCHSCREEN_FLASHLIGHT_DEFAULT);
    }

    public static void setGestureEnabled(boolean state, String gesture) {
        String[] cmd = null;

        switch (gesture) {
            case TOUCHSCREEN_CAMERA_GESTURE_KEY:
                cmd = new String[] { "c=" };
                break;
            case TOUCHSCREEN_MUSIC_GESTURE_KEY:
                cmd = new String[] { "down=", "left=", "right=" };
                break;
            case TOUCHSCREEN_FLASHLIGHT_GESTURE_KEY:
                cmd = new String[] { "o=" };
                break;
            default:
                return;
        }

        String enabled = state ? "true" : "false";
        StringBuilder builder = new StringBuilder();
        for (String i : cmd) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(i);
            builder.append(enabled);
        }

        FileUtils.writeLine(TOUCHSCREEN_GESTURE_CTRL_NODE, builder.toString());
    }

    public static boolean getGestureEnabled(String gesture) {
        String stateStr = FileUtils.readOneLine(TOUCHSCREEN_GESTURE_CTRL_NODE);
        if (stateStr == null) {
            return false;
        }

        long state = Long.decode(stateStr);
        long hex = 0x00;

        switch (gesture) {
            case TOUCHSCREEN_CAMERA_GESTURE_KEY:
                hex = 0x80;
                break;
            case TOUCHSCREEN_MUSIC_GESTURE_KEY:
                hex = 0x0E;
                break;
            case TOUCHSCREEN_FLASHLIGHT_GESTURE_KEY:
                hex = 0x20;
                break;
            default:
                return false;
        }

        return (state & hex) == hex;
    }

    public static boolean isPreferenceEnabled(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, (Boolean) sNodeDefaultMap.get(key));
    }

    public static String getPreferenceString(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, (String) sNodeDefaultMap.get(key));
    }
}
