package org.toyotacustom.core

data class EcuIdentity(val canId: String, val advertisedInfo: Set<Int> = emptySet(),
 val declaredName: String? = null, val calibrationIds: List<String> = emptyList(),
 val verificationNumbers: List<String> = emptyList(), val issues: List<String> = emptyList())

/** Classic OBD CAN mode 09 only. Declared information does not establish OEM compatibility. */
object VehicleInformation {
 fun supported(raw: String): Map<String, Set<Int>> {
  val result=mutableMapOf<String,Set<Int>>()
  for(message in Obd.messages(raw,"0900")) {
   val b=message.data
   if(b.take(2)!=listOf(0x49,0)) continue
   val id=requireNotNull(message.canId){"En-têtes CAN nécessaires pour attribuer les informations"}
   require(b.size==6){"Bitmap du mode 09 de longueur inattendue"}
   val bits=(1..32).filter{n->b[2+(n-1)/8] and (1 shl(7-(n-1)%8))!=0}.toSet()
   require(id !in result || result[id]==bits){"Capacités mode 09 contradictoires pour $id"}
   result[id]=bits
  }
  require(result.isNotEmpty()){"Aucune réponse positive à 0900 ; informations ECU non établies"}
  return result
 }
 fun records(message: ObdMessage, pid: Int): List<String> {
  val b=message.data
  require(b.take(2)==listOf(0x49,pid)){"Réponse mode 09 inattendue"}
  val width=when(pid){4->16;6->4;10->20;else->throw IllegalArgumentException("Information non autorisée")}
  val count=b.getOrNull(2)?:throw IllegalArgumentException("Nombre d’éléments absent")
  require(count in 1..255 && (pid!=10 || count==1)){"Nombre d’éléments invalide"}
  require(b.size==3+count*width){"Information mode 09 tronquée ou de longueur inattendue"}
  return b.drop(3).chunked(width).map{record->
   if(pid==6) record.joinToString(""){"%02X".format(it)} else {
    require(record.all{it==0 || it in 32..126}){"Texte ECU non ASCII : interprétation refusée"}
    val text=record.map{it.toChar()}.joinToString("").trim('\u0000',' ')
    require(text.isNotEmpty()){"Identifiant vide"}
    text.replace("\u0000","\\x00")
   }
  }
 }
 suspend fun collect(read: suspend (String)->String): List<EcuIdentity> {
  val capabilities=supported(read("0900"))
  val ecus=capabilities.mapValues{(id,bits)->EcuIdentity(id,bits)}.toMutableMap()
  for(pid in listOf(4,6,10)) {
   val expected=capabilities.filterValues{pid in it}.keys
   if(expected.isEmpty())continue
   val command="09%02X".format(pid)
   val raw=read(command) // Transport failures propagate; no writes or replay are performed.
   val messages=try{Obd.messages(raw,command)}catch(e:IllegalArgumentException){
    expected.forEach{id->ecus[id]=ecus.getValue(id).let{it.copy(issues=it.issues+"$command : ${e.message}")}}
    continue
   }
   val answered=mutableSetOf<String>()
   val seen=mutableMapOf<String,List<Int>>()
   val conflicted=mutableSetOf<String>()
   for(message in messages) {
    val id=message.canId?:continue
    if(message.data.take(2)!=listOf(0x49,pid))continue
    answered.add(id)
    if(id in conflicted)continue
    var ecu=ecus[id]?:EcuIdentity(id,issues=listOf("Aucune capacité reçue pour cette adresse dans 0900"))
    if(id !in expected)ecu=ecu.copy(issues=ecu.issues+"$command : réponse non annoncée dans 0900")
    if(id in seen && seen[id]!=message.data) {
     conflicted.add(id)
     ecu=when(pid){4->ecu.copy(calibrationIds=emptyList());6->ecu.copy(verificationNumbers=emptyList());else->ecu.copy(declaredName=null)}
     ecus[id]=ecu.copy(issues=ecu.issues+"$command : réponses contradictoires, valeur écartée")
     continue
    }
    seen[id]=message.data
    try {
     val values=records(message,pid)
     ecu=when(pid){4->ecu.copy(calibrationIds=values);6->ecu.copy(verificationNumbers=values);else->ecu.copy(declaredName=values.single())}
    }catch(e:IllegalArgumentException){ecu=ecu.copy(issues=ecu.issues+"$command : ${e.message}")}
    ecus[id]=ecu
   }
   (expected-answered).forEach{id->ecus[id]=ecus.getValue(id).let{it.copy(issues=it.issues+"$command : aucune réponse positive attribuable")}}
  }
  return ecus.values.map{ecu->
   if(ecu.calibrationIds.isNotEmpty() && ecu.verificationNumbers.isNotEmpty() && ecu.calibrationIds.size!=ecu.verificationNumbers.size)
    ecu.copy(issues=ecu.issues+"Nombres CALID/CVN différents : correspondance non établie")else ecu
  }.sortedBy{it.canId}
 }
}
