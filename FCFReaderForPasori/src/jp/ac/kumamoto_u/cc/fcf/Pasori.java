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

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class Pasori {

	private static boolean	debug = false;
	
    private static final String TAG = "Pasori";

    static int	RCS_S370_ENDPOINT_COUNT = 2;
    static int	RCS_S370_ENDPOINT0_TYPE = UsbConstants.USB_ENDPOINT_XFER_BULK;
    static int	RCS_S370_ENDPOINT1_TYPE = UsbConstants.USB_ENDPOINT_XFER_BULK;
    static int	RCS_S370_ENDPOINT0_DIRECTION = UsbConstants.USB_DIR_OUT;
    static int	RCS_S370_ENDPOINT1_DIRECTION = UsbConstants.USB_DIR_IN;
    
    static byte S330_RF_ANTENNA_ON[] = { (byte)0xD4, 0x32, 0x01, 0x01 };
    static byte S330_RF_ANTENNA_OFF[] = { (byte)0xD4, 0x32, 0x01, 0x00 };
    static byte S330_FIRM_VERSION[] = { (byte)0xD4, 0x02 };
    static byte S330_DESELECT[] = { (byte)0xD4, 0x44, 0x01 };
    static byte S330_LIST_PASSIVE_TARGET_PREFIX[] = { (byte)0xD4, 0x4a, 0x01, 0x01 };
    
    static	int	RECEIVE_TIMEOUT_IN_MS = 200; // 100 ms
    static	int	SEND_TIMEOUT_IN_MS = 200; // 100 ms
    static	int BUFFER_SIZE = 255;
    private byte receive_packet[] = new byte[BUFFER_SIZE+1];
    private	byte	receivedPacketLength;
    private byte receiveBuffer[] = new byte[BUFFER_SIZE+1];
    private	byte	receivedPayloadLength;
    
    private byte send_buffer[] = new byte[BUFFER_SIZE+1];
    
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbDeviceConnection usbConnection;
    UsbEndpoint input;
    UsbEndpoint output;

    String	versionID;
    
    private int bcd2int(byte v){
    	  int val = ((v >> 4) & 0x0f) * 10 + (v & 0x0f);
    	  
    	  return val;
    }
    
    public byte[] receiveBuffer(){
    	return receiveBuffer;
    }
    
    public byte receiveBufferLength(){
    	return receivedPayloadLength;
    }

    public boolean init(UsbDevice device) {
    	boolean	isSuccess = false;
    	
        Log.d(TAG, "init " + device);

        if(device == null) {
            Log.d(TAG, "init device is null.");
            
            return isSuccess;
        }
        usbDevice = device;
        
        int	interfaceCount = device.getInterfaceCount();
        Log.d(TAG, "init interfaceCount=" + interfaceCount);        
        
        
        if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
            return isSuccess;
        }
        usbInterface = device.getInterface(0);

        // device should have one endpoint
        int	endpointCount = usbInterface.getEndpointCount();
        Log.d(TAG, "init endpointCount=" + endpointCount);        

        if (usbInterface.getEndpointCount() != RCS_S370_ENDPOINT_COUNT) {
            Log.e(TAG, "could not find endpoint");
            return isSuccess;
        }

        UsbEndpoint ep0;
        UsbEndpoint ep1;

        ep0 = usbInterface.getEndpoint(0);
        ep1 = usbInterface.getEndpoint(1);
        int	ep0Type = ep0.getType();
        int	ep1Type = ep1.getType();

        int	ep0Direction = ep0.getDirection();
        int	ep1Direction = ep1.getDirection();
        
        Log.e(TAG, "ep0Type="+ep0Type);
        Log.e(TAG, "ep1Type="+ep1Type);
        Log.e(TAG, "ep0Direction="+ep0Direction);
        Log.e(TAG, "ep1Direction="+ep1Direction);

        if (ep0Type != RCS_S370_ENDPOINT0_TYPE) {
            Log.e(TAG, "endpoint 0 is not bulk transfer type");
            return isSuccess;
        }
        
        if (ep1Type != RCS_S370_ENDPOINT1_TYPE) {
            Log.e(TAG, "endpoint 1 is not bulk transfer type");
            return isSuccess;
        }

        if (ep0Direction != RCS_S370_ENDPOINT0_DIRECTION) {
            Log.e(TAG, "direction of endpoint 0 is not USB_DIR_OUT");
            return isSuccess;
        }
        
        if (ep1Direction != RCS_S370_ENDPOINT1_DIRECTION) {
            Log.e(TAG, "direction of endpoint 1 is not USB_DIR_IN");
            return isSuccess;
        }

        output = ep0;
        input = ep1;
        isSuccess = true;
        
    	return isSuccess;
    }
    
    public boolean open(UsbManager manager, UsbDevice device) {
    	boolean	isSuccess = false;
    	
    	isSuccess = this.init(device);
    	if(isSuccess == false) {
            Log.d(TAG, "open FAIL");

            return isSuccess;
    	}
    	
        if((manager != null) && (usbDevice != null)) {
        	UsbDeviceConnection connection = manager.openDevice(usbDevice);
            if (connection != null && connection.claimInterface(usbInterface, true)) {
            	usbManager = manager;
            	usbDevice = device;
                usbConnection = connection;                
                isSuccess = true;
                
                Log.d(TAG, "open SUCCESS");
            } else {
                usbConnection = null;                
                isSuccess = false;
                
                Log.d(TAG, "open FAIL");
            }
         }

    	return isSuccess;
    }
    
    public void pasori_receive(){
    	int	n;
    	
    	if(debug) {
    		Log.d(TAG, "pasori_receive now receive");    
    	}
    	n = usbConnection.bulkTransfer(input, receive_packet, BUFFER_SIZE, RECEIVE_TIMEOUT_IN_MS);
    	receivedPacketLength = (byte)(n & 0xFF);
    	
    	if(debug) {
    		Log.d(TAG, "pasori_receive receiveLength="+receivedPacketLength);    
        	dump_receive_data();        	
    	}
    }
    

    private void pasori_send(byte data[], int length){
    	int	n;
    	
    	if(debug) {
    		dump_send_data(data,length);
    	}
    	
    	n = usbConnection.bulkTransfer(output, data, length, SEND_TIMEOUT_IN_MS);
    	
    	if(debug) {
    		Log.d(TAG, "pasori_send before ACK n="+n);
    	}
    	
        // get ACK
    	if(debug) {
    		Log.d(TAG, "pasori_send CHECK ACK");
    	}
    	
        pasori_receive();
        
        if(debug) {
        	Log.d(TAG, "pasori_send GET ACK");
        }
    }
    
    byte checksum(byte data[], int length) {
    	int	sum = 0;
    	
    	for(int i=0;i<length;i++) {
    		sum = sum + data[i];
    	}

    	sum = sum & 0xFF;
    	byte checksum = (byte)(0x100 - sum);
    	
    	return checksum;
    }
    
    public void pasori_packet_read(){
    	int	prefixLength = 5;
    	int	n;
    	
    	if(debug) {
        Log.d(TAG, "pasori_packet_read now receive");
    	}
    	
        pasori_receive();
        receivedPayloadLength = receive_packet[3];
        byte	lengthChecksum = receive_packet[4];
        byte	payloadChecksum = receive_packet[prefixLength+receivedPayloadLength];

        for(int i=0;i<receivedPayloadLength;i++){
        	receiveBuffer[i] = receive_packet[prefixLength+i];
        }

        if(debug) {
        	dump_receiveBuffer();
        }
        
        if(debug) {
	        Log.d(TAG, "pasori_packet_read receivePayloadLength="+receivedPayloadLength);    	
	        Log.d(TAG, "pasori_packet_read lengthChecksum="+lengthChecksum);    	
	        Log.d(TAG, "pasori_packet_read payloadChecksum="+payloadChecksum);    	
        }
        
        // Verify checksum here
        if(receivedPayloadLength + lengthChecksum != 0x00) {
            Log.d(TAG, "pasori_packet_read length checksum error");    	        	
        }
                
        byte	checksum = checksum(receiveBuffer,receivedPayloadLength);
        
        if(debug) {
        	Log.d(TAG, "pasori_packet_read checksum="+checksum);    	
        }
        
        if(checksum != payloadChecksum) {
            Log.d(TAG, "pasori_packet_read payload checksum error");    	        	
            Log.d(TAG, "pasori_packet_read payload checksum="+checksum);    	        	
            Log.d(TAG, "pasori_packet_read payload payloadChecksum="+payloadChecksum);    	        	
        }

        if(debug) {
        	Log.d(TAG, "pasori_packet_read receiveLength="+receivedPacketLength);    	
        }
    }
    
    private void pasori_packet_write(byte data[], int length){
    	int	n;
    	int	payloadLength;
    	int	packetLength;
    	int	prefixLength = 5;
    	int	suffixLength = 2;
    	int	maxPayloadLength = BUFFER_SIZE - (prefixLength + suffixLength);
    	
    	payloadLength = (length > maxPayloadLength ? maxPayloadLength : length);
    	int	checksum;
    	
    	// set prefix
    	send_buffer[0] = 0x00;
    	send_buffer[1] = 0x00;
    	send_buffer[2] = (byte)0xff;
    	send_buffer[3] = (byte)payloadLength;
    	send_buffer[4] = (byte)(0x100 - payloadLength);
    	
    	// set payload
    	for(int i=0;i<payloadLength;i++) {
    		send_buffer[prefixLength + i] = data[i];
    	}
    	
    	// set checksum as suffix
    	
    	send_buffer[prefixLength + payloadLength] = checksum(data,payloadLength);
    	send_buffer[prefixLength + payloadLength + 1] = 0x00;
    	
    	packetLength = payloadLength + (prefixLength + suffixLength);
    	
    	//n = usbConnection.bulkTransfer(output, send_buffer, packetLength, SEND_TIMEOUT_IN_MS);
    	pasori_send(send_buffer,packetLength);
    	
    	if(debug) {
    		Log.d(TAG, "pasori_packet_write end");
    	}
    }

    public void pasori_write(byte data[], int length){
    	int		prefixLength = 3;
    	byte	buffer[] = new byte[prefixLength + length];
    	
    	buffer[0] = (byte)0xD4;
    	buffer[1] = 0x42;
    	buffer[2] = (byte)(length + 1);
    	for(int i=0;i<length;i++){
    		buffer[i+prefixLength] = data[i];
    	}
    	
    	pasori_packet_write(buffer,buffer.length);
    }
    
   	public void dump_receive_data(){
    	StringBuffer	message = new StringBuffer("dump_receive_data");
    	for(int i=0;i<receivedPacketLength;i++){
    		message.append(' ');
    		message.append(Integer.toHexString(0xFF & receive_packet[i]));
    	}

        Log.d(TAG, message.toString());    	
    }

   	public void dump_receiveBuffer(){
    	StringBuffer	message = new StringBuffer("dump_receiveBuffer");
    	for(int i=0;i<receivedPayloadLength;i++){
    		message.append(' ');
    		message.append(Integer.toHexString(0xFF & receiveBuffer[i]));
    	}

        Log.d(TAG, message.toString());    	
    }

   	public void dump_send_data(byte sendData[], int sendLength){
    	StringBuffer	message = new StringBuffer("dump_send_data ");
    	for(int i=0;i<sendLength;i++){
    		message.append(' ');
    		message.append(Integer.toHexString(0xFF & sendData[i]));
    	}

        Log.d(TAG, message.toString());    	
    }

    public void init_test(){
        Log.d(TAG, "init_test now packet_write");    	
    	pasori_packet_write(S330_RF_ANTENNA_ON,S330_RF_ANTENNA_ON.length);

        Log.d(TAG, "init_test now pasori_receive");    	
        pasori_receive();
    }

    public void list_passive_target(byte felicaCommand[], int commandLength){
    	int pasoriCommandLength = commandLength + S330_LIST_PASSIVE_TARGET_PREFIX.length;
    	byte	pasoriCommand[] = new byte[pasoriCommandLength];
    	
    	byte	tagTypes = 0x01; // Felica(212kbps);
    	byte	maxTargetNumbers = 1;
    	
    	for(int i=0;i<S330_LIST_PASSIVE_TARGET_PREFIX.length;i++){
    		pasoriCommand[i] = S330_LIST_PASSIVE_TARGET_PREFIX[i];
    	}
    	pasoriCommand[2] = maxTargetNumbers;
    	pasoriCommand[3] = tagTypes;
    	
    	int	offset = S330_LIST_PASSIVE_TARGET_PREFIX.length;
    	for(int i=0;i<commandLength;i++) {
    		pasoriCommand[i+offset] = felicaCommand[i];
    	}
    	
    	pasori_packet_write(pasoriCommand,pasoriCommandLength);
    }
    
    public void version(){
        Log.d(TAG, "version now packet_write");    	
    	pasori_packet_write(S330_FIRM_VERSION,S330_FIRM_VERSION.length);

        Log.d(TAG, "version now pasori_receive");
        pasori_packet_read();
        
        int	v1 = bcd2int(receiveBuffer[3]);
        int	v2 = bcd2int(receiveBuffer[4]);
        
        Log.d(TAG, "version now pasori_receive v1="+v1+" v2="+v2);
        
        versionID = v1 + "." + v2;
        Log.d(TAG, "firimware version versionID="+versionID);
    }
    
    public void reset(){
        Log.d(TAG, "reset now packet_write1");    	
    	pasori_packet_write(S330_RF_ANTENNA_OFF,S330_RF_ANTENNA_OFF.length);

        Log.d(TAG, "reset now pasori_receive1");    	
        pasori_receive();

        Log.d(TAG, "reset now packet_write2");    	
    	pasori_packet_write(S330_DESELECT,S330_DESELECT.length);

        Log.d(TAG, "reset now pasori_receive2");    	
        pasori_receive();
    }
}
