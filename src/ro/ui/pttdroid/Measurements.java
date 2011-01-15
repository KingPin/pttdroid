package ro.ui.pttdroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.DecimalFormat;

import ro.ui.pttdroid.settings.CommSettings;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class Measurements extends Activity implements OnSeekBarChangeListener, OnClickListener {
		
	private TextView datagramSizeText;
	private SeekBar datagramSize;
	private int datagramSizeValue;

	private TextView datagramsToSendText;
	private SeekBar datagramsToSend;
	private int datagramsToSendValue;
	
	private Button pingButton;
	private Button replyButton;
	
	DatagramSocket socketS, socketR;
	DatagramPacket packetS, packetR;	
	
	private ProgressDialog pingProgress;		
	private ProgressDialog replyProgress;	
	
	private static final int PING_PROGRESS = 0;	
	private static final int REPLY_PROGRESS = 1;
	private static final int SEND_REPORT_MSG = 0;
	private static final int RECEIVE_REPORT_MSG = 1;
	private static final int HEADER_SIZE = 12;
	
	private TextView sendReport;
	private TextView receiveReport;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measurements);        
                                 
        datagramSize = (SeekBar) findViewById(R.id.datagram_size);
        datagramSize.setOnSeekBarChangeListener(this);        
        datagramSizeText = (TextView) findViewById(R.id.datagram_size_text);
        datagramSizeValue = Integer.parseInt(datagramSizeText.getText().toString());
        
        datagramsToSend = (SeekBar) findViewById(R.id.datagrams_to_send);
        datagramsToSend.setOnSeekBarChangeListener(this);        
        datagramsToSendText = (TextView) findViewById(R.id.datagrams_to_send_text);
        datagramsToSendValue = Integer.parseInt(datagramsToSendText.getText().toString());
                
        pingButton = (Button) findViewById(R.id.ping_button);
        pingButton.setOnClickListener(this);

        replyButton = (Button) findViewById(R.id.reply_button);
        replyButton.setOnClickListener(this);
        
        sendReport = (TextView) findViewById(R.id.send_report);
        receiveReport = (TextView) findViewById(R.id.receive_report);
    }	
        
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    	switch(seekBar.getId()) {
    	case R.id.datagram_size:
    		datagramSizeValue = (progress + 1) * 10;
    		datagramSizeText.setText(Integer.toString(datagramSizeValue));    		
    		break;
    	case R.id.datagrams_to_send:
    		datagramsToSendValue = (progress + 1) * 10;
    		datagramsToSendText.setText(Integer.toString((datagramsToSendValue)));
    		break;
    	}
    }
    
    public void onStartTrackingTouch(SeekBar seekBar) {
    	
    }
    
    public void onStopTrackingTouch(SeekBar seekBar) {
    	
    }
    
    public void onClick(View v) {
    	switch(v.getId()) {
    	case R.id.ping_button:
    		removeDialog(PING_PROGRESS);
    		showDialog(PING_PROGRESS);    		
    		receive();    		
    		send();
    		break;
    	case R.id.reply_button:
    		removeDialog(REPLY_PROGRESS);
    		showDialog(REPLY_PROGRESS);    		
    		reply();    		    		    	
    		break;
    	}
    }
    
    protected Dialog onCreateDialog(int id) {    	
        switch(id) {
        case PING_PROGRESS:
        	pingProgress = new ProgressDialog(this); 
        	pingProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				public void onDismiss(DialogInterface dialog) {
					socketR.close();
					socketS.close();					
				}
			});
        	pingProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	pingProgress.setTitle(R.string.ping_progress_title);
        	pingProgress.setMax(Integer.parseInt((datagramsToSendText.getText().toString())));        	
            return pingProgress;
            
        case REPLY_PROGRESS:
        	replyProgress = new ProgressDialog(this);
        	replyProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				public void onDismiss(DialogInterface dialog) {	
					socketR.close();
					socketS.close();										
				}
			});
        	replyProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	replyProgress.setTitle(R.string.reply_progress_title);
        	replyProgress.setMax(Integer.parseInt((datagramsToSendText.getText().toString())));            
            return replyProgress;
            
        default:
        	return null;
        }    	
    }
    
    private Handler handler = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		Bundle bundle = msg.getData();
    		Resources res = getResources();
    		String report;
    		
    		switch(msg.what) {
    		case SEND_REPORT_MSG:
    			report = res.getString(R.string.send_report);
    			
    			int datagramsSent = bundle.getInt("datagramsSent");
    			
    			sendReport.setText(report
    					.replace("[datagramsSent]", Integer.toString(datagramsSent) 
    							+ " (" + 100 * datagramsSent / datagramsToSendValue + "%)"));    			
    			sendReport.setVisibility(View.VISIBLE);    			
    			break;
    			
    		case RECEIVE_REPORT_MSG:    			
    			report = res.getString(R.string.receive_report);
    			
    			int datagramsReceived = bundle.getInt("datagramsReceived");
    			int unorderedDatagrams = bundle.getInt("unorderedDatagrams");
    			long minDelay = bundle.getLong("minDelay");
    			long maxDelay = bundle.getLong("maxDelay");
    			double averageDelay = bundle.getDouble("averageDelay");
    			
    			receiveReport.setText(report
    					.replace("[datagramsReceived]", Integer.toString(datagramsReceived) 
    							+ " (" + 100 * datagramsReceived / datagramsToSendValue + "%)")
    					.replace("[unorderedDatagrams]", Integer.toString(unorderedDatagrams) 
    							+ " (" + 100 * unorderedDatagrams / datagramsToSendValue + "%)")
    					.replace("[minDelay]", Long.toString(minDelay) + "ms")
    					.replace("[maxDelay]", Long.toString(maxDelay) + "ms")
    					.replace("[averageDelay]", new DecimalFormat("#.##").format(averageDelay) + "ms"));    			
    			receiveReport.setVisibility(View.VISIBLE);      			
    			break;
    		}
    	}
    	
    };
            
    private void send() {    	     	 
    	 Thread thread = new Thread() {
			
			public void run() {
				byte[] datagram = new byte[datagramSizeValue];
				long time = System.currentTimeMillis();
				
				try {
					socketS = new DatagramSocket();
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						socketS.setBroadcast(true);
								
					packetS = new DatagramPacket(
							datagram, 
							datagramSizeValue, 
							CommSettings.getUnicastAddr(), 
							CommSettings.getPort());
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						packetS.setAddress(CommSettings.getBroadcastAddr());
				
					long current;	
					int datagramsSent = 0;
					ByteArrayOutputStream baos = new ByteArrayOutputStream(HEADER_SIZE);
					DataOutputStream dos = new DataOutputStream(baos);
					
					for(int i=0; i<datagramsToSendValue; i++) {
						try {								
							dos.writeInt(i);
							dos.writeLong(System.currentTimeMillis());
							byte[] temp = baos.toByteArray();
							baos.reset();
							
							System.arraycopy(temp, 0, datagram, 0, HEADER_SIZE);							
							socketS.send(packetS);
							
							datagramsSent++;							
							time += 20;
							current = System.currentTimeMillis();							
							if(current<time)
								Thread.sleep(time-current);		    										
						}
						catch(InterruptedException e) {}
						catch(IOException e) {}
					}	
					socketS.close();
					
					Message msg = handler.obtainMessage(SEND_REPORT_MSG);
					Bundle bundle = new Bundle();
					bundle.putInt("datagramsSent", datagramsSent);
					msg.setData(bundle);
					handler.sendMessage(msg);
				}	
				catch(SocketException e) {}
			}
    	};
			
		thread.start();		
    }
    
    private void receive() {
    	Thread thread = new Thread() {
			
			public void run() {
				byte[] datagram = new byte[datagramSizeValue];								
				
				try {
					socketR = new DatagramSocket(CommSettings.getPort());
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						socketR.setBroadcast(true);
					packetR = new DatagramPacket(datagram, datagramSizeValue);
							
					int datagramsReceived = 0;
					int unorderedDatagrams = 0;
					long minDelay = Long.MAX_VALUE;
					long maxDelay = Long.MIN_VALUE;
					long averageDelay = 0;
					
					for(int i=0; i<datagramsToSendValue; i++) {
						try {											
							socketR.receive(packetR);
							datagramsReceived++;
							if(pingProgress!=null)
								pingProgress.incrementProgressBy(1);
							
							ByteArrayInputStream bais = new ByteArrayInputStream(datagram, 0, HEADER_SIZE);
							DataInputStream dis = new DataInputStream(bais);
							int orderNumber = dis.readInt();
							long delay = System.currentTimeMillis() - dis.readLong();							
							
							if(orderNumber!=i)
								unorderedDatagrams++;
							if(delay<minDelay)
								minDelay = delay;
							if(delay>maxDelay)
								maxDelay = delay;
							averageDelay += delay;							
						}
						catch(IOException e) {}
					}
					socketR.close();
					
					if(pingProgress!=null)
						pingProgress.dismiss();
					
					Message msg = handler.obtainMessage(RECEIVE_REPORT_MSG);
					Bundle bundle = new Bundle();
					bundle.putInt("datagramsReceived", datagramsReceived);
					bundle.putInt("unorderedDatagrams", unorderedDatagrams);
					bundle.putLong("minDelay", minDelay);
					bundle.putLong("maxDelay", maxDelay);					
					bundle.putDouble("averageDelay", (double) averageDelay / datagramsReceived);
					msg.setData(bundle);
					handler.sendMessage(msg);					
				}	
				catch(SocketException e) {}
			}
    	};
			
		thread.start();		    	
    }

    private void reply() {
    	Thread thread = new Thread() {
			
			public void run() {
				byte[] datagram = new byte[datagramSizeValue];								
				
				try {
					socketR = new DatagramSocket(CommSettings.getPort());				
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						socketR.setBroadcast(true);
					
					packetR = new DatagramPacket(datagram, datagramSizeValue);
					
					socketS = new DatagramSocket();
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						socketS.setBroadcast(true);
								
					packetS = new DatagramPacket(
							datagram, 
							datagramSizeValue, 
							CommSettings.getUnicastAddr(), 
							CommSettings.getPort());
					if(CommSettings.getCastType()==CommSettings.BROADCAST)
						packetS.setAddress(CommSettings.getBroadcastAddr());
					
					int datagramsReceived = 0;
					int unorderedDatagrams = 0;
					long minDelay = Long.MAX_VALUE;
					long maxDelay = Long.MIN_VALUE;
					long averageDelay = 0;					
												
					for(int i=0; i<datagramsToSendValue; i++) {
						try {											
							socketR.receive(packetR);
							datagramsReceived++;
							socketS.send(packetS);
							if(replyProgress!=null)
								replyProgress.incrementProgressBy(1);
							
							ByteArrayInputStream bais = new ByteArrayInputStream(datagram, 0, HEADER_SIZE);
							DataInputStream dis = new DataInputStream(bais);
							int orderNumber = dis.readInt();
							long delay = System.currentTimeMillis() - dis.readLong();							
							
							if(orderNumber!=i)
								unorderedDatagrams++;
							if(delay<minDelay)
								minDelay = delay;
							if(delay>maxDelay)
								maxDelay = delay;
							averageDelay += delay;														
						}
						catch(IOException e) {}
					}
					socketR.close();
					socketS.close();
					
					if(replyProgress!=null)
						replyProgress.dismiss();
					
					Message msg = handler.obtainMessage(RECEIVE_REPORT_MSG);
					Bundle bundle = new Bundle();
					bundle.putInt("datagramsReceived", datagramsReceived);
					bundle.putInt("unorderedDatagrams", unorderedDatagrams);
					bundle.putLong("minDelay", minDelay);
					bundle.putLong("maxDelay", maxDelay);					
					bundle.putDouble("averageDelay", (double) averageDelay / datagramsReceived);
					msg.setData(bundle);
					handler.sendMessage(msg);										
				}	
				catch(SocketException e) {}
			}
    	};
			
		thread.start();		    	
    }    
    
}
