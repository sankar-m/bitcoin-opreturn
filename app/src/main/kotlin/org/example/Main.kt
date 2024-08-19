package com.example

import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun getBitcoinRPC(method: String, params: List<Any> = emptyList()): String {
    val rpcUrl = URL("http://localhost:38332/")
    val connection = rpcUrl.openConnection() as HttpURLConnection
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("testuser:testpass".toByteArray()))
    connection.requestMethod = "POST"

    val formattedParams = params.map {
        when (it) {
            is String -> "\"$it\""
            else -> it.toString()
        }
    }

    val payload = """{"jsonrpc":"1.0","id":"curltext","method":"$method","params":$formattedParams}"""
    println("Sending payload: $payload")

    val wr = OutputStreamWriter(connection.outputStream)
    wr.write(payload)
    wr.flush()

    if (connection.responseCode != 200) {
        val errorResponse = connection.errorStream.bufferedReader().readText()
        throw IOException("Server returned HTTP response code: ${connection.responseCode} for URL: $rpcUrl with message: $errorResponse")
    }

    return connection.inputStream.bufferedReader().readText()
}

fun extractOpReturnData(txid: String): List<String> {
    val rawTx = getBitcoinRPC("getrawtransaction", listOf(txid, true))
    val opReturnData = mutableListOf<String>()

    val json = org.json.JSONObject(rawTx)

    if (!json.isNull("error")) {
        println("Error fetching transaction: ${json.getJSONObject("error").getString("message")}")
        return emptyList()
    }

    val result = json.getJSONObject("result")
    val blockhash = result.getString("blockhash")
    val vouts = result.getJSONArray("vout")

    for (i in 0 until vouts.length()) {
        val vout = vouts.getJSONObject(i)
        val scriptPubKey = vout.getJSONObject("scriptPubKey")
        if (scriptPubKey.getString("type") == "nulldata") {
            val asm = scriptPubKey.getString("asm")
            val opReturnHexs = asm.split(" ").drop(1)
            opReturnData.addAll(opReturnHexs)
        }
    }

    // Store in PostgreSQL
    storeOpReturnData(txid, blockhash, opReturnData)

    return opReturnData
}

fun storeOpReturnData(txid: String, blockhash: String, opReturnData: List<String>) {
    val url = "jdbc:postgresql://localhost:5432/opreturn_db"
    val user = "postgres"
    val password = "password"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(url, user, password)
        val stmt = connection.createStatement()

        opReturnData.forEach { hex ->
            val sql = "INSERT INTO op_return_data (txid, blockhash, op_return_hex) VALUES ('$txid', '$blockhash', '$hex')"
            stmt.executeUpdate(sql)
        }

        println("OP_RETURN data stored successfully.")
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }
}

//fun main() {
//    val blockchainInfo = getBitcoinRPC("getblockchaininfo")
//    println("Blockchain Info: $blockchainInfo")
//
//    val exampleTxid = "ffddb3c33035bba09a62813526a8ccef027bdb21ebb8f333cef4e54ce3e88de5"
//    val opReturnData = extractOpReturnData(exampleTxid)
//
//    println("OP_RETURN Data: $opReturnData")
//}

fun testDbConnection() {
    val url = "jdbc:postgresql://localhost:5432/opreturn_db"
    val user = "postgres"
    val password = "password"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(url, user, password)
        println("Connected to the PostgreSQL server successfully.")
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }
}

//fun main() {
//    testDbConnection()
//}

//fun main() {
//    embeddedServer(Netty, port = 8080) {
//        routing {
//            get("/opreturn/{opReturnData}") {
//                val opReturnData = call.parameters["opReturnData"]
//                // Query the database for matching opReturnData and return results
//                call.respondText("Results for $opReturnData", ContentType.Text.Plain)
//            }
//        }
//    }.start(wait = true)
//}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/opreturn/{txid}") {
                println("hitting......")
                val txid = call.parameters["txid"] ?: return@get call.respondText(
                    "Missing or malformed transaction ID",
                    status = HttpStatusCode.BadRequest
                )

                val data = getOpReturnData(txid)
                if (data.isEmpty()) {
                    call.respondText("No OP_RETURN data found for transaction ID: $txid", status = HttpStatusCode.NotFound)
                } else {
                    call.respond(data)
                }
            }
        }
    }.start(wait = true)
}

fun getOpReturnData(txid: String): List<String> {
    val url = "jdbc:postgresql://localhost:5432/opreturn_db"
    val user = "postgres"
    val password = "password"
    val opReturnData = mutableListOf<String>()

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(url, user, password)
        val stmt = connection.prepareStatement("SELECT op_return_hex FROM op_return_data WHERE txid = ?")
        stmt.setString(1, txid)
        val rs = stmt.executeQuery()

        while (rs.next()) {
            opReturnData.add(rs.getString("op_return_hex"))
        }

        rs.close()
        stmt.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return opReturnData
}
