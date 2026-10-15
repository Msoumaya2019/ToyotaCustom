package org.toyotacustom.core

data class ObdMessage(val canId: String?, val data: List<Int>)

/** Receive-side, normal-addressed ISO-TP on classic CAN. ELM still sends flow control.
 * Headers on, DLC off. No extended addressing, CAN FD, reordering or partial messages.
 */
object CanIsoTp {
 private data class Pending(val length: Int, val data: MutableList<Int>, var sequence: Int = 1)

 fun decode(raw: String, command: String): List<ObdMessage>? {
  val frames = mutableListOf<Pair<String, List<Int>>>()
  var unframed = false
  for (line in raw.uppercase().replace('>', '\n').split(Regex("[\\r\\n]+"))) {
   val text = line.trim()
   val compact = text.replace(Regex("\\s+"), "")
   if (compact == command || compact.isEmpty() || text.startsWith("SEARCHING")) continue
   val parts=text.split(Regex("\\s+"))
   val first=parts.first()
   val idLength = when {
    parts.size>1 && first.matches(Regex("[0-9A-F]{3}")) && first.toInt(16)<=0x7FF -> 3
    first.matches(Regex("[0-9A-F]{8}")) && first.toLong(16)<=0x1FFFFFFF -> 8
    parts.size>=4 && parts.take(4).all{it.matches(Regex("[0-9A-F]{2}"))} && parts.take(4).joinToString("").toLong(16)<=0x1FFFFFFF -> 8
    compact.length >= 7 && compact.length % 2 == 1 && compact.take(3).toIntOrNull(16)?.let { it <= 0x7FF } == true -> 3
    compact.length >= 12 && compact.length % 2 == 0 && compact.take(8).toLongOrNull(16)?.let { it <= 0x1FFFFFFF } == true -> 8
    else -> 0
   }
   if (idLength == 0) {
    if (compact.matches(Regex("[0-9A-F]+")) || text.contains(':')) unframed = true
    continue
   }
   val body = compact.drop(idLength)
   require(body.matches(Regex("(?:[0-9A-F]{2})+"))) { "Octets CAN invalides" }
   val bytes = body.chunked(2).map { it.toInt(16) }
   require(bytes.size in 2..8) { "Longueur CAN classique invalide (DLC doit être désactivé)" }
   frames.add(compact.take(idLength) to bytes)
  }
  if (frames.isEmpty()) return null
  require(!unframed) { "Réponses CAN et réponses sans en-tête mélangées" }
  val pending = mutableMapOf<String, Pending>()
  val result = mutableListOf<ObdMessage>()
  for ((id, bytes) in frames) {
   when (bytes[0] shr 4) {
    0 -> {
     require(id !in pending) { "Message CAN interrompu pour $id" }
     val length = bytes[0] and 15
     require(length in 1..7 && bytes.size >= length + 1) { "Trame CAN simple tronquée pour $id" }
     result.add(ObdMessage(id, bytes.drop(1).take(length)))
    }
    1 -> {
     require(id !in pending && bytes.size == 8) { "Première trame ISO-TP invalide pour $id" }
     val length = ((bytes[0] and 15) shl 8) + bytes[1]
     require(length in 8..4095) { "Longueur ISO-TP non prise en charge" }
     pending[id] = Pending(length, bytes.drop(2).toMutableList())
    }
    2 -> {
     val message = requireNotNull(pending[id]) { "Trame ISO-TP sans début pour $id" }
     require(bytes[0] and 15 == message.sequence) { "Séquence ISO-TP incorrecte pour $id" }
     val take = minOf(7, message.length - message.data.size)
     require(bytes.size >= take + 1) { "Trame ISO-TP consécutive tronquée pour $id" }
     message.data.addAll(bytes.drop(1).take(take))
     message.sequence = (message.sequence + 1) and 15
     if (message.data.size == message.length) {
      result.add(ObdMessage(id, message.data.toList()))
      pending.remove(id)
     }
    }
    else -> throw IllegalArgumentException("Type ISO-TP non pris en charge pour $id")
   }
  }
  require(pending.isEmpty()) { "Message ISO-TP incomplet : ${pending.keys.joinToString()}" }
  return result
 }
}
