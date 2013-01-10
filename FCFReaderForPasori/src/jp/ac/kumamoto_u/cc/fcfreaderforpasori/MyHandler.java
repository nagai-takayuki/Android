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

import java.util.Date;

import android.os.Handler;

public class MyHandler extends Handler {
	private String	handlerID;

	private static int TIMEOUT_MESSAGE = 1;
	private static int INTERVAL_MS = 1000;
	
	private boolean	doesContinue = true;
	
	MyHandler(){
		super();
		Date now = new Date();
		handlerID = now.toString();
	}
	
	MyHandler(int milliseconds){
		this();
		setIntervalInMills(milliseconds);
	}

	public String id(){
		return handlerID;
	}
	
	public void setIntervalInMills(int milliseconds){
		INTERVAL_MS = milliseconds;
	}
	
	public void next(){
		if(doesContinue) {
			sendEmptyMessageDelayed(TIMEOUT_MESSAGE,INTERVAL_MS);
		}
	}
	
	public void start(){
		doesContinue = true;
		sendEmptyMessage(TIMEOUT_MESSAGE);
	}

	public void stop(){
		doesContinue = false;
	}
}
