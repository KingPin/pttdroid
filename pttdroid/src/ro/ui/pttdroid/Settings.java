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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
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
	
	private static int speakMode;
	
	public static final int SPEAK_MODE_TOUCH_HOLD = 0;
	public static final int SPEAK_MODE_TAP_SPEAK_TAP = 1;
		
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		findPreference("reset").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) 
			{
				return resetSettings();				
			}
		});
	}	
	
    /**
     * Reset settings to their default value
     * @return
     */
    private boolean resetSettings() 
    {
    	synchronized(Settings.class)
    	{
	    	new AlertDialog.Builder(this)
	    		.setTitle(R.string.reset_label)
	    		.setMessage(R.string.reset_confirm)
	    		.setPositiveButton(android.R.string.yes, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) 
					{
				    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				    	
				    	Editor editor = prefs.edit();
				    	editor.clear();
				    	editor.commit();  
				    	
				    	finish();
					}
				})
				.setNegativeButton(android.R.string.no, null)
	    		.show();
	    	
	    	return true;
    	}
    }	
	
	/**
	 * Build cache settings
	 * @param context
	 */
	public static synchronized void buildCache(Context context)
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
        	
        	speakMode = Integer.parseInt(prefs.getString(
        			"speak_mode", 
        			res.getStringArray(R.array.speak_mode_values)[0]));
		}
		catch(UnknownHostException e) 
		{
			Utils.log(Settings.class, e);
		}
	}
	
	public static synchronized int getCastType() 
	{
		return castType;
	}
		
	public static synchronized InetAddress getBroadcastAddr() 
	{
		return broadcastAddr;
	}	
	
	public static synchronized InetAddress getMulticastAddr() 
	{
		return multicastAddr;
	}
	
	public static synchronized InetAddress getUnicastAddr() 
	{
		return unicastAddr;
	}	
	
	public static synchronized int getPort() 
	{
		return port;
	}		

	public static synchronized boolean useSpeex() 
	{
		return useSpeex;
	}	

	public static synchronized int getSpeexQuality() 
	{
		return speexQuality;
	}
	
	public static synchronized boolean getEchoState() 
	{
		return echoState;
	}

	public static synchronized int getSpeakMode()
	{
		return speakMode;
	}
}
