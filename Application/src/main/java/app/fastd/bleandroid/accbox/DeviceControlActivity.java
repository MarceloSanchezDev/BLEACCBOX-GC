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
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.fastd.android.accbox.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */


public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView tv;
    private TextView nm;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean isNavigatingToScanner = false;
    private final Handler disconnectHandler = new Handler();
    private boolean isServiceBound = false;
    private boolean isReceiverRegistered = false;
    private boolean TODO = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;


    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private int ESTADO = 0;
    private int estado = 0;
    private int NBT = 0;
    private int tipoBT = 0;
    private int EPARK = 0;
    private int EBLOCK = 0;
    private int dataValue = 0;
    private int dataValueCRC = 0;
    private int STEP = 0;
    private String DT = null;
    private String dt = null;
    private Button mbuttonN, mbuttonC, mbuttonS, mbuttonR, mbuttonP, mbuttonB, mbuttonEco, mbuttonE, mbuttonPlus, mbuttonMenos;
    private ImageView IM4;
    private ImageView IM3;
    private ImageView IM2;
    private ImageView IM1;
    private ImageView IM5;
    private ImageView IP5;
    private ImageView IP4;
    private ImageView IP3;
    private ImageView IP2;
    private ImageView IP1;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "***** SERVICE CONECTADO");

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
                return;
            }

            if (dt != null) {
                Log.d(TAG, "Modo demo activo: no se intenta conexión BLE");
                return;
            }

