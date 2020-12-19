package com.jio.library;

import android.graphics.drawable.Drawable;

public class AppInfo implements Comparable<AppInfo> {
    private final String mAppName;
    private final String mPackageName;
    private final Drawable mAppIcon;
    private final String mVersionName;
    private final long mVersionCode;
    private final String mMainActivityClassName;

    public AppInfo(String appName, String packageName, Drawable appIcon, String versionName, long versionCode, String mainActivityClassName) {
        mAppName = appName;
        mPackageName = packageName;
        mAppIcon = appIcon;
        mVersionName = versionName;
        mVersionCode = versionCode;
        mMainActivityClassName = mainActivityClassName;
    }

    public String getAppName() {
        return mAppName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public long getVersionCode() {
        return mVersionCode;
    }

    public String getMainActivityClassName() {
        return mMainActivityClassName;
    }

    @Override
    public int compareTo(AppInfo appInfo) {
        return this.getAppName().compareTo(appInfo.getAppName());
    }
}
