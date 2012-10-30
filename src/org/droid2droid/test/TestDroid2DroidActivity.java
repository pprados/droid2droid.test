/******************************************************************************
 *
 * droid2droid - Distributed Android Framework
 * ==========================================
 *
 * Copyright (C) 2012 by Atos (http://www.http://atos.net)
 * http://www.droid2droid.org
 *
 ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
******************************************************************************/
package org.droid2droid.test;

import static org.droid2droid.Droid2DroidManager.QRCODE_URI;

import java.io.IOException;
import java.io.InputStream;

import org.droid2droid.Droid2DroidManager;
import org.droid2droid.tools.MarketSherlockFragmentActivity;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
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
public class TestDroid2DroidActivity extends MarketSherlockFragmentActivity
{
	public static final String	TAG		= "RA-Test";

	private FragmentManager				mFragmentManager;

	private TestDroid2DroidListFragment	mFragment;
	
	private ImageView					mQrCode;
	private boolean						mQrCodeBig;

	@TargetApi(11)
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
			if (VERSION.SDK_INT>=VERSION_CODES.HONEYCOMB)
			{
				in=getContentResolver()
						.openTypedAssetFileDescriptor(QRCODE_URI, "image/png", null)
						.createInputStream();
			}
			else
			{
				in=getContentResolver().openInputStream(QRCODE_URI);
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
		mFragment = (TestDroid2DroidListFragment) mFragmentManager
				.findFragmentById(R.id.fragment);
	}
	@Override
	protected Droid2DroidManager getRemoteAndroidManager()
	{
		return mFragment.getManager();
	}
}
