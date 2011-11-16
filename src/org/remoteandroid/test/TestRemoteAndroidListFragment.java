package org.remoteandroid.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.remoteandroid.ListRemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.test.RemoteAndroidContext.OnRemoteAndroidContextUpdated;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
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

// Checkbox pour la découverte des ProximityNetwork
public class TestRemoteAndroidListFragment extends ListFragment 
implements View.OnClickListener, OnItemSelectedListener, OnRemoteAndroidContextUpdated, OnCheckedChangeListener,
	ListRemoteAndroidInfo.DiscoverListener
{
	public static final String TAG="RA-Test";
	
	private static final int DIALOG_MARKET=1; // FIXME
	
	private static final String URL_NOT_KNOWN="Not known";
	private static final String EXTRA_BURST="burst";

	private static final int REQUEST_CONNECT_CODE=1;
	
    private ExecutorService mExecutors=Executors.newCachedThreadPool();
	private Handler mHandler=new Handler();
	SharedPreferences mPreferences;
	MenuItem mDiscover;
	MenuItem mBurst;
	
	boolean mIsDiscover;

	private List<String> mItems=new ArrayList<String>();

	private View mViewer;
	private static Retain mRetain;
	
	static class Retain
	{
		private ListRemoteAndroidInfo mDiscoveredAndroid;
		private ArrayList<RemoteAndroidContext> mRemoteAndroids=new ArrayList<RemoteAndroidContext>(5);
		private BaseAdapter mAdapter;
		private ArrayAdapter<String> mItemAdapter;
		private boolean mBurst;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
	
//	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mRetain;
	}
	
	@Override
	public void onDiscoverStart()
	{
		FragmentActivity activity=(FragmentActivity)getActivity();
		if (activity!=null)
		{
			setDiscover(true);
		}
	}
	@Override
	public void onDiscoverStop()
	{
		FragmentActivity activity=(FragmentActivity)getActivity();
		if (activity!=null)
		{
			setDiscover(false);
		}
	}
	@Override
	public void onDiscover(RemoteAndroidInfo info,boolean replace)
	{
		if (!mIsDiscover && !replace) return;
		addRemoteAndroid(info, replace);
	}

	private void addRemoteAndroid(RemoteAndroidInfo info, boolean replace)
	{
		String[] uris=info.getUris();
		mRetain.mItemAdapter.remove(URL_NOT_KNOWN);
    	for (String s:uris)
    	{
    		final String key='['+info.getName()+"] "+s;
    		mRetain.mItemAdapter.remove(key);
    		mRetain.mItemAdapter.add(key);
    	}
    	if (!mRetain.mDiscoveredAndroid.contains(info))
    	{
    		mRetain.mDiscoveredAndroid.add(info);
    		replace=false;
    	}
		if (!replace)
		{
			if (uris.length!=0)
			{
				mRetain.mRemoteAndroids.add(new RemoteAndroidContext(this));
		    	int position=mRetain.mRemoteAndroids.size()-1;
		    	RemoteAndroidContext racontext=mRetain.mRemoteAndroids.get(position);
		    	racontext.mUri='['+info.getName()+"] "+uris[0];
			}
			else
			{
				Log.e(TAG,"Discover error"); //FIXME: Discover error
			}
		}
		mRetain.mItemAdapter.sort(new Comparator<String>()
		{
			@Override
			public int compare(String object1, String object2)
			{
				return object1.compareTo(object2);
			}
		});
		mRetain.mItemAdapter.notifyDataSetChanged();
		mRetain.mAdapter.notifyDataSetChanged();
	}

	private void setDiscover(boolean status)
	{
		mDiscover.setChecked(status);
		mDiscover.setIcon(status ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_menu_search);
		((FragmentActivity)getActivity()).setProgressBarIndeterminateVisibility(status ? Boolean.TRUE : Boolean.FALSE);
	}
	private void setBurst(boolean status)
	{
		mBurst.setChecked(status);
		mBurst.setIcon(status ? R.drawable.ic_menu_burst_toggle : R.drawable.ic_menu_burst);
		mRetain.mBurst=status;
	}
	
	
    /** Called when the activity is first created. */
	class Caches
	{
		Spinner mUri;
		ToggleButton mActive;
		Button mInstall;
		Button mBind;
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
		outState.putBoolean(EXTRA_BURST, mRetain.mBurst);
	}
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        Intent market=RemoteAndroidManager.getIntentForMarket(getActivity());
        if (market==null)
        {
        	final RemoteAndroidManager manager=RemoteAndroidManager.getManager(getActivity());
	        //mRetain=(Retain)getActivity().getLastNonConfigurationInstance(); // FIXME: single fragment in activity
        	mRetain=null;
	        if (mRetain==null)
	        {
	        	mRetain=new Retain();
	        	if (savedInstanceState!=null)
	        	{
	        		mRetain.mBurst=savedInstanceState.getBoolean(EXTRA_BURST);
	        	}
	        	
	            mRetain.mItemAdapter=
	        		new ArrayAdapter<String>(getActivity(),
	        				android.R.layout.simple_spinner_item,mItems);
	            mRetain.mAdapter = new BaseAdapter()
	    		{
	    			
	    			@Override
	    			public View getView(int position, View convertView, ViewGroup parent)
	    			{
	    				View view;
	    				Caches caches;
	    				if (convertView==null)
	    				{
	    					view=getActivity().getLayoutInflater().inflate(R.layout.test_item, null);
	    					caches=new Caches();
	    					caches.mUri=(Spinner)view.findViewById(R.id.uri);
	    					caches.mUri.setOnItemSelectedListener(TestRemoteAndroidListFragment.this);
	    					caches.mActive=(ToggleButton)view.findViewById(R.id.active);
	    					caches.mActive.setOnClickListener(TestRemoteAndroidListFragment.this);
	    					caches.mInstall=(Button)view.findViewById(R.id.install);
	    					caches.mInstall.setOnClickListener(TestRemoteAndroidListFragment.this);
	    					caches.mBind=(Button)view.findViewById(R.id.bind);
	    					caches.mBind.setOnClickListener(TestRemoteAndroidListFragment.this);
	    					caches.mInvoke=(Button)view.findViewById(R.id.invoke);
	    					caches.mInvoke.setOnClickListener(TestRemoteAndroidListFragment.this);
	    					caches.mStatus=(TextView)view.findViewById(R.id.status);
	    					view.setTag(caches);
	    					
	    					mRetain.mItemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    					caches.mUri.setAdapter(mRetain.mItemAdapter);
	    					
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
	    				RemoteAndroidContext racontext=mRetain.mRemoteAndroids.get(position);
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
	    				caches.mActive.setEnabled((racontext.mState!=RemoteAndroidContext.State.BindingRemoteAndroid));
	    				caches.mActive.setChecked(racontext.mRemoteAndroid!=null);
	    				boolean enabled=(racontext.mRemoteAndroid!=null);
	    				caches.mInstall.setEnabled(enabled);
	    				caches.mBind.setEnabled(enabled);
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
	    				return mRetain.mRemoteAndroids.size();
	    			}
	    		};
	        }
	        setListAdapter(mRetain.mAdapter);
	        setHasOptionsMenu(true);
        	new AsyncTask<Void, Void, ListRemoteAndroidInfo>()
        	{
        		@Override
        		protected ListRemoteAndroidInfo doInBackground(Void... paramArrayOfParams)
        		{
    	        	manager.setLog(RemoteAndroidManager.FLAG_LOG_ALL, true);
        			return manager.newDiscoveredAndroid(TestRemoteAndroidListFragment.this);
        		}
        		protected void onPostExecute(ListRemoteAndroidInfo result) 
        		{
        			mRetain.mDiscoveredAndroid=result;
    	        	setDiscover(manager.isDiscovering());
    	        	setBurst(mRetain.mBurst);
        		}
        	}.execute();
        }
    }
	
    @Override
    public void onResume()
    {
    	super.onResume();
    	
       	initAndroids();
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
    			mPreferences=preferences;
    			int size=Integer.parseInt(preferences.getString("init.androids", "0"));
    	    	if (mRetain!=null && mRetain.mRemoteAndroids!=null && mRetain.mRemoteAndroids.size()<=size)
    	    	{
    				{
    					for (int i=mRetain.mRemoteAndroids.size();i<size;++i)
    					{
    						RemoteAndroidContext context=new RemoteAndroidContext(TestRemoteAndroidListFragment.this);
    						context.mUri=(mItems.size()>0) ? mItems.get(0) : URL_NOT_KNOWN;
    						mRetain.mRemoteAndroids.add(context);
    					}
    					mRetain.mItemAdapter.notifyDataSetChanged();
    					mRetain.mAdapter.notifyDataSetChanged();
    				}
    	    	}
    		}
    	}.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, android.view.MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu, menu);
		mDiscover=menu.findItem(R.id.discover);
		mBurst=menu.findItem(R.id.burst);
		getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);
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
				if (mRetain.mDiscoveredAndroid==null) return false;

				SharedPreferences preferences=mPreferences;
				int flags=Integer.parseInt(preferences.getString("discover.mode","1"));
				mRetain.mDiscoveredAndroid.start(flags,RemoteAndroidManager.DISCOVER_INFINITELY);
				mIsDiscover=true;
			}
			else
			{
				mRetain.mDiscoveredAndroid.cancel();
				mIsDiscover=false;
			}
		}
		else if (id == R.id.add)
		{
			startActivityForResult(new Intent(RemoteAndroidManager.ACTION_CONNECT_ANDROID), REQUEST_CONNECT_CODE);
		}
		else if (id == R.id.clear)
		{
			if (mRetain.mDiscoveredAndroid!=null)
			{
				mRetain.mDiscoveredAndroid.clear();
				mRetain.mRemoteAndroids.clear();
		    	initAndroids();
				mRetain.mAdapter.notifyDataSetChanged();
			}
		}
		else if (id == R.id.burst)
		{
			setBurst(!item.isChecked());
		}
		else if (id == R.id.config)
		{
			getActivity().startActivity(new Intent(getActivity(), TestRemoteAndroidPreferenceActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode==REQUEST_CONNECT_CODE && resultCode==Activity.RESULT_OK)
		{
			
			RemoteAndroidInfo info=(RemoteAndroidInfo)data.getParcelableExtra(RemoteAndroidManager.EXTRA_DISCOVER);
			addRemoteAndroid(info, false);
		}
	}
	@Override
	public void onClick(final View view)
	{
		final int position=(Integer)view.getTag();
		RemoteAndroidContext context=mRetain.mRemoteAndroids.get(position);
		switch (view.getId())
		{
			case R.id.active:
				Log.d(TAG,"Active "+position);
				view.setEnabled(false);
				if (mRetain.mBurst)
				{
					for (final RemoteAndroidContext ra:mRetain.mRemoteAndroids)
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
					if (!((ToggleButton)view).isChecked())
						context.disconnect(getActivity());
					else
						context.connect(getActivity());
				}
				break;
				
			case R.id.install:
				Log.d(TAG,"Install "+position);
				if (mRetain.mBurst)
				{
					for (RemoteAndroidContext ra:mRetain.mRemoteAndroids) // FIXME Install en mode burst
					{
						ra.install(getActivity());
					}					
				}
				else
					context.install(getActivity());
				break;
				
			case R.id.bind:
				Log.d(TAG,"Invoke "+position);
				if (mRetain.mBurst)
				{
					for (final RemoteAndroidContext ra:mRetain.mRemoteAndroids)
					{
						mExecutors.execute(new Runnable()
						{
							@Override
							public void run()
							{
								ra.bindService(getActivity());
							}
						});
					}					
				}
				else
					context.bindService(getActivity());
				break;
				
			case R.id.invoke:
				Log.d(TAG,"Invoke "+position);
				if (mRetain.mBurst)
				{
					for (final RemoteAndroidContext ra:mRetain.mRemoteAndroids)
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
		RemoteAndroidContext context=mRetain.mRemoteAndroids.get(position);
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
				if (mRetain!=null)
					mRetain.mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
	}
}