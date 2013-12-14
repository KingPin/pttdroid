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
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Audio;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class Player extends Service
{
	private PlayerThread 	playerThread;
	
	private IBinder playerBinder = new PlayerBinder();
	
	private TelephonyManager	telephonyManager;
	private PhoneCallListener	phoneCallListener;
	
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
		playerThread = new PlayerThread();
		playerThread.start();
		
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		phoneCallListener = new PhoneCallListener();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		Notification notification = new Notification(R.drawable.notif_icon, 
				getText(R.string.app_name),
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),
		        getText(R.string.app_running), pendingIntent);
		startForeground(1, notification);
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{			
		return START_NOT_STICKY;
	}
	
	@Override
    public IBinder onBind(Intent intent)
	{    
		return playerBinder;
    }	
	
	@Override
	public void onDestroy() 
	{
		playerThread.shutdown();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_NONE);
	}
	
	public int getProgress() 
	{
		return playerThread.getProgress();
	}
	
	private class PlayerThread extends Thread
	{
		private AudioTrack 	player;
		
		private volatile boolean running = true;	
		private volatile boolean playing = true;
		
		private DatagramSocket 	socket;		
		private DatagramPacket 	packet;	
		
		private short[] pcmFrame = new short[Audio.FRAME_SIZE];
		private byte[] 	encodedFrame;
		
		private AtomicInteger progress = new AtomicInteger(0);
		
		public void run() 
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);				 
			
			while(isRunning())
			{				
				init();
				
				while(isPlaying()) 
				{							
					try 
					{				
						socket.receive(packet);	
					}
					catch(SocketException e) //Due to socket.close() 
					{
						break;
					}
					catch(IOException e) 
					{
						Log.error(getClass(), e);
					}					

					if(AudioSettings.getEchoState()==AudioSettings.ECHO_OFF && IP.contains(packet.getAddress()))
						continue;

					if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) 
					{
						Speex.decode(encodedFrame, encodedFrame.length, pcmFrame);
						player.write(pcmFrame, 0, Audio.FRAME_SIZE);
					}
					else 
					{			
						player.write(encodedFrame, 0, Audio.FRAME_SIZE_IN_BYTES);
					}	

					progress.incrementAndGet();
				}

				player.stop();
				player.release();
				
				synchronized(this)
				{
					try 
					{	
						if(isRunning())
							wait();
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
						socket = new MulticastSocket(CommSettings.getPort());
						((MulticastSocket) socket).joinGroup(CommSettings.getMulticastAddr());										
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
		
		private synchronized boolean isRunning()
		{
			return running;
		}
			
		private synchronized boolean isPlaying()
		{
			return playing;
		}
				
		public synchronized void pauseAudio() 
		{				
			playing = false;
			
			try
			{
				if(socket instanceof MulticastSocket)
					((MulticastSocket) socket).leaveGroup(CommSettings.getMulticastAddr());
				socket.close();
			}
			catch (IOException e) 
			{
				Log.error(getClass(), e);
			}					
		}
		
		public synchronized void resumeAudio() 
		{
			playing = true;
			notify();
		}
									
		private synchronized void shutdown() 
		{			
			pauseAudio();
			running = false;						
			notify();
		}
		
		public int getProgress() 
		{
			return progress.intValue();
		}

	}
	
	private class PhoneCallListener extends PhoneStateListener
	{
		
		@Override
		public void onCallStateChanged (int state, String incomingNumber)
		{
			if(state==TelephonyManager.CALL_STATE_OFFHOOK)
				playerThread.pauseAudio();
			else if(state==TelephonyManager.CALL_STATE_IDLE)
				playerThread.resumeAudio();
		}
		
	}
			
}
