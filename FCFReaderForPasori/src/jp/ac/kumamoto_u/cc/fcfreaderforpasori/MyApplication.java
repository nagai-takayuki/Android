/*******************************************************************************
 * Copyright (c) 2013 Takayuki Nagai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Takayuki Nagai - initial API and implementation
 ******************************************************************************/
package jp.ac.kumamoto_u.cc.fcfreaderforpasori;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import jp.ac.kumamoto_u.cc.fcf.Felica;
import jp.ac.kumamoto_u.cc.fcf.Pasori;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

// http://techbooster.jpn.org/andriod/application/2353/
// http://yuki312.blogspot.jp/2011/11/activity.html

public class MyApplication extends Application {

	private static final String TAG = "MyApplication";
	private String _instanceID;

	private static String	deviceListString;
	
	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection;
	private UsbEndpoint mEndpointIntr;

	static final int RCS_S370_VENDOR_ID = 0x054c;
	static final int RCS_S370_PRODUCT_ID = 0x02e1;

	static final int RCS_S370_ENDPOINT_COUNT = 2;
	static final int RCS_S370_ENDPOINT0_TYPE = UsbConstants.USB_ENDPOINT_XFER_BULK;
	static final int RCS_S370_ENDPOINT1_TYPE = UsbConstants.USB_ENDPOINT_XFER_BULK;

	private UsbEndpoint pasori_ep0;
	private UsbEndpoint pasiri_ep1;

	private Pasori pasori;
	private Felica felica;

	private Calendar startTime;
	private Calendar endTime;

	private boolean	isLocked = false;
	
	@Override
	public void onCreate() {
		_instanceID = Installation.id(getBaseContext());
		Log.d(TAG, "onCreate: instanceID=" + _instanceID);

		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		detectPasori();
	}

	public String instanceID() {
		return _instanceID;
	}

	public String deviceListString() {
		return deviceListString;
	}

	public void flushExternalStorage() {
		// update sdcard storage
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}

	public void detectPasori() {
		// detect Pasori hardware
		// enumerate USB Devices

		pasori = null;

		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		deviceListString = deviceList.values().toString();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		Log.d(TAG, "detectPasori: deviceList=" + deviceListString);
		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			int vendorID = device.getVendorId();
			int productID = device.getProductId();
			Log.d(TAG, "detectPasori: deviceName=" + device.getDeviceName());
			Log.d(TAG, "detectPasori: vendorID=" + vendorID);
			Log.d(TAG, "detectPasori: vendorID=" + productID);

			if ((vendorID == RCS_S370_VENDOR_ID)
					&& (productID == RCS_S370_PRODUCT_ID)) {
				setDevice(device);
			}
		}
	}

	private void setDevice(UsbDevice device) {
		boolean status = false;

		Log.d(TAG, "setDevice device=" + device);
		Log.d(TAG, "setDevice pasori=" + pasori);

		mDevice = device;
		if (pasori == null) {
			pasori = new Pasori();
			status = pasori.open(mUsbManager, mDevice);
		} else {
			// We already have Pasori device.
			status = true;
		}

		Log.d(TAG, "setDevice status=" + status);

		if (status == true) {
			pasori.init_test();
			pasori.version();
			felica = new Felica(pasori);
		} else {
			pasori = null;
		}

	}

	public Pasori pasori() {
		return pasori;
	}

	public Felica felica() {
		return felica;
	}

	public void pasoriAttached(Intent intent) {
		String action = intent.getAction();

		// Log.d(TAG, "deviceName: " + device.getDeviceName());
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			UsbDevice device = (UsbDevice) intent
					.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			setDevice(device);
		}
	}

	public void pasoriDettached(Intent intent) {
		String action = intent.getAction();

		if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			UsbDevice device = (UsbDevice) intent
					.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (mDevice != null && mDevice.equals(device)) {
				setDevice(null);
			}
		}
	}
	
	public void saveAttendanceDuration(Calendar start, Calendar end) {
		startTime = start;
		endTime = end;
	}
	
	public long startTimeInMillis(){
		if(startTime != null) {
			return startTime.getTimeInMillis();
		}
		
		return -1;
	}

	public long endTimeInMillis(){
		if(endTime != null) {
			return endTime.getTimeInMillis();
		}
		
		return -1;
	}
	
	public boolean isLockMode(){
		return	isLocked;
	}
	
	public void setLockMode(boolean flag) {
		isLocked = flag;
	}
}
