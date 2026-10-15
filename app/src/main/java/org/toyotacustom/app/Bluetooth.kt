package org.toyotacustom.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.os.Build
import kotlinx.coroutines.*
import org.toyotacustom.core.ElmTransport
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class Device(val address: String, val name: String, val bonded: Boolean, val ble: Boolean = false)

@SuppressLint("MissingPermission")
class BluetoothDevices(private val context: Context, private val found: (Device)->Unit, private val failed:(String)->Unit={}) {
 val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
 private var registered = false
 private val receiver=object: BroadcastReceiver() {
  override fun onReceive(c: Context?, i: Intent?) {
   val device = if(Build.VERSION.SDK_INT>=33) i?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice::class.java) else @Suppress("DEPRECATION") i?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
   runCatching {device?.let { report(it,false) }}.onFailure{failed("Accès Bluetooth interrompu : ${it.message}")}
  }
 }
 private val bleCallback=object: ScanCallback() {
  override fun onScanResult(type: Int,result: ScanResult) {runCatching {report(result.device,true)}.onFailure{failed("Accès BLE interrompu : ${it.message}")}}
  override fun onScanFailed(code:Int) {failed("Recherche BLE impossible (code $code). Les appareils associés restent sélectionnables.")}
 }
 private fun report(d: BluetoothDevice,ble: Boolean) = found(Device(d.address,d.name?:"Appareil sans nom",d.bondState==BluetoothDevice.BOND_BONDED,ble))
 fun scan() {
  val a=adapter ?: error("Bluetooth absent")
  check(a.isEnabled) { "Activez le Bluetooth dans les paramètres Android" }
  if(!registered) {
   val f=IntentFilter(BluetoothDevice.ACTION_FOUND).apply {addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)}
   if(Build.VERSION.SDK_INT>=33) context.registerReceiver(receiver,f,Context.RECEIVER_EXPORTED) else context.registerReceiver(receiver,f)
   registered=true
  }
  a.bondedDevices.forEach { report(it,false) }
  if(!a.startDiscovery())failed("Recherche Bluetooth Classic indisponible. Vérifiez le Bluetooth et la localisation sur Android 10/11.")
  a.bluetoothLeScanner?.startScan(bleCallback)
 }
 fun stop() { runCatching { adapter?.cancelDiscovery();adapter?.bluetoothLeScanner?.stopScan(bleCallback) } }
 fun dispose() { stop();if(registered) {context.unregisterReceiver(receiver);registered=false} }
 fun pair(address: String) = adapter?.getRemoteDevice(address)?.createBond()
}

@SuppressLint("MissingPermission")
class ClassicTransport(private val adapter: BluetoothAdapter,private val address: String): ElmTransport {
 @Volatile private var socket: BluetoothSocket?=null
 private val executor=Executors.newSingleThreadExecutor()
 suspend fun connect() = withContext(Dispatchers.IO) {
  adapter.cancelDiscovery()
  val s=adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
  socket=s
  val future=executor.submit { s.connect() }
  try { future.get(15,TimeUnit.SECONDS) } catch(e:Exception) { close();throw IllegalStateException("Connexion SPP impossible (15 s). Appairez NEXAS dans Android.",e) }
 }
 override suspend fun exchange(command: String): String = withContext(Dispatchers.IO) {
  val s=socket ?: error("Boîtier déconnecté")
  val future=executor.submit<String> {
   s.outputStream.write((command+"\r").toByteArray(Charsets.US_ASCII));s.outputStream.flush()
   val output=StringBuilder()
   while(output.length < 65536) {
    val b=s.inputStream.read()
    check(b>=0) { "Connexion Bluetooth interrompue" }
    output.append(b.toChar())
    if(b==62) return@submit output.toString()
   }
   error("Réponse ELM trop longue")
  }
  try { future.get(if(command=="0100") 30L else 12L,TimeUnit.SECONDS) }
  catch(e:Exception) { close();throw IllegalStateException("Communication interrompue ou délai dépassé : $command",e) }
 }
 override fun close() { runCatching { socket?.close() };socket=null;executor.shutdownNow() }
}
