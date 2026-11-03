package cn.taxlearning.ledger

import android.content.Context
import android.graphics.*
import android.view.View

/** Reference sprite artwork when available; native paths for remaining controls. */
internal class ReferenceIcon(context: Context, private val kind: String, private val tint: Int, private val active: Boolean=false) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        if(ReferenceAssets.icon(context,c,kind,width,height,tint,active)) return
        c.save();c.scale(width/32f,height/32f);p.color=tint;p.style=Paint.Style.STROKE;p.strokeWidth=2.2f;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND
        fun path(vararg points: Float) { val path=Path().apply { moveTo(points[0],points[1]);var i=2;while(i<points.size){lineTo(points[i],points[i+1]);i+=2} };c.drawPath(path,p) }
        when(kind) {
            "home" -> { if(active)p.style=Paint.Style.FILL;path(3f,14f,16f,3f,29f,14f,29f,28f,20f,28f,20f,20f,12f,20f,12f,28f,4f,28f,4f,14f);if(active){p.color=Color.WHITE;c.drawRoundRect(13f,20f,19f,29f,2f,2f,p)} }
            "todo" -> { c.drawRoundRect(5f,6f,27f,29f,3f,3f,p);path(11f,3f,11f,10f);path(21f,3f,21f,10f);path(10f,19f,14f,23f,22f,15f) }
            "services" -> { if(active){p.style=Paint.Style.FILL;c.drawRoundRect(4f,3f,28f,29f,4f,4f,p);p.style=Paint.Style.STROKE;p.color=Color.WHITE};c.drawRoundRect(4f,3f,28f,29f,4f,4f,p);path(9f,10f,17f,10f);path(9f,15f,13f,15f);path(18f,24f,21f,15f,25f,17f,22f,25f,18f,24f) }
            "message" -> { c.drawRoundRect(4f,4f,28f,28f,11f,11f,p);path(4f,20f,4f,28f,13f,28f);path(10f,12f,22f,12f);path(10f,18f,18f,18f) }
            "mine" -> { if(active){p.style=Paint.Style.FILL;c.drawCircle(16f,16f,14f,p);p.color=Color.WHITE;p.style=Paint.Style.STROKE}else c.drawCircle(16f,16f,13f,p);c.drawArc(9f,10f,23f,22f,25f,130f,false,p) }
            "family","job","bank" -> {
                p.style=Paint.Style.FILL;p.color=0x22B3C6DE;c.drawRoundRect(4f,8f,30f,30f,7f,7f,p)
                p.color=tint;p.shader=LinearGradient(3f,4f,27f,30f,intArrayOf(tint,blend(tint,.87f)),null,Shader.TileMode.CLAMP)
                if(kind=="family") { path(3f,13f,15f,3f,27f,13f,27f,27f,6f,27f,6f,13f);p.shader=null;p.color=0xFFFFB0A0.toInt();c.drawPath(Path().apply { moveTo(23f,29f);cubicTo(4f,18f,20f,10f,23f,19f);cubicTo(31f,8f,40f,22f,23f,29f);close() },p) }
                else if(kind=="job") { c.drawRoundRect(4f,8f,28f,28f,4f,4f,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawRoundRect(11f,3f,21f,12f,3f,3f,p);p.style=Paint.Style.FILL;p.color=0xFFFFE4A1.toInt();c.drawOval(16f,15f,31f,31f,p);p.style=Paint.Style.STROKE;p.color=0xFFE8A637.toInt();p.strokeWidth=1.5f;path(20f,19f,24f,23f,28f,19f);path(24f,23f,24f,28f);path(21f,24f,27f,24f) }
                else { c.drawRoundRect(3f,6f,29f,26f,5f,5f,p);p.shader=null;p.color=0xFF5FECDA.toInt();c.drawCircle(21f,16f,5f,p);c.drawOval(13f,22f,30f,31f,p);p.color=0x55FFFFFF;c.drawRoundRect(6f,9f,18f,12f,1f,1f,p) }
            }
            "care" -> c.drawPath(Path().apply { moveTo(16f,28f);cubicTo(-9f,13f,5f,-1f,16f,9f);cubicTo(26f,-1f,41f,13f,16f,28f);moveTo(14f,24f);lineTo(23f,13f) },p)
            "security" -> {path(16f,3f,27f,8f,26f,21f,16f,29f,6f,21f,5f,8f,16f,3f);path(11f,16f,15f,20f,22f,12f)}
            "help" -> {c.drawCircle(16f,13f,10f,p);path(9f,20f,12f,24f,20f,24f,23f,20f);path(13f,29f,19f,29f)}
            "consult" -> {c.drawArc(5f,3f,27f,29f,180f,180f,false,p);c.drawRoundRect(3f,14f,8f,25f,2f,2f,p);c.drawRoundRect(24f,14f,29f,25f,2f,2f,p);path(25f,25f,22f,29f,17f,29f)}
            "about" -> {c.drawCircle(16f,16f,13f,p);path(16f,14f,16f,24f);p.style=Paint.Style.FILL;c.drawCircle(16f,8f,1.5f,p)}
            "search" -> {c.drawCircle(13f,13f,10f,p);path(21f,21f,29f,29f)}
            "scan" -> {path(4f,10f,4f,4f,10f,4f);path(22f,4f,28f,4f,28f,10f);path(28f,22f,28f,28f,22f,28f);path(10f,28f,4f,28f,4f,22f);path(10f,16f,22f,16f)}
            "flame" -> {p.style=Paint.Style.FILL;c.drawCircle(16f,16f,14f,p);p.color=Color.WHITE;c.drawPath(Path().apply {moveTo(16f,27f);cubicTo(3f,23f,11f,13f,13f,10f);lineTo(14f,17f);cubicTo(20f,13f,17f,5f,19f,5f);cubicTo(32f,18f,25f,26f,16f,27f);close()},p)}
            "notice" -> {p.style=Paint.Style.FILL;c.drawRoundRect(9f,4f,23f,23f,7f,7f,p);c.drawRoundRect(5f,21f,27f,25f,2f,2f,p);c.drawCircle(16f,28f,2f,p)}
            "eye" -> {c.drawArc(4f,5f,28f,21f,15f,150f,false,p);if(active)c.drawCircle(16f,13f,3f,p)else{path(8f,19f,6f,23f);path(16f,21f,16f,25f);path(24f,19f,26f,23f)}}
            else -> {
                p.style=Paint.Style.FILL;p.color=0x339DC3E8;c.drawRoundRect(4f,1f,27f,28f,6f,6f,p);p.color=tint
                p.shader=LinearGradient(4f,4f,28f,29f,intArrayOf(tint,blend(tint,.92f)),null,Shader.TileMode.CLAMP);c.drawRoundRect(3f,4f,29f,30f,6f,6f,p);p.shader=null
                p.style=Paint.Style.STROKE;p.color=Color.WHITE;p.strokeWidth=2f
                when(kind){
                    "income" -> {p.style=Paint.Style.FILL;c.drawRoundRect(8f,9f,25f,25f,3f,3f,p);p.color=tint;c.drawRoundRect(12f,12f,20f,13.4f,.7f,.7f,p);p.style=Paint.Style.STROKE;p.strokeWidth=1.2f;path(18f,16f,20f,18f,22f,16f);path(18f,19f,22f,19f);path(20f,18f,20f,23f)}
                    "annual" -> {p.style=Paint.Style.FILL;c.drawRoundRect(8f,10f,25f,16f,1f,1f,p);p.alpha=170;c.drawRoundRect(8f,21f,15f,24f,1f,1f,p);c.drawRoundRect(10f,19f,13f,26f,1f,1f,p);p.alpha=255;c.drawRoundRect(19f,20f,25f,22f,1f,1f,p);c.drawRoundRect(19f,24f,25f,26f,1f,1f,p)}
                    "record" -> {path(8f,11f,23f,11f);path(8f,16f,15f,16f);path(18f,21f,24f,21f,19f,26f,24f,26f)}
                    "deduction" -> {path(8f,11f,22f,11f);path(8f,17f,14f,17f);path(18f,25f,20f,18f,24f,20f,22f,26f,18f,25f)}
                    "invoice" -> {c.drawRect(8f,9f,12f,13f,p);c.drawRect(20f,9f,24f,13f,p);c.drawRect(8f,21f,12f,25f,p);path(19f,21f,24f,21f,24f,26f);path(17f,10f,17f,18f,10f,18f)}
                    "personnel" -> {c.drawRoundRect(7f,10f,25f,25f,2f,2f,p);c.drawCircle(16f,15f,3f,p);c.drawArc(11f,18f,21f,26f,180f,180f,false,p)}
                    "lookup" -> {path(8f,11f,22f,11f);path(8f,16f,14f,16f);c.drawCircle(21f,22f,4f,p);path(24f,25f,27f,28f)}
                    else -> {c.drawRoundRect(8f,10f,24f,25f,2f,2f,p);path(11f,15f,21f,15f);path(11f,20f,17f,20f)}
                }
            }
        }
        c.restore()
    }
    private fun blend(color: Int, factor: Float)=Color.rgb((Color.red(color)*factor).toInt(),(Color.green(color)*factor).toInt(),(Color.blue(color)*factor).toInt())
}
