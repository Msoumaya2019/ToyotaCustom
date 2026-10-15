package org.toyotacustom.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VehicleInformationTest {
 private fun info(pid:Int,count:Int,data:List<Int>)=ObdMessage("7E8",listOf(0x49,pid,count)+data)
 private fun text(value:String,size:Int)=value.padEnd(size,'\u0000').map{it.code}
 @Test fun capabilitiesAreKeptPerCanId() {
  val caps=VehicleInformation.supported("7E8 06 49 00 54 40 00 00 00\r7E9 06 49 00 10 40 00 00 00>")
  assertEquals(setOf(2,4,6,10),caps["7E8"])
  assertEquals(setOf(4,10),caps["7E9"])
 }
 @Test(expected=IllegalArgumentException::class) fun cannotAttributeHeaderlessCapabilities() {
  VehicleInformation.supported("49 00 54 40 00 00>")
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsTruncatedBitmap() {
  VehicleInformation.supported("7E8 05 49 00 54 40 00 00 00>")
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsConflictingCapabilities() {
  VehicleInformation.supported("7E8 06 49 00 54 40 00 00 00\r7E8 06 49 00 00 00 00 00 00>")
 }
 @Test fun decodesMultipleCalibrationsWithoutMergingThem() {
  assertEquals(listOf("CAL-A","CAL-B"),VehicleInformation.records(info(4,2,text("CAL-A",16)+text("CAL-B",16)),4))
 }
 @Test fun decodesDeclaredNameAndCvn() {
  assertEquals(listOf("SIM-ECU-MOTEUR"),VehicleInformation.records(info(10,1,text("SIM-ECU-MOTEUR",20)),10))
  assertEquals(listOf("00000000","12AB34CD"),VehicleInformation.records(info(6,2,listOf(0,0,0,0,0x12,0xAB,0x34,0xCD)),6))
 }
 @Test fun embeddedNullIsVisibleRatherThanJoiningIdentifiers() {
  assertEquals(listOf("CAL\\x00A"),VehicleInformation.records(info(4,1,text("CAL\u0000A",16)),4))
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsTruncatedRecords() {
  VehicleInformation.records(info(4,2,text("ONE",16)),4)
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsNonAsciiText() {
  VehicleInformation.records(info(4,1,listOf(255)+List(15){0}),4)
 }
 @Test(expected=IllegalArgumentException::class) fun rejectsBlankIdentifiers() {
  VehicleInformation.records(info(4,1,List(16){0}),4)
 }
 @Test fun skipsUnadvertisedQueries()=runBlocking {
  val commands=mutableListOf<String>()
  val ecus=VehicleInformation.collect {cmd->commands.add(cmd);"7E8 06 49 00 00 00 00 00 00>"}
  assertEquals(listOf("0900"),commands);assertEquals(1,ecus.size);assertNull(ecus.single().declaredName)
 }
 @Test fun advertisedButUnavailableIsAnIssueNotAValue()=runBlocking {
  val ecus=VehicleInformation.collect {cmd->if(cmd=="0900")"7E8 06 49 00 10 00 00 00 00>"else"NO DATA>"}
  assertTrue(ecus.single().calibrationIds.isEmpty());assertTrue(ecus.single().issues.single().contains("0904"))
 }
 @Test fun contradictoryRecordsStayDiscardedEvenIfFirstValueRepeats()=runBlocking {
  val ecus=VehicleInformation.collect {cmd->
   if(cmd=="0900")"7E8 06 49 00 04 00 00 00 00>"
   else "7E8 07 49 06 01 00 00 00 01\r7E8 07 49 06 01 00 00 00 02\r7E8 07 49 06 01 00 00 00 01>"
  }
  assertTrue(ecus.single().verificationNumbers.isEmpty());assertTrue(ecus.single().issues.any{it.contains("contradictoires")})
 }
 @Test fun simulatorProvidesDistinctDeclaredInformation()=runBlocking {
  val session=ElmSession(SimTransport());session.initialize();session.enableCanHeaders()
  val ecus=VehicleInformation.collect(session::read)
  assertEquals(listOf("7E8","7E9"),ecus.map{it.canId})
  assertEquals("SIM-ECU-MOTEUR",ecus[0].declaredName)
  assertEquals(listOf("SIM-CAL-0001"),ecus[0].calibrationIds)
  assertEquals(listOf("01020304"),ecus[0].verificationNumbers)
  assertEquals("SIM-ECU-AUXILIAIRE",ecus[1].declaredName)
  assertTrue(ecus[1].verificationNumbers.isEmpty());assertTrue(ecus.all{it.issues.isEmpty()})
 }
}
