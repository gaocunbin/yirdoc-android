package com.example.yirdoc_atomizer.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

public class MyBluetoothGattCallback extends BluetoothGattCallback {

    private String TAG = "MyBluetoothGattCallback";
    private BluetoothInfoChangeListener mBluetoothInfoChangeListener = null;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        Log.i(TAG, "ConnectionStateChange: " + newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {

            if(mBluetoothInfoChangeListener != null){
                mBluetoothInfoChangeListener.onConnectionStateChange(BLEControlService.ACTION_GATT_CONNECTED);
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            if(mBluetoothInfoChangeListener != null){
                mBluetoothInfoChangeListener.onConnectionStateChange(BLEControlService.ACTION_GATT_DISCONNECTED);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        Log.i(TAG, "ServicesDiscovered");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if(mBluetoothInfoChangeListener != null){
                mBluetoothInfoChangeListener.onServicesDiscovered(BLEControlService.ACTION_GATT_SERVICES_DISCOVERED);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        
    	Log.i(TAG, "onCharacteristicRead");
    	if (status == BluetoothGatt.GATT_SUCCESS) {

            if(mBluetoothInfoChangeListener != null){
                mBluetoothInfoChangeListener.onCharacteristicRead(BLEControlService.ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

    	Log.i(TAG, "onCharacteristicChanged");
        if(mBluetoothInfoChangeListener != null){
            mBluetoothInfoChangeListener.onCharacteristicChanged(BLEControlService.ACTION_DATA_AVAILABLE, characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);

        Log.i(TAG, "onDescriptorRead");
        if(mBluetoothInfoChangeListener != null){
            mBluetoothInfoChangeListener.onDescriptorRead(BLEControlService.ACTION_DATA_AVAILABLE, descriptor);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        
        Log.i(TAG, "onDescriptorWrite");
        if(mBluetoothInfoChangeListener != null){
            mBluetoothInfoChangeListener.onDescriptorWrite(BLEControlService.ACTION_DATA_AVAILABLE, descriptor);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        
        Log.i(TAG, "onReadRemoteRssi");
        if(mBluetoothInfoChangeListener != null){
            mBluetoothInfoChangeListener.onReadRemoteRssi(BLEControlService.ACTION_DATA_AVAILABLE, rssi);
        }
    }

    public void setBluetoothInfoChangeListener(BluetoothInfoChangeListener bluetoothInfoChangeListener){
        this.mBluetoothInfoChangeListener = bluetoothInfoChangeListener;
    }

    public interface BluetoothInfoChangeListener{
        void onConnectionStateChange(String action);
        void onServicesDiscovered(String action);
        void onCharacteristicRead(String action, BluetoothGattCharacteristic characteristic);
        void onCharacteristicChanged(String action, BluetoothGattCharacteristic characteristic);
        void onDescriptorRead(String action, BluetoothGattDescriptor descriptor);
        void onDescriptorWrite(String action, BluetoothGattDescriptor descriptor);
        void onReadRemoteRssi(String action, int rssi);
    }
}
