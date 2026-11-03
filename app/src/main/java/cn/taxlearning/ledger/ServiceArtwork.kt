package cn.taxlearning.ledger

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View

/** Decorative local artwork; all text and interactive controls are native views. */
internal class ServiceArtwork(context: Context, private val kind: String) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        if(kind.startsWith("article")) {
            val index=kind.removePrefix("article").toIntOrNull()?.coerceIn(0,3) ?: 0
            p.color=intArrayOf(0xFFFFEBEE.toInt(),0xFFFFF2D8.toInt(),0xFFC9F2FC.toInt(),0xFFE9ECFF.toInt())[index]
            val w=width.toFloat();val h=height.toFloat()
            val shape=Path().apply {moveTo(w*.18f,0f);lineTo(w,0f);lineTo(w,h);lineTo(0f,h);close()}
            c.save();c.clipPath(shape);c.drawColor(p.color)
            ReferenceAssets.draw(context,c,"article_$index.png",RectF(0f,0f,w,h));c.restore();return
        }
        if(kind=="warning") {
            // The original banner includes its text. This view displays only its illustration.
            ReferenceAssets.draw(context,c,"warning_banner.png",RectF(0f,0f,width.toFloat(),height.toFloat()),Rect(0,0,320,210));return
        }
        if(kind=="hero") {
            val w=width.toFloat();val h=height.toFloat()
            p.color=0x1672B9F5
            c.save();c.translate(w*.06f,h*.43f);c.rotate(-10f)
            c.drawRoundRect(0f,0f,w*.32f,h*.72f,w*.04f,w*.04f,p)
            p.color=0x1572B9F5
            for(x in 0..2)for(y in 0..3)c.drawRoundRect(w*(.04f+x*.085f),h*(.06f+y*.13f),w*(.10f+x*.085f),h*(.15f+y*.13f),w*.012f,w*.012f,p)
            c.restore()
            val target=RectF(w*(1f-476f/1136f),h*(1f-300f/632f),w,h)
            val layer=c.saveLayer(target,null)
            ReferenceAssets.draw(context,c,"hero_decoration.png",target)
            // Fade only the crop edge into the native background to avoid a rectangular seam.
            p.color=Color.WHITE; p.shader=LinearGradient(target.left,0f,target.left+w*.025f,0f,intArrayOf(Color.TRANSPARENT,Color.WHITE),null,Shader.TileMode.CLAMP)
            p.xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_IN);c.drawRect(target,p)
            p.shader=null;p.xfermode=null;c.restoreToCount(layer);return
        }
        if(kind=="promo") {
            ReferenceAssets.draw(context,c,"promo_banner.png",RectF(0f,0f,width.toFloat(),height.toFloat()));return
        }
        c.save();c.scale(width/200f,height/120f)
        fun fill(color: Int) { p.shader=null;p.color=color;p.style=Paint.Style.FILL }
        fun gradient(a: Int,b: Int,x: Float=0f,y: Float=0f) { p.color=Color.WHITE;p.shader=LinearGradient(x,y,x+90,y+100,intArrayOf(a,b),null,Shader.TileMode.CLAMP) }
        fun document(x: Float,y: Float,face: Int,edge: Int) {
            fill(edge);c.drawRoundRect(x+7,y-4,x+69,y+84,7f,7f,p);gradient(Color.WHITE,face,x,y);c.drawRoundRect(x,y,x+62,y+86,7f,7f,p)
            fill(0x553A8DDC);c.drawRoundRect(x+10,y+18,x+50,y+24,2f,2f,p);c.drawRoundRect(x+10,y+36,x+45,y+42,2f,2f,p);c.drawRoundRect(x+10,y+54,x+37,y+60,2f,2f,p)
        }
        when {
            kind=="hero" -> {
                c.save();c.translate(8f,55f);c.rotate(-10f);p.color=0x15A9D8FF;c.drawRoundRect(0f,0f,68f,100f,10f,10f,p);fill(0x22A9D8FF);for(x in 0..2)for(y in 0..3)c.drawRoundRect(12f+x*17,13f+y*18,24f+x*17,24f+y*18,3f,3f,p);c.restore()
                fill(0x4063CEED);c.drawOval(114f,105f,196f,120f,p)
                c.save();c.translate(126f,58f);c.rotate(-12f);document(0f,0f,0xFFB5E5FC.toInt(),0xFF90B7F7.toInt());c.restore()
                fill(0xFF82D7FA.toInt());c.drawRoundRect(139f,64f,181f,85f,5f,5f,p);fill(Color.WHITE);c.drawRoundRect(145f,69f,158f,80f,2f,2f,p);fill(0xFF7DDCD0.toInt());c.drawRoundRect(123f,91f,154f,103f,4f,4f,p)
                fill(0xFF243D61.toInt());c.drawRoundRect(170f,81f,178f,115f,3f,3f,p);c.drawRoundRect(179f,81f,185f,117f,3f,3f,p)
                gradient(0xFFFFE9C1.toInt(),0xFFEBC693.toInt(),164f,60f);c.drawRoundRect(165f,61f,186f,89f,6f,6f,p);fill(0xFFEDC0A4.toInt());c.drawCircle(176f,55f,7f,p);fill(0xFF30253D.toInt());c.drawArc(168f,46f,184f,62f,175f,220f,true,p);c.drawOval(178f,52f,186f,71f,p)
                p.style=Paint.Style.STROKE;p.color=0xFFFFE4B8.toInt();p.strokeWidth=6f;p.strokeCap=Paint.Cap.ROUND;c.drawLine(168f,70f,157f,61f,p);c.drawLine(157f,61f,151f,50f,p);p.style=Paint.Style.FILL
                fill(0xFF8CC3F6.toInt());c.drawRoundRect(119f,48f,147f,59f,4f,4f,p)
            }
            kind=="promo" || kind=="warning" -> {
                if(kind=="warning"){fill(0xFFFFF3C7.toInt());c.drawOval(-5f,23f,176f,150f,p)}
                c.save();c.translate(if(kind=="promo")138f else 58f,9f);c.rotate(8f)
                fill(0xFFF39D62.toInt());c.drawRoundRect(0f,5f,55f,113f,7f,7f,p);fill(0xFFFFF4CE.toInt());c.drawRoundRect(4f,5f,51f,108f,5f,5f,p)
                fill(0xFFFFC35F.toInt());c.drawRoundRect(13f,0f,41f,14f,4f,4f,p);fill(0xFFEFCDA9.toInt());for(i in 0..3)c.drawRoundRect(12f,29f+i*18,42f,35f+i*18,2f,2f,p);c.restore()
                if(kind=="warning"){fill(0xFFFFA94B.toInt());c.drawCircle(137f,86f,24f,p);fill(0xFFFFF1B4.toInt());c.drawRoundRect(134f,68f,141f,88f,3f,3f,p);c.drawCircle(137.5f,98f,3.5f,p)}
            }
            else -> {
                fill(if(kind=="article0")0xFFFFEDED.toInt() else if(kind=="article1")0xFFFFF3D7.toInt() else 0xFFD7F5FD.toInt());c.drawRoundRect(0f,0f,200f,120f,14f,14f,p)
                c.save();c.translate(48f,10f);c.skew(0f,-.13f)
                fill(0xFFB1D1FC.toInt());c.drawRoundRect(-18f,70f,130f,119f,12f,12f,p)
                if(kind=="article1") {
                    gradient(0xFF96C3FA.toInt(),0xFF438CE9.toInt());c.drawOval(10f,5f,57f,55f,p);c.drawRoundRect(-2f,49f,68f,110f,14f,14f,p);gradient(0xFFDEECFF.toInt(),0xFF8FB5ED.toInt());c.drawOval(63f,15f,102f,58f,p);c.drawRoundRect(54f,55f,117f,111f,14f,14f,p)
                } else {document(14f,0f,0xFFD8EAFE.toInt(),0xFF5B9DEB.toInt());fill(0xFF478BE5.toInt());p.textSize=25f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(if(kind=="article3")"保" else "税",31f,58f,p)}
                c.restore();fill(0xFF74E0C7.toInt());c.drawRoundRect(18f,27f,63f,53f,5f,5f,p);fill(0xFFBAF4DD.toInt());c.drawRoundRect(25f,34f,56f,39f,1f,1f,p)
            }
        }
        c.restore()
    }
}

internal class PageTopBackground(private val blueHeight: Int) : Drawable() {
    private val paint=Paint()
    override fun draw(canvas: Canvas) { canvas.drawColor(BG);paint.shader=LinearGradient(0f,0f,bounds.width().toFloat(),blueHeight.toFloat(),intArrayOf(0xFF287CEF.toInt(),0xFF65A6FF.toInt()),null,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,bounds.width().toFloat(),blueHeight.toFloat(),paint) }
    override fun setAlpha(alpha: Int){paint.alpha=alpha}
    override fun setColorFilter(filter: ColorFilter?){paint.colorFilter=filter}
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.OPAQUE
}
