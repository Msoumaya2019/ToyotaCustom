package org.toyotacustom.app

import org.json.JSONArray
import org.json.JSONObject

data class ExchangeTrace(val time: Long, val command: String, val response: String)

object TechnicalReport {
 fun create(state: ScreenState, includeIdentifiers: Boolean = false): String = JSONObject().apply {
  put("format", "toyota-custom-session-v1")
  put("appVersion", "0.2.0")
  put("exportedAt", java.time.Instant.now().toString())
  put("simulation", state.simulation)
  put("connected", state.connected)
  put("adapter", state.adapter)
  put("bluetoothAddress", if(includeIdentifiers) state.deviceAddress else JSONObject.NULL)
  put("protocol", state.protocol)
  put("canHeadersVerified", state.canHeaders)
  put("observedCanResponders", JSONArray(state.canIds.sorted()))
  put("ecuIdentityVerified", false)
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
