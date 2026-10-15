package org.toyotacustom.app

import org.json.JSONArray
import org.json.JSONObject

data class ExchangeTrace(val time: Long, val command: String, val response: String)

object TechnicalReport {
 fun create(state: ScreenState, includeIdentifiers: Boolean = false): String = JSONObject().apply {
  put("format", "toyota-custom-session-v1")
  put("appVersion", BuildConfig.VERSION_NAME)
  put("sessionId",state.sessionId?:JSONObject.NULL)
  put("sessionStartedAt",state.sessionStartedAt?.let{java.time.Instant.ofEpochMilli(it).toString()}?:JSONObject.NULL)
  put("exportedAt", java.time.Instant.now().toString())
  put("simulation", state.simulation)
  put("connected", state.connected)
  put("adapter", state.adapter)
  put("bluetoothAddress", if(includeIdentifiers) state.deviceAddress else JSONObject.NULL)
  put("protocol", state.protocol)
  put("canHeadersVerified", state.canHeaders)
  put("observedCanResponders", JSONArray(state.canIds.sorted()))
  put("ecuIdentityVerified", false)
  put("declaredStandardInfo",JSONArray().apply {state.ecuIdentities.forEach{ecu->put(JSONObject().apply {
   put("canId",ecu.canId)
   put("advertisedMode09",JSONArray(ecu.advertisedInfo.sorted().map{"%02X".format(it)}))
   put("declaredName",ecu.declaredName?:JSONObject.NULL)
   put("calibrationIds",JSONArray(ecu.calibrationIds))
   put("verificationNumbers",JSONArray(ecu.verificationNumbers))
   put("issues",JSONArray(ecu.issues))
   put("toyotaCompatibilityVerified",false)
  })}})
  put("vehicleVin", if(includeIdentifiers) state.vin else JSONObject.NULL)
  put("vehicleModelVerified", false)
  put("oemWritesEnabled", false)
  put("vinResponseMasked", !includeIdentifiers)
  put("supportedPids", JSONArray(state.supported.sorted().map { "%02X".format(it) }))
  put("exchanges", JSONArray().apply {
   state.traces.forEach { trace -> put(JSONObject().apply {
    put("time", java.time.Instant.ofEpochMilli(trace.time).toString())
    put("command", trace.command)
    put("response", if(!includeIdentifiers && trace.command=="0902") "[Réponse VIN masquée]" else trace.response)
   }) }
  })
 }.toString(2)
}
