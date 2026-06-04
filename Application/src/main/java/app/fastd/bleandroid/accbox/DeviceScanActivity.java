/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.fastd.bleandroid.accbox;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fastd.android.accbox.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@RequiresApi(api = Build.VERSION_CODES.S)
public class DeviceScanActivity extends Activity {

    private final Handler connectTimeoutHandler = new Handler();
    private boolean openedControlActivity = false;
    private boolean bleConnectedOrFound = false;

    private final Runnable openControlFallback = new Runnable() {
        @Override
        public void run() {
            if (!openedControlActivity && !bleConnectedOrFound) {
                Log.d(TAG, "Timeout de 10s: entrando a DeviceControlActivity sin conexión BLE");

                openedControlActivity = true;

                Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
                // NO mandar variable_DemoTest
                startActivity(intent);
                finish();
            }
        }
    };
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final boolean TODO = true;
//private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mbluetoothLeScanner;
    ScanCallback mBtScanCallback;

    private boolean mScanning;
    private Handler mHandler;
    public String DT = null;

    private int scanAttempts = 0;
    private final int MAX_SCAN_ATTEMPTS = 2; // Primer scan + 1 reintento
    private boolean userSelectedDevice = false;
    private boolean foundFastD = false;
    private boolean fastDFound = false;


    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000; //5000;//10000;

    private final String[] permissions = { //only dangerous permissions are run-time permissions
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED};
    // with the SDK 31 and higher (Android 12) we need to have also BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions
    @RequiresApi(api = Build.VERSION_CODES.S)
    private final String[] permissionsForS = { //only dangerous permissions are run-time permissions
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED};

    // fastd
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final static String TAG = DeviceScanActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        handlePermissions();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            Toast.makeText(this, "BluetoothManager no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

/*
    @Override
    //@RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                //mLeDeviceListAdapter.clear();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return TODO;
                }
                new Handler().postDelayed(() -> {
                    scanLeDevice(true);
                }, 200);
                DT = null;
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                DT = null;
                break;
            case R.id.demoTest:
                DT = "1";
                Intent DTintent = new Intent(this, DeviceControlActivity.class);
                DTintent.putExtra("variable_DemoTest", DT);
                Log.d(TAG, "DTest en scan:" + DT);
                startActivity(DTintent);
                break;
        }
        return true;
    }
*/
    //@SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        openedControlActivity = false;
        bleConnectedOrFound = false;

        if (mBluetoothAdapter == null) return;

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        mbluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mbluetoothLeScanner == null) {
            Toast.makeText(this, "No se pudo obtener el scanner BLE", Toast.LENGTH_SHORT).show();
            return;
        }

        connectTimeoutHandler.removeCallbacks(openControlFallback);
        connectTimeoutHandler.postDelayed(openControlFallback, 10000);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            scanLeDevice(true);
        }, 500);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    protected void onPause() {
        super.onPause();
        connectTimeoutHandler.removeCallbacks(openControlFallback);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        scanLeDevice(false);
        //mLeDeviceListAdapter.clear();
    }

    //@SuppressLint("MissingPermission")
    /*
    *
    *
    @Override
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (device.getName() != null && device.getName().contains("FastD")) {
            userSelectedDevice = true;
        }
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mbluetoothLeScanner.stopScan(mBtScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
*/
    ////@SuppressLint("MissingPermission")
    //@SuppressLint("MissingPermission")
// ???? con suppressLint funcion para Android 9 sin y agregando el otro permiso con Android 14
    // funciono en ambos cuando el permiso fue Manifest.permission.ACCESS_FINE_LOCATION no SCAN
    //@SuppressLint("MissingPermission")

