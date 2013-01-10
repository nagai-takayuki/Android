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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import jp.ac.kumamoto_u.cc.fcf.Pasori;
import jp.ac.kumamoto_u.cc.fcf.Felica;
import jp.ac.kumamoto_u.cc.fcf.FelicaCard;
import jp.ac.kumamoto_u.cc.fcf.FelicaService;
import jp.ac.kumamoto_u.cc.fcf.FCFService;
import jp.ac.kumamoto_u.cc.fcf.KumadaiService;

public class AttendanceActivity extends Activity implements
		View.OnClickListener {

	private static final String TAG = "AttendanceActivity";
	static boolean verbose = false;
	static boolean debug = false;

	private static String msgNowInitializing;
	private static String msgPlaceYourCard;
	private static String msgNotOpened;
	private static String msgAlreadyClosed;

	private static final String	TAG_SESSION_STARTED = "SessionStarted";
	private static final String	TAG_SESSION_ACTIVATED = "SessionActivated";
	private static final String	TAG_SESSION_DEACTIVATED = "SessionDeactivated";
	private static final String	TAG_SESSION_FINISHED = "SessionFinished";
	
	private static final int	STATUS_NOW_INITIALIZING	= 0;
	private static final int	STATUS_NOT_OPENED	= 1;
	private static final int	STATUS_PLACE_YOUR_CARD	= 2;
	private static final int	STATUS_ALREADY_CLOSED	= 3;
	
	private static int	attendStatus = STATUS_NOW_INITIALIZING;
	
	SimpleDateFormat logTimestampFormat = new SimpleDateFormat(
			"yyyy/MM/dd\tHH:mm:ss\tz");

	SimpleDateFormat displayTimestampFormat = new SimpleDateFormat("HH:mm:ss");

	String LOG_FILENAME = "FCF.log";

	TextView userIDView;
	TextView nameView;
	TextView kumadaiIDView;

	ToneGenerator toneGenerator;

	private static int TIMEOUT_MESSAGE = 1;
	private static boolean polling = true;
	private static int INTERVAL_MS = 50;

	private static boolean isForeground = false;
	boolean readFCF = false;
	FelicaCard lastCard = null;

	private MyHandler handler = null;

	private static long startTimestamp;
	private static long endTimestamp;
	private boolean checkStartTime = false;
	private boolean checkEndTime = false;
	private boolean acceptCheckin = false;

	private String statusMessage;
	private String statusSubMessage;

	private MyHandler createReaderHandler() {
		MyHandler _handler = new MyHandler() {

			@Override
			public void dispatchMessage(Message msg) {
				if (msg.what == TIMEOUT_MESSAGE) {
					if (isForeground) {
						readFelica();
						// displayHandlerID(this);
						// displayHandlerTiming(this);
					}
					updateStatus();
					this.next();
				} else {
					super.dispatchMessage(msg);
				}
			}
		};

		_handler.setIntervalInMills(INTERVAL_MS);
		return _handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attendance);

		msgPlaceYourCard = this.getString(R.string.msg_place_card);
		msgNowInitializing = this.getString(R.string.msg_now_initializing);
		msgNotOpened = this.getString(R.string.msg_not_opened);
		msgAlreadyClosed = this.getString(R.string.msg_already_closed);

		statusMessage = msgNowInitializing;
		attendStatus = STATUS_NOW_INITIALIZING;
		
		userIDView = (TextView) findViewById(R.id.textView1);
		nameView = (TextView) findViewById(R.id.textView2);
		kumadaiIDView = (TextView) findViewById(R.id.textView3);

		userIDView.setText(statusMessage);
		nameView.setText("");
		kumadaiIDView.setText("");

		toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM,
				ToneGenerator.MAX_VOLUME);

		handler = createReaderHandler();

		if (polling) {
			handler.start();
			// handler.sendEmptyMessage(TIMEOUT_MESSAGE);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		isForeground = false;

		if (handler != null) {
			handler.stop();
		}

		//saveSessionLog(TAG_SESSION_DEACTIVATED);
		
		MyApplication application = (MyApplication) getApplication();
		application.flushExternalStorage();

	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();
		Log.d(TAG, "onResume intent: " + intent);
		String action = intent.getAction();

		isForeground = true;

		updateStatus();
		/*
		 * userIDView.setText(statusMessage); nameView.setText("");
		 * kumadaiIDView.setText("");
		 */
		redrawStatus();

		if (handler != null) {
			handler.start();
		}

		MyApplication application = (MyApplication) getApplication();
		startTimestamp = application.startTimeInMillis();
		endTimestamp = application.endTimeInMillis();

		if (startTimestamp > 0) {
			checkStartTime = true;
		} else {
			checkStartTime = false;
		}

		if ((endTimestamp > 0) && (endTimestamp > startTimestamp)) {
			checkEndTime = true;
		} else {
			checkEndTime = false;
		}

		//saveSessionLog(TAG_SESSION_ACTIVATED);		
		application.flushExternalStorage();

		Log.d(TAG, "onResume: startTimestamp=" + startTimestamp);
		Log.d(TAG, "onResume: endTimestamp=" + endTimestamp);
	}

	public void onClick(View v) {
	}

	public void saveFCFLog(FCFService service) {

		if (service == null) {
			return;
		}

		String userID = service.userID();
		String name = service.name();
		String userType = service.userType();
		String schoolCode = service.schoolCode();
		String expireDate = service.expireDate();

		String separator = "\t";
		String logString;

		String prefix = logTimestampFormat.format(new Date());

		logString = prefix + separator + userType + separator + userID
				+ separator + schoolCode + separator + expireDate + separator
				+ name;

		Log.d(TAG, logString);
		saveLog(logString);
	}

	public void saveSessionLog(String	status) {

		if (status == null) {
			return;
		}

		String separator = "\t";
		String logString;

		String prefix = logTimestampFormat.format(new Date());

		logString = prefix + separator + status;

		Log.d(TAG, logString);
		saveLog(logString);
	}

	public void saveLog(String logData) {
		try {
			boolean append = true;
			File sdcardDirectory = getBaseContext().getExternalFilesDir(null);
			sdcardDirectory.mkdirs();
			String filePath = sdcardDirectory.getAbsolutePath()
					+ File.separator + LOG_FILENAME;

			Log.d(TAG, "saveLog filePath=" + filePath);
			// OutputStream out =
			// openFileOutput(LOG_FIELNAME,MODE_WORLD_READABLE|MODE_APPEND);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(filePath, append), "UTF-8"));
			writer.append(logData);
			writer.append("\r\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Log.d(TAG, "saveLog exception=" + e);

			e.printStackTrace();
		}
	}

	public void confirm(FCFService service) {
		if ((service != null) && (service.userID() != null)) {
			beep();
		}
	}

	public void beep() {
		boolean doesBeep = Prefs.isBeepEnabled(getBaseContext());

		if (doesBeep == false) {
			return;
		}

		if (toneGenerator != null) {
			int sound = ToneGenerator.TONE_PROP_BEEP;

			sound = ToneGenerator.TONE_SUP_PIP;
			sound = ToneGenerator.TONE_SUP_CONFIRM;
			sound = ToneGenerator.TONE_PROP_BEEP;
			toneGenerator.startTone(sound);
		}
	}

	public void beepError() {
		boolean doesBeep = Prefs.isBeepEnabled(getBaseContext());

		if (doesBeep == false) {
			return;
		}

		if (toneGenerator != null) {
			int sound = ToneGenerator.TONE_PROP_NACK;

			// toneGenerator.startTone(sound);
		}
	}

	private Felica felica() {
		MyApplication application = (MyApplication) getApplication();

		return application.felica();
	}

	public void readFelica() {
		FelicaCard card = null;
		Felica felica = felica();

		if (acceptCheckin == false) {
			updateStatus();
			/*
			 * userIDView.setText(statusMessage); nameView.setText("");
			 * kumadaiIDView.setText("");
			 */
			redrawStatus();
			lastCard = null;

			return;
		}

		if (felica != null) {
			card = felica.polling();

			if (card != null) {
				felica.request_system_code(card);
			} else {
				updateStatus();
				/*
				 * userIDView.setText(statusMessage); nameView.setText("");
				 * kumadaiIDView.setText("");
				 */
				redrawStatus();

				lastCard = null;
			}
		}

		readFCF = !Prefs.doesReadKumadaiID(getBaseContext());

		if (readFCF) {
			readFCFData(felica, card);
		} else {
			readKumadaiData(felica, card);
		}
	}

	public void dumpFelicaService(Felica felica, FelicaCard card,
			int serviceCode) {

		if (debug) {
			Log.d(TAG, "onClick areaNumber=" + card.areaNumber());
			Log.d(TAG, "onClick serviceNumber=" + card.serviceNumber());
			Log.d(TAG,
					"onClick dumpFelicaService="
							+ Integer.toHexString(serviceCode));
		}

		int serviceIndex = card.indexOfService(serviceCode);
		felica.readServiceDataWithoutEncryption(card, serviceIndex);
		FelicaService service = card.services()[serviceIndex];

		if (debug) {
			service.showData();
		}
	}

	public void dumpFelicaServices(Felica felica, FelicaCard card) {

		felica.search_service(card);
		Log.d(TAG, "onClick areaNumber=" + card.areaNumber());
		Log.d(TAG, "onClick serviceNumber=" + card.serviceNumber());

		FelicaService services[] = card.services();
		for (int i = 0; i < card.serviceNumber(); i++) {
			FelicaService service;

			service = services[i];
			int serviceCode = service.code();
			Log.d(TAG,
					"onClick readService=" + Integer.toHexString(serviceCode));

			/*
			 * if(service.isReadableWithoutEncryption()){
			 * felica.readSingleBlockWithoutEncryption(card, serviceCode, 0); }
			 */

			felica.readServiceDataWithoutEncryption(card, i);

			if (debug) {
				service.showData();
			}
		}
	}

	public void readFCFData(Felica felica, FelicaCard card) {
		FelicaCard fcfCard = null;

		// select FCF system
		if (card != null) {
			int systemCode = Felica.FELICA_SYSTEM_CODE_COMMON_AREA;

			if (card.hasSystem(systemCode)) {
				Log.d(TAG,
						"onClick polling systemCode="
								+ Integer.toHexString(systemCode));
				fcfCard = felica.polling(systemCode);
				if (fcfCard != null) {
					felica.search_service(fcfCard);
				}
			} else {
				Log.d(TAG,
						"onClick no system exists for systemCode="
								+ Integer.toHexString(systemCode));
				fcfCard = null;
			}
		}

		// dump service data
		if (fcfCard != null) {
			int serviceCode = FCFService.serviceCode();
			if (fcfCard.hasService(serviceCode)) {
				dumpFelicaService(felica, fcfCard, serviceCode);
				int serviceIndex = fcfCard.indexOfService(serviceCode);
				FelicaService service = fcfCard.services()[serviceIndex];

				FCFService fcf = FCFService.create(service);

				String userID;
				String name;

				if (fcf != null) {
					if (debug) {
						fcf.show();
					}
					userID = fcf.userID();
					name = fcf.name();
				} else {
					userID = "";
					name = "";

					Log.d(TAG, "onClick FAILED to read FCF data");
				}

				userIDView.setText(userID);
				nameView.setText(name);
				kumadaiIDView.setText("");

				if (lastCard != null) {
					if (fcfCard.hasSameIDm(lastCard) == false) {
						saveFCFLog(fcf);
						confirm(fcf);
					}
				} else {
					saveFCFLog(fcf);
					confirm(fcf);
				}

				lastCard = fcfCard;
			} else {
				beepError();
				Log.d(TAG, "onClick no service exists for serviceCode="
						+ Integer.toHexString(serviceCode));
			}
		}
	}

	public void readKumadaiData(Felica felica, FelicaCard card) {
		FelicaCard kumadaiCard = null;

		// select Kumadai system
		if (card != null) {
			int systemCode = KumadaiService.systemCode();

			if (card.hasSystem(systemCode)) {
				Log.d(TAG,
						"onClick polling systemCode="
								+ Integer.toHexString(systemCode));
				kumadaiCard = felica.polling(systemCode);
				if (kumadaiCard != null) {
					felica.search_service(kumadaiCard);
				}
			} else {
				Log.d(TAG,
						"onClick no system exists for systemCode="
								+ Integer.toHexString(systemCode));
				kumadaiCard = null;
				beepError();
			}
		}

		// dump service data
		if (kumadaiCard != null) {
			int serviceCode = KumadaiService.serviceCode();
			if (kumadaiCard.hasService(serviceCode)) {
				dumpFelicaService(felica, kumadaiCard, serviceCode);
				int serviceIndex = kumadaiCard.indexOfService(serviceCode);
				FelicaService service = kumadaiCard.services()[serviceIndex];

				if (service != null) {
					KumadaiService kumadai = KumadaiService.create(service);
					if (debug) {
						kumadai.show();
					}

					String userID = kumadai.userID();
					String name = kumadai.name();
					String kumadaiID = kumadai.kumadaiID();

					if (userID != null) {
						userIDView.setText(userID);
						nameView.setText(name);
						kumadaiIDView.setText(kumadaiID);
					}

					if (lastCard != null) {
						if (kumadaiCard.hasSameIDm(lastCard) == false) {
							saveFCFLog(kumadai);
							confirm(kumadai);
						}
					} else {
						saveFCFLog(kumadai);
						confirm(kumadai);
					}

					lastCard = kumadaiCard;
				} else {
					beepError();
				}
			} else {
				Log.d(TAG, "onClick no service exists for serviceCode="
						+ Integer.toHexString(serviceCode));

				beepError();

			}

		}
	}

	public void displayHandlerID(MyHandler handler) {
		if (handler != null) {
			String id = handler.id();

			kumadaiIDView.setText(id);
		}
	}

	public void displayHandlerTiming(MyHandler handler) {
		if (handler != null) {
			Date now = new Date();

			kumadaiIDView.setText("" + now.getTime());
		}
	}

	public void updateStatus() {
		int	newStatus = -1;
		
		long timestamp = System.currentTimeMillis();
		statusMessage = msgPlaceYourCard;

		//Log.d(TAG, "updateStatus attendStatus="+attendStatus);

		MyApplication application = (MyApplication) getApplication();
		if (application.pasori() == null) {
			newStatus = this.STATUS_NOW_INITIALIZING;
			attendStatus = newStatus;
			statusMessage = msgNowInitializing;
			statusSubMessage = "";
			acceptCheckin = false;

			return;
		}

		if (checkStartTime) {
			if (timestamp < startTimestamp) {
				newStatus = this.STATUS_NOT_OPENED;
				statusMessage = msgNotOpened;
				statusSubMessage = this.getString(R.string.title_start_time) + 
						displayTimestampFormat.format(new Date(startTimestamp));
				acceptCheckin = false;
			} else {
				
				newStatus = this.STATUS_PLACE_YOUR_CARD;
				statusMessage = msgPlaceYourCard;
				statusSubMessage = "";
				acceptCheckin = true;
			}
		}

		if (checkEndTime) {
			if (timestamp > endTimestamp) {

				newStatus = this.STATUS_ALREADY_CLOSED;
				statusMessage = msgAlreadyClosed;
				statusSubMessage = this.getString(R.string.title_end_time) +
						displayTimestampFormat.format(new Date(endTimestamp));
						
				acceptCheckin = false;
			}
		}
		
		if(newStatus > 0) {
			if((attendStatus != this.STATUS_PLACE_YOUR_CARD) && (newStatus == this.STATUS_PLACE_YOUR_CARD)){
				saveSessionLog(TAG_SESSION_STARTED);
			}

			if((attendStatus != this.STATUS_ALREADY_CLOSED) && (newStatus == this.STATUS_ALREADY_CLOSED)){
				saveSessionLog(TAG_SESSION_FINISHED);
			}

			attendStatus = newStatus;
		}
	}

	public void redrawStatus() {
		userIDView.setText(statusMessage);
		nameView.setText(statusSubMessage);
		kumadaiIDView.setText("");
	}
}
