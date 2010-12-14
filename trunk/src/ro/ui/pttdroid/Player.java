/*
 * Copyright (C) 2010 The pttdroid Open Source Project
 * 
 * This file is part of pttdroid (http://www.code.google.com/p/pttdroid/)
 * 
 * pttdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package ro.ui.pttdroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.util.AudioParams;
import ro.ui.pttdroid.util.PhoneIPs;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class Player extends Thread {
		
	private AudioTrack player;
	private boolean isRunning = true;	
	private boolean isFinishing = false;	
	
	private DatagramSocket socket;
	private MulticastSocket multicastSocket;
	private DatagramPacket packet;	
	
	private short[] pcmFrame = new short[AudioParams.FRAME_SIZE];
	private byte[] encodedFrame;	
	
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);				 
		
		while(!isFinishing()) {			
			init();
			while(isRunning()) {				
				try {				
					socket.receive(packet);		
					if(Settings.getCastType()!=Settings.UNICAST && PhoneIPs.contains(packet.getAddress()))
						continue;
															
					Speex.decode(encodedFrame, encodedFrame.length, pcmFrame);
					player.write(pcmFrame, 0, AudioParams.FRAME_SIZE);							
				}
				catch(IOException e) {
					Log.d("Player", e.toString());
				}	
			}		
		
			release();	
			synchronized(this) {
				try {	
					if(!isFinishing())
						this.wait();
				}
				catch(InterruptedException e) {
					Log.d("Player", e.toString());
				}
			}			
		}				
	}
	
	private void init() {				
		try {						
			player = new AudioTrack(
					AudioManager.STREAM_MUSIC, 
					AudioParams.SAMPLE_RATE, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioParams.ENCODING_PCM_NUM_BITS, 
					AudioParams.TRACK_BUFFER_SIZE, 
					AudioTrack.MODE_STREAM);	

			switch(Settings.getCastType()) {
			case Settings.BROADCAST:
				socket = new DatagramSocket(Settings.getPort());
				socket.setBroadcast(true);
				break;
			case Settings.MULTICAST:
				multicastSocket = new MulticastSocket(Settings.getPort());
				multicastSocket.joinGroup(Settings.getMulticastAddr());
				socket = multicastSocket;				
				break;
			case Settings.UNICAST:
				socket = new DatagramSocket(Settings.getPort());
				break;
			}							
			
			encodedFrame = new byte[Speex.getEncodedSize(Settings.getSpeexQuality())];
			
			packet = new DatagramPacket(encodedFrame, encodedFrame.length);			
			
			player.play();				
		}
		catch(IOException e) {
			Log.d("Player", e.toString());
		}		
	}
	
	private void release() {
		if(player!=null) {
			player.stop();		
			player.release();
		}
	}
	
	public synchronized boolean isRunning() {
		return isRunning;
	}
	
	public synchronized void resumeAudio() {
		isRunning = true;
		this.notify();
	}
		
	public synchronized void pauseAudio() {
		isRunning = false;
		leaveGroup();
		socket.close();
	}
		
	public synchronized boolean isFinishing() {
		return isFinishing;
	}
	
	public synchronized void finish() {
		pauseAudio();
		isFinishing = true;
		this.notify();
	}
	
	private void leaveGroup() {
		try {
			multicastSocket.leaveGroup(Settings.getMulticastAddr());
		}
		catch(IOException e) {
			Log.d("Player", e.toString());
		}
		catch(NullPointerException e) {
			Log.d("Player", e.toString());
		}		
	}
	
}
