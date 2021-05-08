package com.aardvark_visual.ped.bluetooth;

/*
 * Manages the detection, selection and reading from Bluetooth LE sensors.
 * Singleton 
 */

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.aardvark_visual.ped.aardvarkped.*;
import com.aardvark_visual.ped.aardvarkpedanalysis.AardvarkPedAnalysis;
import com.aardvark_visual.ped.aardvarkpedwidget.AardvarkPedWidgetService;

class BtGattDevice {
    public BtGattDevice(BluetoothDevice dev, String nam) {
            device = dev;
            name   = nam;
            gatt   = null;

        };

    @Override
    public boolean equals(Object obj) {
             BtGattDevice that = (BtGattDevice)obj;

             if (obj == null) return false;
             if (this.device == null || that.device == null) {
                 return false;
             }
             if (this.device.getAddress().equals(that.device.getAddress())) {
                 return true;
             }
             return false;
        };

    public BluetoothDevice      device;
    public BluetoothGatt        gatt;
    public BluetoothGattService service;
    public String               name;   // The device.getName() is prone to being null. btAdapter.getRemoteDevice() will make a
                                        // more-or-less blank device object which can be filled out, but until it is connected  
                                        // the name is null; This can cause problems with the UI.  Solution is to retain the name
                                        // and persist the name of the selected device, as well as its addr.
}

public class AardvarkPedBluetooth {

    /**
     * UUIDs of GATT services as per the GATT specification:
     * http://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
     */

