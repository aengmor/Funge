package com.funge.funge;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // 加载第二步中定义的 XML 布局
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
