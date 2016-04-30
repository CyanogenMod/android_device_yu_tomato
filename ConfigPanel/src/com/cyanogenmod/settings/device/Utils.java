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

    // Gesture node
    public static final String GESTURE_CTRL_NODE =
            "/sys/devices/virtual/touchscreen/touchscreen_dev/gesture_ctrl";

    // Preference keys
    public static final String[] ALL_GESTURE_KEYS = {
        "touchscreen_gesture_camera",
        "touchscreen_gesture_music",
        "touchscreen_gesture_flashlight"
    };

    public static void setGestureEnabled(boolean state, String gesture) {
        String[] cmd = null;

        switch (gesture) {
            case ALL_GESTURE_KEYS[0]: // Camera
                cmd = new String[] { "c=" };
                break;
            case ALL_GESTURE_KEYS[1]: // Music
                cmd = new String[] { "down=", "left=", "right=" };
                break;
            case ALL_GESTURE_KEYS[2]: // Flashlight
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

        FileUtils.writeLine(GESTURE_CTRL_NODE, builder.toString());
    }

    public static boolean getGestureEnabled(String gesture) {
        String stateStr = FileUtils.readOneLine(GESTURE_CTRL_NODE);
        if (stateStr == null) {
            return false;
        }

        long state = Long.decode(stateStr);
        long hex = 0x00;

        switch (gesture) {
            case ALL_GESTURE_KEYS[0]: // Camera
                hex = 0x80;
                break;
            case ALL_GESTURE_KEYS[1]: // Music
                hex = 0x0E;
                break;
            case ALL_GESTURE_KEYS[2]: // Flashlight
                hex = 0x20;
                break;
            default:
                return false;
        }

        return (state & hex) == hex;
    }

    public static boolean isPreferenceEnabled(Context context, String key) {
        SharedPreferences preferences;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, getGestureEnabled(key));
    }

    public static boolean hasTouchscreenGestures() {
        return FileUtils.isFileReadable(Utils.GESTURE_CTRL_NODE) &&
                FileUtils.isFileWritable(Utils.GESTURE_CTRL_NODE);
    }
}
