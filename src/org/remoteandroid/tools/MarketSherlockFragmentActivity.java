package org.remoteandroid.tools;

import org.remoteandroid.RemoteAndroidManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class MarketSherlockFragmentActivity extends SherlockFragmentActivity
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
						.setMessage("Install the application RemoteAndroid ?") // FIXME
						.setPositiveButton("Install", new DialogInterface.OnClickListener()
						{

							@Override
							public void onClick(DialogInterface paramDialogInterface, int paramInt)
							{
								Intent intent=RemoteAndroidManager.getIntentForMarket(MarketSherlockFragmentActivity.this);
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
}
