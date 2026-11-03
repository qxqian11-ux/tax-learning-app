package cn.taxlearning.ledger

import android.content.Context
import android.widget.ScrollView

/** No top/bottom edge glow or stretch; normal content scrolling remains available. */
internal class ClampedPageScrollView(context:Context):ScrollView(context) {
    init { overScrollMode=OVER_SCROLL_NEVER;isVerticalFadingEdgeEnabled=false;isHorizontalFadingEdgeEnabled=false }
    override fun overScrollBy(dx:Int,dy:Int,x:Int,y:Int,rangeX:Int,rangeY:Int,maxX:Int,maxY:Int,touch:Boolean):Boolean =
        super.overScrollBy(dx,dy,x,y,rangeX,rangeY,0,0,touch)
}
