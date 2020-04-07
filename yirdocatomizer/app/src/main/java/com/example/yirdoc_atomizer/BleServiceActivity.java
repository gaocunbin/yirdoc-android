package com.example.yirdoc_atomizer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.yirdoc_atomizer.ble.receiver.BLEStatusChangeReceiver;
import com.example.yirdoc_atomizer.ble.service.BLEControlService;
import com.example.yirdoc_atomizer.ble.utility.DebugLogger;

public class BleServiceActivity extends AppCompatActivity {
    private static final String TAG = "BleServiceActivity";

    protected BluetoothDevice mBLEDevice = null;
    protected String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBLEControlService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBLEStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        try {
            unbindService(mServiceConnection);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }


        if(mService != null) {
            mService.stopSelf();
            mService = null;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEControlService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEControlService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEControlService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEControlService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEControlService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    protected void ConnectDevice(BluetoothDevice device){

        mBLEDevice = device;
        mDeviceAddress = device.getAddress();

        mService.connect(device);
    }

    protected void initBLEControlService() {
        //create BLEControService
        Intent bindIntent = new Intent(this, BLEControlService.class);

        //binding BLEControService callback
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        //register listener that listen BLE status change callback
        LocalBroadcastManager.getInstance(this).registerReceiver(mBLEStatusChangeReceiver, makeGattUpdateIntentFilter());

        //connectBLEDevice();
        mBLEStatusChangeReceiver.setOnBLEStatusChangeListener(new BLEStatusChangeReceiver.OnBLEStatusChangeListener() {
            @Override
            public void onConnected() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO: connected
                    }
                });
            }

            @Override
            public void onDisConnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DeviceDisconnected();
                    }
                });
            }

            @Override
            public void onGattServiceDiscovered() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ready4Discover();
                    }
                });
            }

            @Override
            public void onDataChange(String uuid, byte[] value, String type) {

                if(type.equals(BLEControlService.TYPE_READ)) {

                }else if(type.equals(BLEControlService.TYPE_WRITE)){

                }else if(type.equals(BLEControlService.TYPE_DESCRIPTOR_READ)){

                }else if(type.equals(BLEControlService.TYPE_DESCRIPTOR_WRITE)){

                }
                final String fix_uuid = uuid;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }

            @Override
            public void onRssiRead(int rssi, String type) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });


        mBLEStatusChangeReceiver.updateOnReceiveDataListener(new BLEStatusChangeReceiver.OnReceiveDataListener() {

            @Override
            public void getRecivedData(String uuid, byte[] value) {
                // TODO Auto-generated method stub
                DebugLogger.e(TAG, "getRecivedData from uuid:" + uuid);
                dataReceived(uuid, value);
            }

        });

    }

    protected BLEControlService mService = null;
    private final BLEStatusChangeReceiver mBLEStatusChangeReceiver = new BLEStatusChangeReceiver();
    private final int CONNECT_STATUS_CONNECTED = 1;
    private final int CONNECT_STATUS_DISCONNECTED = 2;
    private int mConnectStatus = CONNECT_STATUS_DISCONNECTED;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        //Service connect
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {

            mService = ((BLEControlService.LocalBinder) rawBinder).getService();

            mBLEStatusChangeReceiver.setBLEService(mService);

            mConnectStatus = CONNECT_STATUS_CONNECTED;

            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            } else {
                BLEServiceReady();
            }
        }
        //Service disconnect
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
            mConnectStatus = CONNECT_STATUS_DISCONNECTED;
        }
    };

    protected void BLEServiceReady()
    {

    }
    protected void ready4Discover()
    {

    }
    protected void dataReceived(String uuid, byte[] value)
    {

    }

    protected void DeviceDisconnected()
    {

    }

    protected BluetoothGattService discoverBLEService(UUID target){

        List<BluetoothGattService> services = mService.getBLEServices();
        if(services==null)
        {
            return null;
        }
        for (BluetoothGattService service : services) {
            DebugLogger.e(TAG, "Discovered Service UUID:" + service.getUuid());

            if (service.getUuid().equals(target))
            {
                return service;
            }

        }
        return null;
    }
    protected void readBLECharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if(mService!=null)
        {
            mService.readCharacteristic(characteristic);
        }
    }

    protected void writeBLECharacteristic(BluetoothGattCharacteristic characteristic, byte[] value)
    {
        if(mService !=null)
        {
            mService.writeCharacteristic(characteristic, value);
        }
    }
    protected void enableIndication()
    {
        if(mService != null)
        {
            mService.enableHTIndication();
        }
    }

    protected void enableHTDiscIndication()
    {
        if(mService != null)
        {
            mService.enableHTDiscIndication();
        }

    }

    protected void disconnet()
    {
        if(mService!=null)
        {
            mService.disconnect();
        }
    }


    private final int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
    private final int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
    private final int SHIFT_LEFT_8BITS = 8;
    private final int SHIFT_LEFT_16BITS = 16;
    private final int GET_BIT24 = 0x00400000;
    private static final int FIRST_BIT_MASK = 0x01;
    protected double decodeBreathValue(byte[] data) throws Exception {
        double temperatureValue = 0.0;
        byte flag = data[0];
        byte exponential = data[4];
        short firstOctet = convertNegativeByteToPositiveShort(data[1]);
        short secondOctet = convertNegativeByteToPositiveShort(data[2]);
        short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
        int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS) | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;
        mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
        temperatureValue = (mantissa * Math.pow(10, exponential));
		/*
		 * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
		 * Celsius = (98.6*Fahrenheit -32) 5/9
		 */
        if ((flag & FIRST_BIT_MASK) != 0) {
            temperatureValue = (float) ((98.6 * temperatureValue - 32) * (5 / 9.0));
        }
        return temperatureValue;

    }

    protected short convertNegativeByteToPositiveShort(byte octet) {
        if (octet < 0) {
            return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
        } else {
            return octet;
        }
    }

    protected int getTwosComplimentOfNegativeMantissa(int mantissa) {
        if ((mantissa & GET_BIT24) != 0) {
            return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
        } else {
            return mantissa;
        }
    }


    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed");
        if(mService!=null)
        {
            mService.disconnect();
        }
        super.onBackPressed();
        return;
    }

}
