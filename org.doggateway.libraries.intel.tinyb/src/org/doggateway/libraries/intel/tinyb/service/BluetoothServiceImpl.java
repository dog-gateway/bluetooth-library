/*
 * Dog - Bluetooth Low Energy OSGi wrapper for Intel TinyB
 * 
 * Copyright (c) 2016 Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.doggateway.libraries.intel.tinyb.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class BluetoothServiceImpl implements BluetoothService
{
	// time after which retrying to get a device
	public static long RETRY_AFTER_MILLIS = 4000;
	
	// the BluetoothManager singleton
	private static BluetoothManager theManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.intel.tinyb.service.BluetoothService#deviceToString(tinyb.
	 * BluetoothDevice)
	 */
	@Override
	public String deviceToString(BluetoothDevice device)
	{
		// the device string representation
		StringBuffer deviceString = new StringBuffer();

		// build a very simple device representation, JSON-like
		deviceString.append("{");
		deviceString.append("\"name\":\"" + device.getName() + "\",");
		deviceString.append("\"address\":\"" + device.getAddress() + "\",");
		deviceString.append("\"connected\":" + device.getConnected() + ",");
		deviceString.append("\"rssi\":" + device.getRssi());
		deviceString.append("}");

		return deviceString.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.intel.tinyb.service.BluetoothService#getManager()
	 */
	@Override
	public BluetoothManager getManager()
	{
		// generate a singleton (as it is not clear if the manager is a
		// singleton or not)
		if (BluetoothServiceImpl.theManager == null)
			BluetoothServiceImpl.theManager = BluetoothManager
					.getBluetoothManager();

		return BluetoothServiceImpl.theManager;
	}

	@Override
	public BluetoothDevice getDevice(String address, long timeoutBetweenTrials,
			int nTrials)
	{
		// get the Bluetooth manager
		BluetoothManager manager = this.getManager();

		// the Bluetooth device to get
		BluetoothDevice sensor = null;

		// the number of attempted trials
		int nTrialsDone = 0;

		// the device found flag
		boolean found = false;

		// look for the device up to the given amount of times
		while ((!found) && ((nTrials < 0)
				|| ((nTrials > 0) && (nTrialsDone < nTrials))))
		{
			// increment the number of attempted trials
			if (nTrials > 0)
				nTrialsDone++;

			// get the available devices from the bluetooth manager
			List<BluetoothDevice> list = manager.getDevices();

			// iterate over all devices listed in the manager
			for (BluetoothDevice device : list)
			{
				// if the device address matches, store it and stop searching
				if (device.getAddress().equals(address))
				{
					// store the device
					sensor = device;

					// interrupt
					found = true;
				}
			}

			// avoid sleeping if found
			if (!found)
			{
				try
				{
					// wait before a new attempt, if a not valid time is
					// specified, use default one.
					Thread.sleep(timeoutBetweenTrials < 0 ? timeoutBetweenTrials
							: BluetoothServiceImpl.RETRY_AFTER_MILLIS);
				}
				catch (InterruptedException e)
				{
					// do nothing and exit
					found = true;
				}
			}
		}
		// return the found device, or null
		return sensor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.intel.tinyb.service.BluetoothService#getCharacteristic(tinyb.
	 * BluetoothGattService, java.lang.String)
	 */
	@Override
	public BluetoothGattCharacteristic getCharacteristic(
			BluetoothGattService service, String UUID)
	{
		// the characteristic corresponding to the given UUID, initially null
		BluetoothGattCharacteristic ch = null;

		// the list of all service characteristics
		List<BluetoothGattCharacteristic> characteristics = service
				.getCharacteristics();

		// iterate over the list of characteristics to find the characteristic
		// corresponding to the given UUID
		for (BluetoothGattCharacteristic characteristic : characteristics)
		{
			// check for UUID match
			if (characteristic.getUuid().equals(UUID))
				// store the found characteristics
				ch = characteristic;
		}

		// return the found characteristic or null
		return ch;
	}

	@Override
	public BluetoothGattService getService(BluetoothDevice device, String UUID,
			long timeoutBetweenTrials, int nTrials)
	{
		// the service to get
		BluetoothGattService serv = null;

		// the list of services offered by the devices
		List<BluetoothGattService> bluetoothServices = null;

		int nTrialsDone = 0;
		boolean found = false;

		// look for the device up to the given amount of times
		while (((!found) && (nTrials < 0))
				|| ((nTrials > 0) && (nTrialsDone < nTrials)))
		{
			// increment the number of attempted trials
			if (nTrials > 0)
				nTrialsDone++;

			// can be empty
			bluetoothServices = device.getServices();

			// if not empty, then all services have been considered
			// TODO: check if it is true
			if ((bluetoothServices != null) && (!bluetoothServices.isEmpty()))
			{
				// set the found flag
				found = true;

				// search for the given UUID
				for (BluetoothGattService service : bluetoothServices)
				{
					// check if the given UUID matches with the UUID of the
					// current
					// service
					if (service.getUuid().equals(UUID))
					{
						// store the found service
						serv = service;

					}
				}
			}
			else
			{
				try
				{
					// wait before a new attempt, if a not valid time is
					// specified, use default one.
					Thread.sleep(timeoutBetweenTrials < 0 ? timeoutBetweenTrials
							: BluetoothServiceImpl.RETRY_AFTER_MILLIS);
				}
				catch (InterruptedException e)
				{
					// do nothing and exit
					found = true;
				}
			}
		}

		// return the found service or null
		return serv;
	}

	@Override
	public Map<String, BluetoothGattService> getAllServices(
			BluetoothDevice device, long timeoutBetweenTrials, int nTrials)
	{
		// the map of services to return
		HashMap<String, BluetoothGattService> servicesMap = new HashMap<>();

		// the list of services offered by the devices
		List<BluetoothGattService> bluetoothServices = null;

		int nTrialsDone = 0;
		boolean found = true;

		// look for the device up to the given amount of times
		while ((!found) && ((nTrials < 0)
				|| ((nTrials > 0) && (nTrialsDone < nTrials))))
		{
			// increment the number of attempted trials
			if (nTrials > 0)
				nTrialsDone++;

			// can be empty
			bluetoothServices = device.getServices();

			// if not empty, then all services have been considered
			// TODO: check if it is true
			if ((bluetoothServices != null) && (!bluetoothServices.isEmpty()))
			{
				// found services
				found = true;

				// fill the map to return
				for (BluetoothGattService bService : bluetoothServices)
				{
					servicesMap.put(bService.getUuid(), bService);
				}
			}
			else
			{
				try
				{
					// wait before a new attempt, if a not valid time is
					// specified, use default one.
					Thread.sleep(timeoutBetweenTrials < 0 ? timeoutBetweenTrials
							: BluetoothServiceImpl.RETRY_AFTER_MILLIS);
				}
				catch (InterruptedException e)
				{
					// do nothing and exit
					found = true;
				}
			}
		}
		return servicesMap;
	}

}
