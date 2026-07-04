package com.fuck.snooping;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Config {
    public static final String PREF_NAME = "com.fuck.snooping_preferences";
    public static final String KEY_ENABLED_APPS = "enabled_apps";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static Set<String> getEnabledApps(Context context) {
        return getPrefs(context).getStringSet(KEY_ENABLED_APPS, new HashSet<>());
    }

    public static void setAppEnabled(Context context, String packageName, boolean enabled) {
        Set<String> apps = new HashSet<>(getEnabledApps(context));
        if (enabled) {
            apps.add(packageName);
        } else {
            apps.remove(packageName);
        }
        getPrefs(context).edit().putStringSet(KEY_ENABLED_APPS, apps).commit();
        
        // 尝试修复权限，虽然在 LSPosed 下这通常由框架处理
        fixPermissions(context);
    }

    private static void fixPermissions(Context context) {
        try {
            File dataDir = context.getDataDir();
            File prefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, PREF_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
            // 某些环境下需要对文件夹也设置权限
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false);
                prefsDir.setExecutable(true, false);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isAppEnabled(Context context, String packageName) {
        if ("com.tencent.mm".equals(packageName)) return true;
        return getEnabledApps(context).contains(packageName);
    }
}
