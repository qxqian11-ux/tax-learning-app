package cn.taxlearning.ledger

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import kotlin.math.roundToInt

/** Reference measurements use a 400-unit canvas; system font scaling remains respected. */
internal fun Screen.ru(value: Int)=(resources.displayMetrics.widthPixels*value/400f).roundToInt()
internal fun Screen.referenceText(value: String,size: Float=16f,color: Int=INK,bold: Boolean=false)=
    text(value,size*resources.displayMetrics.widthPixels/resources.displayMetrics.density/400f,color,bold).apply {
        includeFontPadding=false;setLineSpacing(0f,1f)
    }
internal fun Screen.referenceGap(parent: LinearLayout,n: Int=12){parent.addView(View(this),LinearLayout.LayoutParams(1,ru(n)))}

internal class ChevronView(context: Context,private val color: Int,private val back: Boolean=false):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas){
        paint.color=color;paint.strokeWidth=resources.displayMetrics.widthPixels/400f*1.6f
        val a=if(back).8f else .2f;val b=1-a
        canvas.drawPath(Path().apply {moveTo(width*a,height*.12f);lineTo(width*b,height*.5f);lineTo(width*a,height*.88f)},paint)
    }
}

internal class RibbonDrawable(private val blue: Boolean):Drawable(){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat();val r=h/2;val slope=h*.4f
        paint.shader=LinearGradient(0f,0f,0f,h,if(blue)intArrayOf(0xFF27A8FC.toInt(),0xFF008CF4.toInt())else intArrayOf(0xFFFFD24C.toInt(),0xFFFFAC00.toInt()),null,Shader.TileMode.CLAMP)
        val p=Path()
        if(blue){p.moveTo(r,0f);p.lineTo(w,0f);p.lineTo(w-slope,h);p.lineTo(r,h);p.cubicTo(-r*.33f,h,-r*.33f,0f,r,0f)}
        else{p.moveTo(slope,0f);p.lineTo(w-r,0f);p.cubicTo(w+r*.33f,0f,w+r*.33f,h,w-r,h);p.lineTo(0f,h);p.close()}
        c.drawPath(p,paint)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

internal class RaisedCardDrawable(private val fill:Int,private val radius:Float):Drawable(){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=fill }
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat();val r=radius
        c.drawPath(Path().apply {
            moveTo(r,0f);cubicTo(w*.55f,0f,w*.7f,r*.45f,w-r,r*.8f)
            quadTo(w,r*.9f,w,r*1.7f);lineTo(w,h-r);quadTo(w,h,w-r,h)
            lineTo(r,h);quadTo(0f,h,0f,h-r);lineTo(0f,r);quadTo(0f,0f,r,0f);close()
        },paint)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
