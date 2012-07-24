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
package org.droid2droid.tools;

import org.droid2droid.Droid2DroidManager;

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
		Intent market = Droid2DroidManager.getIntentForMarket(this);
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
								Intent intent=Droid2DroidManager.getIntentForMarket(MarketSherlockFragmentActivity.this);
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

	protected abstract Droid2DroidManager getRemoteAndroidManager();
}
