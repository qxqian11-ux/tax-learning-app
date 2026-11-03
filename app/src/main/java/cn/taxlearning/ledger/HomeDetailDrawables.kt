package cn.taxlearning.ledger

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

/** Reject the yellow ribbon's transparent left corner so the blue face underneath receives taps. */
internal class HomeRibbonLabel(host:MainActivity,label:String,private val blue:Boolean):AppCompatTextView(host) {
    private val inset=host.u(3).toFloat()
    init {
        text=label;setTextSize(TypedValue.COMPLEX_UNIT_PX,host.type(label,14f).textSize)
        setTextColor(Color.WHITE);includeFontPadding=false;setLineSpacing(host.u(-1).toFloat(),1f)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(!blue && event.actionMasked==MotionEvent.ACTION_DOWN) {
            val h=height-inset*2;val y=(event.y-inset).coerceIn(0f,h)
            if(event.x-inset<h*.446f*(1f-y/h))return false
        }
        return super.onTouchEvent(event)
    }
}

/** Search and scrolled content sample the same original header at the same global coordinate. */
internal class HomeHeaderDrawable(private val host:MainActivity,private val offset:Int):Drawable() {
    override fun draw(canvas:Canvas) {
        val save=canvas.save();canvas.clipRect(bounds);canvas.drawColor(BG)
        val y=-host.u(offset).toFloat();val w=bounds.width().toFloat()
        ReferenceAssets.draw(host,canvas,"home_header_surface.png",RectF(0f,y,w,y+w*1335f/1125f));canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha:Int){}
    override fun setColorFilter(filter:ColorFilter?){}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.OPAQUE
}

internal class HomeBitmapDrawable(private val context:Context,private val file:String,private val source:Rect?=null):Drawable() {
    override fun draw(canvas:Canvas){ReferenceAssets.draw(context,canvas,file,RectF(bounds),source)}
    override fun setAlpha(alpha:Int){}
    override fun setColorFilter(filter:ColorFilter?){}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

internal class HomeCornerTagDrawable(private val fill:Int):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=fill}
    override fun draw(c:Canvas) {
        val w=bounds.width().toFloat();val h=bounds.height().toFloat();val r=h*.58f
        c.drawPath(Path().apply {
            moveTo(r,0f);lineTo(w,0f);lineTo(w,h);lineTo(0f,h)
            cubicTo(r*.65f,h,r*.05f,h*.62f,r*.17f,h*.38f);quadTo(r*.3f,0f,r,0f);close()
        },paint)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

/** Padding reserves space for the separate blue/yellow shadows inside the software layer. */
internal class HomeRibbonDrawable(private val blue:Boolean,private val padding:Float):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas) {
        val save=c.save();c.translate(padding,padding)
        val w=bounds.width()-padding*2;val h=bounds.height()-padding*2;val radius=h/2;val slope=h*.446f;val join=h*.055f
        paint.color=Color.WHITE
        paint.shader=LinearGradient(0f,0f,0f,h,if(blue)intArrayOf(0xFF25AAFC.toInt(),0xFF008CF4.toInt())else intArrayOf(0xFFFFD347.toInt(),0xFFFFAF00.toInt()),null,Shader.TileMode.CLAMP)
        paint.setShadowLayer(padding,0f,padding*.5f,if(blue)0x550098F4 else 0x55FFB500)
        val path=Path()
        if(blue) {
            path.moveTo(radius,0f);path.lineTo(w-join,0f);path.quadTo(w,0f,w-join*.3f,join)
            path.lineTo(w-slope+join,h-join);path.quadTo(w-slope,h,w-slope-join,h);path.lineTo(radius,h)
            path.arcTo(RectF(0f,0f,h,h),90f,180f);path.close()
        } else {
            path.moveTo(slope+join,0f);path.lineTo(w-radius,0f);path.arcTo(RectF(w-h,0f,w,h),270f,180f)
            path.lineTo(join,h);path.quadTo(0f,h,join*.3f,h-join);path.lineTo(slope-join,join);path.quadTo(slope,0f,slope+join,0f);path.close()
        }
        c.drawPath(path,paint);c.restoreToCount(save)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

internal class HomeClockDrawable(private val size:Int):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=size
    override fun draw(c:Canvas) {
        val cx=bounds.exactCenterX();val cy=bounds.exactCenterY();val r=minOf(bounds.width(),bounds.height())*.47f
        paint.color=0xFFBEC0C3.toInt();paint.style=Paint.Style.FILL;c.drawCircle(cx,cy,r,paint)
        paint.color=Color.WHITE;paint.strokeWidth=r*.2f;paint.strokeCap=Paint.Cap.ROUND;c.drawLine(cx,cy,cx,cy-r*.58f,paint);c.drawLine(cx,cy,cx+r*.43f,cy+r*.24f,paint)
    }
    override fun setAlpha(alpha:Int){paint.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
