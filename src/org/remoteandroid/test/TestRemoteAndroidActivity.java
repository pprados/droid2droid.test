package org.remoteandroid.test;

import org.remoteandroid.RemoteAndroidManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.Window;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

// TODO: Approche Action bar dans le context menu
// TODO: Ou approche action bar en simulation complète
// TODO: Manque l'affichage de l'action bar

// Checkbox pour la découverte des ProximityNetwork
public class TestRemoteAndroidActivity extends FragmentActivity
{
	public static final String		TAG				= "RA-Test";

	private static final int		DIALOG_MARKET	= 1;

	SharedPreferences				mPreferences;

	FragmentManager					mFragmentManager;

	TestRemoteAndroidListFragment	mFragment;

	@Override
	public void onBackPressed()
	{
		finish();
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

// FIXME		
//		new AsyncTask<Void, Void, Void>()
//		{
//			private Set<BluetoothDevice> devices;
//			protected void onPreExecute() 
//			{
//				BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
//				devices=adapter.getBondedDevices();
//				
//			}
//			protected Void doInBackground(Void[] params) 
//			{
//				try
//				{
//					BluetoothDevice dev=devices.iterator().next();
//					UUID uuid=uuid=BluetoothSocketBossSender.sKeys[0];
//					BluetoothSocket socket=dev.createRfcommSocketToServiceRecord(uuid);
//					socket.connect();
//					socket.close();
//				}
//				catch (IOException e)
//				{
//					e.printStackTrace();
//				}
//				return null;
//			}
//		}.execute();
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_CONTEXT_MENU);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.fragment_main);
		setProgressBarIndeterminateVisibility(Boolean.FALSE); // Important: Use Boolean value !
		final ActionBar ab = getSupportActionBar();
		ab.setSubtitle(R.string.sub_title);

		mFragmentManager = getSupportFragmentManager();
		mFragment = (TestRemoteAndroidListFragment) mFragmentManager
				.findFragmentById(R.id.fragment);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		boolean menu=true;
		if ((getResources().getConfiguration().screenLayout 
				& Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) 
		{
			menu=false;
		}		
		if ((getResources().getConfiguration().screenLayout 
				& Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) 
		{
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			if (display.getOrientation()==Surface.ROTATION_0)
				menu=false;
		}		
		getSupportActionBar().setDisplayOptions(0,ActionBar.DISPLAY_SHOW_HOME);

		// Strict mode
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
								startActivity(RemoteAndroidManager
										.getIntentForMarket(TestRemoteAndroidActivity.this));
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
				return null;
		}
	}
}
