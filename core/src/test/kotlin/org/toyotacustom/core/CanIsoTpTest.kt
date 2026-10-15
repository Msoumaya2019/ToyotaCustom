package org.toyotacustom.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CanIsoTpTest {
 @Test fun singleFrameDropsPaddingAndKeepsSource() {
  assertEquals(listOf(ObdMessage("7E8",listOf(0x41,0x0D,0))),Obd.messages("7E8 03 41 0D 00 AA AA AA AA>","010D"))
  assertEquals(0.0,Obd.value("7E8 03 41 0D 00 00 00 00 00>",13)!!,0.0)
 }
 @Test fun accepts29bitHeadersSpacedOrCompact() {
  val spaced=Obd.messages("18 DA F1 10 03 41 0D 00 00 00 00 00>","010D")
  assertEquals("18DAF110",spaced.single().canId)
  assertEquals(spaced,Obd.messages("18DAF11003410D0000000000>","010D"))
 }
 @Test fun assemblesVinAcrossInterleavedEcus() {
  val raw="7E8 10 14 49 02 01 4A 54 44\r7E9 03 41 0D 00 00 00 00 00\r7E8 21 41 41 41 41 41 41 41\r7E8 22 41 30 30 30 30 30 31>"
  assertEquals(setOf("7E8","7E9"),Obd.messages(raw,"0902").mapNotNull{it.canId}.toSet())
  assertEquals("JTDAAAAAAAA000001",Obd.vin(raw))
 }
 @Test fun supportsConsecutiveFrameSequenceWrap() {
  val data=(0..119).toList()
  val frames=mutableListOf("7E8 10 78 00 01 02 03 04 05")
  for(index in 0..16) {
   val part=data.drop(6+index*7).take(7)
   frames.add("7E8 %02X ".format(0x20 or ((index+1) and 15))+(part+List(7-part.size){0}).joinToString(" "){"%02X".format(it)})
  }
  assertEquals(data,Obd.messages(frames.joinToString("\r")+">","03").single().data)
 }
 @Test(expected=IllegalArgumentException::class) fun missingFinalFrameIsRejected() {
  Obd.messages("7E8 10 14 49 02 01 4A 54 44\r7E8 21 41 41 41 41 41 41 41>","0902")
 }
 @Test(expected=IllegalArgumentException::class) fun wrongSequenceIsRejected() {
  Obd.messages("7E8 10 14 49 02 01 4A 54 44\r7E8 22 41 41 41 41 41 41 41>","0902")
 }
 @Test(expected=IllegalArgumentException::class) fun orphanConsecutiveFrameIsRejected() {
  Obd.messages("7E8 21 41 41 41 41 41 41 41>","0902")
 }
 @Test(expected=IllegalArgumentException::class) fun truncatedSingleFrameIsRejected() {
  Obd.messages("7E8 04 41 0C 00>","010C")
 }
 @Test(expected=IllegalArgumentException::class) fun duplicateFirstFrameIsRejected() {
  Obd.messages("7E8 10 14 49 02 01 4A 54 44\r7E8 10 14 49 02 01 4A 54 44>","0902")
 }
 @Test(expected=IllegalArgumentException::class) fun mixedFramingCannotHideEcuResponse() {
  Obd.value("7E8 03 41 0D 00 00 00 00 00\r41 0D 01>",13)
 }
 @Test fun conflictingCanRespondersPreventSafetyReading() {
  assertNull(Obd.value("7E8 03 41 0D 00 00 00 00 00\r7E9 03 41 0D 01 00 00 00 00>",13))
 }
 @Test(expected=IllegalArgumentException::class) fun malformedSecondEcuCannotDisappear() {
  Obd.value("7E8 03 41 0D 00 00 00 00 00\r7E9 0G>",13)
 }
 @Test(expected=IllegalArgumentException::class) fun malformed29bitBodyIsRejected() {
  Obd.value("18 DA F1 10 03 41 0D 00\r18 DA F1 11 0G>",13)
 }
 @Test fun framedDtcCounterAndNegativeReply() {
  assertEquals(listOf("P0133"),Obd.dtcs("7E8 04 43 01 01 33 00 00 00>"))
  assertNull(Obd.value("7E8 03 7F 01 12 00 00 00 00>",13))
 }
 @Test fun headerCapabilityMustBeProvedByReceivedFrames()=runBlocking {
  val sent=mutableListOf<String>()
  val s=ElmSession(object:ElmTransport {
   override suspend fun exchange(command:String):String {sent.add(command);return if(command=="0100")"41 00 80 00 00 00>"else"OK>"}
   override fun close(){}
  })
  assertFalse(s.enableCanHeaders());assertEquals("ATH0",sent.last())
 }
 @Test fun simulatorExercisesHeaderModeAndReadback()=runBlocking {
  val s=ElmSession(SimTransport());s.initialize();assertTrue(s.enableCanHeaders())
  assertEquals(setOf("7E8","7E9"),Obd.messages(s.read("0100"),"0100").mapNotNull{it.canId}.toSet())
  assertNotNull(Obd.vin(s.read("0902")))
  assertEquals(listOf("P0133"),Obd.dtcs(s.read("03")))
  s.clearStandard(true,true);assertTrue(Obd.dtcs(s.read("03")).isEmpty())
 }
}
