package org.toyotacustom.core

data class LiveReading(val pid:Int,val canId:String?,val value:Double?,val issue:String?=null)

/** Original decoder for the small, explicitly supported standard mode-01 set. */
object LiveMeasurements {
 val pids=setOf(0x04,0x05,0x0C,0x0D,0x11,0x42,0x5B)
 fun decode(raw:String,pid:Int):List<LiveReading> {
  require(pid in pids){"PID sans décodeur de mesure pris en charge dans cette version"}
  val messages=Obd.messages(raw,"01%02X".format(pid))
  if(messages.isEmpty())return listOf(LiveReading(pid,null,null,"Aucune réponse de mesure reçue"))
  return messages.groupBy{it.canId}.map{(id,replies)->
   val values=mutableListOf<Double>()
   val issues=mutableListOf<String>()
   for(message in replies) {
    val b=message.data
    when {
     b.take(2)==listOf(0x7F,1)->issues.add(if(b.size==3)"Réponse négative OBD : NRC %02X".format(b[2])else "Réponse négative OBD mal formée")
     b.take(2)!=listOf(0x41,pid)->issues.add("Service ou PID de réponse inattendu")
     b.size!=(if(pid in setOf(0x0C,0x42))4 else 3)->issues.add("Longueur de mesure inattendue")
     else->{val a=b[2];values.add(when(pid){
      0x04,0x11,0x5B->a*100.0/255
      0x05->a-40.0
      0x0D->a.toDouble()
      0x0C->(256*a+b[3])/4.0
      0x42->(256*a+b[3])/1000.0
      else->error("PID non pris en charge")
     })}
    }
   }
   if(values.distinct().size>1)issues.add("Valeurs contradictoires pour une même source")
   if(issues.isNotEmpty())LiveReading(pid,id,null,issues.distinct().joinToString(" ; "))
   else LiveReading(pid,id,values.distinct().singleOrNull(),if(values.isEmpty())"Aucune mesure valide"else null)
  }
 }
 /** Conservative consensus for safety checks, never an arbitrary selected ECU. */
 fun consensus(readings:List<LiveReading>):Double? =
  if(readings.isEmpty() || readings.any{it.value==null || it.issue!=null} || readings.map{it.pid}.distinct().size!=1)null
  else readings.map{it.value}.distinct().singleOrNull()
}
