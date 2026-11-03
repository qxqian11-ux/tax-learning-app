package cn.taxlearning.ledger

import android.content.Context
import android.graphics.*

/** Packaged reference artwork. Keep original bytes; select sprite cells only at draw time. */
internal object ReferenceAssets {
    private val cache = mutableMapOf<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private fun bitmap(context: Context, name: String): Bitmap = cache.getOrPut(name) {
        context.assets.open("reference-ui/$name").use { BitmapFactory.decodeStream(it)!! }
    }
    fun draw(context: Context, canvas: Canvas, name: String, bounds: RectF, source: Rect? = null) {
        canvas.drawBitmap(bitmap(context, name), source, bounds, paint)
    }
    fun icon(context: Context, canvas: Canvas, kind: String, width: Int, height: Int, tint: Int, active: Boolean): Boolean {
        val nav=kind in listOf("home","todo","services","message","mine")
        val crop=when {
            nav -> "nav_"+kind+(if(active && kind in listOf("home","services","mine")) "_active" else "")
            kind in listOf("bank","care","security","help","consult","about","notice","flame","search","scan") -> kind
            else -> null
        }
        if(crop!=null) {
            // Active todo/message references were not supplied; retain their outline and use active blue.
            if(nav && active && kind in listOf("todo","message")) paint.colorFilter=PorterDuffColorFilter(tint,PorterDuff.Mode.SRC_IN)
            draw(context,canvas,"crop_$crop.png",RectF(0f,0f,width.toFloat(),height.toFloat()))
            paint.colorFilter=null
            return true
        }
        if(kind.endsWith("_feature")) {
            draw(context,canvas,"$kind.png",RectF(0f,0f,width.toFloat(),height.toFloat()));return true
        }
        val cell = when(kind) {
            "annual" -> 0 to 0
            "deduction" -> 1 to 0
            "invoice" -> 2 to 0
            "invoice_title" -> 3 to 0
            "invoice_wallet" -> 4 to 0
            "filing" -> 6 to 0
            "voucher" -> 7 to 0
            "lookup" -> 8 to 0
            "record" -> 0 to 1
            "income" -> 1 to 1
            "appeal" -> 2 to 1
            "tax_document" -> 3 to 1
            "agency" -> 4 to 1
            "institution" -> 5 to 1
            "policy" -> 1 to 2
            "guide" -> 2 to 2
            "hot" -> 3 to 2
            "pension" -> 4 to 3
            "more" -> 4 to 4
            "invoice_application" -> 5 to 5
            "donation" -> 0 to 6
            "business" -> 2 to 6
            "agent_declaration" -> 5 to 6
            "personnel" -> 6 to 6
            "arrears" -> 0 to 7
            else -> null
        }
        if(cell != null) {
            val x = 151 + cell.first * 216
            val y = 54 + cell.second * 216
            draw(context, canvas, "service_icons.png", RectF(0f,0f,width.toFloat(),height.toFloat()), Rect(x,y,x+144,y+144))
            return true
        }
        val name=when(kind){"family"->"family.png";"job"->"employment.png";else->return false}
        draw(context, canvas, name, RectF(0f,0f,width.toFloat(),height.toFloat()), if(kind=="family") Rect(0,0,136,136) else Rect(0,0,128,136))
        return true
    }
}
