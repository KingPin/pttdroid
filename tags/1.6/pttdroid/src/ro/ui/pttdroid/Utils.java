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

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

public abstract class Utils 
{
	
	/** PCM	*/
	public static final int SAMPLE_RATE = 8000;
	public static final int FRAME_SIZE = 160;
	public static final int FRAME_SIZE_IN_SHORTS = 160;
	public static final int FRAME_SIZE_IN_BYTES = 320;
	public static final int ENCODING_PCM_NUM_BITS = AudioFormat.ENCODING_PCM_16BIT;	
				
	public static final int RECORD_BUFFER_SIZE = Math.max(
			SAMPLE_RATE, 
			ceil(AudioRecord.getMinBufferSize(
					SAMPLE_RATE, 
					AudioFormat.CHANNEL_IN_MONO, 
					ENCODING_PCM_NUM_BITS)));
	
	public static final int TRACK_BUFFER_SIZE = Math.max(
			FRAME_SIZE, 
			ceil(AudioTrack.getMinBufferSize(
					SAMPLE_RATE, 
					AudioFormat.CHANNEL_OUT_MONO, 
					ENCODING_PCM_NUM_BITS)));			
	
	private static int ceil(int size) 
	{
		return (int) Math.ceil(((double) size / FRAME_SIZE )) * FRAME_SIZE;
	}
	
	/**	Network Interfaces	*/
	private static ArrayList<InetAddress> addresses = new ArrayList<InetAddress>(); 

	/**
	 * 
	 */
	public static void loadNetworkInterfaces() 
	{
		addresses.clear();
		try 
		{
			Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
			
			while(ni.hasMoreElements()) 
			{								
				Enumeration<InetAddress> inetAddresseEnum = ni.nextElement().getInetAddresses();
				
				while(inetAddresseEnum.hasMoreElements())
					addresses.add(inetAddresseEnum.nextElement());
			}
		}
		catch(IOException e) 
		{
			log(Utils.class, e);
		}
	}
	
	/**
	 * 
	 * @param addr
	 * @return
	 */
	public static boolean existsNetworkInterface(InetAddress addr) 
	{
		return addresses.contains(addr);
	}
	
	/**
	 * 
	 * @param c
	 * @param e
	 */
	public static void log(Class<?> c, Exception e)
	{
		Log.e(c.getCanonicalName(), e.toString());
	}	
}
