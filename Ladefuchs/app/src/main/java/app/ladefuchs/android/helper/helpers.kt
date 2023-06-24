package app.ladefuchs.android.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.CardMetaData
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import com.beust.klaxon.Klaxon
import java.io.File
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.min
import kotlin.math.roundToInt


private val pricesSemaphore = Semaphore(1)

var currentDialog: Dialog? = null

/**
Function to log while in Debug mode
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
 * Returns the screens width
 */
fun getScreenWidth(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
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
 * A function to safely open other pages
 */
fun NavController.safeNavigate(actionId: Int) {
    currentDestination?.getAction(actionId)?.run { navigate(actionId) }
}

/**
 * This function retrieves the prices for a specific operator
 */
fun getPricesByOperatorId(
    pocOperator: Operator,
    context: Context,
    api: API,
    view: View,
    resources: Resources,
    forceDownload: Boolean = false,
): Boolean {
    //Load Prices JSON from File
    pricesSemaphore.acquire();
    printLog("Getting prices for $pocOperator")
    val (_, acCards, dcCards) = api.retrieveCards(
        pocOperator.identifier,
        forceDownload
    )
    printLog("Re-Filling Cards for $pocOperator")
    val maxListLength = maxOf(acCards.size, dcCards.size)
    pricesSemaphore.release()
    return fillCards(
        pocOperator,
        acCards,
        dcCards,
        maxListLength,
        context,
        view,
        api,
        resources,
    )

}

fun readCardMetadata(context: Context): List<CardMetaData>? {
    //Load Metadata JSON from File
    printLog("Reading de-card_metadata.json")
    val cardMetadata = context.assets?.open("de-card_metadata.json")?.let {
        Klaxon().parseArray<CardMetaData>(
            it
        )
    }
    return cardMetadata
}

fun getImagePath(cardUri: URL, context: Context, cpo: Boolean = false): File {
    val cardChecksum = cardUri.path.substring(cardUri.path.lastIndexOf('/') + 1)
    return File("${context.filesDir}/${if (cpo) "card" else "cpo"}_${cardChecksum}.jpg")
}

private fun TextView.removeLinksUnderline() {
    val spannable = SpannableString(text)
    for (u in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
        spannable.setSpan(object : URLSpan(u.url) {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, spannable.getSpanStart(u), spannable.getSpanEnd(u), 0)
    }
    text = spannable
}

fun createAboutPopup(context: Context, view: View) {
    currentDialog?.dismiss()

    val popUpView: View = LayoutInflater.from(context).inflate(R.layout.fragment_about, null)
    currentDialog = createDialog(popUpView, view, context)
    currentDialog?.show()

    popUpView.findViewById<ImageButton>(R.id.back_button)
        .setOnClickListener {
            currentDialog?.dismiss()
        }

    aboutPopUpSetUp(popUpView, context)
}


@SuppressLint("SetTextI18n")
fun aboutPopUpSetUp(view: View, context: Context) {

    val acknowledgementText = view.findViewById(R.id.acknowledgement_text) as TextView
    acknowledgementText.movementMethod = LinkMovementMethod.getInstance()

    val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
    } else {
        context.packageManager
            .getPackageInfo(context.packageName, 0).versionName
    }
    val versionHolder: TextView = view.findViewById(R.id.version_info)
    versionHolder.text = "Ladefuchs Version $versionName"

    // Making Links in Textviews Clickable... well... really...
    val schlingelSL2 = view.findViewById(R.id.bastiSL2) as TextView
    schlingelSL2.movementMethod = LinkMovementMethod.getInstance()
    schlingelSL2.removeLinksUnderline()
    val schlingelSL3 = view.findViewById(R.id.bastiSL3) as TextView
    schlingelSL3.removeLinksUnderline()

    val malikSL2 = view.findViewById(R.id.malikSL2) as TextView
    malikSL2.movementMethod = LinkMovementMethod.getInstance()
    malikSL2.removeLinksUnderline()
    val malikSL3 = view.findViewById(R.id.malikSL3) as TextView
    malikSL3.removeLinksUnderline()

    val flowinhoSL2 = view.findViewById(R.id.flowinhoSL2) as TextView
    flowinhoSL2.movementMethod = LinkMovementMethod.getInstance()
    flowinhoSL2.removeLinksUnderline()
    val flowinhoSL3 = view.findViewById(R.id.flowinhoSL3) as TextView
    flowinhoSL3.removeLinksUnderline()

    val thorstenSL2 = view.findViewById(R.id.thorstenSL2) as TextView
    thorstenSL2.movementMethod = LinkMovementMethod.getInstance()
    thorstenSL2.removeLinksUnderline()
    val thorstenSL3 = view.findViewById(R.id.thorstenSL3) as TextView
    thorstenSL3.removeLinksUnderline()

    val dominicSL2 = view.findViewById(R.id.dominicSL2) as TextView
    dominicSL2.movementMethod = LinkMovementMethod.getInstance()
    dominicSL2.removeLinksUnderline()

    val roddiSL2 = view.findViewById(R.id.roddiSL2) as TextView
    roddiSL2.movementMethod = LinkMovementMethod.getInstance()
    roddiSL2.removeLinksUnderline()
    val roddiSL3 = view.findViewById(R.id.roddiSL3) as TextView
    roddiSL3.removeLinksUnderline()

    val illuSL2 = view.findViewById(R.id.illufuchsSL) as TextView
    illuSL2.movementMethod = LinkMovementMethod.getInstance()
    illuSL2.removeLinksUnderline()

    // On Click Listeners for Images
    val chargePriceLogo = view.findViewById(R.id.chargeprice_logo) as ImageView
    chargePriceLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), context)
    }

    val audiodDumpLogo = view.findViewById(R.id.podcast_audiodump) as ImageView
    audiodDumpLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), context)
    }

    val bitsundsoLogo = view.findViewById(R.id.podcast_bitsundso) as ImageView
    bitsundsoLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), context)
    }
}

