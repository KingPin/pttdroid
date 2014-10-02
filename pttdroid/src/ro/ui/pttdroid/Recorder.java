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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class Recorder extends Thread
{	
	private AudioRecord recorder;
	
	private volatile boolean recording = false;
	private volatile boolean running = true;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	private short[] pcmFrame = new short[Utils.FRAME_SIZE];
	private byte[] encodedFrame;
			
	public void run() 
	{
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		init();
		
		while(isRunning()) 
		{		
			recorder.startRecording();
			
			while(isRecording()) 
			{
				if(Settings.useSpeex()==Settings.USE_SPEEX) 
				{
					recorder.read(pcmFrame, 0, Utils.FRAME_SIZE);
					Speex.encode(pcmFrame, encodedFrame);						
				}
				else
				{
					recorder.read(encodedFrame, 0, Utils.FRAME_SIZE_IN_BYTES);
				}

				try 
				{																				 
					socket.send(packet);
				}
				catch(IOException e) 
				{
					Utils.log(getClass(), e);
				}	
			}		
			
			recorder.stop();
				
			synchronized(this)
			{
				try 
				{	
					if(isRunning())
						wait();
				}
				catch(InterruptedException e) 
				{
					Utils.log(getClass(), e);
				}
			}
		}		
		
		socket.close();				
		recorder.release();
	}
	
	private void init() 
	{				
		try 
		{	    	
			Utils.loadNetworkInterfaces();
			
			socket = new DatagramSocket();			
			InetAddress addr = null;
			
			switch(Settings.getCastType()) 
			{
				case Settings.BROADCAST:
					socket.setBroadcast(true);		
					addr = Settings.getBroadcastAddr();
				break;
				case Settings.MULTICAST:
					addr = Settings.getMulticastAddr();					
				break;
				case Settings.UNICAST:
					addr = Settings.getUnicastAddr();					
				break;
			}							
			
			if(Settings.useSpeex()==Settings.USE_SPEEX)
				encodedFrame = new byte[Speex.getEncodedSize(Settings.getSpeexQuality())];
			else 
				encodedFrame = new byte[Utils.FRAME_SIZE_IN_BYTES];
			
			packet = new DatagramPacket(
					encodedFrame, 
					encodedFrame.length, 
					addr, 
					Settings.getPort());

	    	recorder = new AudioRecord(
	    			AudioSource.MIC, 
	    			Utils.SAMPLE_RATE, 
	    			AudioFormat.CHANNEL_IN_MONO, 
	    			Utils.ENCODING_PCM_NUM_BITS, 
	    			Utils.RECORD_BUFFER_SIZE);							
		}
		catch(SocketException e) 
		{
			Utils.log(getClass(), e);
		}	
	}
	
	private synchronized boolean isRunning()
	{
		return running;
	}
	
	private synchronized boolean isRecording()
	{
		return recording;
	}
	
	public synchronized void pauseAudio() 
	{				
		recording = false;					
	}	 

	public synchronized void resumeAudio() 
	{				
		recording = true;			
		notify();
	}
				
	public synchronized void shutdown() 
	{
		pauseAudio();
		running = false;				
		notify();
	}
		
}
