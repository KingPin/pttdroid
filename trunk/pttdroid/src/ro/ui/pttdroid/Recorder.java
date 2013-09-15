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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Audio;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class Recorder extends Thread 
{
	
	private final int SO_TIMEOUT = 0;
	
	private AudioRecord recorder;
	
	private volatile boolean recording = false;
	private volatile boolean running = true;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	private short[] pcmFrame = new short[Audio.FRAME_SIZE];
	private byte[] encodedFrame;
			
	public void run() 
	{
		// Set audio specific thread priority
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		init();
		
		while(isRunning()) 
		{		
			recorder.startRecording();
			
			while(isRecording()) 
			{
				try 
				{		
					// Read PCM from the microphone buffer & encode it
					if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) 
					{
						recorder.read(pcmFrame, 0, Audio.FRAME_SIZE);
						Speex.encode(pcmFrame, encodedFrame);						
					}
					else 
						recorder.read(encodedFrame, 0, Audio.FRAME_SIZE_IN_BYTES);						
																		
					// Send encoded frame packed within an UDP datagram
					socket.send(packet);
				}
				catch(IOException e) 
				{
					Log.error(getClass(), e);
				}	
			}		
			
			recorder.stop();
					
			try 
			{	
				synchronized(this)
				{
					if(isRunning())
						wait();
				}
			}
			catch(InterruptedException e) 
			{
				Log.error(getClass(), e);
			}
		}		
		
		//Release allocated resources
		socket.close();				
		recorder.release();
	}
	
	private void init() 
	{				
		try 
		{	    	
			IP.load();
			
			socket = new DatagramSocket();
			socket.setSoTimeout(SO_TIMEOUT);
			InetAddress addr = null;
			
			switch(CommSettings.getCastType()) 
			{
				case CommSettings.BROADCAST:
					socket.setBroadcast(true);		
					addr = CommSettings.getBroadcastAddr();
				break;
				case CommSettings.MULTICAST:
					addr = CommSettings.getMulticastAddr();					
				break;
				case CommSettings.UNICAST:
					addr = CommSettings.getUnicastAddr();					
				break;
			}							
			
			if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX)
				encodedFrame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];
			else 
				encodedFrame = new byte[Audio.FRAME_SIZE_IN_BYTES];
			
			packet = new DatagramPacket(
					encodedFrame, 
					encodedFrame.length, 
					addr, 
					CommSettings.getPort());

	    	recorder = new AudioRecord(
	    			AudioSource.MIC, 
	    			Audio.SAMPLE_RATE, 
	    			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
	    			Audio.ENCODING_PCM_NUM_BITS, 
	    			Audio.RECORD_BUFFER_SIZE);							
		}
		catch(SocketException e) 
		{
			Log.error(getClass(), e);
		}	
	}
	
	private synchronized boolean isRecording()
	{
		return recording;
	}
	
	private synchronized boolean isRunning()
	{
		return running;
	}
		
	public synchronized void resumeAudio() 
	{				
		recording = true;			
		notify();
	}
		
	public synchronized void pauseAudio() 
	{				
		recording = false;			
	}	 
			
	public synchronized void shutdown() 
	{
		pauseAudio();
		running = false;		
		notify();
	}
		
}
