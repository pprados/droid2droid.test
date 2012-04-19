package org.remoteandroid.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.remoteandroid.ListRemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidInfo;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.RemoteAndroidManager.ManagerListener;
import org.remoteandroid.test.RemoteAndroidContext.OnRemoteAndroidContextUpdated;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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


// Checkbox pour la découverte des ProximityNetwork
public class TestRemoteAndroidListFragment extends SherlockListFragment 
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
	MenuItem mDiscoverMenu;
	MenuItem mBurstMenu;
	
	boolean mIsDiscover;

	private List<String> mItems=new ArrayList<String>();

	private volatile ListRemoteAndroidInfo mDiscoveredAndroid;
	private ArrayList<RemoteAndroidContext> mRemoteAndroids=new ArrayList<RemoteAndroidContext>(5);
	private BaseAdapter mAdapter;
	private ArrayAdapter<String> mItemAdapter;
	private boolean mBurst;
	private RemoteAndroidManager mManager;
	
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
				mRemoteAndroids.add(new RemoteAndroidContext(getActivity(),mManager,this));
		    	int position=mRemoteAndroids.size()-1;
		    	RemoteAndroidContext racontext=mRemoteAndroids.get(position);
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
		outState.putBoolean(EXTRA_BURST, mBurst);
	}
	private static final long MAX_WAIT_MANAGER=5000L;
	@Override
    public void onActivityCreated(Bundle savedInstanceState) 
    {
		Log.v(TAG,"Fragment.onCreate");
        super.onCreate(savedInstanceState);
mItems.add("[hardcoded] ip://192.168.0.63"); // FIXME: a virer        
		mDiscoveredAndroid=RemoteAndroidManager.newDiscoveredAndroid(getActivity(),this);
        Intent market=RemoteAndroidManager.getIntentForMarket(getActivity());
        if (market==null)
        {
//	        mRetain=(Retain)getActivity().getLastNonConfigurationInstance(); // FIXME: single fragment in activity
        	RemoteAndroidManager.bindManager(getActivity(), new ManagerListener()
        	{

				@Override
				public void bind(RemoteAndroidManager manager)
				{
					mManager=manager;
				}

				@Override
				public void unbind(RemoteAndroidManager manager)
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
    				RemoteAndroidContext racontext=mRemoteAndroids.get(position);
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
    						RemoteAndroidContext context=new RemoteAndroidContext(getActivity(),mManager,TestRemoteAndroidListFragment.this);
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
				mManager.startDiscover(flags,RemoteAndroidManager.DISCOVER_INFINITELY);
				mIsDiscover=true;
			}
			else
			{
				mManager.cancelDiscover();
				mIsDiscover=false;
			}
		}
		else if (id == R.id.add)
		{
			Intent intent=new Intent(RemoteAndroidManager.ACTION_CONNECT_ANDROID);
//			intent.setFlags(
//				Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
//				|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//				);
			intent.putExtra(RemoteAndroidManager.EXTRA_THEME_ID,android.R.style.Theme_Holo_Light_DarkActionBar);
//			intent.putExtra(RemoteAndroidManager.EXTRA_TITLE, "TEST");
			intent.putExtra(RemoteAndroidManager.EXTRA_SUBTITLE, "Select device.");
			SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
			int flags=Integer.parseInt(preferences.getString("remote_bind.flags","0"));
			intent.putExtra(RemoteAndroidManager.EXTRA_FLAGS, flags);
			
			startActivityForResult(intent, REQUEST_CONNECT_CODE);
		}
		else if (id == R.id.clear)
		{
			if (mDiscoveredAndroid!=null)
			{
				mDiscoveredAndroid.clear();
				for (final RemoteAndroidContext rac:mRemoteAndroids)
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
		final RemoteAndroidContext context=mRemoteAndroids.get(position);
		switch (view.getId())
		{
			case R.id.active:
				Log.d(TAG,"Active "+position);
				view.setEnabled(false);
				if (mBurst)
				{
					for (final RemoteAndroidContext ra:mRemoteAndroids)
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
					for (RemoteAndroidContext ra:mRemoteAndroids) // FIXME Install en mode burst
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
					for (final RemoteAndroidContext ra:mRemoteAndroids)
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
				if (mBurst)
				{
					for (final RemoteAndroidContext ra:mRemoteAndroids)
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
		RemoteAndroidContext context=mRemoteAndroids.get(position);
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
	public final RemoteAndroidManager getRemoteAndroidManager()
	{
		return mManager;
	}
}