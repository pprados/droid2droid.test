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

import java.io.IOException;

import org.droid2droid.Droid2DroidManager;
import org.droid2droid.RemoteAndroid;
import org.droid2droid.RemoteAndroid.PublishListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;


public class Droid2DroidContext
{	
	interface OnRemoteAndroidContextUpdated
	{
		void onUpdated();
	}
	
	private final OnRemoteAndroidContextUpdated mCallback;
	protected String mUri;
	protected enum State { Idle,BindingRemoteAndroid,InstallingApk,BindingRemoteObject,UnbindingRemoteObject,BindApplicationContext};
	protected State mState=State.Idle;
	
	private final Droid2DroidManager mManager;
	private ServiceConnection  mConn;
	protected RemoteAndroid mRemoteAndroid;
	protected boolean binded;
	protected TestRemoteObject mRemoteObject;
	
	protected CharSequence mStatus="idle";
	
	public Droid2DroidContext(Context context,Droid2DroidManager manager,OnRemoteAndroidContextUpdated callback)
	{
		mCallback=callback;
		mManager=manager;
	}
	
	private void setStatus(String status)
	{
		// Invoked in main thread
		mStatus=status;
		if (mCallback!=null)
			mCallback.onUpdated();
	}
	public void connect(final Activity context)
	{
		setStatus("Connecting...");
		if (mManager==null || mRemoteAndroid!=null)
		{
			return;
		}
		String uri=mUri.substring(mUri.indexOf("] ")+2);
		mState=State.BindingRemoteAndroid;
		SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context.getBaseContext());
		int flags=parseFlags(preferences.getString("remote_bind.flags","0"));
		Intent intent=new Intent(Intent.ACTION_MAIN,Uri.parse(uri));
		mManager.bindRemoteAndroid(
				intent, 
				new ServiceConnection()
				{

					@Override
					public void onServiceConnected(ComponentName name, IBinder service)
					{
						mRemoteAndroid=(RemoteAndroid)service;
						SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context.getBaseContext());
						int timeout=Integer.parseInt(preferences.getString("execute.timeout", "60"));
						mRemoteAndroid.setExecuteTimeout(timeout);
						mState=State.Idle;
						setStatus("Android connected");
					}

					@Override
					public void onServiceDisconnected(ComponentName name)
					{
						mState=State.Idle;
						mRemoteAndroid=null;
						mRemoteObject=null;
						setStatus("Android disconnected");
					}
					
				}, flags);
	}
	public void disconnect(Context context)
	{
		if (mRemoteAndroid!=null)
			mRemoteAndroid.close();
		mRemoteObject=null;
		mRemoteAndroid=null;
	}
	
	public void install(Activity context)
	{
		if (mRemoteAndroid==null)
			return;
		setStatus("Installing...");
		SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context.getBaseContext());
		int timeout=Integer.parseInt(preferences.getString("install.timeout", "60")); // Seconds
		int flags=parseFlags(preferences.getString("install.flags","0"));
		mState=State.InstallingApk;
		try
		{
			mRemoteAndroid.pushMe(context,new PublishListener()
			{
				
				@Override
				public void onProgress(int progress)
				{
					setStatus("Progress..."+progress/100+"%");
				}
				
				@Override
				public void onFinish(int status)
				{
					if (status==RemoteAndroid.ERROR_INSTALL_REFUSE_FOR_UNKNOW_SOURCE)
						setStatus("Impossible to install application not from market");
					else if (status==RemoteAndroid.ERROR_INSTALL_REFUSED)
						setStatus("Install refused");
					else if (status==0)
						setStatus("Install not necessary");
					else 
						setStatus("Install done.");
				}
				
				@Override
				public void onError(Throwable e)
				{
					String msg=e.getMessage();
					if (msg==null)
						msg=e.getClass().getName();
					setStatus("Install error: "+msg);
				}
				
				@Override
				public boolean askIsPushApk()
				{
					setStatus("Accept to push apk.");
					return true;
				}
			},flags,timeout);
		}
		catch (RemoteException e)
		{
			setStatus(e.getMessage());
		}
		catch (IOException e)
		{
			setStatus(e.getMessage());
		}
	}
	
	public void bindService(Activity context)
	{
		if (mRemoteAndroid==null)
			return;
		setStatus("Binding...");
		mState=State.BindingRemoteObject;
		SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context.getBaseContext());
		int flags=parseFlags(preferences.getString("bind.flags","1"));
		Intent intent=new Intent("org.droid2droid.test.TestService");
		mConn=new ServiceConnection()
		{
			
			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				mState=State.Idle;
				binded=false;
				setStatus("Binding disconnected");
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service)
			{
				mState=State.Idle;
				mRemoteObject=TestRemoteObject.Stub.asInterface(service);
				binded=true;
				setStatus("Remote object binded");
			}
		};
		boolean rc=mRemoteAndroid.bindService(intent,mConn, flags);
		if (rc==false)
			setStatus("Bind impossible");
			
	}

	public void unbindService(Activity context)
	{
		if (mRemoteAndroid==null)
			return;
		setStatus("Unbinding...");
		mState=State.UnbindingRemoteObject;
		boolean rc=mRemoteAndroid.unbindService(mConn);
		if (rc==false)
			setStatus("Unbind impossible");
		else
		{
			binded=false;
			setStatus("Unbinded.");
		}
	}
	public void invoke(Context context)
	{
		if (mRemoteAndroid==null)
			return;
		if (mRemoteObject==null)
			return;
		setStatus("...");
		new AsyncTask<Void, Void, String>()
		{

			@Override
			protected String doInBackground(Void... paramArrayOfParams)
			{
				try
				{
					return "ok"+mRemoteObject.helloWord("Hello");
				}
				catch (RemoteException e)
				{
					Log.e(TestDroid2DroidActivity.TAG,"Error when invoke.",e);
					return "ko"+e.getMessage();
				}
			}
			@Override
			protected void onPostExecute(String result)
			{
				if (result.startsWith("ok"))
					setStatus(result.substring(2));
				else
					setStatus("Remote object error: "+result.substring(2));
			}
		}.execute();
		
	}

	public static int parseFlags(String flags)
	{
		int result=0;
		if (flags.length()==0)
			return 0;
		String[] vals=flags.split("\\|");
		for (int i=0;i<vals.length;++i)
		{
			result+=Integer.parseInt(vals[i]);
		}
		return result;
	}
}