/*
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            scanAttempts++;
            Log.v(TAG, "scanLeDevice intento " + scanAttempts);

            mLeDeviceListAdapter.clear();

            mBtScanCallback = new BTScanCallback();
            ScanSettings scanSettings = new ScanSettings.Builder().build();


            //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            //    return;
            //}

            // 🔑 Chequeo condicional de permisos según versión Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Falta permiso BLUETOOTH_SCAN");
                    return;
                }
            } else { // Android 10 y 11
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Falta permiso ACCESS_FINE_LOCATION");
                    return;
                }
            }

            mbluetoothLeScanner.startScan(null, scanSettings, mBtScanCallback);
            mScanning = true;
            invalidateOptionsMenu();

            mHandler.postDelayed(() -> {
                // 🚀 Si ya encontramos FastD en onScanResult, no seguimos
                if (fastDFound) {
                    Log.v(TAG, "FastD ya encontrado → no relanzamos scan.");
                    scanAttempts = 0;
                    return;
                }

                mScanning = false;
                invalidateOptionsMenu();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    mbluetoothLeScanner.stopScan(mBtScanCallback);
                    invalidateOptionsMenu();
                }

                // Verificar si hay algún FastD detectado en la lista
                foundFastD = false;
                for (BluetoothDevice dev : mLeDeviceListAdapter.mLeDevices) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        String name = dev.getName();
                        if (name != null && name.contains("FastD")) {
                            foundFastD = true;
                            break;
                        }
                    }
                }

                // Relanzar scan si no se encontró FastD
                if (!foundFastD && scanAttempts < MAX_SCAN_ATTEMPTS && !userSelectedDevice) {
                    Log.v(TAG, "No se encontró FastD → relanzando scan...");
                    Toast.makeText(DeviceScanActivity.this, "Retrying Scan...", Toast.LENGTH_SHORT).show();
                    mHandler.postDelayed(() -> scanLeDevice(true), 1500);
                } else {
                    Log.v(TAG, "Escaneo finalizado. foundFastD=" + foundFastD +
                            ", dispositivos encontrados: " + mLeDeviceListAdapter.getCount());
                    scanAttempts = 0;
                }

            }, SCAN_PERIOD);

        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                mbluetoothLeScanner.stopScan(mBtScanCallback);
                invalidateOptionsMenu();
            }
            scanAttempts = 0;
        }
    }
*/
    private void scanLeDevice(final boolean enable) {
        if (mbluetoothLeScanner == null) return;

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Falta permiso BLUETOOTH_SCAN");
                    return;
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Falta permiso ACCESS_FINE_LOCATION");
                    return;
                }
            }

            mBtScanCallback = new BTScanCallback();
            ScanSettings scanSettings = new ScanSettings.Builder().build();

            mbluetoothLeScanner.startScan(null, scanSettings, mBtScanCallback);
            mScanning = true;
            Log.d(TAG, "Escaneo BLE iniciado");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mScanning) {
                    scanLeDevice(false);
                    Log.d(TAG, "Escaneo BLE detenido por tiempo");
                }
            }, SCAN_PERIOD);

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            if (mBtScanCallback != null && mbluetoothLeScanner != null) {
                mbluetoothLeScanner.stopScan(mBtScanCallback);
            }

            mScanning = false;
        }
    }
/*
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public @NonNull View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            String deviceName = device.getName();

            if (deviceName != null && deviceName.contains("FastD")) {
                viewHolder.deviceName.setText(" " + deviceName);

            } else {
                viewHolder.deviceName.setText(" Unknown_device");
            }
            //delay_pon();
            return view;
        }
    }
  */
    private boolean deviceControlOpened = false;

  /*
private class BTScanCallback extends ScanCallback {
    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    })

    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        BluetoothDevice device = result.getDevice();
        if (device == null) return;

        String name = device.getName();
        if (name != null) {
            Log.v(TAG, "Dispositivo detectado: " + name);
            foundFastD = true;

            // 🚀 Si encontramos "FastD", detenemos el escaneo
            if (name.contains("FastD") && !deviceControlOpened) {
                bleConnectedOrFound = true;
                deviceControlOpened = true;
                fastDFound = true;
                connectTimeoutHandler.removeCallbacks(openControlFallback);
                Log.v(TAG, "Encontrado FastD → deteniendo scan...");

                if (ActivityCompat.checkSelfPermission(DeviceScanActivity.this,
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    mbluetoothLeScanner.stopScan(this);
                }

                mScanning = false;
                invalidateOptionsMenu();

                Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                startActivity(intent);
                finish();
            }
        }

        // Agregar el dispositivo a la lista
        runOnUiThread(() -> {
            mLeDeviceListAdapter.addDevice(device);
            mLeDeviceListAdapter.notifyDataSetChanged();
        });
    }
}
*/

    private class BTScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            String name = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(DeviceScanActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            name = device.getName();

            if (name != null) {
                Log.d(TAG, "Dispositivo detectado: " + name);
            }

            if (name != null && name.contains("FastD") && !openedControlActivity) {
                bleConnectedOrFound = true;
                openedControlActivity = true;

                connectTimeoutHandler.removeCallbacks(openControlFallback);
                scanLeDevice(false);

                Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                startActivity(intent);
                finish();
            }
        }
    }

    // Device scan callback.
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //mLeDeviceListAdapter.addDevice(device);
                           // mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
    private void handlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> missingPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (!missingPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this,
                        missingPermissions.toArray(new String[0]),
                        PERMISSIONS_REQUEST_CODE
                );
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_CODE
                );
            }
        }
    }

    /**
     * Call back method is used to check for granted permissions and provide information in case
     * permissions are not granted but are necessary for the app functionality
     *
     * @param requestCode  requestCode used for requesting the permissions
     * @param permissions  Permissions that were requested
     * @param grantResults result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "All requested permissions granted. Continue..");
            } else {
                Log.e(TAG, "Not all permissions are granted. Accept the permissions request for full app functionality..");
            }
            return;
        }
    }

    public void delay_pon () {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

        }, 150); // 500 2500 1000
    }

    public void delay () {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

        }, 1500); // 15
    }




}

