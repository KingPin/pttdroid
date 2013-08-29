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
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends Activity implements OnTouchListener 
{
	
	private static boolean isFirstLaunch = true;	
	
	private ImageView microphoneImage;	
	
	public static final int MIC_STATE_NORMAL = 0;
	public static final int MIC_STATE_PRESSED = 1;
	public static final int MIC_STATE_DISABLED = 2;
	
	private int microphoneState = MIC_STATE_NORMAL;
		
	private static volatile	Player	player;
	private static Recorder 		recorder;	
	
	// Block recording when playing
	private Handler 			handler = new Handler();
	private MicrophoneSwitcher 	microphoneSwitcher;
	private static int 			storedProgress = 0;	
	private static final int 	PROGRESS_CHECK_PERIOD = 100;
	
	private static ServiceConnection playerServiceConnection;
		
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);        
                      
        init();  
        
		microphoneSwitcher = new MicrophoneSwitcher();
		handler.postDelayed(microphoneSwitcher, PROGRESS_CHECK_PERIOD);
    }
                    
    @Override
    public void onDestroy() 
    {
    	super.onDestroy();
    
    	handler.removeCallbacks(microphoneSwitcher);
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
    	case R.id.quit:
    		release();
    		finish();
    		return true;
    	case R.id.settings_comm:
    		i = new Intent(this, CommSettings.class);
    		startActivityForResult(i, 0);    		
    		return true;
    	case R.id.settings_audio:
    		i = new Intent(this, AudioSettings.class);
    		startActivityForResult(i, 0);    		
    		return true;    
    	case R.id.settings_reset_all:
    		return resetAllSettings();    		
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    /**
     * Reset all settings to their default value
     * @return
     */
    private boolean resetAllSettings() 
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	Editor editor = prefs.edit();
    	editor.clear();
    	editor.commit();   
    	
    	Toast toast = Toast.makeText(this, getString(R.string.setting_reset_all_confirm), Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.CENTER, 0, 0);
    	toast.show();

    	return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	CommSettings.getSettings(this);     	    	
    	AudioSettings.getSettings(this);    	
    }
    
    public boolean onTouch(View v, MotionEvent e) 
    {
    	if(getMicrophoneState()!=MIC_STATE_DISABLED) 
    	{    		
    		switch(e.getAction()) {
    		case MotionEvent.ACTION_DOWN:    			
    			recorder.resumeAudio();
    			setMicrophoneState(MIC_STATE_PRESSED);
    			break;
    		case MotionEvent.ACTION_UP:
    			setMicrophoneState(MIC_STATE_NORMAL);
    			recorder.pauseAudio();    			
    			break;
    		}
    	}
    	return true;
    }
    
    public synchronized void setMicrophoneState(int state) 
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
    
    public synchronized int getMicrophoneState() 
    {
    	return microphoneState;
    }
    
    private void init() 
    {
    	microphoneImage = (ImageView) findViewById(R.id.microphone_image);
    	microphoneImage.setOnTouchListener(this);    	    	    	    	    	
    	    	    	
    	if(isFirstLaunch) 
    	{    		
    		CommSettings.getSettings(this);
    		AudioSettings.getSettings(this);
    		
    		// 
        	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    		
    		//
    		Speex.open(AudioSettings.getSpeexQuality());
    		    	    	    		
    		Intent playerIntent = new Intent(this, Player.class);
    		playerServiceConnection = new PlayerServiceConnection();
    		bindService(playerIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    		
    		//
    		recorder = new Recorder();
    		recorder.start();     		    		
    		    		
    		isFirstLaunch = false;    		
    	}
    }
    
    private void release() 
    {    	
    	unbindService(playerServiceConnection);    		    		
    	recorder.finish();
    		
        Speex.close();        	
    }     
    
    private class PlayerServiceConnection implements ServiceConnection
	{	
		public void onServiceDisconnected(ComponentName arg0) 
		{					
			player.finish();			
		}
		
		public void onServiceConnected(ComponentName arg0, IBinder arg1) 
		{
			player = ((PlayerBinder) arg1).getService();			
		}
	};
	
	private class MicrophoneSwitcher implements Runnable
	{	
		public void run() 
		{					
			
			if(player!=null)
			{
				int currentProgress = player.getProgress();
			
				if(currentProgress!=storedProgress) 
				{
					if(getMicrophoneState()!=MIC_STATE_DISABLED) 
					{
						recorder.pauseAudio();
						setMicrophoneState(MIC_STATE_DISABLED);							
					}						 							
				}
				else 
				{
					if(getMicrophoneState()==MIC_STATE_DISABLED)
						setMicrophoneState(MIC_STATE_NORMAL);
				}
			
				storedProgress = currentProgress;
			}
			
			handler.postDelayed(this, PROGRESS_CHECK_PERIOD);
		}
	};
        
}