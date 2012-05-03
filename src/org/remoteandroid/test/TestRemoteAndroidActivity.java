package org.remoteandroid.test;

import java.io.IOException;
import java.io.InputStream;

import org.remoteandroid.R;
import org.remoteandroid.RemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.tools.NfcSherlockFragmentActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;

// TODO: Approche Action bar dans le context menu
// TODO: Ou approche action bar en simulation complète
// TODO: Manque l'affichage de l'action bar

// Checkbox pour la découverte des ProximityNetwork
public class TestRemoteAndroidActivity extends NfcSherlockFragmentActivity
{
	public static final String		TAG		= "RA-Test";

	private FragmentManager					mFragmentManager;

	private TestRemoteAndroidListFragment	mFragment;
	
	private ImageView						mQrCode;
	private boolean							mQrCodeBig;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_CONTEXT_MENU);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
		setSupportProgressBarIndeterminateVisibility(false);
		final ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		ab.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		ab.setDisplayShowTitleEnabled(true);
		mQrCode=(ImageView)findViewById(R.id.qrcode);
		mQrCode.setVisibility(View.GONE);
		mQrCode.setOnClickListener(new ImageView.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				if (mQrCodeBig)
				{
					mQrCode.getImageMatrix().setScale(0.3f, 0.3f);		
					mQrCodeBig=false;
					mQrCode.invalidate();
				}
				else
				{
					mQrCode.getImageMatrix().setScale(1f, 1f);		
					mQrCode.invalidate();
					mQrCodeBig=true;
				}
			}
		});
		mQrCode.setClickable(true); 
		try
		{
			Bitmap bitmap;
			InputStream in;
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
			{
				in=getContentResolver()
						.openTypedAssetFileDescriptor(RemoteAndroidManager.QRCODE_URI, "image/png", null)
						.createInputStream();
			}
			else
			{
				in=getContentResolver().openInputStream(RemoteAndroidManager.QRCODE_URI);
			}
			bitmap=BitmapFactory.decodeStream(in);
			in.close();
			Bitmap scaBitmap=Bitmap.createScaledBitmap(bitmap, 300, 300, false);
			bitmap.recycle();
			mQrCode.setImageBitmap(scaBitmap);
			mQrCode.getImageMatrix().setScale(0.3f, 0.3f);
			mQrCode.invalidate();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		mFragmentManager = getSupportFragmentManager();
		mFragment = (TestRemoteAndroidListFragment) mFragmentManager
				.findFragmentById(R.id.fragment);
	}

	@Override
	public void onNfcDiscover(RemoteAndroidInfo info)
	{
		mFragment.onDiscover(info, false);
	}
}
