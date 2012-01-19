package org.remoteandroid.test;

import java.io.IOException;

import org.remoteandroid.RemoteAndroid;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.RemoteAndroid.PublishListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;


public class RemoteAndroidContext
{	
	interface OnRemoteAndroidContextUpdated
	{
		void onUpdated();
	}
	
	Handler sHandler=new Handler();
	
	OnRemoteAndroidContextUpdated mCallback;
	String mUri;
	enum State { Idle,BindingRemoteAndroid,InstallingApk,BindingRemoteObject,BindApplicationContext};
	State mState=State.Idle;
	
	RemoteAndroid mRemoteAndroid;
	TestRemoteObject mRemoteObject;
	
	CharSequence mStatus="idle";
	
	public RemoteAndroidContext(OnRemoteAndroidContextUpdated callback)
	{
		mCallback=callback;
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
		if (mRemoteAndroid!=null)
		{
			return;
		}
		String uri=mUri.substring(mUri.indexOf("] ")+2);
		mState=State.BindingRemoteAndroid;
		Intent intent=new Intent(Intent.ACTION_MAIN,Uri.parse(uri));
		RemoteAndroidManager.getManager(context).bindRemoteAndroid(
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
					
				}, 0);
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
		int flags=Integer.parseInt(preferences.getString("install.flags","0"));
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
					if (status==-2)
						setStatus("Impossible to install application not from market");
					else if (status==-1)
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
		int flags=Integer.parseInt(preferences.getString("bind.flags","1"));
		boolean rc=mRemoteAndroid.bindService(
				new Intent("org.remoteandroid.test.TestService"), //Intent to invoke service
				new ServiceConnection()
				{
					
					@Override
					public void onServiceDisconnected(ComponentName name)
					{
						mState=State.Idle;
						mRemoteObject=null;
						setStatus("Binding disconnected");
					}
					
					@Override
					public void onServiceConnected(ComponentName name, IBinder service)
					{
						mState=State.Idle;
						mRemoteObject=(TestRemoteObject)TestRemoteObject.Stub.asInterface(service);
						setStatus("Remote object binded");
					}
				}, flags);
		if (rc==false)
			setStatus("Bind impossible");
			
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
					Log.e(TestRemoteAndroidActivity.TAG,"Error when invoke.",e);
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
	
}
