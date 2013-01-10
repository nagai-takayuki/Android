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

import java.text.Normalizer;
import java.text.Normalizer.Form;


import android.util.Log;

public class FCFService extends FelicaArea {

	FCFService() {
		super();
		// TODO Auto-generated constructor stub
	}

	private static final String TAG = "FCFService";

	public static final int	FCF_DATA_LENGTH = 16 * 4;
	
	public static final int SYSTEM_CODE = 0xFE00;
	public static final int SERVICE_CODE = 0x1A8B;

	String	userType;
	String	userID;
	String	publishCount;
	String	sex;
	String	name;
	String	schoolCode;
	String	publishDate;
	String	expireDate;
	
	public static int systemCode(){
		return SYSTEM_CODE;
	}

	public static int serviceCode(){
		return SERVICE_CODE;
	}
	
	public static FCFService create(FelicaService service) {
		FCFService	fcf = null;

		fcf = new FCFService();

		return create(service,fcf);
	}
	
	public static FCFService create(FelicaService service, FCFService fcf) {
//		FCFService	fcf = null;
		
		byte	data[] = service.data();
		
		if((data == null) || (data.length < FCF_DATA_LENGTH)) {
			return null;
		}
		
//		fcf = new FCFService();
		
		int	offset = 0;
		fcf.userType = "" + (char)data[0] + (char)data[1];
		
		offset = 2;
		fcf.userID = "" + (char)data[offset] + (char)data[offset+1] + (char)data[offset+2] + (char)data[offset+3] + 
				(char)data[offset+4] + (char)data[offset+5] + (char)data[offset+6] + (char)data[offset+7];
		
		offset = 14;
		fcf.publishCount = "" + (char)data[offset];
		
		offset = 15;
		fcf.sex = "" + (char)data[offset];
		
		offset = 16;
		int	nameLength = 16;
		fcf.name = "";
		for(int i=0;i<nameLength;i++) {
			int	val = data[offset+i] & 0xFF;
			
			// val is ASCII character
			if(val == 0) {
				// skip
			} else if(val <= 0x80) {
				fcf.name += (char)val;
			} else if((val >= 0xA1) && (val <= 0xDF)){
				// val is Japanese kana
				
				// c.f. http://charset.7jp.net/jis0201.html
				int	utf16Kana = 0xFF61 + (val - 0xA1);
				fcf.name += (char)utf16Kana;			
			} else {
				fcf.name += '?';				
			}
		}
		fcf.name = Normalizer.normalize(fcf.name, Form.NFKC);
		
		offset = 32;
		fcf.schoolCode = "" + (char)data[offset] + (char)data[offset+1] + (char)data[offset+2] + (char)data[offset+3] + 
				(char)data[offset+4] + (char)data[offset+5] + (char)data[offset+6] + (char)data[offset+7];
		
		offset = 40;
		fcf.publishDate = "" + (char)data[offset] + (char)data[offset+1] + (char)data[offset+2] + (char)data[offset+3] + 
				(char)data[offset+4] + (char)data[offset+5] + (char)data[offset+6] + (char)data[offset+7];
		
		offset = 48;
		fcf.expireDate = "" + (char)data[offset] + (char)data[offset+1] + (char)data[offset+2] + (char)data[offset+3] + 
				(char)data[offset+4] + (char)data[offset+5] + (char)data[offset+6] + (char)data[offset+7];

		return fcf;
	}
	
	public String userType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String userID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String publishCount() {
		return publishCount;
	}

	public void setPublishCount(String publishCount) {
		this.publishCount = publishCount;
	}

	public String sex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String schoolCode() {
		return schoolCode;
	}

	public void setSchoolCode(String schoolCode) {
		this.schoolCode = schoolCode;
	}

	public String publishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}

	public String expireDate() {
		return expireDate;
	}

	public void setExpireDate(String expireDate) {
		this.expireDate = expireDate;
	}

	public String toString(){
		StringBuffer	s = new StringBuffer();
		s.append("userType="+userType);
		s.append('\n');
		s.append("userID="+userID);
		s.append('\n');
		s.append("publishCount="+publishCount);
		s.append('\n');
		s.append("sex="+sex);
		s.append('\n');
		s.append("name="+name);
		s.append('\n');
		s.append("schoolCode="+schoolCode);
		s.append('\n');
		s.append("publishDate="+publishDate);
		s.append('\n');
		s.append("expireDate="+expireDate);
		
		return s.toString();
	}
	
	public void show(){
		String	s = toString();
        Log.d(TAG, s);
	}
}
