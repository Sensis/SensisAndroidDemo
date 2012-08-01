package au.com.sensis.android.demo;

import java.util.List;

import android.os.Build;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PreferencesDemo extends SherlockPreferenceActivity {

    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preferences);
            addPreferencesFromResource(R.xml.preferences2);
        }

        setTitle("Preferences Demo");
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }
}
