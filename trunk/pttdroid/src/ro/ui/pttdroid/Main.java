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

import ro.ui.pttdroid.Player.PlayerBinder;
import ro.ui.pttdroid.codecs.Speex;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class Main extends ActionBarActivity
{
	
	private static boolean firstLaunch = true;	
			
	private static Recorder 		recorder;	
	
	private MicrophoneSwitcher 	microphoneSwitcher;
	
	private static Intent 		playerIntent;
		
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        init();                     
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
		microphoneSwitcher = new MicrophoneSwitcher();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	recorder.pauseAudio();
    	microphoneSwitcher.shutdown();
    	
    	if(!isFinishing())
    		shutdown();
    }
                           
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	getMenuInflater().inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	Intent i; 
    	
    	switch(item.getItemId()) {
    	case R.id.settings:
    		i = new Intent(this, Settings.class);
    		startActivityForResult(i, 0);    		
    		return true;
    	case R.id.quit:
    		shutdown();    		
    		finish();
    		return true;    		
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	Settings.buildCache(this);    	
    }
        
    private void init() 
    {    	    	    	
    	if(firstLaunch) 
    	{    		
    		Settings.buildCache(this);    		
    		 
        	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    		
    		Speex.open(Settings.getSpeexQuality());
    		    	    	 
    		playerIntent = new Intent(this, Player.class);            
            startService(playerIntent);                		
    		
    		recorder = new Recorder();
    		recorder.start();     		    		
    		    		
    		firstLaunch = false;    		
    	}    	
    }
    
    private void shutdown() 
    {    	  
    	firstLaunch = true;    	
    	stopService(playerIntent);
    	recorder.shutdown();    		
        Speex.close();        
    }     
	
	private class MicrophoneSwitcher implements Runnable, OnTouchListener, OnClickListener 
	{	
		private Player		player;

		private Handler 	handler = new Handler();

		private ImageView 	microphoneImage;	
		
		public static final int MIC_STATE_NORMAL = 0;
		public static final int MIC_STATE_PRESSED = 1;
		public static final int MIC_STATE_DISABLED = 2;
		
		private int microphoneState = MIC_STATE_NORMAL;
		
		private int previousProgress = 0;
		
		private static final int	PROGRESS_CHECK_PERIOD = 100;
		
		private Boolean	running = true;
		
		private ServiceConnection	playerServiceConnection;
		
		public MicrophoneSwitcher()
		{
			init();
		}
		
		public void init()
		{
	    	microphoneImage = (ImageView) findViewById(R.id.microphone_image);
	    	if(Settings.getSpeakMode()==Settings.SPEAK_MODE_TOUCH_HOLD)
	    	{
	    		microphoneImage.setOnTouchListener(this);
	    		microphoneImage.setOnClickListener(null);
	    	}
	    	else
	    	{
	    		microphoneImage.setOnTouchListener(null);
	    		microphoneImage.setOnClickListener(this);
	    	}
	    
	    	 
	    	Intent intent = new Intent(Main.this, Player.class); 
    		playerServiceConnection = new PlayerServiceConnection();
    		bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE);

	    	handler.postDelayed(this, PROGRESS_CHECK_PERIOD);	    	
		}
	    
		public void run() 
		{		
			synchronized(running)
			{
				if(running && player!=null)
				{
					int currentProgress = player.getProgress();

					if(currentProgress > previousProgress) 
					{
						if(microphoneState!=MIC_STATE_DISABLED) 
						{
							recorder.pauseAudio();
							setMicrophoneState(MIC_STATE_DISABLED);							
						}						 							
					}
					else 
					{
						if(microphoneState==MIC_STATE_DISABLED)
							setMicrophoneState(MIC_STATE_NORMAL);
					}

					previousProgress = currentProgress;

					handler.postDelayed(this, PROGRESS_CHECK_PERIOD);
				}
			}	
		}
		
		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouch(View v, MotionEvent e) 
	    {
	    	if(microphoneState!=MicrophoneSwitcher.MIC_STATE_DISABLED) 
	    	{    		
	    		switch(e.getAction()) {
	    		case MotionEvent.ACTION_DOWN:    			
	    			recorder.resumeAudio();
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_PRESSED);
	    			break;
	    		case MotionEvent.ACTION_UP:
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_NORMAL);
	    			recorder.pauseAudio();    			
	    			break;
	    		}
	    	}
	    	return true;
	    }
		
		public void onClick(View v) 
		{		
	    	if(microphoneState==MicrophoneSwitcher.MIC_STATE_NORMAL) 
	    	{    		
    			recorder.resumeAudio();
    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_PRESSED);
	    	}
			else if(microphoneState==MicrophoneSwitcher.MIC_STATE_PRESSED) 
			{
				setMicrophoneState(MicrophoneSwitcher.MIC_STATE_NORMAL);
    			recorder.pauseAudio();    			
    	
			}
		}
		
		public void setMicrophoneState(int state) 
	    {
	    	switch(state) {
	    	case MIC_STATE_NORMAL:
	    		microphoneState = MIC_STATE_NORMAL;
	    		microphoneImage.setImageResource(R.drawable.microphone_normal_image);
	    		break;
	    	case MIC_STATE_PRESSED:
	    		microphoneState = MIC_STATE_PRESSED;
	    		microphoneImage.setImageResource(R.drawable.microphone_pressed_image);
	    		break;
	    	case MIC_STATE_DISABLED:
	    		microphoneState = MIC_STATE_DISABLED;
	    		microphoneImage.setImageResource(R.drawable.microphone_disabled_image);
	    		break;    		
	    	}
	    }
	    
		public void shutdown()
		{
			synchronized(running) 
			{
				setMicrophoneState(MicrophoneSwitcher.MIC_STATE_NORMAL);
				
				unbindService(playerServiceConnection);
				handler.removeCallbacks(microphoneSwitcher);			
				running = false;
			}
		}
		
		private class PlayerServiceConnection implements ServiceConnection
		{					
			public void onServiceConnected(ComponentName arg0, IBinder arg1) 
			{
				synchronized(running) 
				{
					player = ((PlayerBinder) arg1).getService();
				}						
			}
			
			public void onServiceDisconnected(ComponentName arg0) 
			{					
				synchronized(running) 
				{
					player = null;
				}						
			}
		}		
		
	};
        
}