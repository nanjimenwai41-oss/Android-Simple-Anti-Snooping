package com.fuck.snooping;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ScopeProvider extends ContentProvider {
    public static final String AUTHORITY = "com.fuck.snooping.scope";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (getContext() == null) return null;
        
        if ("register_scope".equals(method) && arg != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("active_scope_apps", 0);
            Set<String> apps = new HashSet<>(prefs.getStringSet("apps", new HashSet<>()));
            if (apps.add(arg)) {
                prefs.edit().putStringSet("apps", apps).apply();
            }
            return null;
        }

        if ("is_enabled".equals(method) && arg != null) {
            // 特殊逻辑：微信永远启用
            if ("com.tencent.mm".equals(arg)) {
                Bundle b = new Bundle();
                b.putBoolean("enabled", true);
                return b;
            }
            
            SharedPreferences prefs = getContext().getSharedPreferences(Config.PREF_NAME, 0);
            Set<String> enabledApps = prefs.getStringSet(Config.KEY_ENABLED_APPS, new HashSet<>());
            Bundle b = new Bundle();
            b.putBoolean("enabled", enabledApps.contains(arg));
            return b;
        }
        return null;
    }

    @Nullable @Override public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) { return null; }
    @Nullable @Override public String getType(@NonNull Uri uri) { return null; }
    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
}
