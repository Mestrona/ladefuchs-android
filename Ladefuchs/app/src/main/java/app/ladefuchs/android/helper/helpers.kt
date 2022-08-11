package app.ladefuchs.android.helper

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.navigation.NavController
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.dataClasses.ChargeCards
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Function to log while in Debug mode
 */
fun printLog(message: String, type: String = "info") {
    if (BuildConfig.DEBUG) {
        val logPrefix = "🦊"
        var typeIcon = "ℹ️"
        when (type) {
            "error" -> {
                typeIcon = "🛑"
            }
            "warning" -> {
                typeIcon = "⚠️"
            }
            "heart" -> {
                typeIcon = "❤️"
            }
            "network" -> {
                typeIcon = "⏬️"
            }
        }
        println("$logPrefix $typeIcon $message")
    }
}

/**
 * Stores a file in internal storage
 */
fun storeFileInInternalStorage(
    inputStream: InputStream,
    internalStorageFileName: String,
    context: Context
) {
    val outputStream = context.openFileOutput(internalStorageFileName, Context.MODE_PRIVATE)
    val buffer = ByteArray(1024)
    inputStream.use {
        while (true) {
            val byeCount = it.read(buffer)
            if (byeCount < 0) break
            outputStream?.write(buffer, 0, byeCount)
        }
        outputStream?.close()
        printLog("Writing File: " + internalStorageFileName + " to " + context.filesDir.toString())
    }
}

/**
 * Returns the screens width
 */
fun getScreenWidth(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
}

/**
 * This function sanitizes the umlauts
 */
fun umlautSanitization(output: String): String? {
    return output.replace("\u00fc", "ue")
        .replace("\u00f6", "oe")
        .replace("\u00e4", "ae")
        .replace("\u00df", "ss")
        .replace("\u00dc(?=[a-z\u00e4\u00f6\u00fc\u00df ])".toRegex(), "Ue")
        .replace("\u00d6(?=[a-z\u00e4\u00f6\u00fc\u00df ])".toRegex(), "Oe")
        .replace("\u00c4(?=[a-z\u00e4\u00f6\u00fc\u00df ])".toRegex(), "Ae")
        .replace("\u00dc", "UE")
        .replace("\u00d6", "OE")
        .replace("\u00c4", "AE")
}

/**
 * This function does some fancy color manipulation god knows what for
 */
fun manipulateColor(color: Int, factor: Float): Int {
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).roundToInt()
    val g = (Color.green(color) * factor).roundToInt()
    val b = (Color.blue(color) * factor).roundToInt()
    return Color.argb(
        a,
        min(r, 255),
        min(g, 255),
        min(b, 255)
    )
}

/**
 * Returns Maingau prices depending whether the charger is from ionity and whether ac or dc prices are requested
 */
fun getMaingauPrices(type: String, pocOperator: String, context: Context): ChargeCards {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    //Load Pricetoggle from prefs
    val hasMaingauCustomerPrices = prefs.getBoolean("specialMaingauCustomer", false)

    val maingauIonityPrice: Float = 0.75F
    val maingauAcPrice: Float = 0.49F
    val maingauDcPrice: Float = 0.59F

    var maingauPrice = ChargeCards(
        identifier = "",
        name = "",
        provider = "",
        price = 0.0f,
        updated = System.currentTimeMillis() / 1000L
    )
    if (hasMaingauCustomerPrices) {
        when {
            pocOperator.lowercase() == "ionity" && type == "dc" -> {
                maingauPrice = ChargeCards(
                    identifier = "maingau_personalized",
                    name = "Einfach Strom Laden",
                    provider = "Maingau",
                    price = maingauIonityPrice,
                    updated = System.currentTimeMillis() / 1000L
                )
            }
            type == "ac" && pocOperator.lowercase() != "ionity" -> {

                maingauPrice = ChargeCards(
                    identifier = "maingau_personalized",
                    name = "Einfach Strom Laden",
                    provider = "Maingau",
                    price = maingauAcPrice,
                    updated = System.currentTimeMillis() / 1000L
                )
            }
            type == "dc" && pocOperator.lowercase() != "ionity" -> {
                maingauPrice = ChargeCards(
                    identifier = "maingau_personalized",
                    name = "Einfach Strom Laden",
                    provider = "Maingau",
                    price = maingauDcPrice,
                    updated = System.currentTimeMillis() / 1000L
                )
            }
        }
    }
    return maingauPrice
}

fun NavController.safeNavigate(actionId: Int) {
    currentDestination?.getAction(actionId)?.run { navigate(actionId) }
}

