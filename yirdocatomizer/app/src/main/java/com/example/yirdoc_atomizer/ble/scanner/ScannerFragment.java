package com.example.yirdoc_atomizer.ble.scanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.yirdoc_atomizer.R;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.yirdoc_atomizer.ble.utility.DebugLogger;

/**
 * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter devices with standard BLE Service UUID and devices with custom BLE Service UUID It contains a
 * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is implemented by activity in order to receive selected device. The scanning will continue for 5
 * seconds and then stop
 */
public class ScannerFragment extends DialogFragment {
	private final static String TAG = "ScannerFragment";

	private final static String PARAM_UUID = "param_uuid";
	private final static String CUSTOM_UUID = "custom_uuid";
	private final static long SCAN_DURATION = 5000;

	private BluetoothAdapter mBluetoothAdapter;
	private OnDeviceSelectedListener mListener;
	private DeviceListAdapter mAdapter;
	private Handler mHandler = new Handler();
	private Button mScanButton;
	private ImageView mScanTopImg;

	private boolean mIsCustomUUID;
	private UUID mUuid;

	private boolean mIsScanning = false;

	private static final boolean DEVICE_IS_BONDED = true;
	private static final boolean DEVICE_NOT_BONDED = false;
	/* package */static final int NO_RSSI = -1000;

