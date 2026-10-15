package org.toyotacustom.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

data class GattCharacteristicInfo(val uuid:String,val instanceId:Int,val properties:Int,val descriptors:List<String>)
data class GattServiceInfo(val uuid:String,val instanceId:Int,val primary:Boolean,val characteristics:List<GattCharacteristicInfo>)
data class BleInventory(val address:String,val observedAt:Long,val services:List<GattServiceInfo>) {
 fun json():String=JSONObject().apply {
  put("format","toyota-custom-gatt-inventory-v1")
  put("appVersion",BuildConfig.VERSION_NAME)
  put("observedAt",java.time.Instant.ofEpochMilli(observedAt).toString())
  put("bluetoothAddress",JSONObject.NULL)
  put("elmTransportVerified",false)
  put("vehicleCompatibilityVerified",false)
  put("characteristicValuesRead",false)
  put("characteristicOrDescriptorWrites",false)
  put("services",JSONArray().apply{services.forEach{s->put(JSONObject().apply{
   put("uuid",s.uuid);put("instanceId",s.instanceId);put("primary",s.primary)
   put("characteristics",JSONArray().apply{s.characteristics.forEach{c->put(JSONObject().apply{
    put("uuid",c.uuid);put("instanceId",c.instanceId);put("properties",c.properties)
    put("propertyLabels",JSONArray(gattPropertyLabels(c.properties)))
    put("descriptors",JSONArray(c.descriptors))
   })}})
  })}})
 }.toString(2)
}
fun gattPropertyLabels(bits:Int):List<String> = listOf(
 1 to "Diffusion",2 to "Lecture",4 to "Écriture sans réponse",8 to "Écriture",
 16 to "Notification",32 to "Indication",64 to "Écriture signée",128 to "Propriétés étendues"
).filter{bits and it.first != 0}.map{it.second} + if(bits and 255.inv()!=0)listOf("Bits inconnus : 0x%X".format(bits and 255.inv()))else emptyList()

/** Service discovery only: no value reads, subscriptions, descriptor writes or ELM traffic. */
@SuppressLint("MissingPermission")
class BleInspector(private val context:Context) {
 suspend fun inspect(adapter:BluetoothAdapter,address:String):BleInventory = withContext(Dispatchers.Main.immediate) {
  val result=CompletableDeferred<BleInventory>()
  var active=true
  var connection:BluetoothGatt?=null
  val callback=object:BluetoothGattCallback() {
   override fun onConnectionStateChange(gatt:BluetoothGatt,status:Int,newState:Int) {
    if(!active || result.isCompleted)return
    if(status!=BluetoothGatt.GATT_SUCCESS)result.completeExceptionally(IllegalStateException("Connexion GATT refusée (code $status)"))
    else if(newState==BluetoothProfile.STATE_CONNECTED) {
     runCatching{check(gatt.discoverServices()){ "Découverte des services GATT impossible" }}.onFailure{result.completeExceptionally(it)}
    }else if(newState==BluetoothProfile.STATE_DISCONNECTED)result.completeExceptionally(IllegalStateException("Boîtier BLE déconnecté avant la fin de l’inventaire"))
   }
   override fun onServicesDiscovered(gatt:BluetoothGatt,status:Int) {
    if(!active || result.isCompleted)return
    if(status!=BluetoothGatt.GATT_SUCCESS){result.completeExceptionally(IllegalStateException("Découverte GATT refusée (code $status)"));return}
    runCatching {
     BleInventory(address,System.currentTimeMillis(),gatt.services.map{s->GattServiceInfo(s.uuid.toString(),s.instanceId,s.type==BluetoothGattService.SERVICE_TYPE_PRIMARY,s.characteristics.map{c->
      GattCharacteristicInfo(c.uuid.toString(),c.instanceId,c.properties,c.descriptors.map{it.uuid.toString()})
     })})
    }.onSuccess{result.complete(it)}.onFailure{result.completeExceptionally(it)}
   }
  }
  try {
   check(adapter.isEnabled){"Activez le Bluetooth"}
   adapter.cancelDiscovery()
   // The explicit main-looper handler serializes callbacks and cleanup.
   connection=adapter.getRemoteDevice(address).connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE,BluetoothDevice.PHY_LE_1M_MASK,android.os.Handler(android.os.Looper.getMainLooper()))
   checkNotNull(connection){"Connexion GATT indisponible"}
   withTimeout(20000){result.await()}
  }finally{
   active=false
   runCatching{connection?.disconnect()}
   runCatching{connection?.close()}
   result.cancel()
  }
 }
}
