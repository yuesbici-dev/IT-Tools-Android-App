package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class WhoisResult(
    val query: String,
    val objectType: String,
    val registrar: String,
    val status: List<String>,
    val createdDate: String,
    val updatedDate: String,
    val expiresDate: String,
    val nameservers: List<String>,
    val entities: List<String>,
    val rawText: String
)

class WhoisEngine {

    suspend fun lookup(query: String): Result<WhoisResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().removePrefix("http://").removePrefix("https://").split("/").first()
        val isIp = cleanQuery.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")) || cleanQuery.contains(":")

        val endpoint = if (isIp) {
            "https://rdap.org/ip/$cleanQuery"
        } else {
            "https://rdap.org/domain/$cleanQuery"
        }

        runCatching {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/rdap+json, application/json")
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val body = reader.readText()
                reader.close()

                val json = JSONObject(body)
                val objectClassName = json.optString("objectClassName", if (isIp) "ip network" else "domain")

                // Extract status
                val statusList = mutableListOf<String>()
                val statusArr = json.optJSONArray("status")
                if (statusArr != null) {
                    for (i in 0 until statusArr.length()) {
                        statusList.add(statusArr.getString(i))
                    }
                }

                // Extract events (created, expired, changed)
                var created = "N/A"
                var updated = "N/A"
                var expires = "N/A"
                val eventsArr = json.optJSONArray("events")
                if (eventsArr != null) {
                    for (i in 0 until eventsArr.length()) {
                        val evt = eventsArr.getJSONObject(i)
                        val action = evt.optString("eventAction")
                        val date = evt.optString("eventDate").substringBefore("T")
                        when (action) {
                            "registration" -> created = date
                            "last changed", "last update" -> updated = date
                            "expiration" -> expires = date
                        }
                    }
                }

                // Nameservers
                val nsList = mutableListOf<String>()
                val nsArr = json.optJSONArray("nameservers")
                if (nsArr != null) {
                    for (i in 0 until nsArr.length()) {
                        val ns = nsArr.getJSONObject(i)
                        nsList.add(ns.optString("ldhName", ns.optString("handle")))
                    }
                }

                // Entities / Registrar
                var registrar = "N/A"
                val entityList = mutableListOf<String>()
                val entitiesArr = json.optJSONArray("entities")
                if (entitiesArr != null) {
                    for (i in 0 until entitiesArr.length()) {
                        val ent = entitiesArr.getJSONObject(i)
                        val handle = ent.optString("handle")
                        val roles = ent.optJSONArray("roles")
                        var isReg = false
                        if (roles != null) {
                            for (j in 0 until roles.length()) {
                                if (roles.getString(j) == "registrar") isReg = true
                            }
                        }
                        if (isReg && registrar == "N/A") {
                            registrar = handle
                        }
                        if (handle.isNotBlank()) {
                            entityList.add(handle)
                        }
                    }
                }

                if (registrar == "N/A" && json.has("handle")) {
                    registrar = json.getString("handle")
                }

                WhoisResult(
                    query = cleanQuery,
                    objectType = objectClassName.uppercase(),
                    registrar = registrar,
                    status = statusList,
                    createdDate = created,
                    updatedDate = updated,
                    expiresDate = expires,
                    nameservers = nsList,
                    entities = entityList,
                    rawText = json.toString(2)
                )
            } else {
                throw Exception("RDAP/WHOIS lookup returned HTTP $code")
            }
        }
    }
}
