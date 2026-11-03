package cn.taxlearning.ledger

import android.graphics.*
import android.view.*
import android.widget.*
import android.content.Context
import androidx.core.view.doOnLayout
import kotlin.math.abs
import kotlin.math.roundToInt

internal class DoubleChevronView(context:Context,private val up:Boolean):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=ACCENT;style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun onDraw(c:Canvas) {
        paint.strokeWidth=resources.displayMetrics.widthPixels/400f
        for(center in listOf(.31f,.69f)) {
            val outer=height*(center+if(up).13f else -.13f);val middle=height*(center+if(up)-.13f else .13f)
            c.drawPath(Path().apply {moveTo(width*.12f,outer);lineTo(width*.5f,middle);lineTo(width*.88f,outer)},paint)
        }
    }
}

internal class SummaryHelpView(context:Context):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas) {
        val size=minOf(width,height).toFloat();val cx=width/2f;val cy=height/2f
        paint.color=0xFF689CCD.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=size*.045f
        c.drawCircle(cx,cy,size*.43f,paint)
        paint.style=Paint.Style.FILL;paint.textSize=size*.74f;paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL);paint.textAlign=Paint.Align.CENTER
        c.drawText("?",cx,cy-(paint.ascent()+paint.descent())/2,paint)
    }
}

/** Seven visible rows with a centered selection and a debounced snap after native scrolling. */
internal class ReferenceYearWheel(private val screen:Screen,initialYear:Int):FrameLayout(screen) {
    private val years=(2019..Rules.currentYear()).toList()
    private val rowHeight=screen.ru(43)
    private val labels=ArrayList<TextView>()
    private var dragging=false
    private val snap=Runnable {if(!dragging)selectYear(selectedYear,true)}
    private val scroll=object:ScrollView(screen) {
        override fun dispatchTouchEvent(event:MotionEvent):Boolean {
            if(event.actionMasked==MotionEvent.ACTION_DOWN){dragging=true;removeCallbacks(snap)}
            val result=super.dispatchTouchEvent(event)
            if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL){dragging=false;this@ReferenceYearWheel.scheduleSnap()}
            return result
        }
        override fun onScrollChanged(l:Int,t:Int,oldl:Int,oldt:Int) {
            super.onScrollChanged(l,t,oldl,oldt)
            this@ReferenceYearWheel.updateLabels()
            if(!dragging)this@ReferenceYearWheel.scheduleSnap()
        }
    }.apply {id=R.id.query_year_scroll;isVerticalScrollBarEnabled=false;overScrollMode=OVER_SCROLL_NEVER;isNestedScrollingEnabled=false}
    val selectedYear:Int get()=years[(scroll.scrollY.toFloat()/rowHeight).roundToInt().coerceIn(years.indices)]
    init {
        id=R.id.query_year_wheel
        val rows=screen.column()
        rows.addView(View(screen),LinearLayout.LayoutParams(1,rowHeight*3))
        years.forEach { year ->
            val label=screen.referenceText(year.toString(),18f).apply {
                gravity=Gravity.CENTER;contentDescription="$year 年";setOnClickListener {selectYear(year,true)}
            }
            labels.add(label);rows.addView(label,LinearLayout.LayoutParams(-1,rowHeight))
        }
        rows.addView(View(screen),LinearLayout.LayoutParams(1,rowHeight*3))
        scroll.addView(rows);addView(scroll,LayoutParams(-1,-1))
        addView(object:View(screen) {
            private val paint=Paint().apply {color=0xFFE6E6E6.toInt();strokeWidth=1f}
            override fun onDraw(c:Canvas){c.drawLine(0f,rowHeight*3f,width.toFloat(),rowHeight*3f,paint);c.drawLine(0f,rowHeight*4f,width.toFloat(),rowHeight*4f,paint)}
        }.apply {importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO},LayoutParams(-1,-1))
        scroll.doOnLayout {selectYear(initialYear,false)}
    }
    private fun scheduleSnap(){removeCallbacks(snap);postDelayed(snap,160)}
    private fun selectYear(year:Int,smooth:Boolean) {
        val offset=(year-2019).coerceIn(years.indices)*rowHeight
        if(smooth && abs(scroll.scrollY-offset)>1)scroll.smoothScrollTo(0,offset) else scroll.scrollTo(0,offset)
        updateLabels()
    }
    private fun updateLabels() {
        if(labels.isEmpty())return
        val position=scroll.scrollY.toFloat()/rowHeight
        labels.forEachIndexed { index,label ->
            val distance=abs(index-position)
            val color=when {distance<.5f->INK;distance<1.5f->0xFFAAAAAA.toInt();distance<2.5f->0xFFD0D0D0.toInt();else->0xFFEAEAEA.toInt()}
            label.setTextColor(color);label.isSelected=index==(position.roundToInt())
        }
        contentDescription="纳税年度，当前选择：$selectedYear"
    }
    override fun onDetachedFromWindow(){removeCallbacks(snap);super.onDetachedFromWindow()}
}
