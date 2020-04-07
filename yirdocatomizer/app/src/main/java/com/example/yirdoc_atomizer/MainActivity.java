package com.example.yirdoc_atomizer;

import com.example.yirdoc_atomizer.ble.scanner.ScannerFragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BleServiceActivity implements ScannerFragment.OnDeviceSelectedListener {

    private final String TAG = "MainActivity";

    LinearLayout scan_btn;
    LinearLayout atomi_low_btn;
    LinearLayout atomi_middle_btn;
    LinearLayout atomi_high_btn;
    LinearLayout power_off_btn;
    TextView ble_status_text;
    TextView breath_detect_log_text;
    TextView power_percent_value;
    TextView firm_version_value;
    TextView serial_no_value;

    private void initLayoutView(){
        scan_btn = this.findViewById(R.id.id_ble_scan_btn);
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ScannerFragment dialog = ScannerFragment.getInstance(MainActivity.this,
                                mService.HT_SERVICE_UUID, false);
                        dialog.show(getFragmentManager(), "scan_fragment");
                    }
                });
            }
        });

        ble_status_text = this.findViewById(R.id.id_ble_status);
        breath_detect_log_text = this.findViewById(R.id.id_breath_detect_log_text);
        power_percent_value = this.findViewById(R.id.id_power_percent_value);
        firm_version_value = this.findViewById(R.id.id_firm_version_value);
        serial_no_value = this.findViewById(R.id.id_serial_no_value);

        atomi_low_btn = this.findViewById(R.id.id_atomi_low_btn);
        atomi_low_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAtomi_Rate(mService.HT_CONTROL_CMD_DUTY, (byte)33);
                atomi_low_btn.setBackgroundResource(R.drawable.green_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_high_btn.setBackgroundResource(R.drawable.gray_border);
            }
        });
        atomi_middle_btn = this.findViewById(R.id.id_atomi_middle_btn);
        atomi_middle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAtomi_Rate(mService.HT_CONTROL_CMD_DUTY, (byte)40);
                atomi_low_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.green_border);
                atomi_high_btn.setBackgroundResource(R.drawable.gray_border);
            }
        });
        atomi_high_btn = this.findViewById(R.id.id_atomi_high_btn);
        atomi_high_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAtomi_Rate(mService.HT_CONTROL_CMD_DUTY, (byte)50);
                atomi_low_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_high_btn.setBackgroundResource(R.drawable.green_border);
            }
        });
        power_off_btn = this.findViewById(R.id.id_power_off_btn);
        power_off_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPower_Off(mService.HT_CONTROL_CMD_POWEROFF);
            }
        });
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    protected static final int REQUEST_ENABLE_BT = 2;
    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }
    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    private void preBle()
    {
        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preBle();
        initLayoutView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, "Bluetooth is not turned on ", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //TODO: BLE Logic Related
    private BluetoothGattService mBleVersionService = null;
    private BluetoothGattService mBleControlService = null;
    private BluetoothGattService mBleBatteryService = null;
    private String mBreathLogs = "";
    private int mBatteryValue = 0;

    private void readFirmVersion(){
        mBleVersionService = discoverBLEService(mService.DEVICE_INFO_SERVICE);
        if(mBleVersionService!=null)
        {
            BluetoothGattCharacteristic characteristic = mBleVersionService.getCharacteristic(mService.DEVICE_INFO_VISION_CHARACTERISTIC);
            readBLECharacteristic(characteristic);
        }
    }

    private void readSerialNo(){
        mBleVersionService = discoverBLEService(mService.DEVICE_INFO_SERVICE);
        if(mBleVersionService!=null)
        {
            BluetoothGattCharacteristic characteristic = mBleVersionService.getCharacteristic(mService.DEVICE_INFO_SERIAL_CHARACTERISTIC);
            readBLECharacteristic(characteristic);
        }
    }

    private void readBattery()
    {
        mBleBatteryService = discoverBLEService(mService.BATTERY_SERVICE);
        if(mBleBatteryService!=null)
        {
            BluetoothGattCharacteristic characteristic = mBleBatteryService.getCharacteristic(mService.BATTERY_LEVEL_CHARACTERISTIC);
            readBLECharacteristic(characteristic);
        } else {
            Log.e(TAG, "mBleVersionService is NULL");
        }
    }

    private void readBreathOpt()
    {
        mBleControlService = discoverBLEService(mService.HT_SERVICE_UUID);
        if(mBleControlService!=null)
        {
            BluetoothGattCharacteristic characteristic = mBleControlService.getCharacteristic(mService.HT_PARAMS_CHARACTERISTIC_UUID);
            readBLECharacteristic(characteristic);
        } else {
            Log.e(TAG, "mBleControlService is NULL");
        }
    }

    private void setAtomi_Rate(byte cmd, byte cmd_value){
        if(cmd == mService.HT_CONTROL_CMD_DUTY){
            byte[] value = new byte[2];
            value[0] = cmd;
            value[1] = cmd_value;
            mBleControlService = discoverBLEService(mService.HT_SERVICE_UUID);
            if(mBleControlService!=null)
            {
                Log.e(TAG, "writeParams");
                BluetoothGattCharacteristic characteristic = mBleControlService.getCharacteristic(mService.HT_CONTROL_CHARACTERISTIC_UUID);
                writeBLECharacteristic(characteristic, value);
            } else {
                Log.e(TAG, "mBleControlService is NULL");
            }
        }
    }

    private void setPower_Off(byte cmd){
        if(cmd == mService.HT_CONTROL_CMD_POWEROFF){
            byte[] value = new byte[1];
            value[0] = cmd;
            mBleControlService = discoverBLEService(mService.HT_SERVICE_UUID);
            if(mBleControlService!=null)
            {
                Log.e(TAG, "writeParams");
                BluetoothGattCharacteristic characteristic = mBleControlService.getCharacteristic(mService.HT_CONTROL_CHARACTERISTIC_UUID);
                writeBLECharacteristic(characteristic, value);
            } else {
                Log.e(TAG, "mBleControlService is NULL");
            }
        }
    }


    @Override
    protected void BLEServiceReady()
    {
        Log.e(TAG, "BLEServiceReady");
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        ConnectDevice(device);
        ble_status_text.setText("Connecting 30%");
    }

    @Override
    protected void ready4Discover()
    {
        Log.e(TAG, "ready4Discover");
        readFirmVersion();
    }

    private int indication_index = 0;
    @Override
    protected void dataReceived(String uuid, byte[] value)
    {
        if(uuid.equals(mService.DEVICE_INFO_VISION_CHARACTERISTIC.toString()))
        {
            String firm_version = new String(value);
            Log.e(TAG, "dataReceived: device firm version is  " + firm_version);
            updateFirmVersion(firm_version);
            readSerialNo();
            return;
        }

        if(uuid.equals(mService.DEVICE_INFO_SERIAL_CHARACTERISTIC.toString()))
        {
            String serial_num = new String(value);
            Log.e(TAG, "dataReceived: device serial no is  " + serial_num);
            updateSerialNo(serial_num);
            enableIndication();
            indication_index = 0;
            //enableHTDiscIndication();
            return;
        }


        //Read Power Value
        if(uuid.equals(mService.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID.toString()))
        {
            Log.e(TAG, "dataReceived: client config descriptor is received.");

            if(indication_index==0){
                enableHTDiscIndication();
                indication_index = 1;
                return;
            } else if(indication_index==1){
                readBattery();
                indication_index =2;
            }
        }

        ble_status_text.setText("Connected");

        //Breath Opt Value
        if(uuid.equals(mService.BATTERY_LEVEL_CHARACTERISTIC.toString()))
        {
            Log.e(TAG, "dataReceived: battery value is " + value[0]);
            mBatteryValue = value[0];
            updatePowerValue(mBatteryValue);
            readBreathOpt();
        }

        //Atomizer Rate
        if(uuid.equals(mService.HT_PARAMS_CHARACTERISTIC_UUID.toString()))
        {
            Log.e(TAG, "duty value: " + value[0] + "; frequence value: " + value[1]);
            int atomi_rate = value[0];
            updateAtomi_Rate(atomi_rate);
        }

        if(uuid.equals(mService.HT_MEASUREMENT_CHARACTERISTIC_UUID.toString()))
        {
            updateLogBreathOpt(value[0]&0xff);
        }

        if(uuid.equals(mService.HT_DISCONNECT_CHARACTERISTIC_UUID.toString()))
        {
            Log.e(TAG, "dataReceived: disconnect code is " + value[0]);
            updateDisconnectStatus(value[0]);
        }
    }

    @Override
    protected void DeviceDisconnected()
    {
    }

    //TODO: Update UI with BLE data received
    private void updateFirmVersion(String firmversion){
        firm_version_value.setText(firmversion);
        ble_status_text.setText("Connecting 60%");
    }

    private void updateSerialNo(String serialno){
        serial_no_value.setText(serialno);
        ble_status_text.setText("Connecting 90%");
    }

    private void updateLogBreathOpt(int code)
    {
        Log.e(TAG, "onOperation: " + String.valueOf(code));
        switch (code)
        {
            case 1:
                mBreathLogs += "[Breath In],";
                break;
            case 99:

                mBreathLogs += "[Breath Out],";
                break;
            case 255:

                mBreathLogs += "[Breath No Action],";
                break;
        }
        breath_detect_log_text.setText(mBreathLogs);
    }

    private void updatePowerValue(int power){
        power_percent_value.setText(String.valueOf(power)+"%");
    }

    private void updateAtomi_Rate(int atomi_rate){
        switch (atomi_rate){
            case 0:
                atomi_low_btn.setBackgroundResource(R.drawable.green_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_high_btn.setBackgroundResource(R.drawable.gray_border);
                break;
            case 2:
                atomi_low_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.green_border);
                atomi_high_btn.setBackgroundResource(R.drawable.gray_border);
                break;
            case 4:
                atomi_low_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_high_btn.setBackgroundResource(R.drawable.green_border);
                break;
            default:
                atomi_low_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_middle_btn.setBackgroundResource(R.drawable.gray_border);
                atomi_high_btn.setBackgroundResource(R.drawable.gray_border);
                break;
        }
    }

    private void updateDisconnectStatus(int code){
        String reason ="";
        if(code == 1)
        {
            Log.e(TAG, "dataReceived: normal close");
            reason = "Normal Close";
        } else if (code == 2){
            Log.e(TAG, "dataReceived: no watter");
            reason = "No Watter Close";
        } else if (code == 3){
            Log.e(TAG, "dataReceived: low power");
            reason = "Low Power Close";
        } else if (code == 5){
            Log.e(TAG, "dataReceived: closed by app");
            reason = "App Close";
        }
        ble_status_text.setText("Disconnect["+reason+"]");
    }

}
