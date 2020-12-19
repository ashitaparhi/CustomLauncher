package com.jio.launcher;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.launcher.view.AppListAdapter;
import com.jio.library.AppInfo;
import com.jio.library.AppListModel;

import java.util.List;

public class HomeScreenActivity extends AppCompatActivity implements AppListModel.IAppListChange {

    private AppListModel mAppListModel;
    private AppListAdapter mCustomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.appList);
        EditText searchEditText = findViewById(R.id.search);
        searchEditText.addTextChangedListener(mTextWatcher);
        mAppListModel = AppListModel.getInstance(getApplicationContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 4);
        recyclerView.setLayoutManager(gridLayoutManager);
        mCustomAdapter = new AppListAdapter(mAppListModel.getApplicationList());
        recyclerView.setAdapter(mCustomAdapter);
        mAppListModel.addAppListChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAppListModel.removeAppListChangeListener(this);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onAppListUpdated(List<AppInfo> appInfoList) {
        mCustomAdapter.setData(appInfoList);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String searchString = s.toString();
            mAppListModel.search(searchString);
        }
    };
}