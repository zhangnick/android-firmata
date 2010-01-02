package edu.mit.media.hlt.firmata.serial;

import java.util.LinkedList;

import android.util.Log;

import edu.mit.media.hlt.firmata.bluetooth.OnReceivedDataListener;

public abstract class Serial implements OnReceivedDataListener{
	
	public static final String TAG = "Serial";
	
	OnSerialEventListener serialEventListener;
	LinkedList<Byte> buffer;
	
	public Serial(){
		buffer = new LinkedList<Byte>();
	}
	
	public abstract void dispose();
	public abstract void write(int what);
	public abstract void write(byte bytes[]);
	public abstract void write(String what);
	
	public int available() {
		return buffer.size();
	}

	public int read() {
		if (buffer.isEmpty()) 
			return -1;
		return buffer.poll();
	}
	
	public int last(){
		if (buffer.isEmpty()) 
			return -1;
		return buffer.removeLast();
	}
	
	public void clearReadBuffer(){
		buffer.clear();
	}
	
	public void registerArduino(OnSerialEventListener listener){
		this.serialEventListener = listener;
	}
	
	public synchronized void receivedData(byte[] bytes) {
		//Log.d(TAG, "data received: " + new String(bytes));
		for (byte b : bytes)
			buffer.add(b);
		serialEventListener.serialEvent();
	}
}
