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


import java.io.ByteArrayOutputStream;

import android.util.Log;

public class Felica {
	
	private static boolean debug = false;
	
    private static final String TAG = "Felica";

	public static final int FELICA_IDM_LENGTH = 8;
	public static final int FELICA_PMM_LENGTH = 8;
	public static final int FELICA_BLOCK_LENGTH = 16;
	
	public static final int	FELICA_POLLING_ANY = 0xffff;
	public static final int FELICA_POLLING_SUICA = 0x0003;
	public static final int FELICA_POLLING_EDY = 0xfe00;
	
	public static final int FELICA_SYSTEM_CODE_COMMON_AREA = 0xFE00;
	
	public static final int FELICA_SERVICE_CODE_FCF = 0x1A8B;	
	
	public static final byte FELICA_CMD_POLLING = 0x00;
	public static final byte FELICA_CMD_SEARCH_SERVICE_CODE = 0x0a;
	public static final byte FELICA_CMD_REQUEST_SYSTEM = 0x0c;
	public static final byte FELICA_CMD_READ_WITHOUT_ENCRYPTION = 0x06;
	
	private static byte	RC_S370_NORMAL_OFFSET = 4;
	private static byte	RC_S370_POLLING_OFFSET = 5;

	Pasori	pasori;

    static	int BUFFER_SIZE = 255;
    private byte response[] = new byte[BUFFER_SIZE+1];
    private	byte responseLength;

	byte	h8(int val) {
		return	(byte)((val >> 8) & 0xFF);
	}
	
	byte	l8(int val) {
		return	(byte)(val & 0xFF);
	}

   	public void dump_response(){
    	StringBuffer	message = new StringBuffer("dump_response");
    	for(int i=0;i<responseLength;i++){
    		message.append(' ');
    		message.append(Integer.toHexString(0xFF & response[i]));
    	}

        Log.d(TAG, message.toString());    	
    }

	public Felica(Pasori device) {
		pasori = device;
	}
	
	private void felica_pasori_read(){
		felica_pasori_read(RC_S370_NORMAL_OFFSET);
	}
	
	private void felica_pasori_read(byte offset){
		pasori.pasori_packet_read();

		byte	receiveBuffer[] = pasori.receiveBuffer();
		byte	receiveBufferLength = pasori.receiveBufferLength();
		
		responseLength = (byte)(receiveBufferLength - offset);
		if(responseLength > 0) {
			for(int i=0;i<responseLength;i++){
				response[i] = receiveBuffer[offset+i];
			}
		} else {
			int	length = response.length;
			for(int i=0;i<length;i++){
				response[i] = 0x00;
			}
		}
		if(debug) {
			dump_response();
		}
	}
	
	public boolean	isValidResponse(byte command){
		byte status1 = response[0];
		byte status2 = response[1];

		boolean	isValid = (status1 == (command+1));
		
		return isValid;
	}
	
	
	public FelicaCard polling(){
		return polling(FELICA_POLLING_ANY);
	}
	
	public FelicaCard polling(int systemCode){
		FelicaCard	card = null;
		byte	command = FELICA_CMD_POLLING;
		byte	requestCode = 0x00; // NO_REQUEST 
		byte	timeslot = 0x00;	// MAX_TIMESLOT_1
		
		byte	felicaCommand[] = new byte[5];
		felicaCommand[0] = command;

		// CAUTION: system code must be written in big endian.
		felicaCommand[1] = h8(systemCode);
		felicaCommand[2] = l8(systemCode);
		
		felicaCommand[3] = requestCode;
		felicaCommand[4] = timeslot;		
		
		pasori.list_passive_target(felicaCommand, felicaCommand.length);
		
		byte	offset = 5;
		felica_pasori_read(offset);
		
        Log.d(TAG, "polling responseLength="+responseLength);    
		if(responseLength > 0) {
			card = new FelicaCard();
			
			int responseOffset = 1;
			card.setIDm(response,responseOffset);
			card.setPMm(response,responseOffset+FELICA_IDM_LENGTH);
			
			card.showIDm();
			card.showPMm();
		}

		return card;
	}
	
