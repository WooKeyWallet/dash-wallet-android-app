package io.wookey.dash.core

import android.arch.lifecycle.MutableLiveData
import android.util.Log
import io.wookey.dash.support.extensions.sharedPreferences
import kotlinx.coroutines.Runnable
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class ExchangeRatesHelper {
    companion object {
        val instance = SingletonHolder.holder
    }

    private object SingletonHolder {
        val holder = ExchangeRatesHelper()
    }

    var executors: ScheduledExecutorService? = null
    var currency = sharedPreferences().getString("currency", "CNY")
    val rate = MutableLiveData<Pair<String, String>>()
    var rates: JSONObject? = null

    fun startInterval() {
        executors = Executors.newScheduledThreadPool(1)
        executors?.scheduleWithFixedDelay(Runnable {
            try {
                loadRate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 60 * 1000, TimeUnit.MILLISECONDS)
    }

    fun isRunning() = !(executors?.isShutdown ?: true)

    fun stopInterval() {
        executors?.shutdownNow()
        executors = null
    }

    fun loadRate() {
        var connection: HttpsURLConnection? = null
        try {
            connection = URL("https://api.get-spark.com/CNY/USD").openConnection()as? HttpsURLConnection
            connection?.run {
                readTimeout = 3000
                connectTimeout = 3000
                requestMethod = "GET"
                doInput = true
                connect()
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val buffer = StringBuffer()
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    var line = ""
                    while (reader.readLine()?.apply { line = this } != null) {
                        buffer.append(line)
                    }
                    reader.close()
                    val json = JSONObject(buffer.toString())
                    if (!json.optString(currency).isNullOrBlank()) {
                        rates = json
                        rate.postValue(Pair(currency, rates?.optString(currency) ?: ""))
                    }
                }
            }
        } finally {
            connection?.inputStream?.close()
            connection?.disconnect()
        }
    }

    fun getRate(currency: String = this.currency) = rates?.optString(currency) ?: ""

}