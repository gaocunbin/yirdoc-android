package com.example.yirdoc_atomizer.ble.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.yirdoc_atomizer.ble.service.BLEControlService;

public class BLEStatusChangeReceiver extends BroadcastReceiver {
	
	private final static String TAG = BLEStatusChangeReceiver.class.getSimpleName();

    private OnReceiveDataListener mOnReceiveDataListener = null;
    private OnBLEStatusChangeListener mOnBLEStatusChangeListener = null;

    private BLEControlService mService;

    public void updateOnReceiveDataListener(OnReceiveDataListener onReceiveDataListener){
        this.mOnReceiveDataListener = onReceiveDataListener;
    }

    public void setOnBLEStatusChangeListener(OnBLEStatusChangeListener onBLEStatusChangeListener){
        this.mOnBLEStatusChangeListener = onBLEStatusChangeListener;
    }

    public void setBLEService(BLEControlService service){
        this.mService = service;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        //when communicate
        if (action.equals(BLEControlService.ACTION_GATT_CONNECTED)) {
            if(mOnBLEStatusChangeListener != null){
                mOnBLEStatusChangeListener.onConnected();
            }
        }

        //Service Disconnected
        if (action.equals(BLEControlService.ACTION_GATT_DISCONNECTED)) {
            mService.close();
            if(mOnBLEStatusChangeListener != null){
                mOnBLEStatusChangeListener.onDisConnected();
            }
        }

        //Discover BLE GATT Service
        if (action.equals(BLEControlService.ACTION_GATT_SERVICES_DISCOVERED)) {
            if(mOnBLEStatusChangeListener != null){
                mOnBLEStatusChangeListener.onGattServiceDiscovered();
            }
        }

        //Receive BLE Data
        if (action.equals(BLEControlService.ACTION_DATA_AVAILABLE)) {

            if(intent.getStringExtra(BLEControlService.UUID_DATA) != null){
                if(mOnReceiveDataListener != null){
                    final byte[] value = intent.getByteArrayExtra(BLEControlService.RECEIVE_DATA);
                    final String uuid = intent.getStringExtra(BLEControlService.UUID_DATA);
                    if(mOnReceiveDataListener != null){
                    	Log.e(TAG, String.valueOf(value[0]));
                        mOnReceiveDataListener.getRecivedData(uuid, value);
                    }

                    if(mOnBLEStatusChangeListener != null) {
                        if(BLEControlService.TYPE_READ.equals(intent.getStringExtra(BLEControlService.ACTION_TYPE))) {
                            mOnBLEStatusChangeListener.onDataChange(uuid, value, BLEControlService.TYPE_READ);
                        }else if(BLEControlService.TYPE_WRITE.equals(intent.getStringExtra(BLEControlService.ACTION_TYPE))){
                            mOnBLEStatusChangeListener.onDataChange(uuid, value, BLEControlService.TYPE_WRITE);
                        }else if(BLEControlService.TYPE_DESCRIPTOR_READ.equals(intent.getStringExtra(BLEControlService.ACTION_TYPE))){
                            mOnBLEStatusChangeListener.onDataChange(uuid, value, BLEControlService.TYPE_DESCRIPTOR_READ);
                        }else if(BLEControlService.TYPE_DESCRIPTOR_WRITE.equals(intent.getStringExtra(BLEControlService.ACTION_TYPE))){
                            mOnBLEStatusChangeListener.onDataChange(uuid, value, BLEControlService.TYPE_DESCRIPTOR_WRITE);
                        }
                    }
                }
            }else if(BLEControlService.TYPE_RSSI_READ.equals(intent.getStringExtra(BLEControlService.ACTION_TYPE))){
                mOnBLEStatusChangeListener.onRssiRead(intent.getIntExtra(BLEControlService.RECEIVE_DATA, 0), BLEControlService.TYPE_RSSI_READ);
            }
        }

        if (action.equals(BLEControlService.DEVICE_DOES_NOT_SUPPORT_UART)){
        	Log.e(TAG, "Request service is not support");
        }
    }

    public interface OnBLEStatusChangeListener{
        void onConnected();
        void onDisConnected();
        void onGattServiceDiscovered();
        void onDataChange(String uuid, byte[] value, String type);
        void onRssiRead(int rssi, String type);
    }

    public interface OnReceiveDataListener{
        void getRecivedData(String uuid, byte[] value);
    }
}
