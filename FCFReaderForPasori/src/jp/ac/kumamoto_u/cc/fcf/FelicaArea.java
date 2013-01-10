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

public class FelicaArea {
	
	private static boolean debug = false;
	
	private static final String TAG = "FelicaArea";

	protected int	ID;
	protected int	attribute;
	protected int	code;
	protected byte[] data;
	
	public String	byteToHexString(byte val) {
		int	u,l;
		
		u = (val >> 4) & 0x0F;
		l = val & 0x0F;
		
		char	uChar = (char)( u < 10 ? '0' + u : 'A' + (u-10));
		char	lChar = (char)( l < 10 ? '0' + l : 'A' + (l-10));
		
		return ""+uChar+lChar;
	}

	FelicaArea(){
		ID = 0x00;
		attribute = 0;
		code = 0;
	}
	
	FelicaArea(int data) {
		code = data;
		ID = (code >> 6);
//		attribute = (code & 0x3F);
		attribute = code;
		
		if(debug) {
	        Log.d(TAG, "FelicaArea code=" + Integer.toHexString(code));
	        Log.d(TAG, "FelicaArea attribute=" + Integer.toHexString(attribute));
		}
		
	}
	
	public int	code(){
		return code;
	}
	
	public int	attribute(){
		return attribute;
	}

	public int	ID(){
		return ID;
	}
	
	void clearData(){
		data = new byte[0];
	}
	
	void setData(byte _data[]){
		data = _data;
	}
	
	public byte[] data(){
		return data;
	}
	
	private String blockDataString(int blockIndex) {
		StringBuffer	buffer = new StringBuffer();
		int	offset = blockIndex * Felica.FELICA_BLOCK_LENGTH;
		
		for(int i=0;i<Felica.FELICA_BLOCK_LENGTH;i++){
			byte	val = data[offset+i];
			buffer.append(' ');
			buffer.append(byteToHexString(val));
		}
		
		return buffer.toString();
	}
	
	public void showData(){
		int	blockIndex = 0;
		int blockCount = data.length / Felica.FELICA_BLOCK_LENGTH;
		int offset = 0;
		
		for(int i=0;i<blockCount;i++) {
			offset = i * Felica.FELICA_BLOCK_LENGTH;
            Log.d(TAG, "showData " + Integer.toHexString(offset) +":"+ blockDataString(i));

		}
	}
}
