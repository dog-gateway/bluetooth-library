package org.doggateway.libraries.intel.tinyb.example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Author: Petre Eftime <petre.p.eftime@intel.com>
 * Copyright (c) 2016 Intel Corporation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;
import tinyb.BluetoothNotification;

class ValueNotification implements BluetoothNotification<byte[]> {

    public void run(byte[] tempRaw) {
            System.out.print("Temp raw = {");
            for (byte b : tempRaw) {
                System.out.print(String.format("%02x,", b));
            }
            System.out.print("}");

            /*
             * The temperature service returns the data in an encoded format which can be found in the wiki. Convert the
             * raw temperature format to celsius and print it. Conversion for object temperature depends on ambient
             * according to wiki, but assume result is good enough for our purposes without conversion.
             */
            int objectTempRaw = (tempRaw[0] & 0xff) | (tempRaw[1] << 8);
            int ambientTempRaw = (tempRaw[2] & 0xff) | (tempRaw[3] << 8);

            float objectTempCelsius = Notification.convertCelsius(objectTempRaw);
            float ambientTempCelsius = Notification.convertCelsius(ambientTempRaw);

            System.out.println(
                    String.format(" Temp: Object = %fC, Ambient = %fC", objectTempCelsius, ambientTempCelsius));


    }

}

class AccelNotification implements BluetoothNotification<byte[]> {

    public void run(byte[] value) {
    	// interpret the data
    			int gyroXRaw = (value[0] + (value[1] << 8)); // signed integer
    			int gyroYRaw = (value[2] + (value[3] << 8));
    			int gyroZRaw = (value[4] + (value[5] << 8));
    			int accXRaw = (value[6] + (value[7] << 8));
    			int accYRaw = (value[8] + (value[9] << 8));
    			int accZRaw = (value[10] + (value[11] << 8));
    			int magXRaw = (value[12] + (value[13] << 8));
    			int magYRaw = (value[14] + (value[15] << 8));
    			int magZRaw = (value[16] + (value[17] << 8));

            System.out.println(
                    String.format("AccX=%f AccY=%f AccZ=%f\nGyroX=%f GyroY=%f GyroZ=%f", 
                    		accConvert(accXRaw, (byte)0x02),
                    		accConvert(accYRaw, (byte)0x02),
                    		accConvert(accZRaw, (byte)0x02),
                    		gyroConvert(gyroXRaw),
                    		gyroConvert(gyroYRaw),
                    		gyroConvert(gyroZRaw)));

    }
    
    private float gyroConvert(int value)
	{
		return (value * 1.0f) / (65536 / 500);
	}
    
    private float accConvert(int value, byte config)
	{
		float valueFloat = 0;
		switch (config)
		{
			case 0x00:
			{
				valueFloat = (value * 1.0f) / (32768 / 2);
				break;
			}
			case 0x01:
			{
				valueFloat = (value * 1.0f) / (32768 / 4);
				break;
			}
			case 0x02:
			{
				valueFloat = (value * 1.0f) / (32768 / 8);
				break;
			}
			case 0x03:
			{
				valueFloat = (value * 1.0f) / (32768 / 16);
				break;
			}
		}

		return valueFloat;
	}


}

class ConnectedNotification implements BluetoothNotification<Boolean> {

    public void run(Boolean connected) {
            System.out.println("Connected");
    }

}

public class Notification {
    private static final float SCALE_LSB = 0.03125f;
    static boolean running = true;

    static void printDevice(BluetoothDevice device) {
        System.out.print("Address = " + device.getAddress());
        System.out.print(" Name = " + device.getName());
        System.out.print(" Connected = " + device.getConnected());
        System.out.println();
    }

    static float convertCelsius(int raw) {
        return raw / 128f;
    }

