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

public abstract class NfcSherlockFragmentActivity extends SherlockFragmentActivity
{
	private static final int		DIALOG_MARKET	= -1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		onNfcCreate();
	}
	private void onNfcCreate()
	{
//		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
//		{
//			NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//	        if (nfcAdapter != null) 
//	        {
//	        	nfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback()
//	        	{
//
//					@Override
//					public NdefMessage createNdefMessage(NfcEvent event)
//					{
//						RemoteAndroidManager manager=getRemoteAndroidManager();
//						if (manager!=null)
//						{
//							return manager.createNdefMessage();
//						}
//						return null;
//					}
//	        		
//	        	}, this);
//	        }
//		}
	}
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		onNfcNewIntent(intent);
	}
	// Invoked when NFC tag detected
	private void onNfcNewIntent(Intent intent)
	{
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
		{
			RemoteAndroidManager manager=getRemoteAndroidManager();
			if (manager==null)
				return;
			NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
			setIntent(intent);
			final Tag tag=(Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if (tag!=null)
			{
				// Check the caller. Refuse spoof events
				checkCallingPermission("com.android.nfc.permission.NFCEE_ADMIN");
				RemoteAndroidInfo info=manager.parseNfcRawMessages(this, 
					intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES));
				if (info!=null)
				{
					onNfcDiscover(info);
				}
			}
		}
	}	
	@Override
	protected void onResume()
	{
		super.onResume();
		onResumeMarket();
		onNfcResume();
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
								Intent intent=RemoteAndroidManager.getIntentForMarket(NfcSherlockFragmentActivity.this);
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

	private void onNfcResume()
	{
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
		{
			NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
			if (nfcAdapter!=null)
			{
				PendingIntent pendingIntent = 
						PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
				nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
			}
		}
	}
	@Override
	protected void onPause()
	{
		super.onPause();
		onNfcPause();
	}
	// Unregister the exposition of my tag
    private void onNfcPause()
    {
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
		{
			NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
			if (nfcAdapter!=null)
			{
				nfcAdapter.disableForegroundDispatch(this);
			}
		}
    }

	protected abstract RemoteAndroidManager getRemoteAndroidManager();
	public abstract void onNfcDiscover(RemoteAndroidInfo info);
}
