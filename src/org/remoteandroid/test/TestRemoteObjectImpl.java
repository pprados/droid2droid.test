package org.remoteandroid.test;

import android.os.Build;
import android.os.RemoteException;

public class TestRemoteObjectImpl extends TestRemoteObject.Stub
{

	@Override
	public String helloWord(String msg) throws RemoteException
	{
		return msg+" from "+Build.MANUFACTURER+" "+Build.MODEL;
	}

}
