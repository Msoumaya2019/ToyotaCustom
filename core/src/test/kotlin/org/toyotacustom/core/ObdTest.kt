package org.toyotacustom.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ObdTest {
 @Test fun decodesLiveMeasurements() {
  assertEquals(1726.0,Obd.value("41 0C 1A F8>",12)!!,0.01)
  assertEquals(72.0,Obd.value("41 05 70>",5)!!,0.01)
  assertEquals(12.6,Obd.value("41 42 31 38>",66)!!,0.01)
 }
 @Test fun discardsNegativeAndMissingData() {
  assertNull(Obd.value("NO DATA>",12));assertNull(Obd.value("7F 01 12>",12))
  assertNull(Obd.value("41 0C 00>",12))
 }
 @Test fun conflictingEcuValuesCannotAuthorizeClear() {
  assertNull(Obd.value("41 0D 00\r41 0D 01>",13))
  assertNull(Obd.value("41 0C 00 00\r41 0C 00>",12))
 }
 @Test fun supportedBitMapsRespectBaseAndMultipleEcus() {
  assertEquals(setOf(1,32),Obd.supported("41 00 80 00 00 01>",0))
  assertEquals(setOf(33,64),Obd.supported("41 20 80 00 00 00\r41 20 00 00 00 01>",32))
 }
 @Test fun decodesCanDtcCountNotAsDtc() {
  assertEquals(listOf("P0133","U0123"),Obd.dtcs("43 02 01 33 C1 23 00 00>"))
  assertEquals(emptyList<String>(),Obd.dtcs("43 00>"))
  assertEquals(listOf("P0133"),Obd.dtcs("47 01 01 33>","07"))
 }
 @Test(expected=IllegalArgumentException::class) fun refusesTruncatedDtcs() {Obd.dtcs("43 02 01 33>")}
 @Test(expected=IllegalArgumentException::class) fun refusesUnavailableDtcs() {Obd.dtcs("NO DATA>")}
 @Test fun handlesEchoAndSearchMessages() {
  assertEquals(0.0,Obd.value("010D\rSEARCHING...\r41 0D 00\r>",13)!!,0.01)
 }
 @Test fun assemblesVinFromSimulator()=runBlocking {
  val raw=SimTransport().exchange("0902")
  val vin=Obd.vin(raw)
  assertNotNull(vin);assertEquals(17,vin!!.length);assertTrue(Obd.toyota(vin))
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsMissingIndexedFrame() {
  Obd.payloads("0: 49 02 01 4A\r2: 54 44>","0902")
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsDeclaredLengthTruncation() {
  Obd.payloads("014\r0: 49 02 01 4A>","0902")
 }
 @Test fun blocksOemCommandsAndFreeWrites()=runBlocking {
  val sent=mutableListOf<String>()
  val s=ElmSession(object:ElmTransport {
   override suspend fun exchange(command:String):String{sent.add(command);return "OK>"}
   override fun close(){}
  })
  for(command in listOf("2E123401","2701","1003","04","ATSH7B0","22FFFF","010D\r04")) {
   try{s.read(command);fail(command)}catch(_:IllegalArgumentException){}
  }
  assertTrue(sent.isEmpty())
 }
 @Test fun simulatorEndToEndAndClearReadback()=runBlocking {
  val s=ElmSession(SimTransport());s.initialize()
  assertEquals(listOf("P0133"),Obd.dtcs(s.read("03")))
  s.clearStandard(true,true)
  assertTrue(Obd.dtcs(s.read("03")).isEmpty())
 }
 @Test fun deniedConfirmationDoesNotSendAnything()=runBlocking {
  val sent=mutableListOf<String>()
  val s=ElmSession(object:ElmTransport {
   override suspend fun exchange(command:String):String {sent.add(command);return "44>"}
   override fun close(){}
  })
  try{s.clearStandard(false,true);fail()}catch(_:IllegalArgumentException){}
  try{s.clearStandard(true,false);fail()}catch(_:IllegalArgumentException){}
  assertTrue(sent.isEmpty())
 }
 @Test fun movingVehicleCannotClear()=runBlocking {
  val sent=mutableListOf<String>()
  val s=ElmSession(object:ElmTransport {
   override suspend fun exchange(command:String):String {sent.add(command);return "41 0D 01>"}
   override fun close(){}
  })
  try{s.clearStandard(true,true);fail()}catch(_:IllegalArgumentException){}
  assertEquals(listOf("010D"),sent)
 }
 @Test fun missingPromptIsRejected()=runBlocking {
  val s=ElmSession(object:ElmTransport {
   override suspend fun exchange(command:String)="41 0D 00"
   override fun close(){}
  })
  try{s.read("010D");fail()}catch(_:IllegalArgumentException){}
 }
}
