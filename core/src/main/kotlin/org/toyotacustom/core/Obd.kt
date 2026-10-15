package org.toyotacustom.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ElmTransport { suspend fun exchange(command: String): String; fun close() }
enum class Validation { DOCUMENTED, TO_VERIFY, READ_VALIDATED, WRITE_VALIDATED, INCOMPATIBLE }
data class Definition(val id: String, val name: String, val category: String, val description: String,
 val models: List<String> = emptyList(), val years: List<Int> = emptyList(), val ecu: String? = null,
 val protocol: String? = null, val readCommand: String? = null, val writeCommand: String? = null,
 val values: List<String> = emptyList(), val conditions: List<String> = emptyList(),
 val risk: String = "Non évalué", val source: String, val validation: Validation = Validation.TO_VERIFY)

// Only these public, non-OEM commands may ever reach a real transport.
object CommandPolicy {
 val init = listOf("ATZ", "ATE0", "ATL0", "ATS1", "ATH0", "ATSP0", "ATAT1", "ATSTFF")
 val info = setOf("ATI", "ATDP", "ATDPN", "ATRV")
 val reads = setOf("0100", "0120", "0140", "0160", "0101", "0104", "0105", "010C", "010D", "0111", "0142", "015B", "0902", "03", "07", "0A")
 fun readAllowed(command: String) = command in init || command in info || command in reads
}

object Obd {
 // ELM automatic CAN formatting, headers disabled. Indexed ELM messages are joined,
 // separate ECU replies stay separate. Raw ISO-TP CAN frames are intentionally rejected.
 fun payloads(raw: String, command: String): List<List<Int>> {
  val results = mutableListOf<List<Int>>()
  var indexed = mutableListOf<Int>()
  var nextIndex = 0
  var declaredLength: Int? = null
  fun flush() {
   if (indexed.isNotEmpty()) {
    val length=declaredLength ?: indexed.size
    require(indexed.size>=length) { "Message ELM tronqué" }
    results.add(indexed.take(length));declaredLength=null
   }
   indexed.clear(); nextIndex = 0
  }
  raw.replace('>', '\n').split(Regex("[\\r\\n]+" )).forEach { line ->
   val s = line.trim().uppercase()
   if (s.isEmpty() || s.replace(" ", "") == command || s.startsWith("SEARCHING")) return@forEach
   if(s.matches(Regex("[0-9A-F]{3}"))) {flush();declaredLength=s.toInt(16);return@forEach}
   val match = Regex("^([0-9A-F]+):\\s*(.*)$").matchEntire(s)
   val bytesText = match?.groupValues?.get(2) ?: s
   val compact = bytesText.replace(" ", "")
   if (!compact.matches(Regex("(?:[0-9A-F]{2})+"))) return@forEach
   val bytes = compact.chunked(2).map { it.toInt(16) }
   if (match != null) {
    val index = match.groupValues[1].toInt(16)
    if (index == 0) flush()
    require(index == nextIndex) { "Trame ELM incomplète ou désordonnée" }
    indexed.addAll(bytes); nextIndex++
   } else { flush(); results.add(bytes) }
  }
  flush()
  return results
 }
 fun positive(raw: String, command: String, service: Int, pid: Int? = null): List<List<Int>> =
  payloads(raw, command).filter { it.firstOrNull() == service && (pid == null || it.getOrNull(1) == pid) }
 fun vin(raw: String): String? = positive(raw,"0902",0x49,2).mapNotNull { b ->
  if(b.getOrNull(2)!=1 || b.size<20) return@mapNotNull null
  val v = b.drop(3).take(17).map { it.toChar() }.joinToString("")
  v.takeIf { it.matches(Regex("[A-HJ-NPR-Z0-9]{17}")) }
 }.distinct().singleOrNull()
 fun toyota(vin: String?) = vin?.take(3) in setOf("JTD", "JTN", "SB1", "NMT", "VNK", "JT2", "4T1", "5TD", "2T1")
 fun supported(raw: String, base: Int): Set<Int> {
  val cmd = "01%02X".format(base)
  return positive(raw,cmd,0x41,base).flatMap { b ->
   if (b.size < 6) emptyList() else (1..32).filter { n -> b[2+(n-1)/8] and (1 shl (7-(n-1)%8)) != 0 }.map { base+it }
  }.toSet()
 }
 fun value(raw: String, pid: Int): Double? {
  val replies = positive(raw,"01%02X".format(pid),0x41,pid)
  // Ambiguous replies from multiple ECUs must never be used as a safety check.
  val values = replies.mapNotNull { b ->
   val a = b.getOrNull(2) ?: return@mapNotNull null
   when (pid) {
    0x04,0x11 -> a * 100.0/255
    0x05 -> a - 40.0
    0x0D -> a.toDouble()
    0x0C -> b.getOrNull(3)?.let { (256*a+it)/4.0 }
    0x42 -> b.getOrNull(3)?.let { (256*a+it)/1000.0 }
    0x5B -> a*100.0/255
    else -> null
   }
  }
  return values.takeIf { it.size == replies.size }?.distinct()?.singleOrNull()
 }
 fun dtcs(raw: String, command: String = "03"): List<String> {
  val service = command.toInt(16)+0x40
  val replies = positive(raw,command,service)
  require(replies.isNotEmpty()) { "Aucune réponse positive au diagnostic" }
  return replies.flatMap { bytes ->
   // ELM CAF1 strips CAN PCI; each positive payload starts with its service byte.
   val count=bytes.getOrNull(1) ?: error("Nombre de DTC absent")
   require(bytes.size >= 2+count*2) { "Liste de DTC tronquée" }
   bytes.drop(2).take(count*2).chunked(2).mapNotNull { pair ->
    require(pair.size == 2 || pair[0] == 0) { "Réponse DTC tronquée" }
    if (pair.size != 2 || pair.all { it == 0 }) null else {
     val a=pair[0];val b=pair[1]
     "%c%d%X%02X".format("PCBU"[a shr 6],(a shr 4) and 3,a and 15,b)
    }
   }
  }.distinct()
 }
}

