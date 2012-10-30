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

import static org.droid2droid.Droid2DroidManager.ACTION_CONNECT_ANDROID;
import static org.droid2droid.Droid2DroidManager.DISCOVER_INFINITELY;
import static org.droid2droid.Droid2DroidManager.EXTRA_DISCOVER;
import static org.droid2droid.Droid2DroidManager.EXTRA_FLAGS;
import static org.droid2droid.Droid2DroidManager.EXTRA_SUBTITLE;
import static org.droid2droid.Droid2DroidManager.EXTRA_THEME_ID;
import static org.droid2droid.Droid2DroidManager.EXTRA_TITLE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.droid2droid.Droid2DroidManager;
import org.droid2droid.Droid2DroidManager.ManagerListener;
import org.droid2droid.ListRemoteAndroidInfo;
import org.droid2droid.RemoteAndroidInfo;
import org.droid2droid.test.Droid2DroidContext.OnRemoteAndroidContextUpdated;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

// Checkbox pour la decouverte des ProximityNetwork
public class TestDroid2DroidListFragment extends SherlockListFragment 
implements View.OnClickListener, OnItemSelectedListener, OnRemoteAndroidContextUpdated, OnCheckedChangeListener,
	ListRemoteAndroidInfo.DiscoverListener
{
	public static final String TAG="RA-Test";
	
	private static final String URL_NOT_KNOWN="[hack] ips://localhost"; // FIXME "Not known";
	private static final String EXTRA_BURST="burst";

	private static final int REQUEST_CONNECT_CODE=1;
	
	private NfcAdapter mNfcAdapter;
    private final ExecutorService mExecutors=Executors.newCachedThreadPool();
	private final Handler mHandler=new Handler();
	private SharedPreferences mPreferences;
	private MenuItem mDiscoverMenu;
	private MenuItem mBurstMenu;
	
	private final List<String> mItems=new ArrayList<String>();

	private volatile ListRemoteAndroidInfo mDiscoveredAndroid;
	private final ArrayList<Droid2DroidContext> mRemoteAndroids=new ArrayList<Droid2DroidContext>(5);
	private BaseAdapter mAdapter;
	private ArrayAdapter<String> mItemAdapter;
	private boolean mBurst;
	private Droid2DroidManager mManager;
	
	@Override
	@TargetApi(14)
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (VERSION.SDK_INT>VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mNfcAdapter=NfcAdapter.getDefaultAdapter(activity);
			mNfcAdapter.setNdefPushMessageCallback(
				// Hook for compatibility
				new CreateNdefMessageCallback()
				{
					@Override
					public NdefMessage createNdefMessage(NfcEvent event)
					{
						return TestDroid2DroidListFragment.this.createNdefMessage(event);
					}
					
				}, activity);
		}
	}
	
	public final Droid2DroidManager getManager()
	{
		return mManager;
	}
	@Override
	public void onDiscoverStart()
	{
		FragmentActivity activity=getActivity();
		if (activity!=null)
		{
			setDiscover(true);
		}
	}
	@Override
	public void onDiscoverStop()
	{
		FragmentActivity activity=getActivity();
		if (activity!=null)
		{
			setDiscover(false);
		}
	}
	@Override
	public void onDiscover(RemoteAndroidInfo info,boolean replace)
	{
//		if (!mIsDiscover && !replace) return;
		addRemoteAndroid(info, replace);
	}

	private void addRemoteAndroid(RemoteAndroidInfo info, boolean replace)
	{
		String[] uris=info.getUris();
		mItemAdapter.remove(URL_NOT_KNOWN);
    	for (String s:uris)
    	{
    		final String key='['+info.getName()+"] "+s;
    		mItemAdapter.remove(key);
    		mItemAdapter.add(key);
    	}
    	if (mDiscoveredAndroid==null)
    		return;
    	if (!mDiscoveredAndroid.contains(info))
    	{
    		mDiscoveredAndroid.add(info);
    		replace=false;
    	}
		if (!replace)
		{
			if (uris.length!=0)
			{
				mRemoteAndroids.add(new Droid2DroidContext(getActivity(),mManager,this));
		    	int position=mRemoteAndroids.size()-1;
		    	Droid2DroidContext racontext=mRemoteAndroids.get(position);
		    	racontext.mUri='['+info.getName()+"] "+uris[0];
			}
			else
			{
				Log.e(TAG,"Discover error"); //FIXME: Discover error
			}
		}
		mItemAdapter.sort(new Comparator<String>()
		{
			@Override
			public int compare(String object1, String object2)
			{
				return object1.compareTo(object2);
			}
		});
		mItemAdapter.notifyDataSetChanged();
		mAdapter.notifyDataSetChanged();
	}

	private void setDiscover(boolean status)
	{
		mDiscoverMenu.setChecked(status);
		mDiscoverMenu.setIcon(status ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_menu_search);
		((SherlockFragmentActivity)getActivity()).setSupportProgressBarIndeterminateVisibility(status);
	}
	private void setBurst(boolean status)
	{
		mBurstMenu.setChecked(status);
		mBurstMenu.setIcon(status ? R.drawable.ic_menu_burst_toggle : R.drawable.ic_menu_burst);
		mBurst=status;
	}
	
	
    /** Called when the activity is first created. */
	class Caches
	{
		Spinner mUri;
		ToggleButton mActive;
		Button mInstall;
		ToggleButton mBind;
		Button mInvoke;
		Spinner mCmds;
		TextView mStatus;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.main, container);
	}
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean(EXTRA_BURST, mBurst);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) 
    {
		Log.v(TAG,"Fragment.onCreate");
        super.onCreate(savedInstanceState);
		mDiscoveredAndroid=Droid2DroidManager.newDiscoveredAndroid(getActivity(),this);
        Intent market=Droid2DroidManager.getIntentForMarket(getActivity());
        if (market==null)
        {
//	        mRetain=(Retain)getActivity().getLastNonConfigurationInstance(); // FIXME: single fragment in activity
        	Droid2DroidManager.bindManager(getActivity(), new ManagerListener()
        	{

				@Override
				public void bind(Droid2DroidManager manager)
				{
					mManager=manager;
				}

				@Override
				public void unbind(Droid2DroidManager manager)
				{
					mDiscoveredAndroid=null;
    	        	setDiscover(false);
    	        	setBurst(mBurst);
					mManager=null;
				}
        		
        	});
        	if (savedInstanceState!=null)
        	{
        		mBurst=savedInstanceState.getBoolean(EXTRA_BURST);
        	}
        	
            mItemAdapter=
        		new ArrayAdapter<String>(getActivity(),
        				android.R.layout.simple_spinner_item,mItems);
            mAdapter = new BaseAdapter()
    		{
    			
    			@Override
    			public View getView(int position, View convertView, ViewGroup parent)
    			{
    				View view;
    				Caches caches;
    				if (convertView==null)
    				{
    					Activity activity=getActivity();
    					if (activity==null)
    					{
    						Log.d(TAG,"null activity");
    						return null;
    					}
    					view=getActivity().getLayoutInflater().inflate(R.layout.test_item, null);
    					caches=new Caches();
    					caches.mUri=(Spinner)view.findViewById(R.id.uri);
    					caches.mUri.setOnItemSelectedListener(TestDroid2DroidListFragment.this);
    					caches.mActive=(ToggleButton)view.findViewById(R.id.active);
    					caches.mActive.setOnClickListener(TestDroid2DroidListFragment.this);
    					caches.mInstall=(Button)view.findViewById(R.id.install);
    					caches.mInstall.setOnClickListener(TestDroid2DroidListFragment.this);
    					caches.mBind=(ToggleButton)view.findViewById(R.id.bind);
    					caches.mBind.setOnClickListener(TestDroid2DroidListFragment.this);
    					caches.mInvoke=(Button)view.findViewById(R.id.invoke);
    					caches.mInvoke.setOnClickListener(TestDroid2DroidListFragment.this);
    					caches.mStatus=(TextView)view.findViewById(R.id.status);
    					view.setTag(caches);
    					
    					mItemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    					caches.mUri.setAdapter(mItemAdapter);
    					
    				}
    				else
    				{
    					view=convertView;
    					caches=(Caches)view.getTag();
    				}
    				caches.mUri.setTag(position);
    				caches.mActive.setTag(position);
    				caches.mInstall.setTag(position);
    				caches.mBind.setTag(position);
    				caches.mInvoke.setTag(position);
    				Droid2DroidContext racontext=mRemoteAndroids.get(position);
    				if (racontext.mUri!=null)
    				{
    					
    					for (int i=0;i<mItems.size();++i)
    					{
    						if (racontext.mUri.equals(mItems.get(i)))
    						{
    							caches.mUri.setSelection(i);
    							break;
    						}
    					}
    				}
    				caches.mActive.setEnabled((racontext.mState!=Droid2DroidContext.State.BindingRemoteAndroid));
    				caches.mActive.setChecked(racontext.mRemoteAndroid!=null);
    				boolean enabled=(racontext.mRemoteAndroid!=null);
    				caches.mInstall.setEnabled(enabled);
    				caches.mBind.setEnabled(enabled);
    				caches.mBind.setChecked(racontext.binded);
    				caches.mInvoke.setEnabled(racontext.mRemoteObject!=null);
    				caches.mStatus.setText(racontext.mStatus);
    				return view;
    			}

    			@Override
    			public long getItemId(int position)
    			{
    				return position;
    			}
    			
    			@Override
    			public Object getItem(int position)
    			{
    				// TODO Auto-generated method stub
    				return null;
    			}
    			
    			@Override
    			public int getCount()
    			{
    				return mRemoteAndroids.size();
    			}
    		};
	        setListAdapter(mAdapter);
	        setHasOptionsMenu(true);
        }
    }
	
    @Override
    public void onResume()
    {
		Log.v(TAG,"Fragment.onResume");
    	super.onResume();
       	initAndroids();
    }

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mDiscoveredAndroid!=null)
			mDiscoveredAndroid.close();
		if (mManager!=null)
			mManager.close();
	}
	
	private void initAndroids()
	{
		new AsyncTask<Void, Void, SharedPreferences>()
    	{
    		@Override
    		protected SharedPreferences doInBackground(Void... params)
    		{
    			return PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
    		}
    		
    		@Override
    		protected void onPostExecute(SharedPreferences preferences)
    		{
    			if (mItemAdapter==null)
    				return;
    			mPreferences=preferences;
    			int size=Integer.parseInt(preferences.getString("init.androids", "0"));
    	    	if (mRemoteAndroids!=null && mRemoteAndroids.size()<=size)
    	    	{
    				{
    					for (int i=mRemoteAndroids.size();i<size;++i)
    					{
    						Droid2DroidContext context=new Droid2DroidContext(getActivity(),mManager,TestDroid2DroidListFragment.this);
    						context.mUri=(mItems.size()>0) ? mItems.get(0) : URL_NOT_KNOWN;
    						mRemoteAndroids.add(context);
    					}
    					mItemAdapter.notifyDataSetChanged();
    					mAdapter.notifyDataSetChanged();
    				}
    	    	}
    		}
    	}.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu, menu);
		mDiscoverMenu=menu.findItem(R.id.discover);
		mBurstMenu=menu.findItem(R.id.burst);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final int id=item.getItemId();
		if (id == R.id.discover)
		{
			item.setChecked(!item.isChecked());
			if (item.isChecked())
			{
				if (mDiscoveredAndroid==null) return false;

				SharedPreferences preferences=mPreferences;
				int flags=Integer.parseInt(preferences.getString("discover.mode","1"));
				mManager.startDiscover(flags,DISCOVER_INFINITELY);
			}
			else
			{
				mManager.cancelDiscover();
			}
		}
		else if (id == R.id.add)
		{
			Intent intent=new Intent(ACTION_CONNECT_ANDROID);
//			intent.setFlags(
//				Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
//				|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//				|Intent.FLAG_
//				);
			intent.putExtra(EXTRA_THEME_ID,android.R.style.Theme_Holo_Light_DarkActionBar);
			intent.putExtra(EXTRA_TITLE, "Test");
			intent.putExtra(EXTRA_SUBTITLE, "Select device.");
			SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
			int flags=Droid2DroidContext.parseFlags(preferences.getString("discover.mode","1"));
			intent.putExtra(EXTRA_FLAGS, flags);
			
			startActivityForResult(intent, REQUEST_CONNECT_CODE);
		}
		else if (id == R.id.clear)
		{
			if (mDiscoveredAndroid!=null)
			{
				mDiscoveredAndroid.clear();
				for (final Droid2DroidContext rac:mRemoteAndroids)
				{
					if (rac.mRemoteAndroid!=null) 
					{
						// With strict mode
						mExecutors.execute(new Runnable()
						{
							@Override
							public void run()
							{
								rac.mRemoteAndroid.close();
							}
						});
					}
				}
				mRemoteAndroids.clear();
		    	initAndroids();
				mAdapter.notifyDataSetChanged();
			}
		}
		else if (id == R.id.burst)
		{
			setBurst(!item.isChecked());
		}
		else if (id == R.id.config)
		{
			getActivity().startActivity(new Intent(getActivity(), TestDroid2DroidPreferenceActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode==REQUEST_CONNECT_CODE && resultCode==Activity.RESULT_OK)
		{
			
			RemoteAndroidInfo info=(RemoteAndroidInfo)data.getParcelableExtra(EXTRA_DISCOVER);
			addRemoteAndroid(info, false);
		}
	}
	@Override
	public void onClick(final View view)
	{
		final int position=(Integer)view.getTag();
		final Droid2DroidContext context=mRemoteAndroids.get(position);
		switch (view.getId())
		{
			case R.id.active:
				Log.d(TAG,"Active "+position);
				view.setEnabled(false);
				if (mBurst)
				{
					for (final Droid2DroidContext ra:mRemoteAndroids)
					{
						mExecutors.execute(new Runnable()
						{
							@Override
							public void run()
							{
								if (!((ToggleButton)view).isChecked())
									ra.disconnect(getActivity());
								else
									ra.connect(getActivity());
							}
						});
					}
				}
				else
				{
					new AsyncTask<Void, Void, Void>()
					{
						@Override
						protected Void doInBackground(Void... params)
						{
							if (!((ToggleButton)view).isChecked())
								context.disconnect(getActivity());
							else
								context.connect(getActivity());
							return null;
						}
					}.execute();
				}
				break;
				
			case R.id.install:
				Log.d(TAG,"Install "+position);
				if (mBurst)
				{
					for (Droid2DroidContext ra:mRemoteAndroids) // FIXME Install en mode burst
					{
						ra.install(getActivity());
					}					
				}
				else
					context.install(getActivity());
				break;
				
			case R.id.bind:
				Log.d(TAG,"Invoke "+position);
				if (mBurst)
				{
					final boolean toogle=((ToggleButton)view).isChecked();
					for (final Droid2DroidContext ra:mRemoteAndroids) // FIXME
					{
						mExecutors.execute(new Runnable()
						{
							@Override
							public void run()
							{
								if (!toogle)
									ra.unbindService(getActivity());
								else
									ra.bindService(getActivity());
							}
						});
					}					
				}
				else
				{
					final Droid2DroidContext ra=mRemoteAndroids.get(position);
					if (!((ToggleButton)view).isChecked())
						ra.unbindService(getActivity());
					else
						ra.bindService(getActivity());
				}
				break;
				
			case R.id.invoke:
				Log.d(TAG,"Invoke "+position);
				if (mBurst)
				{
					for (final Droid2DroidContext ra:mRemoteAndroids)
					{
						mExecutors.execute(new Runnable()
						{
							@Override
							public void run()
							{
								ra.invoke(getActivity());
							}
						});
					}					
				}
				else
					context.invoke(getActivity());
				break;
		}
	}
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id)
	{
		final int position=(Integer)parent.getTag();
		Droid2DroidContext context=mRemoteAndroids.get(position);
		context.mUri=mItems.get(itemPosition);
	}
	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
	}

	@Override
	public void onUpdated()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
					mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
	}
	public final Droid2DroidManager getRemoteAndroidManager()
	{
		return mManager;
	}
//    @Override
    @TargetApi(14)
	public NdefMessage createNdefMessage(NfcEvent event) 
	{
		return (mManager!=null) ? mManager.createNdefMessage() : null;
	}	
}