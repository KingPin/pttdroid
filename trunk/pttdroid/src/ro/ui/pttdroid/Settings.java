/* Copyright 2011 Ionut Ursuleanu
 
This file is part of pttdroid.
 
pttdroid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
 
pttdroid is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with pttdroid.  If not, see <http://www.gnu.org/licenses/>. */

package ro.ui.pttdroid;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ro.ui.pttdroid.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity 
{
	
	private static InetAddress broadcastAddr;
	private static InetAddress multicastAddr;
	private static InetAddress unicastAddr;	
	
	public static final int BROADCAST = 0;
	public static final int MULTICAST = 1;
	public static final int UNICAST = 2;
		
	private static int castType;	
	private static int port;
	
	private static boolean useSpeex;
	private static int speexQuality;
	private static boolean echoState;

	public static final boolean USE_SPEEX = true;
	public static final boolean DONT_USE_SPEEX = false;	
	public static final boolean ECHO_ON = true;
	public static final boolean ECHO_OFF = false;	
		
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
	}		
	
	/**
	 * Build cache settings
	 * @param context
	 */
	public static void buildCache(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		
		try 
		{
    		castType = Integer.parseInt(prefs.getString(
    				"cast_type", 
    				res.getStringArray(R.array.cast_types_values)[0]));			
    		broadcastAddr = InetAddress.getByName(prefs.getString(
    				"broadcast_addr", 
    				res.getString(R.string.broadcast_addr_default)));			
    		multicastAddr = InetAddress.getByName(prefs.getString(
    				"multicast_addr", 
    				res.getString(R.string.multicast_addr_default)));
    		unicastAddr = InetAddress.getByName(prefs.getString(
    				"unicast_addr", 
    				res.getString(R.string.unicast_addr_default)));
    		port = Integer.parseInt(prefs.getString(
    				"port", 
    				res.getString(R.string.port_default)));
    		    		
        	useSpeex = prefs.getBoolean(
        			"use_speex",
        			USE_SPEEX);    		    		
        	speexQuality = Integer.parseInt(prefs.getString(
        			"speex_quality", 
        			res.getStringArray(R.array.speex_quality_values)[0]));
        	echoState = prefs.getBoolean(
        			"echo",
        			ECHO_OFF);    		    		
		}
		catch(UnknownHostException e) 
		{
			Log.error(Settings.class, e);
		}
	}
	
	public static int getCastType() 
	{
		return castType;
	}
		
	public static InetAddress getBroadcastAddr() 
	{
		return broadcastAddr;
	}	
	
	public static InetAddress getMulticastAddr() 
	{
		return multicastAddr;
	}
	
	public static InetAddress getUnicastAddr() 
	{
		return unicastAddr;
	}	
	
	public static int getPort() 
	{
		return port;
	}		

	public static boolean useSpeex() 
	{
		return useSpeex;
	}	

	public static int getSpeexQuality() 
	{
		return speexQuality;
	}
	
	public static boolean getEchoState() 
	{
		return echoState;
	}		

}
