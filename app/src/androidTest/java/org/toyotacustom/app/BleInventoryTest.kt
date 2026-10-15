package org.toyotacustom.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class BleInventoryTest {
 @Test fun exportPreservesDuplicateUuidInstancesAndMasksAddress() {
  val uuid="00001800-0000-1000-8000-00805f9b34fb"
  val inventory=BleInventory("AA:BB:CC:DD:EE:FF",0,listOf(
   GattServiceInfo(uuid,1,true,listOf(GattCharacteristicInfo(uuid,2,18,listOf(uuid)))),
   GattServiceInfo(uuid,3,false,emptyList())))
  val raw=inventory.json()
  assertFalse(raw.contains(inventory.address))
  val json=JSONObject(raw)
  assertTrue(json.isNull("bluetoothAddress"))
  assertFalse(json.getBoolean("elmTransportVerified"))
  assertFalse(json.getBoolean("vehicleCompatibilityVerified"))
  assertFalse(json.getBoolean("characteristicValuesRead"))
  assertFalse(json.getBoolean("characteristicOrDescriptorWrites"))
  assertEquals(BuildConfig.VERSION_NAME,json.getString("appVersion"))
  val services=json.getJSONArray("services")
  assertEquals(2,services.length())
  assertEquals(3,services.getJSONObject(1).getInt("instanceId"))
  val c=services.getJSONObject(0).getJSONArray("characteristics").getJSONObject(0)
  assertEquals(18,c.getInt("properties"))
  assertEquals(2,c.getJSONArray("propertyLabels").length())
  assertEquals(uuid,c.getJSONArray("descriptors").getString(0))
 }
 @Test fun allPropertyBitsRemainVisibleWithoutAssigningSerialRoles() {
  assertTrue(gattPropertyLabels(0).isEmpty())
  assertEquals(8,gattPropertyLabels(255).size)
  assertEquals(listOf("Lecture","Notification"),gattPropertyLabels(18))
  assertEquals(listOf("Bits inconnus : 0x100"),gattPropertyLabels(256))
 }
}
