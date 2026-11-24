// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
package com.tals.volta.play

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * What the module's launcher icon opens. The module has no screen of its own
 * use: with Volta OS installed, a launch opens Volta OS, this page staying
 * behind it. Without it, this page says what the module is for and leads to
 * Volta OS on Google Play. Either way the legal notices stay one tap away: a
 * button here, and Volta OS's PlayStation card opens [LegalActivity] too.
 */
class AboutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val voltaOs = packageManager.getLaunchIntentForPackage(VOLTOS)

        val dp = resources.displayMetrics.density
        val pad = (24 * dp).toInt()
        // Volta OS's look (ui/theme/Color.kt there): always dark, light grey text,
        // the blue pill for the page's one action.
        window.setBackgroundDrawable(ColorDrawable(PAGE_BG))
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        // The app's logo, large and centred. Not the launcher icon: as an adaptive
        // icon its artwork fills barely half of it, inside a dark square.
        val logoWidth = (200 * resources.displayMetrics.density).toInt()
        logoArtwork()?.let { art ->
            layout.addView(ImageView(this).apply {
                setImageBitmap(art)
                adjustViewBounds = true
                contentDescription = null
            }, LinearLayout.LayoutParams(logoWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = pad
            })
        }
        // No title: the logo above already says which app this is.
        layout.addView(TextView(this).apply {
            text = getString(R.string.about_text)
            textSize = 16f
            setTextColor(BODY_TEXT)
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, pad)
        })
        layout.addView(Button(this).apply {
            // Volta OS's primary button: a 56 dp blue pill, near-black semibold text.
            isAllCaps = false
            textSize = 16f
            typeface = semibold()
            setTextColor(ON_ACCENT)
            stateListAnimator = null
            background = RippleDrawable(
                ColorStateList.valueOf(0x33000000),
                GradientDrawable().apply { setColor(BRAND_BLUE); cornerRadius = 28 * dp },
                null
            )
            if (voltaOs != null) {
                text = getString(R.string.open_voltos)
                setOnClickListener { runCatching { startActivity(voltaOs) } }
            } else {
                text = getString(R.string.get_voltos)
                setOnClickListener { openStore() }
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (56 * dp).toInt()).apply {
            // Set in from the text's edges, so it does not run the page's full width.
            marginStart = pad
            marginEnd = pad
        })

        // The legal notices, at the bottom of the page.
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(layout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        page.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = getString(R.string.legal_title)
            // Volta OS's text buttons ("J'ai besoin d'aide"): blue, semibold.
            isAllCaps = false
            textSize = 14f
            typeface = semibold()
            setTextColor(BRAND_BLUE)
            setOnClickListener { startActivity(Intent(this@AboutActivity, LegalActivity::class.java)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = pad / 2
        })

        // No title bar: a frame keeps the page clear of the status and navigation
        // bars. Not the layout itself: fitsSystemWindows replaces a view's own
        // padding with the bars' insets, which left the text against the edges.
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(PAGE_BG)
            addView(page, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        })

        // A fresh launch goes straight on to Volta OS, in its own task. This page
        // stays behind it rather than closing: back to it from the recent apps (or
        // the icon again) it shows, without sending the user away a second time.
        if (savedInstanceState == null && voltaOs != null) runCatching { startActivity(voltaOs) }
    }

    /** Semibold where the system has weights (Android 9+), bold before. */
    private fun semibold(): Typeface =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) Typeface.create(Typeface.DEFAULT, 600, false)
        else Typeface.DEFAULT_BOLD

    /** Google Play's app when there is one, its web page otherwise. */
    private fun openStore() {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$VOLTOS"))
        try {
            startActivity(market)
        } catch (_: ActivityNotFoundException) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL))) }
        }
    }

    /**
     * The launcher artwork without its background: cropped to what differs from
     * the corner colour, so no dark square shows on the page's own background.
     */
    private fun logoArtwork(): Bitmap? {
        // Not scaled by the screen density: the PNG is already 768 px.
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val src = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_image, opts) ?: return null
        val w = src.width
        val h = src.height
        val px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        val bg = px[0]
        fun differs(c: Int): Boolean {
            val d = abs(Color.red(c) - Color.red(bg)) + abs(Color.green(c) - Color.green(bg)) + abs(Color.blue(c) - Color.blue(bg))
            return Color.alpha(c) > 16 && d > 40
        }
        var left = w; var top = h; var right = -1; var bottom = -1
        for (y in 0 until h) for (x in 0 until w) {
            if (differs(px[y * w + x])) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (right < left || bottom < top) return src
        // The background inside the artwork becomes transparent too.
        val out = IntArray(w * h) { if (differs(px[it])) px[it] else Color.TRANSPARENT }
        val cut = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        return Bitmap.createBitmap(cut, left, top, right - left + 1, bottom - top + 1)
    }

    companion object {
        /** Volta OS's colours: background, body text, BrandBlue and the text on it. */
        private const val PAGE_BG = 0xFF0F1216.toInt()
        private const val BODY_TEXT = 0xFFC3C8D0.toInt()
        private const val BRAND_BLUE = 0xFF5AAEFF.toInt()
        private const val ON_ACCENT = 0xFF0B0F14.toInt()

        /** The app this module serves. */
        const val VOLTOS = "com.tals.volta.os"
        const val STORE_URL = "https://play.google.com/store/apps/details?id=$VOLTOS"

        /** Where the complete source of this version is published (AGPL-3.0 §6). */
        const val SOURCE_URL = "https://github.com/tals-enterprise/volta-ps"
    }
}