	public int[] request_system_code(FelicaCard card){
		int		systemCodes[] = null;
		byte	command = FELICA_CMD_REQUEST_SYSTEM;
		byte	commandLength = 1;
		
		byte	felicaCommand[] = new byte[commandLength + FELICA_IDM_LENGTH];
		felicaCommand[0] = command;
		int	offset = 1;
		byte	IDm[] = card.IDm();
		for(int i=0;i<FELICA_IDM_LENGTH;i++){
			felicaCommand[offset+i] = IDm[i];
		}
		
		pasori.pasori_write(felicaCommand, felicaCommand.length);
		felica_pasori_read();
		
		int	systemNumberOffset = 9;
		int	systemNumber = response[systemNumberOffset];
		
        Log.d(TAG, "request_system_code systemNumber="+systemNumber);    
	
        if(isValidResponse(command) && (systemNumber > 0)){
    		int	systemCodeOffset = 10;
        	systemCodes = new int[systemNumber];
        	for(int i=0;i<systemNumber;i++) {
        		byte	u = response[systemCodeOffset];
        		byte	l = response[systemCodeOffset+1];
        		
        		systemCodes[i] = (u * 256 + l) & 0xFFFF;

        		if(debug) {
        			Log.d(TAG, "request_system_code systemCode["+i+"]="+Integer.toHexString(systemCodes[i]));
        		}
        		
        		systemCodeOffset +=2;
        	}
        	
        	card.setSystemCodes(systemCodes);
        }
        
        return	systemCodes;
	}
	
	public void search_service(FelicaCard card){
		int		systemCodes[] = null;
		byte	command = FELICA_CMD_SEARCH_SERVICE_CODE;
		byte	commandLength = 1;
		byte	serviceIndexLength = 2;
		
		byte	felicaCommand[] = new byte[commandLength + FELICA_IDM_LENGTH + serviceIndexLength];
		felicaCommand[0] = command;
		int	offset = 1;
		byte	IDm[] = card.IDm();
		for(int i=0;i<FELICA_IDM_LENGTH;i++){
			felicaCommand[offset+i] = IDm[i];
		}
		
		card.clearArea();
		card.clearService();
		
		int	serviceIndex = 0;
		int	serviceIndexOffset = commandLength + FELICA_IDM_LENGTH;
//		for(int i=0;i<4;i++){
		while(true) {
			felicaCommand[serviceIndexOffset] = l8(serviceIndex);
			felicaCommand[serviceIndexOffset+1] = h8(serviceIndex);
			
			pasori.pasori_write(felicaCommand, felicaCommand.length);
			felica_pasori_read();
			
			byte status1 = response[0];
			byte status2 = response[1];
			
			if(debug) {
				Log.d(TAG, "request_system_code areaCode command="+Integer.toHexString(command)+" status1="+Integer.toHexString(status1)+" status2="+Integer.toHexString(status2));
			}
			
			// verify response status
			/*
			if(status1 != command+1) {
				break;
			}
			*/
			if(isValidResponse(command) == false) {
				break;
			}
			
			// remember areaCode
			int	areaCodeOffset = FELICA_IDM_LENGTH + 1;
			int	code = (response[areaCodeOffset] & 0xFF) + (((int)response[areaCodeOffset+1] & 0xFF) << 8);
			
			if(debug) {
				Log.d(TAG, "request_system_code areaCode["+serviceIndex+"]="+Integer.toHexString(code));
			}
			
			if(code == 0xFFFF){
				break;
			}

    		if((code & 0x3E) == 0){
    			card.addArea(code);
    		} else {
    			card.addService(code);
    		}
    		
			serviceIndex++;
			
			// for safety
			if(serviceIndex > 255) {
				break;
			}
		}
	}
	
