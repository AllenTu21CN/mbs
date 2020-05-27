package cn.lx.mbs.ui.view;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import cn.lx.mbs.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}
