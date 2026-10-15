package org.toyotacustom.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimulationTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun select(label:String) {compose.onNodeWithText(label).performScrollTo().performClick()}
 private fun simulate() {
  compose.waitUntil(15000) {compose.onAllNodesWithText("Prêt — choisissez un appareil ou la simulation").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Ouvrir la simulation").performScrollTo().performClick()
  compose.waitUntil(15000) {compose.onAllNodesWithText("SIMULATION — aucune connexion véhicule").fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun diagnosticBlockedUntilConnected() {
  select("Diagnostic")
  compose.onNodeWithText("Lire les codes défaut").assertIsNotEnabled()
  compose.onNodeWithText("Effacer les défauts standard…").assertIsNotEnabled()
 }
 @Test fun simulatedDiagnosticAndClearReadback() {
  simulate();select("Diagnostic")
  compose.onNodeWithText("Lire les codes défaut").performScrollTo().performClick()
  compose.waitUntil(10000){compose.onAllNodesWithText("Mémorisé : P0133").fetchSemanticsNodes().isNotEmpty()}
  compose.waitUntil(15000){compose.onAllNodes(hasText("Effacer les défauts standard…") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Effacer les défauts standard…").performScrollTo().performClick()
  compose.onNodeWithText("Confirmer en simulation").performClick()
  compose.waitUntil(10000){compose.onAllNodesWithText("Effacement confirmé pour les défauts mémorisés. Les permanents peuvent rester.").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("SIMULATION • données fictives").assertIsDisplayed()
 }
 @Test fun simulatedCustomizationCreatesExplicitHistory() {
  simulate();select("Personnalisation")
  compose.onNodeWithText("Simuler une modification…").performScrollTo().performClick()
  compose.onNodeWithText("Simuler",useUnmergedTree=true).performClick()
  compose.waitUntil(10000){compose.onAllNodesWithText("Élevée (fictive)").fetchSemanticsNodes().isNotEmpty()}
  select("Historique")
  compose.onAllNodesWithText("Modification simulée",substring=true).onFirst().assertExists()
 }
 @Test fun simulationRecordsCanRespondersAndMasksVinInReport() {
  simulate()
  lateinit var reconnectModel:VehicleViewModel
  compose.runOnUiThread {
   val model=androidx.lifecycle.ViewModelProvider(compose.activity)[VehicleViewModel::class.java]
   reconnectModel=model
   val state=model.state.value
   org.junit.Assert.assertTrue(state.canHeaders)
   org.junit.Assert.assertEquals(setOf("7E8","7E9"),state.canIds)
   val masked=TechnicalReport.create(state)
   org.junit.Assert.assertFalse(masked.contains(requireNotNull(state.vin)))
   val json=org.json.JSONObject(masked)
   org.junit.Assert.assertTrue(json.getBoolean("simulation"))
   org.junit.Assert.assertFalse(json.getBoolean("oemWritesEnabled"))
   val exchanges=json.getJSONArray("exchanges")
   val vinResponse=(0 until exchanges.length()).map{exchanges.getJSONObject(it)}.single{it.getString("command")=="0902"}
   org.junit.Assert.assertEquals("[Réponse VIN masquée]",vinResponse.getString("response"))
   org.junit.Assert.assertTrue(TechnicalReport.create(state,true).contains(requireNotNull(state.vin)))
   model.disconnect()
   org.junit.Assert.assertEquals(state.canIds,model.state.value.canIds)
   org.junit.Assert.assertFalse(model.state.value.connected)
   model.connect(simulate=true)
   org.junit.Assert.assertNotNull(model.state.value.previousReport)
  }
  compose.waitUntil(15000){reconnectModel.state.value.connected && reconnectModel.state.value.traces.count{it.command=="ATZ"}==1}
 }
 @Test fun declaredEcuInformationCanBeReadAndSaved() {
  simulate();select("Diagnostic")
  compose.onNodeWithText("Lire les identifiants OBD des calculateurs").performScrollTo().performClick()
  compose.waitUntil(15000){compose.onAllNodesWithText("SIM-ECU-MOTEUR",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.runOnUiThread {
   val model=androidx.lifecycle.ViewModelProvider(compose.activity)[VehicleViewModel::class.java]
   val info=model.state.value.ecuIdentities
   org.junit.Assert.assertEquals(2,info.size)
   org.junit.Assert.assertEquals("SIM-CAL-0001",info.first().calibrationIds.single())
   org.junit.Assert.assertEquals(2,org.json.JSONObject(TechnicalReport.create(model.state.value)).getJSONArray("declaredStandardInfo").length())
  }
  select("Historique")
  compose.waitUntil(15000){compose.onAllNodesWithText("Rapport OBD • SIMULATION").fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun measurementsKeepCanSourceAndAreIncludedInSavedReports() {
  simulate();select("Diagnostic")
  compose.onNodeWithText("Actualiser les mesures").performScrollTo().performClick()
  compose.waitUntil(15000){compose.onAllNodesWithText("Tension calculateur • 7E8").fetchSemanticsNodes().isNotEmpty()}
  lateinit var model:VehicleViewModel
  compose.runOnUiThread {model=androidx.lifecycle.ViewModelProvider(compose.activity)[VehicleViewModel::class.java]}
  compose.waitUntil(15000){!model.state.value.busy}
  compose.runOnUiThread {
   val state=model.state.value
   org.junit.Assert.assertTrue(state.measurements.isNotEmpty())
   org.junit.Assert.assertTrue(state.measurements.all{it.receivedAt>0 && it.reading.canId=="7E8"})
   val rows=org.json.JSONObject(TechnicalReport.create(state)).getJSONArray("liveMeasurements")
   org.junit.Assert.assertEquals(state.measurements.size,rows.length())
   org.junit.Assert.assertEquals("7E8",rows.getJSONObject(0).getString("canId"))
  }
  select("Historique")
  compose.waitUntil(15000){model.savedReports.value.any{report->
   org.json.JSONObject(report.json).optJSONArray("liveMeasurements")?.length()?.let{it>0}==true
  }}
  compose.runOnUiThread {
   val previous=model.state.value.measurements
   model.disconnect()
   org.junit.Assert.assertEquals(previous,model.state.value.measurements)
   org.junit.Assert.assertTrue(model.state.value.values.isEmpty())
  }
 }

}