    /*
     * This program connects to a TI SensorTag 2.0 and reads the temperature characteristic exposed by the device over
     * Bluetooth Low Energy. The parameter provided to the program should be the MAC address of the device.
     *
     * A wiki describing the sensor is found here: http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User's_Guide
     *
     * The API used in this example is based on TinyB v0.3, which only supports polling, but v0.4 will introduce a
     * simplied API for discovering devices and services.
     */
    public static void main(String[] args) throws InterruptedException {

        if (args.length < 1) {
            System.err.println("Run with <device_address> argument");
            System.exit(-1);
        }

        /*
         * To start looking of the device, we first must initialize the TinyB library. The way of interacting with the
         * library is through the BluetoothManager. There can be only one BluetoothManager at one time, and the
         * reference to it is obtained through the getBluetoothManager method.
         */
        BluetoothManager manager = BluetoothManager.getBluetoothManager();

        /*
         * The manager will try to initialize a BluetoothAdapter if any adapter is present in the system. To initialize
         * discovery we can call startDiscovery, which will put the default adapter in discovery mode.
         */
        boolean discoveryStarted = manager.startDiscovery();

        System.out.println("The discovery started: " + (discoveryStarted ? "true" : "false"));

        /*
         * After discovery is started, new devices will be detected. We can find the device we are interested in
         * through the manager's find method.
         */
        BluetoothDevice sensor = manager.find(null, args[0], null);

        if (sensor == null) {
            System.err.println("No sensor found with the provided address.");
            System.exit(-1);
        }

        sensor.enableConnectedNotifications(new ConnectedNotification());

        System.out.print("Found device: ");
        printDevice(sensor);

        if (sensor.connect())
            System.out.println("Sensor with the provided address connected");
        else {
            System.out.println("Could not connect device.");
            System.exit(-1);
        }

        /*
         * After we find the device we can stop looking for other devices.
         */
        //manager.stopDiscovery();

        final Lock lock = new ReentrantLock();
        final Condition cv = lock.newCondition();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                running = false;
                lock.lock();
                try {
                    cv.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        });

        /*
         * Our device should expose a temperature service, which has a UUID we can find out from the data sheet. The service
         * description of the SensorTag can be found here:
         * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf. The service we are looking for has the
         * short UUID AA00 which we insert into the TI Base UUID: f000XXXX-0451-4000-b000-000000000000
         */
        BluetoothGattService tempService = sensor.find( "f000aa00-0451-4000-b000-000000000000");

        if (tempService == null) {
            System.err.println("This device does not have the temperature service we are looking for.");
            sensor.disconnect();
            System.exit(-1);
        }
        System.out.println("Found service " + tempService.getUUID());

        BluetoothGattCharacteristic tempValue = tempService.find("f000aa01-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic tempConfig = tempService.find("f000aa02-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic tempPeriod = tempService.find("f000aa03-0451-4000-b000-000000000000");

        if (tempValue == null || tempConfig == null || tempPeriod == null) {
            System.err.println("Could not find the correct characteristics.");
            sensor.disconnect();
            System.exit(-1);
        }

        System.out.println("Found the temperature characteristics");

        /*
         * Turn on the Temperature Service by writing 1 in the configuration characteristic, as mentioned in the PDF
         * mentioned above. We could also modify the update interval, by writing in the period characteristic, but the
         * default 1s is good enough for our purposes.
         */
        byte[] config = { 0x01 };
        tempConfig.writeValue(config);
        
        byte[] period = { 100 };
        tempPeriod.writeValue(period);

        tempValue.enableValueNotifications(new ValueNotification());
        
        // -------- accel notifications
        /*
         * Our device should expose a temperature service, which has a UUID we can find out from the data sheet. The service
         * description of the SensorTag can be found here:
         * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf. The service we are looking for has the
         * short UUID AA00 which we insert into the TI Base UUID: f000XXXX-0451-4000-b000-000000000000
         */
        BluetoothGattService movementService = sensor.find( "f000aa80-0451-4000-b000-000000000000");

        if (tempService == null) {
            System.err.println("This device does not have the movement service we are looking for.");
            sensor.disconnect();
            System.exit(-1);
        }
        System.out.println("Found service " + movementService.getUUID());

        BluetoothGattCharacteristic movementValue = movementService.find("f000aa81-0451-4000-b000-000000000000");
        //BluetoothGattCharacteristic movementEnable = movementService.find("f0002902-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic movementConfig = movementService.find("f000aa82-0451-4000-b000-000000000000");
        BluetoothGattCharacteristic movementPeriod = movementService.find("f000aa83-0451-4000-b000-000000000000");

        if (movementValue == null || movementConfig == null  || movementPeriod==null) {
            System.err.println("Could not find the correct characteristics.");
            sensor.disconnect();
            System.exit(-1);
        }

        System.out.println("Found the movement characteristics");

        /*
         * Turn on the Temperature Service by writing 1 in the configuration characteristic, as mentioned in the PDF
         * mentioned above. We could also modify the update interval, by writing in the period characteristic, but the
         * default 1s is good enough for our purposes.
         */
        byte[] configM = { (byte) 0xff, (byte) 0x02 };
        movementConfig.writeValue(configM);
        byte[] enable = { (byte) 0x01,	(byte) 0x00 };
        byte[] periodM = {(byte)0x0A};
        movementPeriod.writeValue(periodM);
        //movementEnable.writeValue(enable);
        movementValue.enableValueNotifications(new AccelNotification());


        lock.lock();
        try {
            while(running)
                cv.await();
        } finally {
            lock.unlock();
        }
        sensor.disconnect();

    }
}
