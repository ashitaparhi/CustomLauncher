package com.jio.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppListModel {

    private static final String TAG = AppListModel.class.getSimpleName();

    //Background Handler message
    private static final int MSG_INIT_APP_LIST = 1000;
    private static final int MSG_APP_REMOVED = 1001;
    private static final int MSG_APP_UPDATED = 1002;
    private static final int MSG_SEARCH_APP = 1003;

    private static final int DELAY_SEARCH_APP = 100;

    //Ui Handler messages
    private static final int MSG_NOTIFY_APP_LIST_UPDATE = 2000;

    private static volatile AppListModel mAppListModel;

    private final Context mContext;
    private final CopyOnWriteArrayList<IAppListChange> mAppListChangeListeners = new CopyOnWriteArrayList<>();
    private final List<AppInfo> mAppList = new ArrayList<>();
    private final MyBackgroundHandler mBackgroundHandler;
    private final MyUIHandler mUIHandler;

    private AppListModel(Context context) {
        mContext = context;
        HandlerThread handlerThread = new HandlerThread("APPLISTTHREAD");
        handlerThread.start();
        mBackgroundHandler = new MyBackgroundHandler(handlerThread.getLooper());
        mUIHandler = new MyUIHandler(Looper.getMainLooper());
        mBackgroundHandler.sendEmptyMessage(MSG_INIT_APP_LIST);
        registerAppPackageMonitorReceiver();
    }

    public static AppListModel getInstance(Context context) {
        if (mAppListModel == null) {
            synchronized (AppListModel.class) {
                if (mAppListModel == null) {
                    mAppListModel = new AppListModel(context);
                }
            }
        }
        return mAppListModel;
    }

    public List<AppInfo> getApplicationList() {
        List<AppInfo> list;
        synchronized (mAppList) {
            list = new ArrayList<>(mAppList);
        }
        return list;
    }

    private void updateAppList(List<AppInfo> appInfoList) {
        synchronized (mAppList) {
            mAppList.clear();
            mAppList.addAll(appInfoList);
        }
    }

    private void initAppList() {
        PackageManager pm = mContext.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allApps = pm.queryIntentActivities(i, 0);
        List<AppInfo> appList = new ArrayList<>();
        for (ResolveInfo ri : allApps) {
            AppInfo appInfo = constructAppInfo(ri);
            if (appInfo != null) {
                appList.add(appInfo);
            }
        }
        Collections.sort(appList);
        updateAppList(appList);
    }

    private class MyBackgroundHandler extends Handler {

        public MyBackgroundHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_INIT_APP_LIST:
                    Log.i(TAG, "Started initializing app list");
                    initAppList();
                    AppListModel.this.sendMessage(mUIHandler, MSG_NOTIFY_APP_LIST_UPDATE, getApplicationList());
                    break;
                case MSG_APP_UPDATED:
                    Log.i(TAG, "Started updating app list");
                    String pName = (String) msg.obj;
                    if (pName != null) {
                        handlePackageChangedOrAdded(msg.arg1 == 1, pName);
                        AppListModel.this.sendMessage(mUIHandler, MSG_NOTIFY_APP_LIST_UPDATE, getApplicationList());
                    }
                    break;
                case MSG_APP_REMOVED:
                    String packageName = (String) msg.obj;
                    if (packageName != null) {
                        handlePackageRemoved(packageName);
                        AppListModel.this.sendMessage(mUIHandler, MSG_NOTIFY_APP_LIST_UPDATE, getApplicationList());
                    }
                    break;
                case MSG_SEARCH_APP:
                    searchApps((String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }

    private class MyUIHandler extends Handler {

        public MyUIHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_NOTIFY_APP_LIST_UPDATE) {
                Log.i(TAG, "Notifying app list update");
                notifyAppListUpdate((List<AppInfo>) msg.obj);
            }
        }
    }


    private void registerAppPackageMonitorReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(mAppBroadcastReceiver, intentFilter);
    }

    private final BroadcastReceiver mAppBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (packageName == null || packageName.isEmpty()) {
                Log.w(TAG, "bad intent received!!!");
                return;
            }

            final String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                sendMessage(false, packageName);
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    sendMessage(mBackgroundHandler, MSG_APP_REMOVED, packageName);
                }
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                sendMessage(!replacing, packageName);
            }
        }
    };

    private void sendMessage(boolean isAdded, String object) {
        Message msg = Message.obtain();
        msg.what = MSG_APP_UPDATED;
        msg.obj = object;
        msg.arg1 = isAdded ? 1 : 0;
        mBackgroundHandler.sendMessage(msg);
    }

    private void sendMessage(Handler handler, int what, Object object) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = object;
        handler.sendMessage(msg);
    }

    private void sendMessage(int what, String object, long delay) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = object;
        mBackgroundHandler.sendMessageDelayed(msg, delay);
    }

    private AppInfo constructAppInfo(ResolveInfo ri) {
        if (ri == null) {
            return null;
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(ri.activityInfo.packageName, 0);
            String packageName = ri.activityInfo.packageName;
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();
            AppInfo appInfo = new AppInfo(ri.loadLabel(pm).toString(), packageName,
                    ri.activityInfo.loadIcon(pm), pInfo.versionName, pInfo.versionCode, className);
            return appInfo;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Error in constructing app info ", e);
        }
        return null;
    }

    private void handlePackageChangedOrAdded(boolean isAdded, final String packageName) {
        ResolveInfo resolveInfo = getAppResolveInfoByPackageName(packageName);
        if (resolveInfo != null) {
            List<AppInfo> appInfoList;
            synchronized (this) {
                appInfoList = getApplicationList();
            }
            AppInfo appInfo = constructAppInfo(resolveInfo);
            if (!isAdded) {
                int size = appInfoList.size();
                for (int i = 0; i < size; i++) {
                    AppInfo info = appInfoList.get(i);
                    if (info.getPackageName().equals(packageName)) {
                        appInfoList.remove(i);
                        break;
                    }
                }
            }
            if (appInfo != null) {
                appInfoList.add(appInfo);
            }
            Collections.sort(appInfoList);
            updateAppList(appInfoList);
        } else {
            handlePackageRemoved(packageName);
        }
    }

    private void handlePackageRemoved(String packageName) {
        List<AppInfo> appInfoList;
        synchronized (this) {
            appInfoList = getApplicationList();
        }
        int size = appInfoList.size();
        for (int i = 0; i < size; i++) {
            AppInfo appInfo = appInfoList.get(i);
            if (appInfo.getPackageName().equals(packageName)) {
                appInfoList.remove(i);
                break;
            }
        }
        Collections.sort(appInfoList);
        updateAppList(appInfoList);
    }

    private ResolveInfo getAppResolveInfoByPackageName(String packageName) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);
        List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos.isEmpty()) {
            return null;
        }
        ResolveInfo resolveInfo = null;
        for (ResolveInfo ri : resolveInfos) {
            if (packageName.equals(ri.activityInfo.packageName)) {
                resolveInfo = ri;
                break;
            }
        }
        return resolveInfo;
    }

    public void search(String appName) {
        mBackgroundHandler.removeMessages(DELAY_SEARCH_APP);
        sendMessage(MSG_SEARCH_APP, appName, DELAY_SEARCH_APP);
    }

    private void searchApps(String appName) {
        List<AppInfo> appInfoList = getApplicationList();
        if (appName == null || appName.isEmpty()) {
            sendMessage(mUIHandler, MSG_NOTIFY_APP_LIST_UPDATE, appInfoList);
        } else {
            List<AppInfo> list = new ArrayList<>();
            for (AppInfo info : appInfoList) {
                if (info.getAppName().toLowerCase(Locale.getDefault()).contains(appName.toLowerCase(Locale.getDefault()))) {
                    list.add((info));
                }
            }
            sendMessage(mUIHandler, MSG_NOTIFY_APP_LIST_UPDATE, list);
        }
    }

    public interface IAppListChange {
        void onAppListUpdated(List<AppInfo> appInfoList);
    }

    public void addAppListChangeListener(IAppListChange listener) {
        if (listener == null) {
            Log.w(TAG, "Listener can not be null !!!");
            return;
        }
        mAppListChangeListeners.addIfAbsent(listener);
    }

    public void removeAppListChangeListener(IAppListChange listener) {
        if (listener == null) {
            Log.w(TAG, "Listener can not be null !!!");
            return;
        }
        mAppListChangeListeners.remove(listener);
    }

    private void notifyAppListUpdate(List<AppInfo> appInfoList) {
        for (IAppListChange listener : mAppListChangeListeners) {
            listener.onAppListUpdated(appInfoList);
        }
    }
}
