package org.remoteandroid.tools;

import org.remoteandroid.RemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.RemoteAndroidNfcHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public abstract class NfcActivity extends Activity
implements RemoteAndroidNfcHelper.OnNfcDiscover
{
	protected RemoteAndroidNfcHelper 		mNfcIntegration;
	
//	@Override
//	protected void onNewIntent(Intent intent)
//	{
//		super.onNewIntent(intent);
//		mNfcIntegration.onNewIntent(this, getRemoteAndroidManager(), intent);
//	}
	
	@Override
	protected void onCreate(Bundle arg0)
	{
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		mNfcIntegration=RemoteAndroidManager.newNfcIntegrationHelper(this);
	}
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		mNfcIntegration.onNewIntent(this, intent);
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		mNfcIntegration.onResume(this);
	}
	@Override
	protected void onPause()
	{
		super.onPause();
		mNfcIntegration.onPause(this);
	}
	public abstract void onNfcDiscover(RemoteAndroidInfo info);
}
