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

public class FelicaCard {
    private static final String TAG = "FelicaCard";

    public static final	int	FELICA_MAX_AREA_NUMBER = 256;
    public static final	int	FELICA_MAX_SERVICE_NUMBER = 256;    
    
	public static final int FELICA_IDM_LENGTH = 8;    
	public static final int FELICA_PMM_LENGTH = 8;
	
	private byte	IDm[];
	private byte	PMm[];
	private	int		systemCodes[];
	
	int	areaNumber;
	int	serviceNumber;
	private	FelicaArea areas[] = new FelicaArea[FELICA_MAX_AREA_NUMBER];
	private	FelicaService services[] = new FelicaService[FELICA_MAX_SERVICE_NUMBER];
	
	public String	byteToHexString(int val) {
		return byteToHexString((byte)(val & 0xFF));
	}
	
	public String	byteToHexString(byte val) {
		int	u,l;
		
		u = (val >> 4) & 0x0F;
		l = val & 0x0F;
		
		char	uChar = (char)( u < 10 ? '0' + u : 'A' + (u-10));
		char	lChar = (char)( l < 10 ? '0' + l : 'A' + (l-10));
		
		return ""+uChar+lChar;
	}
	
	FelicaCard(){
		IDm = new byte[FELICA_IDM_LENGTH];
		PMm = new byte[FELICA_PMM_LENGTH];
	}
			
	public byte[] IDm(){
		return IDm;
	}
	

	public byte[] PMm(){
		return PMm;
	}

	public void setIDm(byte data[], int offset){
		if((data != null) && (offset >= 0) && ((data.length-offset) >= FELICA_IDM_LENGTH)) {
			for(int i=0;i<FELICA_IDM_LENGTH;i++) {
				IDm[i]=data[i+offset];
			}
		}
	}

	public void setPMm(byte data[], int offset){
		if((data != null) && (offset >= 0) && ((data.length-offset) >= FELICA_PMM_LENGTH)) {
			for(int i=0;i<FELICA_PMM_LENGTH;i++) {
				PMm[i]=data[i+offset];
			}
		}		
	}
	
	public void setIDm(byte data[]){
		setIDm(data,0);
	}

	public void setPMm(byte data[]){
		setPMm(data,0);
	}
	
	public void setSystemCodes(int codes[]){
		systemCodes = codes;
	}
	
	public int[] systemCodes(){
		return systemCodes;
	}
	
	public void showIDm(){
    	StringBuffer	message = new StringBuffer("showIDm ");
    	for(int i=0;i<FELICA_IDM_LENGTH;i++){
    		message.append(' ');
//    		message.append(Integer.toHexString(0xFF & IDm[i]));
    		message.append(byteToHexString(0xFF & IDm[i]));
    	}

        Log.d(TAG, message.toString());    	
	}

	public void showPMm(){
    	StringBuffer	message = new StringBuffer("showPMm ");
    	for(int i=0;i<FELICA_IDM_LENGTH;i++){
    		message.append(' ');
//    		message.append(Integer.toHexString(0xFF & PMm[i]));
    		message.append(byteToHexString(0xFF & PMm[i]));
    	}

        Log.d(TAG, message.toString());    	
	}

	public int serviceNumber(){
		return serviceNumber;
	}
	
	public int areaNumber(){
		return areaNumber;
	}

	public void clearService(){
		serviceNumber = 0;
	}
	
	public void clearArea(){
		areaNumber = 0;
	}
	
	public void addArea(int code) {
		areas[areaNumber] = new FelicaArea(code);
		
		areaNumber++;
	}
	
	public void addService(int code) {
		services[serviceNumber] = new FelicaService(code);

		serviceNumber++;
	}

	public FelicaArea[] areas(){
		return areas;
	}
	
	public FelicaService[] services(){
		return services;
	}
	
	public int indexOfSystem(int systemCode) {
		boolean found = false;
		int	index = -1;
		
		int	systemNumber = (systemCodes != null ? systemCodes.length : 0);
		
		for(int i=0;i<systemNumber;i++){
			if(systemCodes[i] == systemCode) {
				found = true;
				index = i;
				
				break;
			}
		}
		
		return index;
	}
	
	public int indexOfService(int serviceCode) {
		boolean found = false;
		int	index = -1;
		
		for(int i=0;i<serviceNumber;i++){
			if(services[i].code() == serviceCode) {
				found = true;
				index = i;
				
				break;
			}
		}
		
		return index;
	}
	
	public boolean hasSystem(int systemCode) {
		return (indexOfSystem(systemCode) >= 0);
	}
	
	public boolean hasService(int serviceCode) {
		return (indexOfService(serviceCode) >= 0);
	}
	
	public boolean hasSameIDm(FelicaCard card){
		boolean	flag = false;
		
		if(card == null) {
			return flag;
		}
		
		byte	IDm1[] = this.IDm();
		byte	IDm2[] = card.IDm();
		
		flag = true;
		for(int i=0;i<FELICA_IDM_LENGTH;i++) {
			if(IDm1[i] != IDm2[i]) {
				flag = false;
				
				break;
			}
		}
		
		return flag;
	}
	
}