    // BLE Services  
	public static final UUID BLE_SERVICE_GENERIC_ACCESS             = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_GENERIC_ATTRIBUTE          = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_IMMEDIATE_ALERT            = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_LINK_LOSS                  = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_TX_POWER                   = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_CURRENT_TIME               = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_REFERENCE_TIME_UPDATE      = UUID.fromString("00001806-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_NEXT_DST_CHANGE            = UUID.fromString("00001807-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_GLUCOSE                    = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_HEALTH_THERMOMETER         = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_DEVICE_INFORMATION         = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_HEART_RATE                 = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_PHONE_ALERT_STATUS         = UUID.fromString("0000180e-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_BATTERY_SERVICE            = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_BLOOD_PRESSURE             = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_ALERT_NOTIFICATION         = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_HUMAN_INTERFACE_DEVICE     = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_SCAN_PARAMETERS            = UUID.fromString("00001813-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_RUNNING_SPEED_AND_CADENCE  = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_CYCLING_SPEED_AND_CADENCE  = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_LOCATION_AND_NAVIGATION    = UUID.fromString("00001819-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_SERVICE_CONTINUOUS_GLUCOSE_MONITOR = UUID.fromString("0000181f-0000-1000-8000-00805f9b34fb");


    // BLE Characteristics
	public static final UUID BLE_CHARACTERISTIC_ALERT_CATEGORY_ID                = UUID.fromString("00002a43-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_ALERT_CATEGORY_ID_BIT_MASK       = UUID.fromString("00002a42-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_ALERT_LEVEL                      = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_ALERT_NOTIFICATION_CONTROL_POINT = UUID.fromString("00002a44-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_ALERT_STATUS                     = UUID.fromString("00002a3f-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_APPEARANCE                       = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_BLOOD_PRESSURE_FEATURE           = UUID.fromString("00002a49-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT       = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_BODY_SENSOR_LOCATION             = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_CURRENT_TIME                     = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_DATE_TIME                        = UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_DAY_DATE_TIME                    = UUID.fromString("00002a0a-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_DAY_OF_WEEK                      = UUID.fromString("00002a09-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_DEVICE_NAME                      = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_DST_OFFSET                       = UUID.fromString("00002a0d-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_EXACT_TIME_256                   = UUID.fromString("00002a0c-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_FIRMWARE_REVISION_STRING         = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_HARDWARE_REVISION_STRING         = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_HEART_RATE_BODY_SENSOR_LOCATION  = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_HEART_RATE_CONTROL_POINT         = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_HEART_RATE_MEASUREMENT           = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_IEEE_11073_20601_REGULATORY      = UUID.fromString("00002a2a-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_INTERMEDIATE_CUFF_PRESSURE       = UUID.fromString("00002a36-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_INTERMEDIATE_TEMPERATURE         = UUID.fromString("00002a1e-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_LOCAL_TIME_INFORMATION           = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_MANUFACTURER_NAME_STRING         = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_MEASUREMENT_INTERVAL             = UUID.fromString("00002a21-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_MODEL_NUMBER_STRING              = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_NEW_ALERT                        = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_PERIPHERAL_PRIVACY_FLAG          = UUID.fromString("00002a02-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_RECONNECTION_ADDRESS             = UUID.fromString("00002a03-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_REFERENCE_TIME_INFORMATION       = UUID.fromString("00002a14-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_RINGER_CONTROL_POINT             = UUID.fromString("00002a40-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_RINGER_SETTING                   = UUID.fromString("00002a41-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SERIAL_NUMBER_STRING             = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SERVICE_CHANGED                  = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SOFTWARE_REVISION_STRING         = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SUPPORTED_NEW_ALERT_CATEGORY     = UUID.fromString("00002a47-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SUPPORTED_UNREAD_ALERT_CATEGORY  = UUID.fromString("00002a48-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_SYSTEM_ID                        = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TEMPERATURE_MEASUREMENT          = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TEMPERATURE_TYPE                 = UUID.fromString("00002a1d-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_ACCURACY                    = UUID.fromString("00002a12-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_SOURCE                      = UUID.fromString("00002a13-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_UPDATE_CONTROL_POINT        = UUID.fromString("00002a16-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_UPDATE_STATE                = UUID.fromString("00002a17-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_WITH_DST                    = UUID.fromString("00002a11-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TIME_ZONE                        = UUID.fromString("00002a0e-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_TX_POWER_LEVEL                   = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
	public static final UUID BLE_CHARACTERISTIC_UNREAD_ALERT_STATUS              = UUID.fromString("00002a45-0000-1000-8000-00805f9b34fb");

    public static final UUID BLE_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIG         = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final byte CLIENT_CHAR_CONFIG_FLAG_NOTIFICATIONS_ENABLED = 1;
    public static final int  MAX_RECONNECT_RETRIES = 4;


    // The new device callback lets the app know there is a new device avaialble.
    // its up to the app to decide what to do about it (probably nothing most of the time).

    public interface NewDeviceCallback {
        public void onNewDevice(int deviceType, int deviceListIdx, String deviceName);
    }

    public interface DeviceDiscoveryCallback {
        public void onDeviceDiscoveryComplete(Object data);
    }

    public interface DeviceReconnectCallback {
        public void onDeviceReconnectComplete(boolean success, BluetoothDevice device, Object data);
    }

    public interface DeviceDisconnectCallback {
        public void onDeviceDisconnectComplete(BluetoothDevice device, Object data);
    }


    public static final int DEVICE_TYPE_HEARTRATE   = 0x180d;
    public static final int DEVICE_TYPE_STEPCOUNTER = 0x1814;
    public static final int DEVICE_TYPE_GPSTRACKER  = 0x1819;


    public  static final int COUNTER_OFFLINE = -1; // If the counter is no longer available, we call the counter update callback with -1. When it comes back,
    // we call the counter callback with greater or equal to 0.

    public  static AardvarkPedBluetooth mSingleInstance = null;
    public  static DiscoveryTask mDiscoveryTask = null;
    public  static ReconnectTask mReconnectTask = null;
    private static Context mContext = null;
    private static Handler mHandler = null;

    private static SharedPreferences mSharedPreferences = null;
    private static List<BtGattDevice> mHeartRateDevicesAvailable = null;
    private static BtGattDevice       mHeartRateDevice = null;
    private static BluetoothGattCharacteristic mHeartRateReadingCharacteristic = null;
    private static CounterUpdateCallback mHeartRateCallback = null;
    private static int mHeartRateReading = COUNTER_OFFLINE;

    private static List<BluetoothDevice> mStepCounterDevicesAvailable = null;
    private static BluetoothDevice mStepCounterDevice = null;
    private static com.aardvark_visual.ped.aardvarkped.CounterUpdateCallback mStepCounterCallback = null;
    private static int mStepCounterReading = COUNTER_OFFLINE;

    private static List<BluetoothDevice> mGPSTrackerDevicesAvailable = null;
    private static BluetoothDevice mGPSTrackerDevice = null;
    private static com.aardvark_visual.ped.aardvarkped.CounterUpdateCallback mGPSTrackerCallback = null;

    private static NewDeviceCallback mNewDeviceCallback = null;
    private static DeviceDiscoveryCallback  mDeviceDiscoveryCallback  = null;
    private static DeviceReconnectCallback  mDeviceReconnectCallback  = null;
    private static DeviceDisconnectCallback mDeviceDisconnectCallback = null;
    private static Object                   mDeviceDiscoveryCallbackData  = null;
    private static Object                   mDeviceReconnectCallbackData  = null;
    private static Object                   mDeviceDisconnectCallbackData = null;
    private static BluetoothManager mBtMgr = null;
    private static BluetoothAdapter mBtAdapter = null;
    private static BluetoothGatt       mBtGatt = null;
    private static CountDownLatch mDiscoveryLatch = null;
    private static CountDownLatch mReconnectLatch = null;
    private static boolean mDiscoveryPending = false;
    private static boolean mReconnectPending = false;
    private static int     mReconnectAttempts = 0;

    // We need some thinking here.... different callback is needed since the gps tracking will be in the form of either double {lat, long}, 
    // double dx, dy, dz, or whatever. Definitely not an int.

    public static class ReconnectGattCallback extends BluetoothGattCallback {
        public static int mHeartRate = -1;

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor desc, int status) {
            System.err.println("reconnect: descriptor written");

            BluetoothGattCharacteristic heart_rate_reading_characteristic = null;
            for (BluetoothGattCharacteristic characteristic : mHeartRateDevice.service.getCharacteristics()) {
                if (characteristic.getUuid().equals(BLE_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
                    heart_rate_reading_characteristic = characteristic;
                    break;
                }
            }
            boolean notification_enabled = mHeartRateDevice.gatt.setCharacteristicNotification(heart_rate_reading_characteristic, true );
            mReconnectLatch.countDown();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                // Two circumstances go through here. In one case, its a reconnect.
                // We know that because there is a reconnect task defined.  Otherwise, 
                // its a discovery.  

                System.err.println("reconnect: services discovered");
                List<BluetoothGattService> service_list = gatt.getServices();

                if (mHeartRateDevice != null ) {
                    if (gatt == mHeartRateDevice.gatt) {
                        if (mHeartRateDevice.service == null) {
 
                            // We have a designated heart rate device, but it is disconnected. Fix that, if possible.

                            BluetoothGattCharacteristic heart_rate_reading_characteristic = null;

                            mHeartRateDevice.service = gatt.getService(BLE_SERVICE_HEART_RATE);

                            // Nice. We got the service. But if we don't set the characteristic desc we will never be notified

                            for (BluetoothGattCharacteristic characteristic : mHeartRateDevice.service.getCharacteristics()) {
                                if (characteristic.getUuid().equals(BLE_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
                                    heart_rate_reading_characteristic = characteristic;
                                    break;
                                }
                            }

                            // Now we setup the characteristic change notification. First, enable notifications on this descriptor

                            BluetoothGattDescriptor desc = heart_rate_reading_characteristic.getDescriptor(BLE_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIG);
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mHeartRateDevice.gatt.writeDescriptor(desc);


                            return;

                        }
                    }
                }
                mReconnectLatch.countDown();
            }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
               int old_heart_rate = mHeartRate;

                // probably the only thing we are looking at right now

                if (characteristic.getUuid().equals(BLE_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
                    System.err.println("descriptors = "+characteristic.getDescriptors().size());
                    byte[] value = characteristic.getValue();
                    if ((value[0] & 0x1) != 0) {
                        System.err.println("two byte heart rate");
                        mHeartRate = (value[1]>>8) + value[2];
                    }
                    else {
                        System.err.println("one byte heart rate = "+Byte.toString(value[1]));
                        mHeartRate = value[1] & 0xff; // Gives us unsigned value. (0-255), even though there are no unsigned types in java. 
                    }

                    if ((value[0] & 0x8) != 0) {
                        System.err.println("energy expended = "+value[2]);
                    }
                    if ((value[0] & 0x10) != 0 ){
                        System.err.println("rr interval = "+Byte.toString(value[3]));
                    }
                    if (mHeartRateCallback != null) {
                        mHeartRateCallback.onCounterUpdate(mHeartRate, old_heart_rate-mHeartRate);
                    }
                }

            };
 
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                 if (newState == BluetoothGatt.STATE_DISCONNECTED ) {
                     System.err.println("device disconnecting");

                     if(mHeartRateDevice != null ) {
                         if (mHeartRateDevice.gatt == gatt) {
                             if (mHeartRateCallback != null) {

                                 mHeartRateCallback.onCounterUpdate(-1, 0);
                             }
                             if (gatt == mHeartRateDevice.gatt) {
                                 mHeartRateDevice.gatt.close();
                                 mHeartRateDevice.gatt    = null;
                                 mHeartRateDevice.service = null;

                                 // Now try to reconnect

                                 if (mReconnectTask == null) {
                                     ReconnectDevice(mHeartRateDevice.device.getAddress(), mHeartRateDevice.name);
                                 }
                             }
                         }
                         mReconnectLatch.countDown();
                     }
                 }
                 else if (newState == BluetoothGatt.STATE_CONNECTED) {
                     boolean discover_done = gatt.discoverServices();
                 }
            };
    };

    public static ReconnectGattCallback reconnectGattCallback = null;

    public static class DiscoveryGattCallback extends BluetoothGattCallback {
        private static List<BtGattDevice> gattDevices = null;
        private static CountDownLatch          latch  = null;

        public void setGattDevices(List<BtGattDevice> gattDev) {
                gattDevices = Collections.synchronizedList(gattDev) ;
            };
        
        public void setLatch(CountDownLatch l) {
                latch = l;
            };
        public CountDownLatch getLatch() {
            return latch;
        };

        @Override
        public synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
                String devname = gatt.getDevice().getName();

                System.err.println("discovery: services discovered");


                for (BtGattDevice this_gatt_dev : gattDevices) {
                    if (gatt == this_gatt_dev.gatt) {
                        List<BluetoothGattService> service_list = gatt.getServices();
                        BluetoothGattService service = gatt.getService(BLE_SERVICE_HEART_RATE);
                        if (service != null) {
                            this_gatt_dev.service = service;

                            if (latch != null) {
                                latch.countDown();
                            }
                        }
                        else {
                            gattDevices.remove(this_gatt_dev);
                            if (latch != null) {
                                latch.countDown();
                            }

                        }
                    }
                }
           }

        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                System.err.println("device connection status changed status = "+status);

                for (BtGattDevice this_gatt_dev : gattDevices) {
                    if (gatt == this_gatt_dev.gatt) {
                         if (newState == BluetoothGatt.STATE_DISCONNECTED ) {
                             System.err.println("device disconnection");
                             this_gatt_dev.gatt.close();
                             this_gatt_dev.gatt    = null;
                             this_gatt_dev.service = null;

                             if (latch != null) {
                                 latch.countDown(); // We're done here. Apparently the stack doesn't want any more connections.
                             }

                         }
                         else if (newState == BluetoothGatt.STATE_CONNECTED) {
                             System.err.println("device connection");
                             boolean discover_done = gatt.discoverServices();
                         }
                    }
                }
           };
    };

    public static DiscoveryGattCallback discoveryGattCallback = null;

    private static class InitialDeviceDiscoveryCallback implements DeviceDiscoveryCallback {
        @Override
        public void onDeviceDiscoveryComplete(Object data) {
                String heart_rate_device_addr = (String) data;
                AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth", "search complete", true);
            }
     };

    private static DeviceDiscoveryCallback discoveryCallback = null;

    private static class BtGattConnect implements Runnable {
                Context      mContext = null;
                BtGattDevice mGattDev = null;
                BluetoothGattCallback mCallback = null;

                BtGattConnect(Context ctx, BtGattDevice gatt_dev, BluetoothGattCallback callback) {
                        mContext  = ctx;
                        mGattDev  = gatt_dev;
                        mCallback = callback;
                    };

                @Override
                public void run() {
                    boolean refresh_done  = false;
                    boolean connect_done  = false;

                    mGattDev.gatt = mGattDev.device.connectGatt(mContext,false, mCallback);
                    if (mGattDev.gatt != null ) {
                        try {
                            Method refresh_method = mGattDev.gatt.getClass().getMethod("refresh", new Class[0]);
                            if (refresh_method != null) {
                                refresh_done = ((Boolean) refresh_method.invoke(mGattDev.gatt, new Object[0])).booleanValue();
                            }
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                            mReconnectLatch.countDown();
                        }

                        connect_done = mGattDev.gatt.connect();
                    }
                    else {
                        mReconnectLatch.countDown();
                    }
                }
            }

      private static class BtGattClose implements Runnable {
                BtGattDevice mGattDev = null;
                CountDownLatch mLatch  = null;

                BtGattClose(BtGattDevice gatt_dev, CountDownLatch l) {
                        mGattDev  = gatt_dev;
                        mLatch    = l;
                    };

                @Override
                public void run() {
                    if (mGattDev.gatt != null ) {
                         mGattDev.gatt.close();
                         mGattDev.gatt    = null;
                         mGattDev.service = null;
                    }
                    else {
                        mLatch.countDown(); // If we are already out of the game, the callback
                                            // can't set the latch.
                    }
                }
            }


    private static class DiscoveryTask extends AsyncTask<Integer, List<BtGattDevice>, Integer> {

        UUID DeviceInfoServiceUUID = UUID.fromString("0000180a-0000-0000-0000-000000000000");

        HashSet<String> deviceSet = null;
        static  List<BtGattDevice>   deviceList = null;


        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {

                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                                String device_name = device.getName();
                                String device_address = device.getAddress();

                                // Sets don't allow dups.  We can get some serious duplication

                                deviceSet.add(device_name+"|"+device_address);

                                System.err.println("bluetooth callback");

                        };

            };

        @Override
        protected synchronized Integer doInBackground(Integer... numMillis) {
            System.err.println("Background bluetooth look\n");
            try {

                deviceSet  = new HashSet<String>();
                deviceList = Collections.synchronizedList(new ArrayList<BtGattDevice>());


                AardvarkPedBluetooth.mBtAdapter.startLeScan(mLeScanCallback);
                Thread.sleep((long) 20000);
                AardvarkPedBluetooth.mBtAdapter.stopLeScan(mLeScanCallback);

                // By accumulating the deviceid+name strings in the set, we eliminate devices
                // that report multiple times.

                for (int i = 0; i < deviceSet.size(); i++) {
                    Object     s = deviceSet.toArray()[i];
                    String[] tok = ((String)(s)).split("\\|");

                    // Tok 0 is the name, tok 1 is the id

                    BtGattDevice new_dev = new BtGattDevice(mBtAdapter.getRemoteDevice(tok[1]), tok[0]);
                    deviceList.add(new_dev);
                }


                String dev_name_list = "Scan sees devices:" ;
                for (BtGattDevice dev : deviceList) {
                    String devname = dev.device.getName();

                    if (null == devname) devname = "unknown";
                    dev_name_list += "\t\n"+devname;
                }
                dev_name_list += "\n";

                AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth", dev_name_list, false);

                discoveryGattCallback.setGattDevices(deviceList);

                // In this loop, we set up the heart rate candidates. We just want the heart rate measurement right now.

                for (BtGattDevice this_device: deviceList) {
                    String devname = this_device.device.getAddress();

                      if (null == devname) devname = "unknown";

                      if (!mHeartRateDevicesAvailable.contains(this_device)) {
                          if (mHeartRateDevice != null && this_device.device.getAddress().equals(mHeartRateDevice.device.getAddress())) {

                          // We don't have to query a device we already know

                              mHeartRateDevicesAvailable.add(mHeartRateDevice);
                          }
                          else {
                              discoveryGattCallback.setLatch(new CountDownLatch(1));
                              mHandler.post(new BtGattConnect(mContext, this_device, discoveryGattCallback));
                              AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth",
                                      dev_name_list+"\ndevice " +  devname, false);
                              discoveryGattCallback.getLatch().await(30000, TimeUnit.MILLISECONDS);

                              if (this_device.service != null) {

                                  AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth",
                                      dev_name_list+"\ndevice " + devname + " has heartrate.\n Add\n", false);

                                  mHeartRateDevicesAvailable.add(this_device);


                                  // We only want to read one characteristic right now. We are assuming the
                                  // characteristic for heart rate monitor is the same for all of the HR devices we have on the air.
                                  // This *should* hold since they are all the same profile and characteristic, but ....
                                  // there's many a slip in firmware

                              }
                              else {
                                  AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth",
                                      dev_name_list+"\ndevice " + devname + " has no heartrate\nremove\n", false);
                              }

                              // no other use for this device. close the instance
                              if (this_device.gatt != null ) {
                                  discoveryGattCallback.setLatch(new CountDownLatch(1));
                                  mHandler.post(new BtGattClose(this_device, discoveryGattCallback.getLatch()));
                                  discoveryGattCallback.getLatch().await(10000, TimeUnit.MILLISECONDS);
                              }
                          }
                      }

                 }
            }
            catch(Exception e) {
                 System.err.println("sleep failed");
                e.printStackTrace();
            }
            AardvarkPedAnalysis.ShowBluetoothProgress("Aardvark Bluetooth", "done", true);
            // Call the discovery callback
            if (mDeviceDiscoveryCallback != null) {
                mDeviceDiscoveryCallback.onDeviceDiscoveryComplete(mDeviceDiscoveryCallbackData );
            }
            System.err.println("Background bluetooth done\n");
            mDiscoveryTask = null;
            return 0;
        };

        protected void onCanceled(Integer rslt) {

        };

        

    };


    private static class ReconnectTask extends AsyncTask<Integer, List<BluetoothDevice>, Integer> {

        private final UUID DeviceInfoServiceUUID = UUID.fromString("0000180a-0000-0000-0000-000000000000");

        private boolean found = false;
        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                                String device_name = device.getName();
                                String device_address = device.getAddress();

                                // AardvarkPedAnalysis.BluetoothProgress("reconnecting ", "looking for "+"found"+device_name);
                                if  ( mHeartRateDevice != null) {

                                    // If this is the device we want to reconnect with, close the gatt instance, 
                                    // refresh the device cache, and connect, dscovery, etc again.

                                    if (mHeartRateDevice.device.getAddress().equals(device_name)) {

                                        // We need to wait for the search to end to do the reconnect.
                                        // (I beleive).

                                        found = true;
                                    }
                                }

                                System.err.println("bluetooth callback");

                        };

            };

        @Override
        protected Integer doInBackground(Integer... numMillis) {
            final int max_retries = 5;
            int retry_count = 0;
            long sleep_time = 0;
 
            System.err.println("Background bluetooth look\n");
            while(mHeartRateDevice != null && mReconnectAttempts < MAX_RECONNECT_RETRIES) {

                // Only way out is either the heart rate device gets nulled by the preference, or 
                // connecting with the device.

                try {
    
                    // If we already have a selected heart rate device, all we have to do is try to reconnect
    
                    retry_count = 0;
                    sleep_time  = 5000; // 5 second sleep time
                    mReconnectAttempts++;

                    if (mHeartRateDevice.gatt == null) {
                        mReconnectLatch = new CountDownLatch(1);
                        mHandler.postAtFrontOfQueue(new BtGattConnect(mContext, mHeartRateDevice, reconnectGattCallback));

                        // Wait for the whole callback chain to play out. 

                        mReconnectLatch.await(20000, TimeUnit.MILLISECONDS); // We lose patience after twenty seconds

                       // We should have a service by now. 
                        if (mHeartRateDevice.service != null) { 
    
                            // Happy ending. We are all ready to go
                            // Call the discovery callback and let the thread die.
    
                            if (mDeviceReconnectCallback != null) {
                                mDeviceReconnectCallback.onDeviceReconnectComplete(true, mHeartRateDevice.device, mDeviceReconnectCallbackData );
                            }

                            // End the task and return 
                            mReconnectTask = null;
                            return 0;
                        }
                        else {
                            if (mDeviceReconnectCallback != null) {
                                mDeviceReconnectCallback.onDeviceReconnectComplete(false, mHeartRateDevice.device, mDeviceReconnectCallbackData );
                            }
                        }
                    }
                    
                    if (mHeartRateDevice.gatt != null) {
                        mReconnectLatch = new CountDownLatch(1);
                        mHandler.post(new BtGattClose(mHeartRateDevice, mReconnectLatch));
                        mReconnectLatch.await(60000, TimeUnit.MILLISECONDS); // We lose patience after a minute
                    }
                    Thread.sleep(5000);  // If we don't have a device out there, we try again.
                }
                catch(Exception e) {
                     System.err.println("sleep failed");
                }
            }
            System.err.println("Background bluetooth done\n");
            mReconnectAttempts = 0;
            mReconnectTask = null;
            return 0;
        };


        protected void onCanceled(Integer rslt) {

               // It ain't over till we kill the thread or we have success.

              if (mDeviceReconnectCallback != null) {
                  mDeviceReconnectCallback.onDeviceReconnectComplete(false, null, mDeviceReconnectCallbackData );
              }
              mReconnectTask = null;
        };

    };




    public static AardvarkPedBluetooth NewInstance(Context context, SharedPreferences prefs) {
             mSharedPreferences = prefs;

             if (mSingleInstance != null) {
                 return mSingleInstance;
             }

             mSingleInstance = new AardvarkPedBluetooth();

             try {
                 mBtMgr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                 if (mBtMgr != null) {
                     mBtAdapter = mBtMgr.getAdapter();
                     if (!mBtAdapter.isEnabled()) {
                         System.err.print("what is this?\n");
                     }
                 }
                 else {
                     System.err.print("No Bluetooth anywhere\n");
                     return null;
                 }
             }
             catch (Exception e) {
                 System.err.print("No Bluetooth anywhere. Not even a class defined. Must be amazon phone.\n");
                 return null;
             }

             reconnectGattCallback = new ReconnectGattCallback();
             discoveryGattCallback = new DiscoveryGattCallback();
             discoveryCallback     = new InitialDeviceDiscoveryCallback();

             // List<BluetoothDevice> bt_devs = mBtMgr.getConnectedDevices(BluetoothProfile.GATT);
             //mBtAdapter.startLeScan(mLeScanCallback);
             mContext = context;
             mHandler = new Handler(mContext.getMainLooper());
             mHeartRateDevicesAvailable = new ArrayList<BtGattDevice>();

             // Reconsitute the list. This makes the selection list readable and doens't require redoing the scan.
             // The scan is not nice. We don't like the scan.
             // BTW, the list is really, really small, no more than a couple of items usually. 

             Set<String> heart_device_set = mSharedPreferences.getStringSet(AardvarkPedWidgetService.PREFKEY_AvailableHeartRateDevices_STRINGSET, new HashSet<String>());
             String selected_heart_device = mSharedPreferences.getString(AardvarkPedWidgetService.PREFKEY_SelectedHeartRate, "");
             Object[] heart_devices = heart_device_set.toArray();
             String[] tok = null;

             // We don't have to look for the heart rate device, since we are here first.

             for (int i = 0; i < heart_devices.length; i++) {
                 tok = ((String)heart_devices[i]).split("\\|");

                 // Tok 0 is the name, tok 1 is the id

                 BtGattDevice new_dev = new BtGattDevice(mBtAdapter.getRemoteDevice(tok[1]), tok[0]);
                 mHeartRateDevicesAvailable.add(new_dev);
                 if (tok[1].equals(selected_heart_device)){
                     mHeartRateDevice = new_dev;
                 }
             }

             return mSingleInstance;
         }


    public static int DiscoverDevices(int numSeconds) {
             if (mDiscoveryTask == null) {
                 mDiscoveryTask = new DiscoveryTask();
                 mDiscoveryTask.execute(numSeconds);
             }

             return 0;
         }

    public static int ReconnectDevice(String address, String name) {
             boolean found = false;

             mReconnectAttempts = 0;
             if (mHeartRateDevice == null) {

                 // mHeartRateDevicesAvailable should not be null, ever.

                 if (mHeartRateDevicesAvailable != null) {

                     // Look for it in the list if there is list

                     for (BtGattDevice dev : mHeartRateDevicesAvailable) {
                         if (dev.device.getAddress().equals(address)) {
                             mHeartRateDevice = dev;
                             mHeartRateDevice.name = name;
                             found = true;
                         }
                     }
                 }
                 if (found == false) {

                     // Get the device from the adapter.
                     mHeartRateDevice = new BtGattDevice(mBtAdapter.getRemoteDevice(address), name);

                     if (mHeartRateDevicesAvailable != null ) {
                         mHeartRateDevicesAvailable.add(mHeartRateDevice);
                     }
                 }
             }

             if (mReconnectTask == null) {
                 mReconnectTask = new ReconnectTask();
                 mReconnectTask.execute(5000);
             }

             return 0;
         }


    // Called when there is a new device encountered.
    public static void setNewDeviceCallback(NewDeviceCallback newDevCallback) {
             mNewDeviceCallback = newDevCallback;
         }

    public static void setDeviceDiscoveryCallback(DeviceDiscoveryCallback discCallback, Object callbackData) {
             mDeviceDiscoveryCallback     = discCallback;
             mDeviceDiscoveryCallbackData = callbackData;
         }

    public static void setDeviceReconnectCallback(DeviceReconnectCallback reconnectCallback, Object callbackData) {
             mDeviceReconnectCallback = reconnectCallback;
             mDeviceReconnectCallbackData = callbackData;
         }

    public static void setDeviceDisconnectCallback(DeviceDisconnectCallback disconnectCallback, Object callbackData) {
             mDeviceDisconnectCallback     = disconnectCallback;
             mDeviceDisconnectCallbackData = callbackData;
         }

    public static boolean isEnabled() {

             // We can disable the whole kit and kaboodle with a return false here
             return (mBtAdapter != null) && (mBtAdapter.isEnabled());
         }


    public static String[] AvailableHeartRateDeviceNames() {

              // If the heart rate device is already connected, it won't show in a scan.
              // so we need to add it back if not present in the list.  This in turn would
              // happen if someone did a scan for devices without disconnecting.

              if (mHeartRateDevice != null) {
                  if (mHeartRateDevice.service != null) {
                      boolean found = false;
                      for (BtGattDevice dev : mHeartRateDevicesAvailable) {
                          if (dev == mHeartRateDevice) {
                              found = true;
                              break;
                          }
                      }
                      if (!found) {
                          mHeartRateDevicesAvailable.add(mHeartRateDevice);
                      }
                  }
              }

              
              int num_elems = ((mHeartRateDevicesAvailable != null)? mHeartRateDevicesAvailable.size() : 0 );
              String[] rslt = new String[num_elems+1];

              int i = 0;
              for (; i < num_elems; i++) {
                  rslt[i] = mHeartRateDevicesAvailable.get(i).device.getName();
                  if (rslt[i] == null) {
                      rslt[i] = mHeartRateDevicesAvailable.get(i).name;
                      if (rslt[i] == null) {
                          rslt[i] = "unknown device";
                      }
                  }
              }
              rslt[i] = "none";
              
              return rslt;
         }

    public static String[] AvailableHeartRateDeviceIdentifiers() {
              int num_elems = ((mHeartRateDevicesAvailable != null)? mHeartRateDevicesAvailable.size() : 0 );
              String[] rslt = new String[num_elems+1];

              int i = 0;
              for (; i < num_elems; i++) {
                  rslt[i] = mHeartRateDevicesAvailable.get(i).device.getAddress();
                  if (rslt[i] == null) {
                      rslt[i] = "";
                  }
              }
              rslt[i] = ""; // No device
              return rslt;
         }


    public static String[] AvailableStepCounterDevices() {
              return null;
         }

    public static String[] AvailableGPSTrackerDevices() {
              return null;
         }

    public static BluetoothDevice getSelectedHeartRateDevice() {
             if (mHeartRateDevice == null) return null;
             return mHeartRateDevice.device;
         }

    public static String getSelectedHeartRateDeviceName() {
             if (mHeartRateDevice == null) return null;

             if (mHeartRateDevice.name != null) {
                return mHeartRateDevice.name;
             }
             else {
                return mHeartRateDevice.device.getName();
             }
         }

    public static boolean getHeartRateDeviceConnected() {
             if (mHeartRateDevice == null) return false;
             return mHeartRateDevice.service != null;
         }

    public static BluetoothDevice getSelectedStepCounterDevice() {
              return null;
         }


    public static BluetoothDevice getSelectedGPSTrackerDevice() {
              return null;
         }

     public static boolean registerHeartRateDevice(String selectedDevice, CounterUpdateCallback updateCallback) { 
             BluetoothGattCharacteristic heart_rate_reading_characteristic = null;
             int selected_device_idx = 0;
             boolean found = false;
          
            
             for (int i = 0; i < mHeartRateDevicesAvailable.size(); i++) {
                 if (selectedDevice.equals(mHeartRateDevicesAvailable.get(i).device.getAddress())) {
                    selected_device_idx = i;
                    found = true;
                     break;
                 }
             }
             if (found) {
                 mHeartRateDevice   = mHeartRateDevicesAvailable.get(selected_device_idx);
             }
             else {
                 // Can't proceed 
                 return false;
             }

             if (mHeartRateDevice.gatt == null) {

                 // The heart rate device was disconnected. We need to reconnect it.
                 // We use the task, and wait for the task.

                 if (mReconnectTask == null) {
                     mReconnectTask = new ReconnectTask();
                     mReconnectTask.execute(5000);
                 }
                 else {
                     // There is a reconnect task happening right now.
                     
                 }
             }

             mHeartRateCallback = updateCallback;
             boolean notification_enabled = false;
             if (mHeartRateDevice.service != null) {
                 for (BluetoothGattCharacteristic characteristic : mHeartRateDevice.service.getCharacteristics()) {
                     if (characteristic.getUuid().equals(BLE_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
                         heart_rate_reading_characteristic = characteristic;
                         break;
                     }
                 }

                 // First, write the config descriptor

                 // Now we setup the characteristic change notification. First, enable notifications on this descriptor

                 BluetoothGattDescriptor desc = heart_rate_reading_characteristic.getDescriptor(BLE_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIG);
                 desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                 mHeartRateDevice.gatt.writeDescriptor(desc);

                 notification_enabled = mHeartRateDevice.gatt.setCharacteristicNotification(heart_rate_reading_characteristic, true);

             }

             return notification_enabled;
         }



     public static void registerStepCounterDevice(int selectedDevice, CounterUpdateCallback updateCallback) throws ArrayIndexOutOfBoundsException {
         }

     public static void registerGPSTrackerDevice(int selectedDevice, CounterUpdateCallback updateCallback) throws ArrayIndexOutOfBoundsException {
         }

     public static void unregisterHeartRateDevice() { 
              if (mHeartRateDevice != null ) {
                  if (mHeartRateDevice.gatt != null) {
                      mHeartRateDevice.gatt.close();
                  }
                  mHeartRateDevice.gatt    = null;
                  mHeartRateDevice.service = null;
                  mHeartRateCallback = null;
                  mHeartRateDevice   = null;
              }
         }

     public static void unregisterStepCounterDevice() {
         }

     public static void unregisterGPSTrackerDevice() {
         }


}
