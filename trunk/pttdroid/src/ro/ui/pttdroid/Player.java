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
import java.net.MulticastSocket;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Audio;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;

public class Player extends Service implements Runnable
{
		
	private AudioTrack 	player;
	private Thread 	playerThread;
	private IBinder playerBinder = new PlayerBinder();
	
	private boolean isRunning = true;	
	private boolean isFinishing = false;	
	
	private DatagramSocket 	socket;
	private MulticastSocket multicastSocket;
	private DatagramPacket 	packet;	
	
	private short[] pcmFrame = new short[Audio.FRAME_SIZE];
	private byte[] 	encodedFrame;
	
	private int progress = 0;
	
	public class PlayerBinder extends Binder 
	{
		Player getService() 
		{        
            return Player.this;
        }
    }
	
	@Override
    public void onCreate() 
	{
		Notification notification = new Notification(R.drawable.icon, 
				getText(R.string.app_name),
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),
		        getText(R.string.app_running), pendingIntent);
		startForeground(1, notification);
    }
	
	@Override
    public IBinder onBind(Intent intent)
	{    
		playerThread = new Thread(this);
		playerThread.start();
		
		return playerBinder;
    }	
	
	@Override
    public boolean onUnbind(Intent intent) 
	{
		finish();
        return false;
    }
	
	public void run() 
	{
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);				 
		
		while(!isFinishing()) 
		{			
			init();
			while(isRunning()) 
			{								
				try 
				{				
					socket.receive(packet);	
					
					// If echo is turned off and I was the packet sender then skip playing
					if(AudioSettings.getEchoState()==AudioSettings.ECHO_OFF && IP.contains(packet.getAddress()))
						continue;
					
					// Decode audio
					if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) 
					{
						Speex.decode(encodedFrame, encodedFrame.length, pcmFrame);
						player.write(pcmFrame, 0, Audio.FRAME_SIZE);
					}
					else 
					{			
						player.write(encodedFrame, 0, Audio.FRAME_SIZE_IN_BYTES);
					}	
					
					// Make some progress
					makeProgress();
				}
				catch(IOException e) 
				{
					Log.error(getClass(), e);
				}	
			}		
		
			release();	
			synchronized(this) 
			{
				try 
				{	
					if(!isFinishing())
						this.wait();
				}
				catch(InterruptedException e) 
				{
					Log.error(getClass(), e);
				}
			}			
		}				
	}
	
	private void init() 
	{	
		try 
		{						
			player = new AudioTrack(
					AudioManager.STREAM_MUSIC, 
					Audio.SAMPLE_RATE, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					Audio.ENCODING_PCM_NUM_BITS, 
					Audio.TRACK_BUFFER_SIZE, 
					AudioTrack.MODE_STREAM);	

			switch(CommSettings.getCastType()) 
			{
				case CommSettings.BROADCAST:
					socket = new DatagramSocket(CommSettings.getPort());
					socket.setBroadcast(true);
				break;
				case CommSettings.MULTICAST:
					multicastSocket = new MulticastSocket(CommSettings.getPort());
					multicastSocket.joinGroup(CommSettings.getMulticastAddr());
					socket = multicastSocket;				
				break;
				case CommSettings.UNICAST:
					socket = new DatagramSocket(CommSettings.getPort());
				break;
			}							
			
			if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) 
				encodedFrame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];
			else 
				encodedFrame = new byte[Audio.FRAME_SIZE_IN_BYTES];
			
			packet = new DatagramPacket(encodedFrame, encodedFrame.length);			
			
			player.play();				
		}
		catch(IOException e) 
		{
			Log.error(getClass(), e);
		}		
	}
	
	private void release() 
	{
		if(player!=null) 
		{
			player.stop();		
			player.release();
		}
	}
	
	private synchronized void makeProgress() 
	{
		progress++;
	}
	
	public synchronized int getProgress() 
	{
		return progress;
	}
	
	private synchronized boolean isRunning() 
	{
		return isRunning;
	}
	
	public synchronized void resumeAudio() 
	{
		isRunning = true;
		this.notify();
	}
		
	public synchronized void pauseAudio() 
	{
		isRunning = false;
		leaveGroup();
		socket.close();
	}
		
	private synchronized boolean isFinishing() 
	{
		return isFinishing;
	}
	
	private synchronized void finish() 
	{
		pauseAudio();
		isFinishing = true;
		this.notify();
	}
	
	private void leaveGroup() 
	{
		try 
		{
			multicastSocket.leaveGroup(CommSettings.getMulticastAddr());
		}
		catch(IOException e) 
		{
			Log.error(getClass(), e);
		}
		catch(NullPointerException e) 
		{
			Log.error(getClass(), e);
		}		
	}
	
	/**
	 * 
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException
	{
		playerThread.join();
	}
		
}
