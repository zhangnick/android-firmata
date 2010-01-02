/*
  Copyright (c) 2009 Bonifaz Kaufmann. 
  
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
package edu.mit.media.hlt.firmata.bluetooth;

import it.gerdavax.android.bluetooth.BluetoothException;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import edu.mit.media.hlt.firmata.R;
import edu.mit.media.hlt.firmata.arduino.Arduino;
import edu.mit.media.hlt.firmata.arduino.Arduino_v1;
import edu.mit.media.hlt.firmata.serial.StandAloneSerial;

public abstract class BTActivity extends Activity implements OnBTEventListener{
	
	private static final String TAG = "BTActivity";
	
	public static final int SHOW_DISCOVERED_DEVICES = 100;
	
	private static final int MENU_SCAN = 1;
	private static final int MENU_BLUETOOTH = 3;
	
	static ArrayList<String> devices;
	static BTHandler btHandler;
	
	protected Handler handler = new Handler();
	ProgressDialog dialog;
	
	
	protected Arduino arduino;
	protected String lastConnectedAddress;

	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
			btHandler = new BTHandler(this);
			btHandler.addOnBTEventListener(this);
			lastConnectedAddress = PreferenceManager.getDefaultSharedPreferences(this).getString("address", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    

	@Override
	protected void onStop() {
		super.onStop();
		disconnect();
		btHandler.removeOnBTEventListener(this);
		btHandler.close();
    }
	
	public boolean connectTo(final String address){
		if (address != null){
			try {
				btHandler.connectTo(address);
				// TODO use a factory to decide which Serial class should be instantiated
				// since we will support different android versions and
				// standalone as well as amarino based communication
				arduino = new Arduino_v1(new StandAloneSerial(btHandler));
				// inform our parent
				connectionEstablished();
				return true;
			} catch (BluetoothException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			Toast.makeText(
					this,
					"No device found! Please scan for devices to connect.",
					Toast.LENGTH_SHORT)
			.show();
		}
		return false;
	}
	
	
	public boolean connect(){
		return connectTo(lastConnectedAddress);
	}
	
	public void disconnect(){
		btHandler.disconnect();
	}
    

    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(Menu.FIRST, MENU_SCAN, Menu.NONE, R.string.menu_scan)
			.setIcon(android.R.drawable.ic_menu_search);
		menu.add(Menu.FIRST+1, MENU_BLUETOOTH, Menu.NONE, R.string.bluetooth_on_off)
			.setIcon(android.R.drawable.button_onoff_indicator_on);

		return true;
	}
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean supRetVal = super.onPrepareOptionsMenu(menu);
		try {
			if (btHandler.isBluetoothEnabled()){
				menu.findItem(MENU_BLUETOOTH).setIcon(android.R.drawable.button_onoff_indicator_on);
			}
			else {
				menu.findItem(MENU_BLUETOOTH).setIcon(android.R.drawable.button_onoff_indicator_off);
			}
		} catch (Exception e) {
			Log.d(TAG, "Local Bluetooth not accessible");
		}
		return supRetVal;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case MENU_SCAN:
				scanForDevices();
				break;
			case MENU_BLUETOOTH:
				toggleBluetooth();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
    
    protected void scanForDevices(){
		Log.d(TAG, "Start scanning...");
		btHandler.scanForDevices();
	}
    
    protected void toggleBluetooth() {
		try {
			if (btHandler.isBluetoothEnabled()){
				showProgressDialog(getString(R.string.bluetooth_disable_dialog_msg));
				btHandler.setBluetoothEnabled(false);
			}
			else {
				showProgressDialog(getString(R.string.enabling_bluetooth_progess_dialog_msg));
				btHandler.setBluetoothEnabled(true);
				
			}
		} catch (Exception e) {
			Log.d(TAG, "Local Bluetooth not accessible");
		}
	}
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK){
			switch (requestCode){
				case SHOW_DISCOVERED_DEVICES:
					lastConnectedAddress = data.getStringExtra(DiscoveredDevicesList.ADDRESS_EXTRA);
					// we immediately pair, if device is already paired this will
					// result in a connect
					btHandler.pairDevices(lastConnectedAddress);
					break;
				default:
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
    
    
	
    protected void showProgressDialog(final String msg) {
		handler.post(new Runnable() {
			public void run() {
				dialog = ProgressDialog.show(
						BTActivity.this, "", msg, true, true);
			}
		});
	}
    
    protected void showInfoDialog(final int title, final int msg) {
		handler.post(new Runnable(){
			@Override
			public void run() {
				new AlertDialog.Builder(BTActivity.this)
					.setTitle(title)
					.setMessage(msg)
					.setCancelable(true)
					.setPositiveButton(R.string.ok, null)
					.create()
					.show();
			}
		});
	}

	
	/*
	 * Hides a previously shown progress dialog.
	 */
    protected void hideProgressDialog() {
		handler.post(new Runnable() {
			public void run() {
				if (dialog != null) {
					dialog.dismiss();
				}
			}
		});
	}
	
	
	public static String getRemoteName(String address){
		try {
			return btHandler.getRemoteName(address);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/*
	 * This method is used to inform our super class that
	 * connection has been established
	 */
	public abstract void connectionEstablished();
	
	
	@Override
	public void bluetoothDisabled() {
		hideProgressDialog();
	}

	@Override
	public void bluetoothEnabled() {
		hideProgressDialog();
	}

	@Override
	public void deviceFound(String address) {
		// not used here, we wait for the complete list
	}

	@Override
	public void scanCompleted(ArrayList<String> devs) {
		Log.d(TAG, "scanCompleted");
		devices = devs;
		hideProgressDialog();
		startActivityForResult(
				new Intent(this, DiscoveredDevicesList.class),
				SHOW_DISCOVERED_DEVICES);
	}

	@Override
	public void scanStarted() {
		Log.d(TAG, "scanStarted");
		devices = new ArrayList<String>();
		showProgressDialog(getString(R.string.scanning_devices_progress_dialog_msg));
	}

	

	@Override
	public void paired() {
		Log.d(TAG, "paired");
		hideProgressDialog();
		
		try {
			btHandler.connectTo(lastConnectedAddress);
			
			// save last successful connected address
			PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putString("address", lastConnectedAddress)
				.commit();
			
			// inform our parent
			connectionEstablished();
			
		} catch (BluetoothException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pinRequested() {
		Log.d(TAG, "pinRequested");
		hideProgressDialog();
		
	}

	@Override
	public void serviceChannelNotAvailable(int arg0) {
	}
	
	@Override
	public void gotServiceChannel(int arg0, int arg1) {
	}
    

}
