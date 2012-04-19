package org.remoteandroid.tools;

import org.remoteandroid.RemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.test.TestRemoteAndroidActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class NfcMarketSherlockFragmentActivity extends NfcSherlockFragmentActivity
{
	private static final int		DIALOG_MARKET	= -1;

	@Override
	protected void onResume()
	{
		super.onResume();
		onResumeMarket();
	}
	private void onResumeMarket()
	{
		Intent market = RemoteAndroidManager.getIntentForMarket(this);
		if (market != null)
		{
			showDialog(DIALOG_MARKET);
		}
	}
	@Override
	public Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_MARKET:
				return new AlertDialog.Builder(this)
						.setMessage("Install the application Remote Android ?")
						.setPositiveButton("Install", new DialogInterface.OnClickListener()
						{

							@Override
							public void onClick(DialogInterface paramDialogInterface, int paramInt)
							{
								Intent intent=RemoteAndroidManager.getIntentForMarket(NfcMarketSherlockFragmentActivity.this);
								startActivity(intent);
								finish();
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
						{

							@Override
							public void onClick(DialogInterface paramDialogInterface, int paramInt)
							{
								finish();
							}
						}).create();
			default:
				return super.onCreateDialog(id);
		}
	}

	protected abstract RemoteAndroidManager getRemoteAndroidManager();
	public abstract void onNfcDiscover(RemoteAndroidInfo info);
}
