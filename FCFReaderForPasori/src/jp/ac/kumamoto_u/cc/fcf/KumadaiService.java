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

public class KumadaiService extends FCFService {

	public static final int SYSTEM_CODE = 0x8D38;	
	public static final int SERVICE_CODE = 0x100B;	

	String	kumadaiID;
	
	public static int systemCode(){
		return SYSTEM_CODE;
	}

	public static int serviceCode(){
		return SERVICE_CODE;
	}
	
	public static KumadaiService create(FelicaService service) {
		KumadaiService	kumadai = new KumadaiService();
		
		FCFService.create(service,kumadai);
		
		final int	offset = 16 * 4;
		byte	data[] = service.data();

		try {
			kumadai.kumadaiID = "" + (char)data[offset] + (char)data[offset+1] + (char)data[offset+2] + (char)data[offset+3] + 
				(char)data[offset+4] + (char)data[offset+5] + (char)data[offset+6] + (char)data[offset+7] + (char)data[offset+8];
		} catch (Exception e) {
			// data is short than expected
		}
		return kumadai;
	}
	
	public String toString(){
		StringBuffer	s = new StringBuffer();
		s.append("userType="+userType);
		s.append('\n');
		s.append("userID="+userID);
		s.append('\n');
		s.append("kumadaiID="+kumadaiID);
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

	public String kumadaiID() {
		return kumadaiID;
	}

	public void setKumadaiID(String kumadaiID) {
		this.kumadaiID = kumadaiID;
	}
	
}