//            if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
//                Log.w(TAG, "mDeviceAddress vacío. Se vuelve al scanner.");
//                abrirScanner();
//                return;
//            }

            if (!hasBluetoothConnectPermission()) {
                Log.w(TAG, "Falta permiso BLUETOOTH_CONNECT");
                return;
            }

            Log.d(TAG, "***** mDeviceAddress = " + mDeviceAddress);
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "***** connect desde onServiceConnected = " + result);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Toast.makeText(DeviceControlActivity.this, "Conectado", Toast.LENGTH_SHORT).show();
                Button btnDemoTop = findViewById(R.id.btnDemoTop);
                btnDemoTop.setEnabled(false);
                btnDemoTop.setAlpha(0.5f); // efecto visual (opcional)
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Button btnBluetoothTop = findViewById(R.id.btnBluetoothTop);
                // btnBluetoothTop.setText("DISCONNECT");
                btnBluetoothTop.setBackgroundResource(R.drawable.true_bluetooth);
                readACCBOX();
                botonesEnable();
                restaurarUIDesdeEstado();
                Log.d(TAG, "***** ACTION_GATT_CONNECTED recibido");
                Log.d(TAG, "***** mConnected ahora = true");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "***** ACTION_GATT_DISCONNECTED recibido");

                mConnected = false;
                updateConnectionState(R.string.disconnected);

                Button btnBluetoothTop = findViewById(R.id.btnBluetoothTop);
                if (btnBluetoothTop != null) {
                    btnBluetoothTop.setBackgroundResource(R.drawable.button_bluetooth);
                }

                Button btnDemoTop = findViewById(R.id.btnDemoTop);
                if (btnDemoTop != null) {
                    btnDemoTop.setEnabled(true);
                    btnDemoTop.setAlpha(1f);
                }

                clearUI();
                coloresNeutros();
                botonesNoEnable();

                Log.d(TAG, "***** mConnected ahora = false");

                // Si estamos en modo demo, NO volvemos a buscar BLE.
                if (dt != null) {
                    Toast.makeText(DeviceControlActivity.this, "Modo demo activo", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Si era una conexión real, volvemos automáticamente al scanner.
                handleBleDisconnectedAndSearchAgain();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    private void restaurarUIDesdeEstado() {
        coloresNeutros();
        seteoStepperPON();

        if (estado == 0) {
            mbuttonN.setBackgroundResource(R.drawable.button_normal);
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);

        } else if (estado == 1 || estado == 121 || estado == 120 || estado == 124 || estado == 125) {
            mbuttonC.setBackgroundResource(R.drawable.city_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);

        } else if (estado == 2 || estado == 237 || estado == 236 || estado == 240 || estado == 241) {
            mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);

        } else if (estado == 3 || estado == 353 || estado == 352 || estado == 356 || estado == 357) {
            mbuttonR.setBackgroundResource(R.drawable.race_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);

        } else if (estado == 5 || estado == 585 || estado == 584 || estado == 588 || estado == 589) {
            mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
        }

        if (estado > 600 && estado < 2000) {
            mbuttonP.setBackgroundResource(R.drawable.true_parking);
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        }

        if (estado >= 2000) {
            mbuttonB.setBackgroundResource(R.drawable.true_blocking);
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        }
    }
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };
    private void handleBleDisconnectedAndSearchAgain() {
        if (isNavigatingToScanner) {
            return;
        }

        isNavigatingToScanner = true;

        Log.d(TAG, "BLE desconectado. Cerrando conexión y volviendo a búsqueda.");

        mConnected = false;

        try {
            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error intentando desconectar BLE", e);
        }

        coloresNeutros();
        botonesNoEnable();

        Toast.makeText(
                DeviceControlActivity.this,
                "Dispositivo desconectado. Buscando otro dispositivo...",
                Toast.LENGTH_SHORT
        ).show();

        disconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, 800);
    }
    private void clearUI() {
        if (mDataField != null) {
            mDataField.setText(R.string.no_data);
        }
    }
    private void logActiveScreenConfig() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        int heightDp = getResources().getConfiguration().screenHeightDp;
        int smallestWidthDp = getResources().getConfiguration().smallestScreenWidthDp;

        float density = getResources().getDisplayMetrics().density;
        int widthPx = getResources().getDisplayMetrics().widthPixels;
        int heightPx = getResources().getDisplayMetrics().heightPixels;

        Log.d(TAG, "===== SCREEN SIZE DEBUG =====");
        Log.d(TAG, "widthDp: " + widthDp);
        Log.d(TAG, "heightDp: " + heightDp);
        Log.d(TAG, "smallestWidthDp: " + smallestWidthDp);
        Log.d(TAG, "widthPx: " + widthPx);
        Log.d(TAG, "heightPx: " + heightPx);
        Log.d(TAG, "density: " + density);
        Log.d(TAG, "=============================");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_control);
        logActiveScreenConfig();
        Button btnDemoTop = findViewById(R.id.btnDemoTop);

        btnDemoTop.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceControlActivity.this, DeviceControlActivity.class);
            intent.putExtra("variable_DemoTest", "demo");
            startActivity(intent);
            finish();
        });


        Button btnBluetoothTop = findViewById(R.id.btnBluetoothTop);

       /* btnBluetoothTop.setOnClickListener(v -> {
            if (mBluetoothLeService == null) {
                return;
            }

            if (ActivityCompat.checkSelfPermission(
                    DeviceControlActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (mConnected) {
                mBluetoothLeService.disconnect();
            } else {
                Toast.makeText(DeviceControlActivity.this, "Conectando...", Toast.LENGTH_SHORT).show();
                mBluetoothLeService.connect(mDeviceAddress);
            }
        });
        */

        btnBluetoothTop.setOnClickListener(v -> {
            if (dt != null) {
                Toast.makeText(DeviceControlActivity.this, "Buscando dispositivo BLE...", Toast.LENGTH_SHORT).show();
                abrirScanner();
                return;
            }

            if (mConnected && mBluetoothLeService != null) {
                if (!hasBluetoothConnectPermission()) {
                    Toast.makeText(DeviceControlActivity.this, "Falta permiso Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }

                mBluetoothLeService.disconnect();
                return;
            }

            if (mBluetoothLeService != null && mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                if (!hasBluetoothConnectPermission()) {
                    Toast.makeText(DeviceControlActivity.this, "Falta permiso Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DeviceControlActivity.this, "Conectando...", Toast.LENGTH_SHORT).show();
                mBluetoothLeService.connect(mDeviceAddress);
            } else {
                abrirScanner();
            }
        });

        mbuttonN = (Button) findViewById(R.id.buttonN);
        mbuttonC = (Button) findViewById(R.id.buttonC);
        mbuttonS = (Button) findViewById(R.id.buttonS);
        mbuttonR = (Button) findViewById(R.id.buttonR);
        mbuttonP = (Button) findViewById(R.id.buttonP);
        mbuttonB = (Button) findViewById(R.id.buttonB);
        mbuttonE = (Button) findViewById(R.id.buttonE);
        mbuttonEco = (Button) findViewById(R.id.buttonEco);
        mbuttonPlus = (Button) findViewById(R.id.buttonPlus);
        mbuttonMenos = (Button) findViewById(R.id.buttonMenos);

        configurarClicksBotones();

        IM5 = (ImageView) findViewById(R.id.imageViewM5);
        IM4 = (ImageView) findViewById(R.id.imageViewM4);
        IM3 = (ImageView) findViewById(R.id.imageViewM3);
        IM2 = (ImageView) findViewById(R.id.imageViewM2);
        IM1 = (ImageView) findViewById(R.id.imageViewM1);
        IP5 = (ImageView) findViewById(R.id.imageViewP5);
        IP4 = (ImageView) findViewById(R.id.imageViewP4);
        IP3 = (ImageView) findViewById(R.id.imageViewP3);
        IP2 = (ImageView) findViewById(R.id.imageViewP2);
        IP1 = (ImageView) findViewById(R.id.imageViewP1);

        mbuttonN.setBackgroundResource(R.drawable.button_block_normal);
        mbuttonC.setBackgroundResource(R.drawable.button_block_city);
        mbuttonS.setBackgroundResource(R.drawable.button_block_sport);
        mbuttonR.setBackgroundResource(R.drawable.button_block_race);
        mbuttonEco.setBackgroundResource(R.drawable.button_block_eco);
        mbuttonP.setBackgroundResource(R.drawable.button_parking);
        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
        mbuttonB.setBackgroundResource(R.drawable.button_block);

        String DT = getIntent().getStringExtra("variable_DemoTest");
        dt = DT;
        Log.d(TAG, "DTest en Control: " + dt);
        if (dt != null) {
            btnDemoTop.setEnabled(false);
            btnDemoTop.setAlpha(0.5f);
        }

        setNumStarsR(0);
        setNumStarsV(0);

        tv = (TextView) findViewById(R.id.data_value);
        readACCBOX();

        EPARK = 1;
        EBLOCK = 1;
        seteoStepperPON();
        Log.d(TAG, String.format("***** estado on create %d", estado));

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        nm = (TextView) findViewById(R.id.data_name);
        String NAMEBT = String.valueOf(mDeviceName);
        nm.setText(NAMEBT);
        /*
        if ("BLEFastD".equals(NAMEBT) || dt != null) {           // version sin Eco no Block
            NBT = 1;
            tipoBT = 0;
        } else if ("BLE-FastD".equals(NAMEBT) || dt != null) {    // version con Eco y Block
            NBT = 1;
            tipoBT = 1;
        } else {
            NBT = 0;
        }
         */
        if (dt != null) {
            NBT = 1;
            tipoBT = 1; // demo con todas las funciones
        } else if (NAMEBT != null && NAMEBT.contains("FastD")) {
            NBT = 1;

            if ("BLE-FastD".equals(NAMEBT)) {
                tipoBT = 1; // versión con Eco y Block
            } else {
                tipoBT = 0; // resto de versiones FastD
            }
        } else {
            NBT = 0;
        }
        Log.d(TAG, "mDeviceName recibido: " + NAMEBT);
        Log.d(TAG, "NBT: " + NBT + " | tipoBT: " + tipoBT);
        // Sets up UI references.
/*
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
*/

        mDataField = (TextView) findViewById(R.id.data_value);
        if (getActionBar() != null) {
            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);

            if (dt != null) {
                getActionBar().setLogo(R.drawable.logoandroid);
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        isServiceBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "***** bindService result = " + isServiceBound);

        if (dt != null) {
            mConnected = true;
            NBT = 1;
            tipoBT = 1;
            ESTADO = 0;
            estado = 0;
            STEP = 0;

            botonesEnable();
            coloresNeutros();
            mbuttonN.setBackgroundResource(R.drawable.button_normal);
            mbuttonMenos.setEnabled(true);
            mbuttonPlus.setEnabled(true);
            setNumStarsV(0);
            setNumStarsR(0);

            btnDemoTop.setEnabled(false);
            btnDemoTop.setAlpha(0.5f);

            Toast.makeText(DeviceControlActivity.this, "Modo demo", Toast.LENGTH_SHORT).show();
        }


        Log.d(TAG, String.format("***** estado on create salida %d", estado));
    }


    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean puedeUsarControles() {
        return mConnected || dt != null;
    }

    private boolean puedeEnviarBle() {
        return mConnected && mBluetoothLeService != null && dt == null;
    }

    private void abrirScanner() {
        Intent intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
        startActivity(intent);
        finish();
    }

    private void configurarClicksBotones() {
        mbuttonN.setOnClickListener(v -> onClickNormal(v));
        mbuttonC.setOnClickListener(v -> onClickCity(v));
        mbuttonS.setOnClickListener(v -> onClickSport(v));
        mbuttonR.setOnClickListener(v -> onClickRace(v));
        mbuttonEco.setOnClickListener(v -> onClickEco(v));

        mbuttonMenos.setOnClickListener(v -> onClickMenos(v));
        mbuttonPlus.setOnClickListener(v -> onClickPlus(v));

        mbuttonP.setOnClickListener(v -> onClickPark(v));
        mbuttonB.setOnClickListener(v -> onClickBlock(v));

        mbuttonE.setOnClickListener(v -> onClickEstado(v));
    }


    @Override
    protected void onResume() {
        Log.d(TAG, String.format("***** estado onResume INGRESO %d", estado));
        super.onResume();

        if (!isReceiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(), Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            }
            isReceiverRegistered = true;
        }

        if (dt != null) {
            botonesEnable();
            restaurarUIDesdeEstado();
            Log.d(TAG, String.format("***** estado onResume SALIDA DEMO %d", estado));
            return;
        }

        if (mBluetoothLeService != null
                && mDeviceAddress != null
                && !mDeviceAddress.isEmpty()
                && hasBluetoothConnectPermission()) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        Log.d(TAG, String.format("***** estado onResume SALIDA %d", estado));
    }


    @Override
    protected void onPause() {
        Log.d(TAG, String.format("***** estado onPause INGRESO %d", estado));
        super.onPause();

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(mGattUpdateReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver ya estaba desregistrado", e);
            }
            isReceiverRegistered = false;
        }

        Log.d(TAG, String.format("***** estado onPause SALIDA %d", estado));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, String.format("***** estado onDestroy INGRESO %d", estado));
        super.onDestroy();

        if (isServiceBound) {
            try {
                unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Service ya estaba desregistrado", e);
            }
            isServiceBound = false;
        }

        mBluetoothLeService = null;

        Log.d(TAG, String.format("***** estado onDestroy SALIDA %d", estado));
    }

    @Override
    //@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, String.format("***** estado onCreateOptionMenu INGRESO %d", estado));
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected || dt != null) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            botonesEnable();

        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            coloresNeutros(); //agregado simil ios
            botonesNoEnable();

        }
        Log.d(TAG, String.format("***** estado onCreateOptionMenu SALIDA %d", estado));
        return true;

    }

    /*
        @Override
        //@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public boolean onOptionsItemSelected(MenuItem item) {
            Log.d(TAG, String.format("***** estado onOptionsItemSelected INGRESO %d", estado));
            switch (item.getItemId()) {
                case R.id.menu_connect:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return TODO;
                    }
                    mBluetoothLeService.connect(mDeviceAddress);
                    return true;
                case R.id.menu_disconnect:
                    mBluetoothLeService.disconnect();
                    return true;
                case android.R.id.home:
                    onBackPressed();
                    return true;
            }
            Log.d(TAG, String.format("***** estado onOptionsItemSelected SALIDA %d", estado));
            return super.onOptionsItemSelected(item);

        }
    */
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConnectionState != null) {
                    mConnectionState.setText(resourceId);
                }

                if (mConnected || dt != null) {
                    botonesEnable();
                    restaurarUIDesdeEstado();
                } else {
                    coloresNeutros();
                    botonesNoEnable();
                }
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickNormal(View v)  {
        if (NBT == 0 && dt == null){
            coloresNeutros();
            return;
        }
        dataValue= 0x00;
        dataValueCRC= 0xe9;
        if (puedeUsarControles()) {
            estadoNormal();
            coloresNeutros();
            STEP = 0;
            //mbuttonN.setBackgroundColor(0xFF565656);
            mbuttonN.setBackgroundResource(R.drawable.button_normal);
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
            //mbuttonN.setBackgroundColor(0xFFFFFFFF);
            //mbuttonN.setBackgroundColor(0xBFA0F048);
        }else {
            msgConectar();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoNormal(){
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataValueAndCRC();
            ESTADO = 0;
            estado = 0;
            saveACCBOX ();
            Log.d(TAG, String.format("estado en normal %d", estado));

// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataValueAndCRC();
            mbuttonMenos.setEnabled(false);
            //mbuttonMenos.setText("");
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setEnabled(false);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);

            //mbuttonPlus.setText("");
            setNumStarsV(0);
            setNumStarsR(0);
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickEco(View v) {
        if (NBT == 0){
            coloresNeutros();
            return;
        }
        dataValue= 0x50;
        dataValueCRC= 0x39;
        if (puedeUsarControles() ) {
            ESTADO = 5;
            estado = 5;
            STEP = 0;
            saveACCBOX ();
            Log.d(TAG, String.format("estado en city %d", estado));
            estadoEco ();
            coloresNeutros();
            //mbuttonC.setBackgroundColor(0xCF26EBF9);
            mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);

            //0xBF26EBF9
            mbuttonMenos.setEnabled(true);
            mbuttonPlus.setEnabled(true);
            setNumStarsV(0);
            setNumStarsR(0);
            //mbuttonC.setBackgroundColor(0xBF12B315);
        }else {
            msgConectar();
        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoEco(){
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataValueAndCRC();


// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataValueAndCRC();

        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickCity(View v) {
        if (NBT == 0){
            coloresNeutros();
            return;
        }
        dataValue= 0x10;
        dataValueCRC= 0xf9;
        if (puedeUsarControles() ) {
            ESTADO = 1;
            estado = 1;
            STEP = 0;
            saveACCBOX ();
            Log.d(TAG, String.format("estado en city %d", estado));
            estadoCity ();
            coloresNeutros();
            //mbuttonC.setBackgroundColor(0xCF26EBF9);
            mbuttonC.setBackgroundResource(R.drawable.city_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);

            //0xBF26EBF9
            mbuttonMenos.setEnabled(true);
            mbuttonPlus.setEnabled(true);
            setNumStarsV(0);
            setNumStarsR(0);
            //mbuttonC.setBackgroundColor(0xBF12B315);
        }else {
            msgConectar();
        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoCity (){
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataValueAndCRC();


// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataValueAndCRC();

        }
    }


    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickSport(View v){
        Log.d(TAG, "CLICK RACE | mConnected=" + mConnected + " | NBT=" + NBT + " | enabled=" + mbuttonR.isEnabled());
        if (NBT == 0){
            coloresNeutros();
            return;
        }
        dataValue= 0x20;
        dataValueCRC= 0x09;
        if (puedeUsarControles()) {
            ESTADO = 2;
            estado = 2;
            STEP = 0;
            saveACCBOX ();
            Log.d(TAG, String.format("estado en sport %d", estado));
            estadoSport ();
            coloresNeutros();
            //mbuttonS.setBackgroundColor(0xFF12B315);
            mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
            //0xBF12B315
            mbuttonMenos.setEnabled(true);
            mbuttonPlus.setEnabled(true);
            setNumStarsV(0);
            setNumStarsR(0);
            //mbuttonS.setBackgroundColor(0xE6FF6060);
            //mbuttonS.setBackgroundColor(0xE4E67821);
        }else {
            msgConectar();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoSport (){
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataValueAndCRC();

// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataValueAndCRC();

        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickRace(View v){
        Log.d(TAG, "CLICK RACE | mConnected=" + mConnected + " | NBT=" + NBT + " | enabled=" + mbuttonR.isEnabled());
        if (NBT == 0){
            coloresNeutros();
            return;
        }
        dataValue= 0x30;
        dataValueCRC= 0x19;
        if (puedeUsarControles()) {
            ESTADO = 3;
            estado = 3;
            STEP = 0;
            saveACCBOX ();
            Log.d(TAG, String.format("estado en race %d", estado));
            estadoRace ();
            coloresNeutros();
            //mbuttonR.setBackgroundColor(0xE6FF2222);
            mbuttonR.setBackgroundResource(R.drawable.race_button_new);
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
            mbuttonMenos.setEnabled(true);
            mbuttonPlus.setEnabled(true);
            setNumStarsV(0);
            setNumStarsR(0);
        }else {
            msgConectar();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoRace () {
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataValueAndCRC();

// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataValueAndCRC();

        }
    }


    public void onClickPark(View v){
        if (NBT == 0){
            coloresNeutros();
            return;
        }

        if (puedeUsarControles()) {

            if ((estado >= 1000)){
                estado = estado - 1000;
                ESTADO = estado;
                saveACCBOX();
                Log.d(TAG, String.format("ESTADO entrando a park %d", ESTADO));
                seteoStepperPON();
                mbuttonE.callOnClick();
                mbuttonB.setEnabled(true);
                EPARK = 0;
                restaurarUIDesdeEstado();
                return;
            }

            AlertDialog.Builder dialog1 = new AlertDialog.Builder(this);
            dialog1.setTitle("ACCBox");
            dialog1.setMessage("Enter Parking?");
            dialog1.setCancelable(false);
            dialog1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
                public void onClick(DialogInterface dialog, int which) {
                    aceptar();
                }
            });
            dialog1.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelar();
                }
            });
            dialog1.show();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void aceptar(){

        Log.d(TAG, String.format("estado entrando primera vez a park %d", estado));
        estado = estado + 1000;
        ESTADO = estado;
        saveACCBOX();

        estadoPark();
        coloresNeutros();
        //mbuttonP.setBackgroundColor(Color.parseColor("#ffc000"));
        mbuttonP.setBackgroundResource(R.drawable.true_parking);
        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        mbuttonB.setEnabled(false);
        //mbuttonP.setBackgroundColor(Color.GRAY);
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoPark () {
        if(mBluetoothLeService != null) {
            headtx();
            envioDataPark();
// AGREGADO DOBLE
            delayy();
            headtx();
            envioDataPark();
            EPARK = 0;


            if (estado < 1000) {
                estado = estado + 1000;
                //ESTADO = estado;
            }
            ESTADO = estado;
            Log.d(TAG, String.format("estado antes guardar en park %d", estado));
            //ESTADO = 64;
            saveACCBOX();
            STEP = 0;
            mbuttonMenos.setEnabled(false);
            mbuttonMenos.setText("");
            mbuttonPlus.setEnabled(false);
            mbuttonPlus.setText("");
            setNumStarsR(0);
            setNumStarsV(0);
        }
    }

    public void cancelar (){

    }



    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void envioDataValueAndCRC () {
        if (!puedeEnviarBle()) {
            return;
        }
        mBluetoothLeService.writeCustomCharacteristic(dataValue);
        //mBluetoothLeService.writeCustomCharacteristic(0x10);
        delay ();
        mBluetoothLeService.writeCustomCharacteristic(dataValueCRC);
        //mBluetoothLeService.writeCustomCharacteristic(0xf9);
        delay ();
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void envioDataPark() {
        if (!puedeEnviarBle()) {
            return;
        }
        mBluetoothLeService.writeCustomCharacteristic(0x40);
        delay ();
        mBluetoothLeService.writeCustomCharacteristic(0x29);
        delay ();
    }

    // ????

    public void onClickBlock(View v){
        if (NBT == 0){
            coloresNeutros();
            return;
        }

        if ((puedeUsarControles() && tipoBT == 1) || dt != null){ // solo para la version con Block y Eco y demo

            if ((estado >= 2000)){
                estado = estado - 2000;
                ESTADO = estado;
                saveACCBOX ();
                Log.d(TAG, String.format("ESTADO entrando a block %d", ESTADO));
                seteoStepperPON();
                mbuttonE.callOnClick(); // - rompi algo
                EBLOCK = 0;
                restaurarUIDesdeEstado();
                return;
            }

            AlertDialog.Builder dialog1 = new AlertDialog.Builder(this);
            dialog1.setTitle("ACCBox");
            dialog1.setMessage("Enter Block?");
            dialog1.setCancelable(false);
            dialog1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
                public void onClick(DialogInterface dialog, int which) {
                    aceptarblock();
                }
            });
            dialog1.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelarblock();
                }
            });
            dialog1.show();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void aceptarblock(){

        Log.d(TAG, String.format("estado entrando primera vez a park %d", estado));
        estado = estado + 2000;
        ESTADO = estado;
        saveACCBOX ();

        estadoBlock ();
        coloresNeutros();
        //mbuttonP.setBackgroundColor(Color.parseColor("#ffc000"));
        mbuttonB.setBackgroundResource(R.drawable.true_blocking);
        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        mbuttonP.setEnabled(false);
        //mbuttonP.setBackgroundColor(Color.GRAY);
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void estadoBlock () {
        if(mBluetoothLeService != null) {
            headtx ();
            envioDataBlock();
// AGREGADO DOBLE
            delayy ();
            headtx ();
            envioDataBlock();
            EPARK = 0;


            if (estado < 2000) {
                estado = estado + 2000;
                //ESTADO = estado;
            }
            ESTADO = estado;
            Log.d(TAG, String.format("estado antes guardar en park %d", estado));
            //ESTADO = 64;
            saveACCBOX ();
            STEP = 0;
            mbuttonMenos.setEnabled(false);
            //mbuttonMenos.setText("");
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setEnabled(false);
            //mbuttonPlus.setText("");
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            setNumStarsR(0);
            setNumStarsV(0);
        }
    }

    public void cancelarblock (){
    }

    /*
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        private void envioDataValueAndCRC () {
            mBluetoothLeService.writeCustomCharacteristic(dataValue);
            //mBluetoothLeService.writeCustomCharacteristic(0x10);
            delay ();
            mBluetoothLeService.writeCustomCharacteristic(dataValueCRC);
            //mBluetoothLeService.writeCustomCharacteristic(0xf9);
            delay ();
        }
    */

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void envioDataBlock() {
        if (!puedeEnviarBle()) {
            return;
        }
        mBluetoothLeService.writeCustomCharacteristic(0x60);
        delay ();
        mBluetoothLeService.writeCustomCharacteristic(0x49);
        delay ();
    }



    // ????

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickEstado(View v){
        if (NBT == 0){
            coloresNeutros();
            return;
        }
        if (puedeUsarControles()) {
///*
            readACCBOX ();

//*/

            Log.d(TAG, String.format("estado en ciego %d", estado));
            coloresNeutros ();

            switch (estado){
                case 0:
                    dataValue= 0x00;
                    dataValueCRC= 0xe9;
                    estadoNormal();
                    //mbuttonN.setBackgroundColor(0xFF565656);
                    mbuttonN.setBackgroundResource(R.drawable.button_normal);
                    mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                    mbuttonPlus.setBackgroundResource(R.drawable.block_button_less);
                    //mbuttonN.setBackgroundColor(0xFFFFFFFF);
                    break;
                case 1: case 125: case 121: case 120: case 124:
                    envioConAjustes();
                    //mbuttonC.setBackgroundColor(0xCF26EBF9);
                    mbuttonC.setBackgroundResource(R.drawable.button_city);
                    mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                    mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                    //0xBF26EBF9
                    break;
                case 2: case 241: case 237: case 236: case 240:
                    envioConAjustes();

                    //mbuttonS.setBackgroundColor(0xFF12B315);
                    mbuttonS.setBackgroundResource(R.drawable.button_sport);
                    mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                    mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                    //0xBF12B315
                    break;
                case 3: case 357: case 353: case 352: case 356:
                    envioConAjustes();
                    //mbuttonR.setBackgroundColor(0xE6FF2222);
                    mbuttonR.setBackgroundResource(R.drawable.button_race);
                    mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                    mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                    break;
                //case 1000: case 1001: case 1002: case 1003: case 64:
                case 5: case 589: case 585: case 584: case 588:
                    envioConAjustes();
                    //mbuttonR.setBackgroundColor(0xE6FF2222);
                    mbuttonEco.setBackgroundResource(R.drawable.button_eco);
                    mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                    mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                    break;

                default:
                    if (estado > 600 && estado < 2000){
                        //mbuttonP.setBackgroundColor(Color.parseColor("#ffc000"));
                        mbuttonP.setBackgroundResource(R.drawable.parkingp);
                        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);// block subir bajar
                        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                    }else if (estado >= 2000) {
                        mbuttonB.setBackgroundResource(R.drawable.candadorojo);
                        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                    }else  {
                        mbuttonE.callOnClick();
                    }
                    /*//estadoPark();
                    //mbuttonP.setBackgroundColor(Color.parseColor("#ffc000"));
                        mbuttonP.setBackgroundResource(R.drawable.parkingp);
                        //mbuttonB.setBackgroundResource(R.drawable.candadorojo);*/
                    break;


            }
        }else {
            //mBluetoothLeService.connect(mDeviceAddress); //para reconectar con click imagen
            Log.d(TAG, String.format(" ##### estado en ClickEstado %d", estado));
            msgConectar();
        }

        seteoStepperPON ();
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickType(View v) {
        if (estado == 1) {
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);
            mbuttonC.setBackgroundResource(R.drawable.city_button_new);
        }

        if (estado == 2) {
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
            mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
        }

        if (estado == 3) {
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
            mbuttonR.setBackgroundResource(R.drawable.race_button_new);
        }

        if (estado == 5){
            mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
            mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
            mbuttonE.setBackgroundResource(R.drawable.eco_button_new);
        }

    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickMenos(View v){
        Log.d(TAG, String.format("******  estado on ClickMenos ******** %d", estado));
        if ((estado == 0) || (estado == 64) || (estado >= 1000)) { // sin ajuste fino Normal y Park
            STEP = 0;
            mbuttonMenos.setEnabled(true);
            //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
            //mbuttonMenos.setText("");
            mbuttonPlus.setEnabled(true);
            //mbuttonPlus.setText("");
            //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
            onClickType(v);

            setNumStarsV(0);
            setNumStarsR(0);
            return;
        }

        if (STEP <= -2){
            STEP = -2;
        }else{
            STEP = STEP -1;
        }

        switch (STEP){
            case -2:
                mbuttonMenos.setEnabled(false);
                //mbuttonMenos.setText("");// less no va
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                //mbuttonPlus.setText("+");
                onClickType(v);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);

                setNumStarsV(4);
                setNumStarsR(0);
                break;
            case -1:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                //mbuttonMenos.setText("-");
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                setNumStarsV(2);
                setNumStarsR(0);
                break;
            case 0:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);

                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                setNumStarsV(0);
                setNumStarsR(0);
                break;
            case 1:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                setNumStarsR(2);
                setNumStarsV(0);
                break;
            case 2:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                mbuttonPlus.setEnabled(false);
                //mbuttonPlus.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                //mbuttonPlus.setText("");
                onClickType(v);
                setNumStarsR(4);
                setNumStarsV(0);
                break;
            default:
                break;
        }
        //Log.d(TAG, String.format("STEP en MENOS antes de envio con Ajustes %d", STEP));
        //Log.d(TAG, String.format("estado antes envio con Ajustes %d", estado));
        envioConAjustes();
        //Log.d(TAG, String.format("estado despues envio con Ajustes %d", estado));
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickPlus(View v){
        Log.d(TAG, String.format("******  estado on ClickPlus ******** %d", estado));
        if ((estado == 0) || (estado == 64) || (estado >= 1000)) { // sin ajuste fino Normal y Park
            STEP = 0;
            mbuttonMenos.setEnabled(true);
            //mbuttonMenos.setText("");
            //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
            mbuttonPlus.setEnabled(true);
            //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
            onClickType(v);

            //mbuttonPlus.setText("");
            setNumStarsV(0);
            setNumStarsR(0);
            return;
        }

        if (STEP >= 2){
            STEP = 2;
        }else{
            STEP = STEP +1;
        }

        switch (STEP){
            case -2:
                mbuttonMenos.setEnabled(false);
                //mbuttonMenos.setText("");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);

                setNumStarsV(4);
                setNumStarsR(0);
                break;
            case -1:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                setNumStarsV(2);
                setNumStarsR(0);
                break;
            case 0:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                //mbuttonPlus.setText("+");
                onClickType(v);

                setNumStarsV(0);
                setNumStarsR(0);
                break;
            case 1:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                //mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                onClickType(v);
                setNumStarsR(2);
                setNumStarsV(0);
                break;
            case 2:
                mbuttonMenos.setEnabled(true);
                //mbuttonMenos.setText("-");
                //mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(false);
                //mbuttonPlus.setText("");
                onClickType(v);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);

                setNumStarsR(4);
                setNumStarsV(0);
                break;
            default:
                break;
        }
        //Log.d(TAG, String.format("STEP en MAS antes de envio con Ajustes %d", STEP));
        //Log.d(TAG, String.format("estado antes envio con Ajustes %d", estado));
        envioConAjustes();
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void envioConAjustes() {

        if (estado == 1 || estado == 125 || estado == 121 || estado == 120 || estado == 124) {
            envioCityFino();

            ESTADO = estado;
            saveACCBOX ();
            Log.d(TAG, String.format("####### estado envioconAjustes %d", estado));
            return;
        }
        if (estado == 2 || estado == 241 || estado == 237 || estado == 236 || estado == 240){
            envioSportFino();

            ESTADO = estado;
            saveACCBOX ();
            Log.d(TAG, String.format("####### estado envioconAjustes %d", estado));
            return;
        }

        if (estado == 5 || estado == 589 || estado == 585 || estado == 584 || estado == 588){
            envioEcoFino();

            ESTADO = estado;
            saveACCBOX ();
            Log.d(TAG, String.format("####### estado envioconAjustes %d", estado));
            return;
        }

        if (estado == 3 || estado == 357 || estado == 353 || estado == 352 || estado == 356) {
            envioRaceFino();

            ESTADO = estado;
            saveACCBOX ();
            Log.d(TAG, String.format("####### estado envioconAjustes %d", estado));
            return;
        }


    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void envioCityFino() {
        switch (STEP) {
            case -2:
                Log.d(TAG, String.format("STEP City -2"));

                headtx();

                dataValue = 0x19;
                dataValueCRC = 0x02;
                estado = 125;
                ESTADO = 125;

                estadoCity();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonC.setBackgroundResource(R.drawable.city_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);
                //0xBF26EBF9


                break;
            case -1:
                Log.d(TAG, String.format("STEP City -1"));

                headtx();
                dataValue = 0x15;
                dataValueCRC = 0xfe;
                estado = 121;
                ESTADO = 121;

                estadoCity();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonC.setBackgroundResource(R.drawable.city_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);
                //0xBF26EBF9


                break;
            case 0:
                Log.d(TAG, String.format("STEP City 0"));

                headtx();
                dataValue= 0x10;
                dataValueCRC= 0xf9;
                estado = 1;
                ESTADO = 1;

                estadoCity();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonC.setBackgroundResource(R.drawable.city_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);
                //0xBF26EBF9


                break;
            case 1:
                Log.d(TAG, String.format("STEP City 1"));

                headtx();

                dataValue = 0x14;
                dataValueCRC = 0xfd;
                estado = 120;
                ESTADO = 120;

                estadoCity();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonC.setBackgroundResource(R.drawable.city_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_city);
                //0xBF26EBF9


                break;
            case 2:
                Log.d(TAG, String.format("STEP City 2"));

                headtx();

                dataValue = 0x18;
                dataValueCRC = 0x01;
                estado = 124;
                ESTADO = 124;

                estadoCity();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonC.setBackgroundResource(R.drawable.city_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_city);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                //0xBF26EBF9


                break;
            default:
                break;

        }
    }


    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void envioEcoFino() {
        switch (STEP) {
            case -2:
                Log.d(TAG, String.format("STEP City -2"));

                headtx();

                dataValue = 0x59;
                dataValueCRC = 0x42;
                estado = 589;
                ESTADO = 589;

                estadoEco();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
                //0xBF26EBF9


                break;
            case -1:
                Log.d(TAG, String.format("STEP City -1"));

                headtx();
                dataValue = 0x55;
                dataValueCRC = 0x3e;
                estado = 585;
                ESTADO = 585;

                estadoEco();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
                //0xBF26EBF9


                break;
            case 0:
                Log.d(TAG, String.format("STEP City 0"));

                headtx();
                dataValue= 0x50;
                dataValueCRC= 0x39;
                estado = 5;
                ESTADO = 5;

                estadoEco();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
                //0xBF26EBF9


                break;
            case 1:
                Log.d(TAG, String.format("STEP City 1"));

                headtx();

                dataValue = 0x54;
                dataValueCRC = 0x3d;
                estado = 584;
                ESTADO = 584;

                estadoEco();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_eco);
                //0xBF26EBF9


                break;
            case 2:
                Log.d(TAG, String.format("STEP City 2"));

                headtx();

                dataValue = 0x58;
                dataValueCRC = 0x41;
                estado = 588;
                ESTADO = 588;

                estadoEco();
                coloresNeutros();
                //mbuttonC.setBackgroundColor(0xCF26EBF9);
                mbuttonEco.setBackgroundResource(R.drawable.eco_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_eco);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                //0xBF26EBF9


                break;
            default:
                break;

        }
    }




    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void envioSportFino() {

        switch (STEP){
            case -2:
                headtx();

                dataValue = 0x29;
                dataValueCRC = 0x12;
                estado = 241;

                estadoSport ();
                coloresNeutros();
                //mbuttonS.setBackgroundColor(0xFF12B315);
                mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
                //0xBF12B315
                break;
            case -1:
                headtx();

                dataValue = 0x25;
                dataValueCRC = 0x0e;
                estado = 237;
                ESTADO = 237;

                estadoSport ();
                coloresNeutros();
                //mbuttonS.setBackgroundColor(0xFF12B315);
                mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
                //0xBF12B315
                break;
            case 0:
                headtx();

                dataValue= 0x20;
                dataValueCRC= 0x09;
                estado = 2;
                ESTADO = 2;

                estadoSport ();
                coloresNeutros();
                //mbuttonS.setBackgroundColor(0xFF12B315);
                mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
                //0xBF12B315
                break;

            case 1:
                headtx();

                dataValue = 0x24;
                dataValueCRC = 0x0d;
                estado = 236;
                ESTADO = 236;

                estadoSport ();
                coloresNeutros();
                //mbuttonS.setBackgroundColor(0xFF12B315);
                mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_sport);
                //0xBF12B315
                break;
            case 2:
                headtx();

                dataValue = 0x28;
                dataValueCRC = 0x11;
                estado = 240;
                ESTADO = 240;

                estadoSport ();
                coloresNeutros();
                //mbuttonS.setBackgroundColor(0xFF12B315);
                mbuttonS.setBackgroundResource(R.drawable.sport_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_sport);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                //0xBF12B315
                break;
            default:
                break;
        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void envioRaceFino(){
        switch (STEP){
            case -2:
                headtx();

                dataValue = 0x39;
                dataValueCRC = 0x22;
                estado = 357;
                ESTADO = 357;

                estadoRace ();
                coloresNeutros();
                //mbuttonR.setBackgroundColor(0xE6FF2222);
                mbuttonR.setBackgroundResource(R.drawable.race_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
                break;

            case -1:
                headtx();

                dataValue = 0x35;
                dataValueCRC = 0x1e;
                estado = 353;
                ESTADO = 353;

                estadoRace ();
                coloresNeutros();
                //mbuttonR.setBackgroundColor(0xE6FF2222);
                mbuttonR.setBackgroundResource(R.drawable.race_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
                break;
            case 0:
                headtx();

                dataValue= 0x30;
                dataValueCRC= 0x19;
                estado = 3;
                ESTADO = 3;

                estadoRace ();
                coloresNeutros();
                //mbuttonR.setBackgroundColor(0xE6FF2222);
                mbuttonR.setBackgroundResource(R.drawable.race_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
                break;
            case 1:
                headtx();

                dataValue = 0x34;
                dataValueCRC = 0x1d;
                estado= 352;
                ESTADO = 352;

                estadoRace ();
                coloresNeutros();
                //mbuttonR.setBackgroundColor(0xE6FF2222);
                mbuttonR.setBackgroundResource(R.drawable.race_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more_race);
                break;
            case 2:
                headtx();

                dataValue = 0x38;
                dataValueCRC = 0x21;
                estado = 356;
                ESTADO = 356;

                estadoRace ();
                coloresNeutros();
                //mbuttonR.setBackgroundColor(0xE6FF2222);
                mbuttonR.setBackgroundResource(R.drawable.race_button_new);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less_race);
                mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
                break;
            default:
                break;

        }
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void headtx () {
        if (!puedeEnviarBle()) {
            return;
        }
        mBluetoothLeService.writeCustomCharacteristic(0xc0);
        delay ();
        mBluetoothLeService.writeCustomCharacteristic(0x29);
        delay ();
    }

    public void delay () {
        try {
            Thread.sleep(10 );//10
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    public void delayy () {
        try {
            Thread.sleep(100 ); //100
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
    public void delayyy () {
        try {
            Thread.sleep(1000 );
            //Thread.sleep(1000 );
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public void onClickRead(View v){
        if (puedeEnviarBle()) {
            mBluetoothLeService.readCustomCharacteristic();
        }
    }

    public String PREFS_KEY = "dataaccbox";

    public void guardaACCBOX(Context context,String valor) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor;
        editor = settings.edit();
        editor.putString("myACCBOX",valor);
        editor.commit();
    }

    public String obtieneACCBOX (Context context){
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY,MODE_PRIVATE);
        return preferences.getString("myACCBOX","0");
    }

    public void saveACCBOX (){
        if (dt != null) {
            return;
        }

        String verESTADO = String.valueOf(ESTADO);
        if (tv != null) {
            tv.setText(verESTADO);
        }
        guardaACCBOX(getApplicationContext(), verESTADO);
    }

    public void readACCBOX (){
        if (dt != null) {
            estado = 0;
            ESTADO = 0;
            return;
        }

        String savedValue = obtieneACCBOX(getApplicationContext());

        if (savedValue == null || savedValue.trim().isEmpty()) {
            savedValue = "0";
        }

        try {
            estado = Integer.parseInt(savedValue);
            ESTADO = estado;
            if (tv != null) {
                tv.setText(String.valueOf(estado));
            }
        } catch (NumberFormatException e) {
            estado = 0;
            ESTADO = 0;
            if (tv != null) {
                tv.setText("0");
            }
            guardaACCBOX(getApplicationContext(), "0");
            Log.w(TAG, "Valor guardado inválido. Se resetea a 0.", e);
        }
    }

    private void coloresNeutros (){
        //mbuttonN.setBackgroundColor(0xFFC9C9C8);
        //mbuttonC.setBackgroundColor(0xFFC9C9C8);
        //mbuttonS.setBackgroundColor(0xFFC9C9C8);
        //mbuttonR.setBackgroundColor(0xFFC9C9C8);
        //mbuttonP.setBackgroundColor(0xFFC9C9C8);

        //mbuttonN.setBackgroundColor(0xFF9C9C9C);
        mbuttonN.setBackgroundResource(R.drawable.button_block_normal);
        //mbuttonEco.setBackgroundColor(0xFF9C9C9C);
        mbuttonEco.setBackgroundResource(R.drawable.button_block_eco);
        //mbuttonC.setBackgroundColor(0xFF9C9C9C);
        mbuttonC.setBackgroundResource(R.drawable.button_block_city);
        //mbuttonS.setBackgroundColor(0xFF9C9C9C);
        mbuttonS.setBackgroundResource(R.drawable.button_block_sport);
        //mbuttonR.setBackgroundColor(0xFF9C9C9C);
        mbuttonR.setBackgroundResource(R.drawable.button_block_race);
        //mbuttonP.setBackgroundColor(0xFF9C9C9C);
        mbuttonP.setBackgroundResource(R.drawable.button_parking);
        selectCandados();
        mbuttonP.setEnabled(true);
        mbuttonB.setEnabled(true);
        //mbuttonPlus.setBackgroundColor(0xFF9C9C9C);
        mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
        //mbuttonMenos.setBackgroundColor(0xFF9C9C9C);
        mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        mbuttonB.setBackgroundResource(R.drawable.button_block);// block rojo puede ser bloque tachado

        //mbuttonN.setBackgroundColor(0xFFC9C9C8);
    }

    private void selectCandados(){
        if (!mConnected) {
            mbuttonB.setBackgroundResource(R.drawable.button_block);
            mbuttonMenos.setBackgroundResource(R.drawable.block_button_less);
            mbuttonPlus.setBackgroundResource(R.drawable.block_button_more);
        }else{
            if (estado >= 2000){
                mbuttonB.setBackgroundResource(R.drawable.true_blocking);// block rojo puede ser bloque tachado

            }else {
                mbuttonB.setBackgroundResource(R.drawable.button_block);// block verde?
            }
        }

    }

    private void botonesEnable(){
        mbuttonN.setEnabled(true);
        mbuttonC.setEnabled(true);
        mbuttonS.setEnabled(true);
        mbuttonR.setEnabled(true);
        mbuttonP.setEnabled(true);

        mbuttonMenos.setEnabled(true);
        mbuttonPlus.setEnabled(true);

        if (dt != null) {
            mbuttonB.setEnabled(true);
            mbuttonEco.setEnabled(true);
            return;
        }

        if (tipoBT == 1) {
            mbuttonB.setEnabled(true);
            mbuttonEco.setEnabled(true);
        } else {
            mbuttonB.setEnabled(false);
            mbuttonEco.setEnabled(false);
            mbuttonEco.setText(" ");
        }
    }

    private void botonesNoEnable(){
        mbuttonN.setEnabled(false);
        mbuttonEco.setEnabled(false);
        mbuttonC.setEnabled(false);
        mbuttonS.setEnabled(false);
        mbuttonR.setEnabled(false);
        mbuttonP.setEnabled(false);
        mbuttonB.setEnabled(false);
        mbuttonMenos.setEnabled(false);
        mbuttonPlus.setEnabled(false);

        setNumStarsR(0);
        setNumStarsV(0);
    }

    // MARK: - seteo stepper power on

    private void seteoStepperPON () {

        Log.d(TAG, String.format("estado en setoestepperpon %d", estado));
        switch (estado) {
            case 125: case 241: case 357: case 589:
                STEP = -2;
                mbuttonMenos.setEnabled(false);
                mbuttonMenos.setText("");
                mbuttonPlus.setEnabled(true);
                //  mbuttonPlus.setText("+");
                setNumStarsV(4);
                setNumStarsR(0);
                break;

            case 121: case 237: case 353: case 585:
                STEP = -1;
                mbuttonMenos.setEnabled(true);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                //mbuttonMenos.setText("-");
                mbuttonPlus.setEnabled(true);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                //mbuttonPlus.setText("+");
                setNumStarsV(2);
                setNumStarsR(0);
                break;
            case 0: case 1: case 2: case 3: case 4: case 5:
                STEP = 0;
                mbuttonMenos.setEnabled(true);

                //mbuttonMenos.setText("-");
                mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                mbuttonPlus.setEnabled(true);
                //mbuttonPlus.setText("+");
                mbuttonPlus.setBackgroundResource(R.drawable.button_more);

                setNumStarsV(0);
                setNumStarsR(0);
                break;
            case 120: case 236: case 352: case 584:
                STEP = 1;
                mbuttonMenos.setEnabled(true);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                //mbuttonMenos.setText("-");
                mbuttonPlus.setEnabled(true);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more);

                //mbuttonPlus.setText("+");
                setNumStarsR(2);
                setNumStarsV(0);
                break;
            case 124: case 240: case 356: case 588:
                STEP = 2;
                mbuttonMenos.setEnabled(true);
                mbuttonMenos.setBackgroundResource(R.drawable.button_less);
                //mbuttonMenos.setText("-");
                mbuttonPlus.setEnabled(false);
                mbuttonPlus.setBackgroundResource(R.drawable.button_more);
                //mbuttonPlus.setText("");
                setNumStarsR(4);
                setNumStarsV(0);
                break;
            default:
                break;
        }


    }
    private void msgConectar() {
        Toast.makeText(this, "ACCBox no conectado. Usá BLE o modo demo.", Toast.LENGTH_SHORT).show();
    }

    private void setNumStarsR (int numStars) {
        if (puedeUsarControles()){

            switch (numStars) {
                case 4:
                    IP5.setVisibility(View.VISIBLE);
                    IP4.setVisibility(View.VISIBLE);
                    IP3.setVisibility(View.VISIBLE);
                    IP2.setVisibility(View.VISIBLE);
                    IP1.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    IP5.setVisibility(View.INVISIBLE);
                    IP4.setVisibility(View.INVISIBLE);
                    IP3.setVisibility(View.VISIBLE);
                    IP2.setVisibility(View.VISIBLE);
                    IP1.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    IP5.setVisibility(View.INVISIBLE);
                    IP4.setVisibility(View.INVISIBLE);
                    IP3.setVisibility(View.INVISIBLE);
                    IP2.setVisibility(View.INVISIBLE);
                    IP1.setVisibility(View.INVISIBLE);
                    break;
                default:
                    break;
            }
        }else {
            IP5.setVisibility(View.INVISIBLE);
            IP4.setVisibility(View.INVISIBLE);
            IP3.setVisibility(View.INVISIBLE);
            IP2.setVisibility(View.INVISIBLE);
            IP1.setVisibility(View.INVISIBLE);
            mbuttonPlus.setEnabled(false);
            mbuttonPlus.setText("");
        }

    }
    private void setNumStarsV (int numStars) {
        if (puedeUsarControles()){

            switch (numStars) {
                case 4:
                    IM5.setVisibility(View.VISIBLE);
                    IM4.setVisibility(View.VISIBLE);
                    IM3.setVisibility(View.VISIBLE);
                    IM2.setVisibility(View.VISIBLE);
                    IM1.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    IM5.setVisibility(View.INVISIBLE);
                    IM4.setVisibility(View.INVISIBLE);
                    IM3.setVisibility(View.VISIBLE);
                    IM2.setVisibility(View.VISIBLE);
                    IM1.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    IM5.setVisibility(View.INVISIBLE);
                    IM4.setVisibility(View.INVISIBLE);
                    IM3.setVisibility(View.INVISIBLE);
                    IM2.setVisibility(View.INVISIBLE);
                    IM1.setVisibility(View.INVISIBLE);
                    break;
                default:
                    break;
            }
        }else {
            IM5.setVisibility(View.INVISIBLE);
            IM4.setVisibility(View.INVISIBLE);
            IM3.setVisibility(View.INVISIBLE);
            IM2.setVisibility(View.INVISIBLE);
            IM1.setVisibility(View.INVISIBLE);
            mbuttonMenos.setEnabled(false);
            mbuttonMenos.setText("");
        }

    }
}