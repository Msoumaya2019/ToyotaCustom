package org.toyotacustom.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LiveMeasurementsTest {
 @Test fun preservesDifferentValuesByCanSource() {
  val r=LiveMeasurements.decode("7E8 03 41 0D 00 00 00 00 00\r7E9 03 41 0D 05 00 00 00 00>",13)
  assertEquals(listOf("7E8","7E9"),r.map{it.canId})
  assertEquals(listOf(0.0,5.0),r.map{it.value})
  assertNull(LiveMeasurements.consensus(r))
 }
 @Test fun equalSourcesCanFormConsensusWithoutLosingAttribution() {
  val r=LiveMeasurements.decode("7E8 03 41 0D 00\r7E9 03 41 0D 00>",13)
  assertEquals(2,r.size);assertEquals(0.0,LiveMeasurements.consensus(r)!!,0.0)
 }
 @Test fun sameSourceContradictionsRemainInvalid() {
  val r=LiveMeasurements.decode("7E8 03 41 0D 00\r7E8 03 41 0D 01\r7E8 03 41 0D 00>",13).single()
  assertNull(r.value);assertNotNull(r.issue)
 }
 @Test fun malformedReplyCannotHideBehindValidReply() {
  val r=LiveMeasurements.decode("7E8 04 41 0C 00 00\r7E8 03 41 0C 00>",12).single()
  assertNull(r.value);assertTrue(r.issue!!.contains("Longueur"))
 }
 @Test fun repeatedIdenticalReplyFromSameSourceIsConsistent() {
  val r=LiveMeasurements.decode("7E8 03 41 0D 00\r7E8 03 41 0D 00>",13)
  assertEquals(1,r.size);assertEquals(0.0,r.single().value!!,0.0)
 }
 @Test fun headerlessDoesNotInventSourceOrSelectFirst() {
  val r=LiveMeasurements.decode("41 0D 00\r41 0D 01>",13).single()
  assertNull(r.canId);assertNull(r.value);assertNotNull(r.issue)
 }
 @Test fun negativeResponseAndValidReplyCannotAuthorizeConsensus() {
  val r=LiveMeasurements.decode("7E8 03 41 0D 00\r7E9 03 7F 01 12>",13)
  assertEquals("Réponse négative OBD : NRC 12",r.last().issue)
  assertNull(LiveMeasurements.consensus(r))
 }
 @Test fun wrongPidIsVisibleAndNotUsed() {
  val r=LiveMeasurements.decode("41 05 70>",13).single()
  assertNull(r.value);assertNotNull(r.issue)
 }
 @Test fun noDataHasNoValueOrFakeSource() {
  val r=LiveMeasurements.decode("NO DATA>",13).single()
  assertNull(r.value);assertNull(r.canId);assertNotNull(r.issue)
 }
 @Test fun payloadLengthsAndUnitsAreStrict() {
  for(pid in LiveMeasurements.pids) {
   val length=if(pid in setOf(12,66))2 else 1
   val valid="41 %02X ".format(pid)+List(length){"00"}.joinToString(" ")
   assertNotNull(Obd.value(valid+">",pid))
   assertNull(Obd.value(valid+" 00>",pid))
   assertNull(Obd.value(valid.substringBeforeLast(' ')+">",pid))
  }
  assertEquals(-40.0,Obd.value("41 05 00>",5)!!,0.0)
  assertEquals(100.0,Obd.value("41 5B FF>",91)!!,0.0)
  assertEquals(65535.0/4,Obd.value("41 0C FF FF>",12)!!,0.0)
 }
 @Test(expected=IllegalArgumentException::class) fun unknownPidIsNotDecoded() {LiveMeasurements.decode("41 FF 00>",255)}
 @Test fun extraBytesInSafetyResponsePreventAnyErase()=runBlocking {
  for(reply in listOf("41 0D 00 01>","41 0D 00\r7F 01 12>")) {
   val sent=mutableListOf<String>()
   val s=ElmSession(object:ElmTransport{
    override suspend fun exchange(command:String):String{sent.add(command);return reply}
    override fun close(){}
   })
   try{s.clearStandard(true,true);fail("Effacement non bloqué")}catch(_:IllegalArgumentException){}
   assertEquals(listOf("010D"),sent)
  }
 }
}
