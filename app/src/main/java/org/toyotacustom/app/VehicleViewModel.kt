package org.toyotacustom.app

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.toyotacustom.core.*
import java.util.UUID

data class ScreenState(val status:String="Déconnecté",val connected:Boolean=false,val busy:Boolean=false,
 val simulation:Boolean=false,val vin:String?=null,val adapter:String="Non connecté",val protocol:String="Non détecté",
 val supported:Set<Int> = emptySet(), val values:Map<Int,Double> = emptyMap(),val codes:List<String>?=null,
 val devices:List<Device> = emptyList(),val logs:List<String> = emptyList(),val custom:Map<String,String> = emptyMap(),
 val canHeaders:Boolean=false,val canIds:Set<String> = emptySet(),val traces:List<ExchangeTrace> = emptyList(),
 val deviceAddress:String?=null,val autoConnect:Boolean=true,val savedDeviceName:String?=null,val previousReport:String?=null,
 val sessionId:String?=null,val sessionStartedAt:Long?=null,val ecuIdentities:List<EcuIdentity> = emptyList(),val bleInventory:BleInventory?=null)

class VehicleViewModel(app:Application):AndroidViewModel(app) {
 private val db=Room.databaseBuilder(app,LocalDatabase::class.java,"toyota-custom.db").addMigrations(DatabaseMigrations.FROM_1_TO_2).build()
 private val store=db.store()
 val history=store.history().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val savedReports=store.reports().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val preferences=app.getSharedPreferences("connection",0)
 val state=MutableStateFlow(ScreenState(autoConnect=preferences.getBoolean("autoConnect",true),savedDeviceName=preferences.getString("deviceName",null)))
 private var session:ElmSession?=null
 private var sessionId=""
 private var connectionJob:Job?=null
 private var pollJob:Job?=null
 private var operationJob:Job?=null
 private var lastDevice:Device?=null
 private var generation=0
 private var scanJob:Job?=null
 private var bleJob:Job?=null
 private val reportMutex=Mutex()
 val bluetooth=BluetoothDevices(app,found={ d -> state.update { s->s.copy(devices=(s.devices.filterNot { it.address==d.address }+d.copy(ble=d.ble || s.devices.any{it.address==d.address && it.ble})).sortedBy { !it.name.contains("NEX",true) }) } },failed={ message->
  state.update{it.copy(status=message)};log(message)
 })
 var definitions=emptyList<Definition>();private set
 init { viewModelScope.launch {
  val text=app.assets.open("toyota-catalog.json").bufferedReader().use{it.readText()}
  val array=JSONArray(text)
  val stored=(0 until array.length()).map { StoredDefinition(array.getJSONObject(it).getString("id"),array.getJSONObject(it).toString()) }
  store.definition(stored)
  definitions=store.definitions().map { row->val o=org.json.JSONObject(row.json);Definition(o.getString("id"),o.getString("name"),o.getString("category"),o.getString("description"),source=o.getString("source")) }
  state.update { if(it.connected || it.busy) it else it.copy(status="Prêt — choisissez un appareil ou la simulation") }
 } }
 private fun log(line:String) {state.update { it.copy(logs=(it.logs+("${java.time.Instant.now()} $line")).takeLast(400)) }}
 fun scan() {
  if(state.value.connected || state.value.busy) return
  scanJob?.cancel()
  runCatching {
   state.update {it.copy(devices=emptyList(),status="Recherche Bluetooth pendant 12 secondes")}
   bluetooth.scan()
   scanJob=viewModelScope.launch {
    delay(12000);bluetooth.stop()
    val saved=preferences.getString("deviceAddress",null)
    val match=state.value.devices.singleOrNull {it.address==saved && it.bonded}
    if(state.value.autoConnect && !state.value.connected && !state.value.busy && match!=null) connect(match)
    else if(!state.value.connected && !state.value.busy)state.update{it.copy(status="Recherche terminée : ${it.devices.size} appareil(s)")}
   }
  }.onFailure { error(it) }
 }
 fun setAutoConnect(enabled:Boolean) {
  preferences.edit().putBoolean("autoConnect",enabled).apply()
  state.update{it.copy(autoConnect=enabled)}
 }
 fun inspectBle(d:Device) {
  if(state.value.connected || state.value.busy)return
  scanJob?.cancel();bluetooth.stop()
  val inspectionGeneration=generation
  state.update{it.copy(busy=true,bleInventory=null,status="Inventaire des services BLE… (20 s maximum)")}
  bleJob=viewModelScope.launch {
   try {
    val inventory=BleInspector(getApplication()).inspect(bluetooth.adapter?:error("Bluetooth absent"),d.address)
    if(generation==inspectionGeneration)state.update{it.copy(bleInventory=inventory,status="Inventaire BLE terminé : ${inventory.services.size} service(s). Transport ELM non validé.")}
   }catch(e:TimeoutCancellationException){if(generation==inspectionGeneration)error(IllegalStateException("Inventaire BLE : délai de 20 secondes dépassé"))}
   catch(e:CancellationException){throw e}
   catch(e:Exception){if(generation==inspectionGeneration)error(e)}
   finally{if(generation==inspectionGeneration)state.update{it.copy(busy=false)}}
  }
 }
 fun pair(d:Device) {runCatching {bluetooth.pair(d.address);state.update{it.copy(status="Terminez l’appairage Android puis relancez la recherche")}}.onFailure{error(it)}}
 private fun error(e:Throwable) {state.update { it.copy(status=e.message?:"Erreur de communication",busy=false) };log("Erreur : ${e.message}")}
 fun connect(d:Device?=null,simulate:Boolean=false) {
  disconnect()
  lastDevice=d
  val old=state.value
  val previousReport=if(old.traces.isNotEmpty())TechnicalReport.create(old)else old.previousReport
  sessionId=UUID.randomUUID().toString()
  state.update {ScreenState(devices=it.devices,autoConnect=it.autoConnect,savedDeviceName=it.savedDeviceName,simulation=simulate,deviceAddress=d?.address,previousReport=previousReport,sessionId=sessionId,sessionStartedAt=System.currentTimeMillis())}
  val connectionGeneration=generation
  connectionJob=viewModelScope.launch {
   persistReport(old)
   var attempt=0
   while(isActive && attempt<3) {
    var attemptSession:ElmSession?=null
    state.update {it.copy(busy=true,status=if(attempt==0) "Connexion…" else "Reconnexion ${attempt}/2…",simulation=simulate)}
    try {
     bluetooth.stop()
     val transport=if(simulate) SimTransport() else ClassicTransport(bluetooth.adapter?:error("Bluetooth absent"),d?.address?:error("Appareil absent"))
     val s=ElmSession(transport,{line->if(generation==connectionGeneration)log(line)},{command,reply->
      if(generation==connectionGeneration) {
       val ids=if(command in CommandPolicy.reads || command=="04")runCatching{Obd.messages(reply,command).mapNotNull{it.canId}.toSet()}.getOrDefault(emptySet())else emptySet()
       state.update{it.copy(canIds=it.canIds+ids,traces=(it.traces+ExchangeTrace(System.currentTimeMillis(),command,reply)).takeLast(200))}
      }
     });attemptSession=s;session=s
     if(transport is ClassicTransport) transport.connect()
     val info=s.initialize()
     val first=s.read("0100")
     require(Obd.positive(first,"0100",0x41,0).isNotEmpty()) {"Aucune communication OBD avec le véhicule"}
     val supported=Obd.supported(first,0).toMutableSet()
     for(base in listOf(0x20,0x40,0x60)) {if(base in supported) supported.addAll(Obd.supported(s.read("01%02X".format(base)),base)) else break}
     val protocol=s.read("ATDP").replace(">","").trim()
     val protocolNumber=s.read("ATDPN").replace(">","").trim().removePrefix("A")
     require(protocolNumber in setOf("6","7","8","9")) {"Cette version ne décode que l’OBD sur CAN ISO 15765-4. Protocole : $protocol"}
     val headers=s.enableCanHeaders()
     val vin=Obd.vin(s.read("0902"))
     ensureActive()
     if(!simulate && d!=null)preferences.edit().putString("deviceAddress",d.address).putString("deviceName",d.name).apply()
     state.update{it.copy(connected=true,busy=false,canHeaders=headers,deviceAddress=d?.address,adapter=info.replace(">","").trim(),supported=supported,protocol=protocol,vin=vin,savedDeviceName=preferences.getString("deviceName",null),status=if(simulate) "SIMULATION — aucune connexion véhicule" else if(Obd.toyota(vin)) "Toyota identifiée par WMI — modèle exact non confirmé" else "VIN Toyota non confirmé : diagnostic bloqué")}
     persistReport(state.value)
     break
    } catch(e:CancellationException) {attemptSession?.close();throw e}
    catch(e:Exception) {
     attemptSession?.close()
     if(generation!=connectionGeneration) return@launch
     if(session===attemptSession)session=null;error(e);attempt++
     if(!simulate && attempt<3) delay(2000L*attempt) else break
    }
   }
  }
 }
 fun disconnect() {
  generation++
  scanJob?.cancel();bleJob?.cancel();bluetooth.stop()
  connectionJob?.cancel();pollJob?.cancel();operationJob?.cancel();session?.close();session=null
  state.update {it.copy(status="Déconnecté — dernière session conservée pour export",connected=false,busy=false,values=emptyMap(),codes=null,custom=emptyMap())}
 }
 private fun allowed()=state.value.connected&&(state.value.simulation||Obd.toyota(state.value.vin))
 private suspend fun persistReport(snapshot:ScreenState):Boolean = reportMutex.withLock {
  val id=snapshot.sessionId?:return@withLock false
  if(snapshot.traces.isEmpty())return@withLock false
  try {
   store.saveReport(SavedReport(id,snapshot.sessionStartedAt?:System.currentTimeMillis(),snapshot.simulation,TechnicalReport.create(snapshot)))
   true
  }catch(e:CancellationException){throw e}catch(e:Exception){log("Sauvegarde du rapport impossible : ${e.message}");false}
 }
 fun saveReport() {
  val snapshot=state.value
  viewModelScope.launch {
   val saved=persistReport(snapshot)
   if(state.value.sessionId==snapshot.sessionId)state.update{it.copy(status=if(saved)"Rapport sauvegardé localement (VIN masqué)"else "Rapport non sauvegardé : aucune session ou erreur de stockage")}
  }
 }
 fun identifyEcus()=operation {s->
  require(state.value.canHeaders){"En-têtes CAN vérifiés nécessaires pour attribuer les informations"}
  val ecus=VehicleInformation.collect(s::read)
  state.update{it.copy(ecuIdentities=ecus,status="Informations OBD reçues de ${ecus.size} répondant(s). Compatibilité Toyota non déduite.")}
  persistReport(state.value)
 }
 private fun operation(block:suspend (ElmSession)->Unit) {
  if(state.value.busy || !allowed()) return
  val s=session?:return
  val operationGeneration=generation
  state.update{it.copy(busy=true)}
  operationJob=viewModelScope.launch {
   try{block(s)}catch(e:CancellationException){throw e}catch(e:Exception){
    if(generation!=operationGeneration) return@launch
    error(e)
    // A timed-out transport is closed by the driver. Never reuse its stream.
    if(e is IllegalStateException){
     s.close();session=null
     val reconnectDevice=lastDevice
     val failedGeneration=generation
     state.update{it.copy(connected=false,values=emptyMap(),supported=emptySet(),vin=null)}
     if(reconnectDevice!=null) viewModelScope.launch {delay(2000);if(session==null && generation==failedGeneration)connect(reconnectDevice)}
    }
   }finally{if(generation==operationGeneration)state.update{it.copy(busy=false)}}
  }
 }
 fun diagnostic()=operation { s ->
  val codes=mutableListOf<String>()
  var answered=0
  val unavailable=mutableListOf<String>()
  for((cmd,label) in listOf("03" to "Mémorisé","07" to "En attente","0A" to "Permanent")) {
   val reply=s.read(cmd)
   if(reply.uppercase().contains("NO DATA")) {log("$label : réponse indisponible");unavailable.add(label);continue}
   codes.addAll(Obd.dtcs(reply,cmd).map{"$label : $it"})
   answered++
  }
  require(answered>0){"Aucun mode DTC n’a répondu : absence de défaut non établie"}
  state.update{it.copy(codes=codes,status="Lecture terminée : $answered mode(s) répondu(s). Indisponible : ${unavailable.joinToString().ifEmpty{"aucun"}}")}
  persistReport(state.value)
 }
 fun refresh()=operation { s ->
  val values=mutableMapOf<Int,Double>()
  for(pid in listOf(4,5,12,13,17,66,91).filter{it in state.value.supported}) {
   Obd.value(s.read("01%02X".format(pid)),pid)?.let{values[pid]=it}
  }
  state.update{it.copy(values=values,status="Mesures actualisées à ${java.time.LocalTime.now().withNano(0)}")}
 }
 fun polling(enable:Boolean) {pollJob?.cancel();if(enable)pollJob=viewModelScope.launch{while(isActive && state.value.connected){refresh();delay(3000)}}}
 fun clear(confirmed:Boolean)=operation { s ->
  require(0x0C in state.value.supported && 0x0D in state.value.supported) {"Régime et vitesse nécessaires à l’effacement"}
  val sim=state.value.simulation
  store.add(History(session=sessionId,parameter="DTC standard",before=state.value.codes?.joinToString()?:"Non lu",after="",result="Demande confirmée ; effet non encore vérifié",simulated=sim))
  s.clearStandard(confirmed,Obd.toyota(state.value.vin))
  val remaining=Obd.dtcs(s.read("03"))
  state.update{it.copy(codes=remaining.map{"Mémorisé : $it"},status=if(remaining.isEmpty()) "Effacement confirmé pour les défauts mémorisés. Les permanents peuvent rester." else "Réponse positive, mais défauts toujours présents")}
  store.add(History(session=sessionId,parameter="DTC standard",before="Voir entrée précédente",after=remaining.joinToString(),result=state.value.status,simulated=sim))
  persistReport(state.value)
 }
 // No vehicle customization transport exists until ECU-specific evidence is available.
 fun simulateChange(id:String,value:String,restore:Boolean=false) {
  if(!state.value.simulation || !state.value.connected) return
  viewModelScope.launch {
   val before=state.value.custom[id]?:"Normale (fictive)"
   store.add(History(session="simulation",parameter=id,before=before,after=value,result=if(restore)"Restauration simulée" else "Modification simulée",simulated=true))
   state.update{it.copy(custom=it.custom+(id to value),status="SIMULATION : valeur relue dans le simulateur uniquement")}
  }
 }
 override fun onCleared() {disconnect();bluetooth.dispose();db.close()}
}
