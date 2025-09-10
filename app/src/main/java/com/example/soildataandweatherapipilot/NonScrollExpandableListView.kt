package com.example.soildataandweatherapipilot  // change to your package

import android.content.Context
import android.util.AttributeSet
import android.widget.ExpandableListView

class NonScrollExpandableListView(context: Context, attrs: AttributeSet? = null) :
    ExpandableListView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Expand height to fit all children
        val expandSpec = MeasureSpec.makeMeasureSpec(
            Integer.MAX_VALUE shr 2, MeasureSpec.AT_MOST
        )
        super.onMeasure(widthMeasureSpec, expandSpec)
    }
}