fun opeLinkInBrowser(url: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun createCardDetailPopup(
    view: View,
    currentCard: ChargeCards,
    chargeCardsAC: List<ChargeCards>,
    chargeCardsDC: List<ChargeCards>,
    currentType: ChargeType,
    cardImageDrawable: Drawable?,
    cardBitmap: Bitmap?,
    operator: Operator,
    api: API,
    context: Context,
) {
    currentDialog?.dismiss()

    val overlayView = View(context)
    overlayView.setBackgroundColor(Color.parseColor("#80000000"))
    val params = view.layoutParams
    val parentViewGroup = view.parent as ViewGroup
    parentViewGroup.addView(overlayView, params)
    val inflater =
        view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView: View =
        inflater.inflate(R.layout.card_detail_dialog, null)

    currentDialog = createDialog(popupView, view, context)
    currentDialog?.show()
    // set onClick Listeners for backButtons
    popupView.findViewById<ImageButton>(R.id.back_button)
        .setOnClickListener {
            currentDialog?.dismiss()
        }

    // Set card Image
    popupView.findViewById<ImageView>(R.id.card_logo).background =
        if (cardImageDrawable !== null) cardImageDrawable else BitmapDrawable(
            context.resources,
            cardBitmap
        )
    // Set Card Details
    val currentCardAc: ChargeCards? =
        if (currentType == ChargeType.AC) currentCard else chargeCardsAC.find { it.identifier == currentCard.identifier }
    val currentCardDc: ChargeCards? =
        if (currentType == ChargeType.DC) currentCard else chargeCardsDC.find { it.identifier == currentCard.identifier }
    popupView.findViewById<TextView>(R.id.detail_header1).text =
        currentCard.name
    popupView.findViewById<TextView>(R.id.detail_header2).text =
        currentCard.provider
    if (currentCardAc !== null) {
        popupView.findViewById<TextView>(R.id.priceAC).text =
            java.text.DecimalFormat("#,##0.00").format(currentCardAc.price)
        popupView.findViewById<TextView>(R.id.blockFeeAC).text =
            "> ab Min. ${currentCardAc.blockingFeeStart}\n> ${currentCardAc.blockingFee} € /Min."
        popupView.findViewById<TextView>(R.id.monthlyFeeContent).text =
            if (currentCardAc.monthlyFee == 0.0f) "keine" else "${currentCardAc.monthlyFee} €"
        if (currentCardAc.blockingFee == 0.0f)
            popupView.findViewById<ImageView>(R.id.huetchen_ac).visibility = View.GONE
        else
            popupView.findViewById<ImageView>(R.id.huetchen_ac).visibility = View.VISIBLE
    }
    if (currentCardDc !== null) {
        popupView.findViewById<TextView>(R.id.priceDC).text =
            java.text.DecimalFormat("#,##0.00").format(currentCardDc.price)
        popupView.findViewById<TextView>(R.id.blockFeeDC).text =
            "> ab Min. ${currentCardDc.blockingFeeStart}\n> ${currentCardDc.blockingFee} € /Min."
        popupView.findViewById<TextView>(R.id.monthlyFeeContent).text =
            if (currentCardDc.monthlyFee == 0.0f) "keine" else "${currentCardDc.monthlyFee} €"
        if (currentCardDc.blockingFee == 0.0f)
            popupView.findViewById<ImageView>(R.id.huetchen_dc).visibility = View.GONE
        else
            popupView.findViewById<ImageView>(R.id.huetchen_dc).visibility = View.VISIBLE
    }

    if (!currentCard.note.isNullOrEmpty()) {
        popupView.findViewById<ConstraintLayout>(R.id.notes).visibility = View.VISIBLE
        popupView.findViewById<ImageView>(R.id.huetchenNotes).visibility = View.VISIBLE
        popupView.findViewById<TextView>(R.id.notesText).text = currentCard.note;
    }

    popupView.findViewById<Button>(R.id.getCard).setOnClickListener {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(currentCard.url.toString())
        )
        context.startActivity(urlIntent)
    }

    if (currentCard.url == null) {
        popupView.findViewById<Button>(R.id.getCard).visibility = View.INVISIBLE
    }

    // Retrieve Operator Image
    var operatorImage: Drawable? = null
    if (!operator.image.isNullOrEmpty()) {
        val imgPath = getImagePath(URL(operator.image), context, true)
        if (!imgPath.exists())
            api.downloadImageToInternalStorage(imageURL = URL(operator.image), cpo = true)
        try {
            operatorImage = Drawable.createFromPath(imgPath.absolutePath)!!
        } catch (e: Exception) {
            //Download was to slow or failed no need for error handling because generated image will be used
        }
    }
    // creating an own image
    if (operatorImage == null) {
        // TODO add text to placeholder
        operatorImage = AppCompatResources.getDrawable(context, R.drawable.cpo_generic)
    }
    popupView.findViewById<ImageView>(R.id.cpo_logo).setImageDrawable(operatorImage)
}

private fun createDialog(
    dialogView: View,
    anchorView: View,
    context: Context
): Dialog {

    val statusbarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = anchorView.rootWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        insets.top
    } else {
        val statusBarResId =
            context.resources?.getIdentifier("status_bar_height", "dimen", "android")
        if (statusBarResId != null) context.resources?.getDimensionPixelSize(statusBarResId)!! else 0
    }

    val height =
        anchorView.context.resources.displayMetrics.heightPixels - if (statusbarHeight > 110) 0 else 110
    val width = anchorView.context.resources.displayMetrics.widthPixels

    val dialog = Dialog(context)
    (dialogView.parent as? ViewGroup)?.removeView(dialogView) // Remove view from its current parent
    dialog.setContentView(dialogView)
    dialog.window?.setLayout(width, height - 20)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.attributes?.windowAnimations = R.style.popup_window_animation

    return dialog
}

