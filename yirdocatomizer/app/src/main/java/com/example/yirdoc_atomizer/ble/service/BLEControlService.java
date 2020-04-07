package com.example.yirdoc_atomizer.ble.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BLEControlService  extends Service {
    private final static String TAG = BLEControlService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private final MyBluetoothGattCallback mGattCallback = new MyBluetoothGattCallback();;

    public static final String TYPE_READ = "4";
    public static final String TYPE_WRITE = "5";
    public static final String TYPE_DESCRIPTOR_READ = "6";
    public static final String TYPE_DESCRIPTOR_WRITE = "7";
    public static final String TYPE_RSSI_READ = "8";

    //broadcast
    public final static String ACTION_GATT_CONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String RECEIVE_DATA = "com.nordicsemi.nrfUART.RECEIVE_DATA";
    public final static String UUID_DATA = "com.nordicsemi.nrfUART.UUID_DATA";

    public final static String ACTION_TYPE = "com.nordicsemi.nrfUART.ACTION_TYPE";

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final String SYS_UUID_REAR_STR = "0000-1000-8000-00805f9b34fb";

    public final static int HT_TYPE = 1;
    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001830-0000-1000-8000-00805f9b34fb");
    public static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00000002-0000-1000-8000-00805f9b34fb");
    public static final UUID HT_DISCONNECT_CHARACTERISTIC_UUID = UUID.fromString("00000003-0000-1000-8000-00805f9b34fb");
    public static final UUID HT_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("00000004-0000-1000-8000-00805f9b34fb");
    public static final byte HT_CONTROL_CMD_DUTY = 1;
    public static final byte HT_CONTROL_CMD_FREQ = 2;
    public static final byte HT_CONTROL_CMD_POWEROFF = 3;
    public static final UUID HT_PARAMS_CHARACTERISTIC_UUID = UUID.fromString("00000005-0000-1000-8000-00805f9b34fb");


    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final static UUID GENERIC_ATTRIBUTE_SERVICE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private final static UUID SERVICE_CHANGED_CHARACTERISTIC = UUID.fromString("00002A05-0000-1000-8000-00805f9b34fb");

    public final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    public final static UUID DEVICE_INFO_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    public final static UUID DEVICE_INFO_VISION_CHARACTERISTIC = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public final static UUID DEVICE_INFO_SERIAL_CHARACTERISTIC = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");

    public class LocalBinder extends Binder {
        public BLEControlService getService() {
            return BLEControlService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    private Handler mHandler;
    public boolean connect(BluetoothDevice device) {

        mHandler = new Handler();
        if (mBluetoothAdapter == null || device == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null && device.getAddress().equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        mGattCallback.setBluetoothInfoChangeListener(new MyBluetoothGattCallback.BluetoothInfoChangeListener() {
            @Override
            public void onConnectionStateChange(String action) {
                broadcastUpdate(action);

                if(action == BLEControlService.ACTION_GATT_CONNECTED){
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Some proximity tags (e.g. nRF PROXIMITY) initialize bonding automatically when connected.
                            if(mBluetoothGatt!=null){
                                if (mBluetoothGatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
                                    Log.v(TAG, "Discovering Services...");
                                    Log.d(TAG, "gatt.discoverServices()");
                                    mBluetoothGatt.discoverServices();
                                }
                            } else {
                                // reconnect?
                                Log.e(TAG, "mBluetoothGatt is null");
                            }

                        }
                    }, 600);
                }


            }
            @Override
            public void onServicesDiscovered(String action) {

                if (ensureServiceChangedEnabled(mBluetoothGatt))
                    return;

                broadcastUpdate(action);
            }
            @Override
            public void onCharacteristicRead(String action, BluetoothGattCharacteristic characteristic) {
                Log.e(TAG, "onCharacteristicRead:"+characteristic.getUuid());
                broadcastUpdate(action, characteristic, TYPE_READ);
            }
            @Override
            public void onCharacteristicChanged(String action, BluetoothGattCharacteristic characteristic) {
                Log.e(TAG, "onCharacteristicChanged:"+characteristic.getUuid());
                broadcastUpdate(action, characteristic, TYPE_WRITE);
            }
            @Override
            public void onDescriptorRead(String action, BluetoothGattDescriptor descriptor) {
                broadcastUpdate(action, descriptor, TYPE_DESCRIPTOR_READ);
            }
            @Override
            public void onDescriptorWrite(String action, BluetoothGattDescriptor descriptor) {
                Log.e(TAG, "onDescriptorWrite:"+descriptor.getUuid());
                broadcastUpdate(action, descriptor, TYPE_DESCRIPTOR_WRITE);
            }
            @Override
            public void onReadRemoteRssi(String action, int rssi) {
                broadcastUpdate(action, rssi, TYPE_RSSI_READ);
            }
        });

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        mBluetoothDeviceAddress = device.getAddress();
        return true;
    }


    private boolean ensureServiceChangedEnabled(final BluetoothGatt gatt) {
        if (gatt == null)
            return false;

        // The Service Changed indications have sense only on bonded devices
        final BluetoothDevice device = gatt.getDevice();
        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
            return false;

        final BluetoothGattService gaService = gatt.getService(GENERIC_ATTRIBUTE_SERVICE);
        if (gaService == null)
            return false;

        final BluetoothGattCharacteristic scCharacteristic = gaService.getCharacteristic(SERVICE_CHANGED_CHARACTERISTIC);
        if (scCharacteristic == null)
            return false;

        Log.i("TAG", "Service Changed characteristic found on a bonded device");
        return enableIndications(scCharacteristic);
    }


    protected final boolean enableIndications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0)
            return false;

        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            Log.v(TAG, "Enabling indications for " + characteristic.getUuid());
            Log.d(TAG, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x02-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.w(TAG, "readCharacteristic:"+characteristic.getUuid());
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readDiscriptor(BluetoothGattDescriptor descriptor){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value){

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        characteristic.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void readRemoteRssi(){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readRemoteRssi();
    }

    public void enableHTDiscIndication()
    {
        if(mBluetoothGatt==null) {
            return;
        }

        BluetoothGattService HtService = mBluetoothGatt.getService(HT_SERVICE_UUID);

        if (HtService == null) {
            showMessage("Ht service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic HtChar = HtService.getCharacteristic(HT_DISCONNECT_CHARACTERISTIC_UUID);
        if (HtChar == null) {
            showMessage("HT_DISCONNECT_CHARACTERISTIC_UUID not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(HtChar, true);

        BluetoothGattDescriptor descriptor = HtChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void enableHTIndication()
    {

        BluetoothGattService HtService = mBluetoothGatt.getService(HT_SERVICE_UUID);

        if (HtService == null) {
            showMessage("Ht service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic HtChar = HtService.getCharacteristic(HT_MEASUREMENT_CHARACTERISTIC_UUID);
        if (HtChar == null) {
            showMessage("HtM charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(HtChar, true);

        BluetoothGattDescriptor descriptor = HtChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void enableTXNotification()
    {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void writeRXCharacteristic(byte[] value)
    {
        BluetoothGattService RxService = null;
        //通过UUID获取 接收端的服务
        if(mBluetoothGatt != null) {
            RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        }else{
            Toast.makeText(BLEControlService.this, "please reconnect", Toast.LENGTH_SHORT).show();
            return ;
        }

        showMessage("mBluetoothGatt null"+ mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        //写入值
        RxChar.setValue(value);

        //写回信息
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

        Log.d(TAG, "write TXchar - status=" + status);
    }


    public void getDisInfo(){
        BluetoothGattService disService = mBluetoothGatt.getService(DIS_UUID);
        if(disService == null){
            showMessage("Dis charateristic not found!");
        }else{
            showMessage("Dis:" + disService.getCharacteristics());
        }
    }

    /**
     * get BLEService by uuid
     * @param uuid
     * @return
     */
    public BluetoothGattService getBLEService(UUID uuid){

        BluetoothGattService disService = mBluetoothGatt.getService(uuid);
        return disService;
    }

    public List<BluetoothGattService> getBLEServices(){

        if(mBluetoothGatt == null){
            Log.e(TAG, "BluetoothGatt is null");
            return null;
        }

        return mBluetoothGatt.getServices();
    }

    public boolean WriteBLECharacteristic(BluetoothGattService service, UUID characteristicUuid, byte[] value){
        BluetoothGattCharacteristic bluetoothGattCharacteristic = service.getCharacteristic(characteristicUuid);
        bluetoothGattCharacteristic.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        return status;
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, String type) {
        final Intent intent = new Intent(action);

        intent.putExtra(BLEControlService.RECEIVE_DATA, characteristic.getValue());
        intent.putExtra(BLEControlService.UUID_DATA, characteristic.getUuid().toString()); //此处不能直接使用intent传递UUID类型的数据
        intent.putExtra(BLEControlService.ACTION_TYPE, type);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattDescriptor descriptor, String type) {
        final Intent intent = new Intent(action);

        intent.putExtra(BLEControlService.RECEIVE_DATA, descriptor.getValue());
        intent.putExtra(BLEControlService.UUID_DATA, descriptor.getUuid().toString()); //此处不能直接使用intent传递UUID类型的数据
        intent.putExtra(BLEControlService.ACTION_TYPE, type);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, int rssi, String type){
        final Intent intent = new Intent(action);
        intent.putExtra(BLEControlService.RECEIVE_DATA, rssi);
        intent.putExtra(BLEControlService.ACTION_TYPE, type);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