class ElmSession(private val transport: ElmTransport, private val log: (String)->Unit = {}) {
 private val mutex = Mutex()
 suspend fun read(command: String): String = mutex.withLock {
  require(CommandPolicy.readAllowed(command)) { "Commande absente de la liste de lecture autorisée" }
  send(command)
 }
 private suspend fun send(command: String): String {
  log("> $command")
  val reply = transport.exchange(command)
  log("< $reply")
  require(reply.contains('>')) { "Invite ELM manquante : reconnectez le boîtier" }
  require(listOf("?","UNABLE TO CONNECT","CAN ERROR","BUS ERROR","STOPPED","BUFFER FULL").none { reply.uppercase().contains(it) }) { "Erreur ELM : $reply" }
  return reply
 }
 suspend fun initialize(): String {
  for (cmd in CommandPolicy.init) {
   val raw=read(cmd)
   require(cmd == "ATZ" || raw.uppercase().contains("OK")) { "Initialisation refusée : $cmd" }
  }
  return read("ATI")
 }
 // Erasing emission DTCs is separate from customization and always needs fresh readings.
 suspend fun clearStandard(confirmed: Boolean, toyotaIdentified: Boolean): String = mutex.withLock {
  require(confirmed && toyotaIdentified) { "Confirmation et identification Toyota requises" }
  require(Obd.value(send("010D"),0x0D) == 0.0) { "Immobilisation non confirmée par OBD" }
  require(Obd.value(send("010C"),0x0C) == 0.0) { "Le moteur thermique doit être arrêté" }
  val reply=send("04")
  require(Obd.positive(reply,"04",0x44).isNotEmpty()) { "Effacement non confirmé" }
  reply
 }
 fun close() = transport.close()
}

class SimTransport : ElmTransport {
 private var cleared=false
 override suspend fun exchange(command: String): String = when(command) {
  "ATZ", "ATI" -> "Toyota Custom — simulateur indépendant>"
  "ATDP" -> "AUTO, ISO 15765-4 (CAN 11/500)>"
  "ATDPN" -> "A6>"
  "ATRV" -> "12.6V>"
  "0100" -> "41 00 98 18 80 01>"
  "0120" -> "41 20 00 00 00 01>"
  "0140" -> "41 40 40 00 00 00>"
  "0902" -> "014\r0: 49 02 01 4A 54 44\r1: 41 41 41 41 41 41 41\r2: 41 30 30 30 30 30 31>"
  "010C" -> "41 0C 00 00>"
  "010D" -> "41 0D 00>"
  "0105" -> "41 05 70>"
  "0104" -> "41 04 30>"
  "0111" -> "41 11 00>"
  "0142" -> "41 42 31 38>"
  "03" -> if(cleared) "43 00>" else "43 01 01 33>"
  "07", "0A" -> "%02X 00>".format(command.toInt(16)+0x40)
  "04" -> {cleared=true;"44>"}
  else -> if (command in CommandPolicy.init) "OK>" else "NO DATA>"
 }
 override fun close() {}
}
