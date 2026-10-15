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
  compose.runOnUiThread {
   val model=androidx.lifecycle.ViewModelProvider(compose.activity)[VehicleViewModel::class.java]
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
   org.junit.Assert.assertEquals(1,model.state.value.traces.count{it.command=="ATZ"})
  }
 }
}
