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

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

// http://www.adakoda.com/android/000104.html
// http://d.hatena.ne.jp/Korsakov/20100704/1278221724
// http://android.keicode.com/basics/ui-spinner.php

public class ConfigActivity extends Activity implements View.OnClickListener,
		OnTimeChangedListener, OnItemSelectedListener {

	private static final String TAG = "ConfigActivity";

	TimePicker startTimePicker;
	//TimePicker endTimePicker;
	Button resetButton;
	Spinner durationSpinner;

	int startHour = 0;
	int startMinute = 0;
	int	duration = 0;

	String[] durations;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		durations = getResources().getStringArray(R.array.duration_list);

		startTimePicker = (TimePicker) findViewById(R.id.startTimePicker);
		durationSpinner = (Spinner) findViewById(R.id.durationSpinner);

		startTimePicker.setIs24HourView(true);
		startTimePicker.setOnTimeChangedListener(this);
		resetStartTimePicker();

		/*
		endTimePicker = (TimePicker) findViewById(R.id.endTimePicker);
		endTimePicker.setIs24HourView(true);
		 */

		resetButton = (Button) findViewById(R.id.button_reset);
		resetButton.setOnClickListener(this);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, durations);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		durationSpinner.setAdapter(adapter);
		durationSpinner.setOnItemSelectedListener(this);

	}

	@Override
	public void onPause() {
		final int	MINUTE_IN_MILLIS = 60 * 1000;
		super.onPause();
		
		// save configuration in MyApplication
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
		startCalendar.set(Calendar.MINUTE, startMinute);
		startCalendar.set(Calendar.SECOND, 0);
		
		long	startTimestamp = startCalendar.getTimeInMillis();
		long	endTimestamp = startTimestamp + duration * MINUTE_IN_MILLIS;
		
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTimeInMillis(endTimestamp);
		
		MyApplication application = (MyApplication) getApplication();
		application.saveAttendanceDuration(startCalendar,endCalendar);
	}
	
	public void onClick(View v) {
		if (v == resetButton) {
			resetStartTimePicker();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d(TAG, "onTouchEvent: event=" + event);

		return false;
	}
	
	public void resetStartTimePicker() {
		int hourOfDay = 0;
		int minute = 0;

		Calendar calendar = Calendar.getInstance();
		hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		minute = calendar.get(Calendar.MINUTE);

		startHour = hourOfDay;
		startMinute = minute;

		startTimePicker.setCurrentHour(hourOfDay);
		startTimePicker.setCurrentMinute(minute);

		// reset duration spinner too
		durationSpinner.setSelection(0);
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		startHour = hourOfDay;
		startMinute = minute;

		Log.d(TAG, "onTimeChanged: startTime=" + startHour + ":" + startMinute);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Spinner spinner = (Spinner) parent;
		String item = (String) spinner.getSelectedItem();
		
		duration = Integer.parseInt(item);
		Log.d(TAG, "onItemSelected: duration=" + duration);
	}
	
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
		Log.d(TAG, "onNothingSelected: duration=" + duration);
    }

}
