package org.remoteandroid.test;

import org.remoteandroid.test.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TestRemoteAndroidPreferenceActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
}
