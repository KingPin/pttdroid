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
import java.net.InetAddress;
import java.net.SocketException;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.util.AudioParams;
import ro.ui.pttdroid.util.PhoneIPs;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class Recorder extends Thread {
	
	private final int SO_TIMEOUT = 0;
	
	private AudioRecord recorder;
	/*
	 * True if thread is running, false otherwise.
	 * This boolean is used for internal synchronization.
	 */
	private boolean isRunning = false;	
	/*
	 * True if thread is safely stopped.
	 * This boolean must be false in order to be able to start the thread.
	 * After changing it to true the thread is finished, without the ability to start it again.
	 */
	private boolean isFinishing = false;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	private short[] pcmFrame = new short[AudioParams.FRAME_SIZE];
	private byte[] encodedFrame;
	
	public void run() {
		// Set audio specific thread priority
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		while(!isFinishing()) {		
			init();
			while(isRunning()) {				
				try {		
					// Read PCM from the microphone buffer
					recorder.read(pcmFrame, 0, AudioParams.FRAME_SIZE);
										
					// Encode frame
					Speex.encode(pcmFrame, encodedFrame);					
					
					// Send encoded frame packed within an UDP datagram
					socket.send(packet);
				}
				catch(IOException e) {
					Log.d("Recorder", e.toString());
				}	
			}		
		
			release();	
			/*
			 * While is not running block the thread.
			 * By doing it, CPU time is saved.
			 */
			synchronized(this) {
				try {	
					if(!isFinishing())
						this.wait();
				}
				catch(InterruptedException e) {
					Log.d("Recorder", e.toString());
				}
			}					
		}							
	}
	
	private void init() {				
		try {	    	
			PhoneIPs.load();
			
			socket = new DatagramSocket();
			socket.setSoTimeout(SO_TIMEOUT);
			InetAddress addr = null;
			
			switch(Settings.getCastType()) {
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
			
			encodedFrame = new byte[Speex.getEncodedSize(Settings.getSpeexQuality())];
			
			packet = new DatagramPacket(
					encodedFrame, 
					encodedFrame.length, 
					addr, 
					Settings.getPort());

	    	recorder = new AudioRecord(
	    			AudioSource.MIC, 
	    			AudioParams.SAMPLE_RATE, 
	    			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
	    			AudioParams.ENCODING_PCM_NUM_BITS, 
	    			AudioParams.RECORD_BUFFER_SIZE);
	    	
			recorder.startRecording();				
		}
		catch(SocketException e) {
			Log.d("Recorder", e.toString());
		}	
	}
	
	private void release() {				
		if(recorder!=null) {
			recorder.stop();
			recorder.release();
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
	
}
