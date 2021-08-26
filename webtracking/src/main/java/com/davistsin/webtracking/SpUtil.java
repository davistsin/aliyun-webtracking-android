package com.davistsin.webtracking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

final class SpUtil {

    private SpUtil() {
    }

    public static void putStringSet(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences("webtracking", MODE_PRIVATE);
        Set<String> originSet = sp.getStringSet(key, null);
        Set<String> saveSet = new HashSet<>();
        saveSet.add(value);
        if (originSet != null) {
            saveSet.addAll(originSet);
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(key, saveSet);
        editor.apply();
    }

    public static Set<String> getStringSet(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences("webtracking", MODE_PRIVATE);
        return sp.getStringSet(key, null);
    }

    public static void remove(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences("webtracking", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.apply();
    }
}
