package mbs.studio.view;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.studio.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}
