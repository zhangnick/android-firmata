/*
  Firmata_SampleApp - Firmata implementation for Android
  Copyright (c) 2009 Bonifaz Kaufmann.  All right reserved.
  
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package edu.mit.media.hlt.firmata;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;
import edu.mit.media.hlt.firmata.arduino.Arduino;
import edu.mit.media.hlt.firmata.bluetooth.BTActivity;

/**
 * This application demonstrates how to control an Arduino microcontroller
 * without writing a separate Arduino sketch.
 * To make this happen, you have to upload the Firmata firmware onto your Arduino.
 * 
 * The code below reads digial and analog input data and displays them.
 * Pin 13 (onboard led) is used as an output pin.
 * 
 * 
 * Note: only Firmata firmware version 1 has been tested so far.
 * 
 * @author Bonifaz Kaufmann
 *
 */
public class Firmata_SampleApp extends BTActivity implements OnClickListener{
	
	private static final String TAG = "Firmata";
	private static final int LOOP_PERIOD = 200; // 100ms (can go even faster ~ 30ms still works nicely)

	ArrayList<TextView> digitalPins;
	ArrayList<ProgressBar> analogPins;
	Button connectBtn;
	Timer timer;

	boolean pin13 = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        connectBtn = (Button) findViewById(R.id.connectBtn);
        // when we have already successfully established a connection before
        // the last address is restored otherwise address is null
        if (lastConnectedAddress != null)
        	connectBtn.setText(getString(R.string.connect, lastConnectedAddress));
        else
        	connectBtn.setText(R.string.menu_scan);
          
        connectBtn.setOnClickListener(this);
        
        ViewGroup vg_digital = (ViewGroup)findViewById(R.id.container_digital);
        ViewGroup vg_analog = (ViewGroup)findViewById(R.id.container_analog);
        
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT,
    			LayoutParams.FILL_PARENT);
        lp.bottomMargin = 1;
        
        // create views for digital pins
        digitalPins = new ArrayList<TextView>(12);
        for (int i=2; i<=13; i++){
        	TextView tv = new TextView(this);
        	// we use tag to identify our view later
        	// we cannot use id since this must be unique in a view hierarchy
        	tv.setTag(i);
        	tv.setText("Pin " + i);
        	tv.setBackgroundColor(Color.GRAY);
        	tv.setTextColor(Color.BLACK);
        	tv.setTextSize(16);
        	tv.setPadding(8, 2, 0, 2);
        	tv.setLayoutParams(lp);
        	digitalPins.add(tv);
        	vg_digital.addView(tv);
        }
        
        // create views for analog pins
        analogPins = new ArrayList<ProgressBar>(6);
        for (int i=0;i<=5;i++){
        	TextView tv = new TextView(this);
        	tv.setText("Analog " + i);
        	tv.setTextColor(Color.GRAY);
        	tv.setTextSize(16);
        	tv.setPadding(8, 2, 2, 2);
        	tv.setLayoutParams(lp);
        	vg_analog.addView(tv);
        	
        	ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        	pb.setPadding(8, 4, 0, 2);
        	pb.setLayoutParams(lp);
        	pb.setMax(1023); // according to max of analogRead
        	pb.setProgress(50);
        	pb.setTag(i);
        	analogPins.add(pb);
        	vg_analog.addView(pb);
        }

    }
    
    
	@Override
	protected void onStop() {
		super.onStop();
		if (timer != null){
			timer.cancel();
		}
	}


	@Override
	public void connectionEstablished() {
		Log.d(TAG, "connected");
		connectBtn.setText(R.string.disconnect);
		
		/* put your arduino setup code in here */
		
		// we set one output pin and the rest are input pins
		arduino.pinMode(13, Arduino.OUTPUT);		
		for (int i=0; i<13;i++){
			arduino.pinMode(i, Arduino.INPUT);
			// for some reason pull up doesn't work
			//arduino.digitalWrite(i, Arduino.HIGH);
		}

		
		// start a timer to a nice slow looper
		timer = new Timer();
        timer.schedule(new MyTimerTask(), 1000, LOOP_PERIOD);
	}
	

	@Override
	public void onClick(View v) {
		
		switch (v.getId()){
			case R.id.connectBtn:
				if (connectBtn.getText().toString().equals(getString(R.string.disconnect))){
					if (timer != null){
						timer.cancel();
					}
					disconnect();
					resetPinStates();
					connectBtn.setText(getString(R.string.connect, lastConnectedAddress));
				}
				else {
					if (lastConnectedAddress == null){
						scanForDevices();
					}
					else {
						// connects to the last successful connected device
						connectToArduino();
					}
				}
				break;
		}
	}
	
	private void resetPinStates(){
		for (TextView tv : digitalPins)
			tv.setBackgroundColor(Color.GRAY);
		pin13 = false;
	}

	
	private void connectToArduino() {
		if (!connect()){
			connectBtn.setText(getString(R.string.connect, lastConnectedAddress));
			Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void togglePin13(TextView tv){
		if (pin13) {
			arduino.digitalWrite(13, Arduino.LOW);
			tv.setBackgroundColor(Color.GRAY);
		}
		else {
			arduino.digitalWrite(13, Arduino.HIGH);
			tv.setBackgroundColor(Color.GREEN);
		}
		pin13 = !pin13;
	}

	Handler arduinoLoop = new Handler(){
		public void handleMessage(Message msg){
			// check state of all digital pins
			for (TextView tv : digitalPins){
				int pin = (Integer)tv.getTag();
				
				// ok pin 13 is an output pin so lets us it that way
				if (pin == 13){
					// toggle our output pin
					togglePin13(tv);
				}
				else {
					// all the other pins are input, read and update UI
					if (arduino.digitalRead(pin)==Arduino.HIGH){
						tv.setBackgroundColor(Color.GREEN);
					}
					else {
						tv.setBackgroundColor(Color.GRAY);
					}
				}
			}
			// update UI for analog pin states
			//int tag;
			for (ProgressBar pb : analogPins){
				//tag = (Integer)pb.getTag();
				//if (tag == 5) Log.d(TAG, "5: " + arduino.analogRead(5) );
				pb.setProgress(arduino.analogRead((Integer)pb.getTag()));
			}
			
		}
	};
	
	class MyTimerTask extends TimerTask{

		@Override
		public void run() {
			// use a handler, since this operations does not happen on the UI Thread
			arduinoLoop.sendEmptyMessage(0);
		}
	};


}

