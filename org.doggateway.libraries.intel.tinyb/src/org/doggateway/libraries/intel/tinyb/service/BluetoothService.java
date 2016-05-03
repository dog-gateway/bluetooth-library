/**
 * 
 */
package org.doggateway.libraries.intel.tinyb.service;

import java.util.Map;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;

/**
 * @author bonino
 *
 */
public interface BluetoothService
{
	/**
	 * provides a human-readable {@link String} representation of the device
	 * 
	 * @param device
	 *            The device to represent
	 * @return The device string representation.
	 */
	public String deviceToString(BluetoothDevice device);

	/**
	 * Provides a {@link BluetoothDevice} representing the actual device having
	 * the given MAC address.
	 * 
	 * @param address
	 *            The MAC address of the device to get.
	 * @param timeoutBetweenTrials
	 *            The timeout in milliseconds to wait between device "discovery"
	 *            attempts.
	 * @param nTrials
	 *            The amount of discovery attempts performed to find the given
	 *            device, -1 for infinite attempts.
	 * @return The device if it exists, or null.
	 */
	public BluetoothDevice getDevice(String address, long timeoutBetweenTrials,
			int nTrials);

	/**
	 * Provides the {@link BluetoothGattService} corresponding to the given UUID
	 * 
	 * @param device
	 *            The Bluetooth device from which the given service shall be
	 *            retrieved;
	 * @param UUID
	 *            The UUID of the service to retrieve;
	 * @param timeoutBetweenTrials
	 *            The timeout in milliseconds to wait between service
	 *            "attachment" attempts.
	 * @param nTrials
	 *            The amount of service "attachment" attempts performed, -1 for
	 *            infinite attempts.
	 * @return The required service or null.
	 */
	public BluetoothGattService getService(BluetoothDevice device, String UUID,
			long timeoutBetweenTrials, int nTrials);

	/**
	 * Provides all the services offered by the given device, in a map having
	 * the service UUID as key.
	 * 
	 * @param device
	 *            The Bluetooth device hosting the services.
	 * @param timeoutBetweenTrials
	 *            The timeout in milliseconds to wait between service
	 *            "attachment" attempts.
	 * @param nTrials
	 *            The amount of service "attachment" attempts performed, -1 for
	 *            infinite attempts.
	 * @return The map of available services.
	 */
	public Map<String, BluetoothGattService> getAllServices(
			BluetoothDevice device, long timeoutBetweenTrials, int nTrials);

	/**
	 * Get a characteristic from a given GATT service
	 * 
	 * @param service
	 *            The service from/to which the characteristic shall be
	 *            read/written
	 * @param UUID
	 *            The UUID of the characteristic, as a string
	 * @return The characteristic, or null.
	 */
	public BluetoothGattCharacteristic getCharacteristic(
			BluetoothGattService service, String UUID);

	/**
	 * Provides a reference to the Bluetooth manager, as singleton. Actually
	 * only one manager can exist at time, this method allows different bundles
	 * to exploit the same manager, transparently.
	 * 
	 * @return The only instance of {@link BluetoothManager}
	 */
	public BluetoothManager getManager();
}
