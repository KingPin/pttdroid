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

import ro.ui.pttdroid.codecs.Speex;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class Main extends Activity implements OnTouchListener {
	
	/*
	 * True if the activity is really starting for the first time.
	 * False if the activity starts after it was previously closed by a configuration change, like screen orientation. 
	 */
	private static boolean isStarting = true;	
	
	private ImageView microphoneImage;	
	
	/*
	 * Threads for recording and playing audio data.
	 * This threads are stopped only if isFinishing() returns true on onDestroy(), meaning the back button was pressed.
	 * With other words, recorder and player threads will still be running if an screen orientation event occurs.
	 */	
	private static Player player;	
	private static Recorder recorder;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
                                      
        init();        
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	// Initialize codec 
    	Speex.open(Settings.getSpeexQuality());
    	
    	player.resumeAudio();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	
    	player.pauseAudio();
    	recorder.pauseAudio();
    	
    	// Release codec resources
    	Speex.close();
    }
            
    @Override
    public void onDestroy() {
    	super.onDestroy();  
    	uninit();    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.settings:       		
    		startActivityForResult(new Intent(this, Settings.class), 0);
    		return true;    		
    	}
    	return false;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Settings.getSettings(this);     	    	
    }
    
    public boolean onTouch(View v, MotionEvent e) {    	    	
    	switch(e.getAction()) {
    	case MotionEvent.ACTION_DOWN:
    		recorder.resumeAudio();
    		microphoneImage.setImageResource(R.drawable.microphone_pressed_image);
    		break;
    	case MotionEvent.ACTION_UP:    		
    		microphoneImage.setImageResource(R.drawable.microphone_normal_image);
    		recorder.pauseAudio();
    		break;
    	}
    	return true;
    }
    
    private void init() {
    	microphoneImage = (ImageView) findViewById(R.id.microphone_image);
    	microphoneImage.setOnTouchListener(this);    	    	    	    	    	
    	
    	// When the volume keys will be pressed the audio stream volume will be changed. 
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	    	    	
    	/*
    	 * If the activity is first time created and not destroyed and created again like on an orientation screen change event.
    	 * This will be executed only once.
    	 */    	    	    	    	
    	if(isStarting) {    		
    		Settings.getSettings(this);
    		    	    	    		
    		player = new Player();    		    		     		    	
    		recorder = new Recorder();    		    		    		    		
    		
    		player.start();
    		recorder.start();
    		
    		isStarting = false;    		
    	}
    }
    
    private void uninit() {    	
    	// If the back key was pressed.
    	if(isFinishing()) {

    		// Force threads to finish.
    		player.finish();    		    		
    		recorder.finish();
    		
    		try {
    			player.join();
    			recorder.join();
    		}
    		catch(InterruptedException e) {
    			Log.d("PTT", e.toString());
    		}
    		player = null;
    		recorder = null;
    	    		
    		// Resetting isStarting.
    		isStarting = true;     		
    	}
    }
        
}