package com.jakobhildebrand.garagenapp;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by jakob on 7/22/16.
 */
public class SettingsFragment extends PreferenceFragment{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
