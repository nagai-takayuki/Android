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
package jp.ac.kumamoto_u.cc.fcf;


import android.util.Log;

public class FelicaService extends FelicaArea {

	public static boolean debug = false;
	
	private static final String TAG = "FelicaService";

	FelicaService(int code) {
		super(code);
		
		if(debug) {
			Log.d(TAG, "FelicaService code=" + Integer.toHexString(code));
			Log.d(TAG, "FelicaService attribute=" + Integer.toHexString(attribute));
		}
		
	}
	
	boolean	isReadOnly(){
		return ((attribute & 0x02) != 0);
	}

	boolean	isReadableWithoutEncryption(){
		
		if(debug) {
			Log.d(TAG, "isReadableWithoutEncryption attribute=" + Integer.toHexString(attribute));
		}
		
		return ((attribute & 0x01) != 0);
	}
}