	/**
	 * Static implementation of fragment so that it keeps data when phone orientation is changed For standard BLE Service UUID, we can filter devices using normal android provided command
	 * startScanLe() with required BLE Service UUID For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required device
	 */
	public static ScannerFragment getInstance(Context context, UUID uuid, boolean isCustomUUID) {
		final ScannerFragment fragment = new ScannerFragment();

		final Bundle args = new Bundle();
		args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
		args.putBoolean(CUSTOM_UUID, isCustomUUID);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Interface required to be implemented by activity
	 */
	public static interface OnDeviceSelectedListener {
		/**
		 * Fired when user selected the device
		 * 
		 * @param device
		 *            the device to connect to
		 */
		public void onDeviceSelected(BluetoothDevice device);
	}

	/**
	 * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			this.mListener = (OnDeviceSelectedListener) activity;
		} catch (final ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle args = getArguments();
		final ParcelUuid pu = args.getParcelable(PARAM_UUID);
		mUuid = pu.getUuid();
		mIsCustomUUID = args.getBoolean(CUSTOM_UUID);

		final BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
	}

	@Override
	public void onDestroyView() {
		stopScan();
		super.onDestroyView();
	}

	/**
	 * When dialog is created then set AlertDialog with list and button views
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.Diglog_Transparent));
		final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
		final ListView listview = (ListView) dialogView.findViewById(android.R.id.list);

		listview.setEmptyView(dialogView.findViewById(android.R.id.empty));
		listview.setAdapter(mAdapter = new DeviceListAdapter(getActivity()));

		final AlertDialog dialog = builder.setView(dialogView).create();
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
		dialog.setView(dialogView,0,0,0,0);
		
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				stopScan();
				dialog.cancel();
				mListener.onDeviceSelected(((ExtendedBluetoothDevice) mAdapter.getItem(position)).device);
			}
		});

		mScanTopImg = (ImageView) dialogView.findViewById(R.id.id_seldec_top_words);
		if(isZh()==true)
		{
			mScanTopImg.setImageResource(R.drawable.ic_seldec_top_words);
		}else{
			if(isDe())
			{
				mScanTopImg.setImageResource(R.drawable.ic_seldec_top_words_de);
			} else {
				if (isKo()) {
					mScanTopImg.setImageResource(R.drawable.ic_seldec_top_words_ko);

				} else {
					mScanTopImg.setImageResource(R.drawable.ic_seldec_top_words_en);
				}

			}

		}

		mScanButton = (Button) dialogView.findViewById(R.id.action_cancel);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.action_cancel) {
					if (mIsScanning) {
						dialog.cancel();
					} else {
						startScan();
					}
				}
			}
		});

		addBondedDevices();
		
		if (savedInstanceState == null)
			startScan();
		return dialog;
	}

	protected boolean isZh() {
		Locale locale = this.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		if (language.endsWith("zh"))
			return true;
		else
			return false;
	}

	protected boolean isDe(){
		Locale locale = this.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		//Log.e(TAG, language);
		if (language.endsWith("de"))
			return true;
		else
			return false;
	}

	protected boolean isKo(){
		Locale locale = this.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		//Log.e(TAG, language);
		if (language.endsWith("ko"))
			return true;
		else
			return false;
	}

	private final int REQUEST_PERMISSION_REQ_CODE = 1;
	@Override
	public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_PERMISSION_REQ_CODE: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startScan();
				}
				break;
			}
		}
	}

	/**
	 * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out
	 * using class ScannerServiceParser
	 */
	private void startScan() {

		if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) ) {
				return;
			}
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
			return;
		}


		mAdapter.clearDevices();
		if(isZh()==true)
		{
			mScanButton.setBackgroundResource(R.drawable.ic_seldec_cancel);
		} else {
			if(isDe())
			{
				mScanButton.setBackgroundResource(R.drawable.ic_seldec_cancel_de);
			} else {
				if (isKo()) {

					mScanButton.setBackgroundResource(R.drawable.ic_seldec_cancel_ko);
				} else {
					mScanButton.setBackgroundResource(R.drawable.ic_seldec_cancel_en);
				}

			}

		}

		mIsCustomUUID = true; // Samsung Note II with Android 4.3 build JSS15J.N7100XXUEMK9 is not filtering by UUID at all. We have to disable it
		if (mIsCustomUUID) {
			mBluetoothAdapter.startLeScan(mLEScanCallback);
		} else {
			final UUID[] uuids = new UUID[1];
			uuids[0] = mUuid;
			mBluetoothAdapter.startLeScan(uuids, mLEScanCallback);
		}

		mIsScanning = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mIsScanning) {
					stopScan();
				}
			}
		}, SCAN_DURATION);
	}

	/**
	 * Stop scan if user tap Cancel button
	 */
	private void stopScan() {
		if (mIsScanning) {
			if(isZh()==true)
			{
				mScanButton.setBackgroundResource(R.drawable.ic_seldec_scan);
			} else {
				if(isDe())
				{
					mScanButton.setBackgroundResource(R.drawable.ic_seldec_scan_de);
				} else {
					if (isKo()) {

						mScanButton.setBackgroundResource(R.drawable.ic_seldec_scan_ko);
					} else {
						mScanButton.setBackgroundResource(R.drawable.ic_seldec_scan_en);
					}

				}

			}
			mBluetoothAdapter.stopLeScan(mLEScanCallback);
			mIsScanning = false;
		}
	}

	private void addBondedDevices() {
		final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
		for (BluetoothDevice device : devices) {
			mAdapter.addBondedDevice(new ExtendedBluetoothDevice(device, NO_RSSI, DEVICE_IS_BONDED));
		}
	}

	/**
	 * if scanned device already in the list then update it otherwise add as a new device
	 */
	private void addScannedDevice(final BluetoothDevice device, final int rssi, final boolean isBonded) {
		try{
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.addOrUpdateDevice(new ExtendedBluetoothDevice(device, rssi, isBonded));
				}
			});
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * if scanned device already in the list then update it otherwise add as a new device
	 */
	private void updateScannedDevice(final BluetoothDevice device, final int rssi) {
		try{
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.updateRssiOfBondedDevice(device.getAddress(), rssi);
				}
			});
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * Callback for scanned devices class {@link ScannerServiceParser} will be used to filter devices with custom BLE service UUID then the device will be added in a list
	 */
	private BluetoothAdapter.LeScanCallback mLEScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (device != null) {
				updateScannedDevice(device, rssi);
				if (mIsCustomUUID) {
					ScannerServiceParser parser = ScannerServiceParser.getParser();
					try {
						parser.decodeDeviceAdvData(scanRecord, mUuid);

						if (parser.isValidSensor()) {
							addScannedDevice(device, rssi, DEVICE_NOT_BONDED);
						}
					} catch (Exception e) {
						DebugLogger.e(TAG, "Invalid data in Advertisement packet " + e.toString());
					}
				} else {
					addScannedDevice(device, rssi, DEVICE_NOT_BONDED);
				}
			}
		}
	};
}
