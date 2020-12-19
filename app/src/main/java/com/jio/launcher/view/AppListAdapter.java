package com.jio.launcher.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jio.launcher.R;
import com.jio.library.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final List<AppInfo> mAppsList = new ArrayList<>();

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView textView;
        public ImageView img;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.appName);
            img = itemView.findViewById(R.id.appIcon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            Context context = v.getContext();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(mAppsList.get(pos).getPackageName());
            context.startActivity(launchIntent);
        }
    }

    public AppListAdapter(List<AppInfo> appInfoList) {
        mAppsList.addAll(appInfoList);
    }

    public void setData(List<AppInfo> appInfoList) {
        mAppsList.clear();
        mAppsList.addAll(appInfoList);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(AppListAdapter.ViewHolder viewHolder, int i) {
        AppInfo appInfo = mAppsList.get(i);
        viewHolder.textView.setText(appInfo.getAppName());
        viewHolder.img.setImageDrawable(appInfo.getAppIcon());
    }

    @Override
    public int getItemCount() {
        return mAppsList.size();
    }

    @Override
    public AppListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }
}
