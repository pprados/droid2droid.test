package org.remoteandroid.test;

import java.io.IOException;

import org.remoteandroid.RemoteAndroid;
import org.remoteandroid.RemoteAndroid.PublishListener;
import org.remoteandroid.RemoteAndroidManager;

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
	
	Handler mHandler=new Handler();
	
	OnRemoteAndroidContextUpdated mCallback;
	String mUri;
	enum State { Idle,BindingRemoteAndroid,InstallingApk,BindingRemoteObject,UnbindingRemoteObject,BindApplicationContext};
	State mState=State.Idle;
	
	RemoteAndroidManager mManager;
	ServiceConnection  mConn;
	RemoteAndroid mRemoteAndroid;
	TestRemoteObject mRemoteObject;
	
	CharSequence mStatus="idle";
	
	public RemoteAndroidContext(Context context,RemoteAndroidManager manager,OnRemoteAndroidContextUpdated callback)
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
		Intent intent=new Intent("org.remoteandroid.test.TestService");
		mConn=new ServiceConnection()
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
				mRemoteObject=TestRemoteObject.Stub.asInterface(service);
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
			setStatus("Unbinded.");
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