	public byte[] readSingleBlockWithoutEncryption(FelicaCard card, int serviceCode, int blockIndex){
		byte blockData[] = null;

		byte	command = FELICA_CMD_READ_WITHOUT_ENCRYPTION;
		byte	commandLength = 1;
		
		byte	serviceNumber = 1;
		byte	serviceNumberLength = 1;
		byte	serviceCodeListLength = (byte)(serviceNumber * 2);
		byte	blockNumber = 1;
		byte	blockNumberLength = 1;
		byte	blockListLength = (byte)(blockNumber * 2);
		
		byte	argumentLength = (byte)(serviceNumberLength + serviceCodeListLength + blockNumberLength + blockListLength);
		
		byte	felicaCommand[] = new byte[commandLength + FELICA_IDM_LENGTH + argumentLength];
		felicaCommand[0] = command;
		int	offset = 1;
		byte	IDm[] = card.IDm();
		for(int i=0;i<FELICA_IDM_LENGTH;i++){
			felicaCommand[offset+i] = IDm[i];
		}
		
		int	argumentOffset = commandLength + FELICA_IDM_LENGTH;
		felicaCommand[argumentOffset] = serviceNumber;
		// specify service code in little endian
		felicaCommand[argumentOffset+1] = l8(serviceCode);
		felicaCommand[argumentOffset+2] = h8(serviceCode);
		
		felicaCommand[argumentOffset+3] = blockNumber;

		// specify block element in little endian
		final byte serviceCodeIndex = 0;
		final byte TWO_BYTE_ELEMENT_FLAG = (byte)0x80;
		final byte RANDOM_SERVICE_FLAG = 0x00;
		
		byte	blockElementHeader = TWO_BYTE_ELEMENT_FLAG | RANDOM_SERVICE_FLAG | serviceCodeIndex;
		byte	blockElementArgument = (byte)(blockIndex & 0xFF);
		
		felicaCommand[argumentOffset+4] = blockElementHeader;
		felicaCommand[argumentOffset+5] = blockElementArgument;
	
		pasori.pasori_write(felicaCommand, felicaCommand.length);
		felica_pasori_read();

		// check status
		int	statusFlag1Offset = FELICA_IDM_LENGTH + 1;
		int	statusFlag2Offset = FELICA_IDM_LENGTH + 2;
		int	blockNumberOffset = FELICA_IDM_LENGTH + 3;
		int	blockDataOffset = FELICA_IDM_LENGTH + 4;
		
		int	status1 = response[statusFlag1Offset] & 0xFF;
		int	status2 = response[statusFlag2Offset] & 0xFF;
		int	receivedBlockNumber = response[blockNumberOffset] & 0xFf;
		
		if(debug) {
			Log.d(TAG, "readSingleBlockWithoutEncryption status1="+status1+" status2="+Integer.toHexString(status2));
		}
		
		if((isValidResponse(command) == false) || (status1 != 0) || (receivedBlockNumber == 0)){
			blockData = null;
			
			return blockData;
		}
		
		blockData = new byte[FELICA_BLOCK_LENGTH];
		for(int i=0;i<FELICA_BLOCK_LENGTH;i++){
			blockData[i] = response[blockDataOffset + i];

			//card.addBlockDataForService(blockData,i);
		}

		return blockData;
	}
	
	public byte[] readServiceDataWithoutEncryption(FelicaCard card, int serviceIndex){
    	FelicaService service;
    	
    	if((serviceIndex < 0) || (card == null) || (serviceIndex  >= card.serviceNumber)) {
    		return null;
    	}
    	
        FelicaService services[] = card.services();
    	service = services[serviceIndex];
		if(service.isReadableWithoutEncryption() == false) {
			service.clearData();
			
			return null;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		int	serviceCode = service.code();
		int blockIndex=0;
		while (true){
			byte blockData[] = this.readSingleBlockWithoutEncryption(card, serviceCode, blockIndex);
			
			if(blockData == null) {
				break;
			}
			
			// for safety
			final int MAX_BLOCK_INDEX = 1024;
			if(blockIndex > MAX_BLOCK_INDEX) {
				break;
			}
			
			if(debug) {
				Log.d(TAG, "readServiceDataWithoutEncryption serviceCode="+Integer.toHexString(serviceCode)+" blockIndex="+blockIndex);
			}
			
			stream.write(blockData,0,blockData.length);
			blockIndex++; 
		}

		byte data[] = stream.toByteArray();
		service.setData(data);
		
		return data;
	}
}
