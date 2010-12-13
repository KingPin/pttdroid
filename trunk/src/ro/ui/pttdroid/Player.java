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
				socket = new DatagramSocket(AudioParams.PORT);
				socket.setBroadcast(true);
				break;
			case Settings.MULTICAST:
				multicastSocket = new MulticastSocket(AudioParams.PORT);
				multicastSocket.joinGroup(Settings.getMulticastAddr());
				socket = multicastSocket;				
				break;
			case Settings.UNICAST:
				socket = new DatagramSocket(AudioParams.PORT);
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
