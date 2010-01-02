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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.mit.media.hlt.firmata.Firmata_SampleApp;
import edu.mit.media.hlt.firmata.R;

/**
 * This ListActivity is shown when a list of discovered devices
 * is available. 
 * 
 * Call this Activity using startActivityForResult, 
 * and it will returns the selected device address. 
 * Attached to the intent as an extra called ADDRESS_EXTRA.
 * 
 * @author Bonifaz Kaufmann
 *
 */
public class DiscoveredDevicesList extends ListActivity {
	 
	public static String ADDRESS_EXTRA = "device_address";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discovered_devices_list);
		setTitle("Discovered Devices");

		DeviceAdapter adapter = new DeviceAdapter();
		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView lv, View view, int position, long id) {
		super.onListItemClick(lv, view, position, id);
		
		final String address = BTActivity.devices.get(position);

		new AlertDialog.Builder(this)
			.setMessage(R.string.connect_to_device)
			.setCancelable(false)
			.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent i = new Intent();
							i.putExtra(ADDRESS_EXTRA, address);
							setResult(RESULT_OK, i);
							finish();
						}
					})
			.setNegativeButton(R.string.no, 
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					})
			.create()
			.show();
	}
	
	protected class DeviceAdapter extends BaseAdapter {

		public int getCount() {
			if (BTActivity.devices != null) {
				return BTActivity.devices.size();
			}
			return 0;
		}

		public Object getItem(int position) {
			return BTActivity.devices.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view = null;

			if (convertView == null) {
				view = new LinearLayout(DiscoveredDevicesList.this);
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater vi = (LayoutInflater) DiscoveredDevicesList.this.getSystemService(inflater);
				vi.inflate(R.layout.discovered_device, view, true);
			} else {
				view = (LinearLayout) convertView;
			}

			TextView addressTextView = (TextView) view.findViewById(R.id.address);
			TextView nameTextView = (TextView) view.findViewById(R.id.name);

			String address = BTActivity.devices.get(position);
			String name = "null";
			String deviceClass = "";
			
			try {
				name = Firmata_SampleApp.getRemoteName(address);
			} catch (Exception e) {
				e.printStackTrace();
				name = "NAME NOT PUBLISHED";
			}

			if (name != null) {
				deviceClass = name;
			}

			addressTextView.setText(address);
			nameTextView.setText(deviceClass);

			return view;
		}

	}

}
