package com.johnson.sketchclock.pickers.illustration_picker

import android.annotation.SuppressLint
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")
class SquareImageView @JvmOverloads constructor(
    context: android.content.Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}