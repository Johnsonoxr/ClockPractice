package com.johnson.sketchclock.pickers.illustration_picker

import android.annotation.SuppressLint
import android.widget.ImageView
import com.johnson.sketchclock.R
import kotlin.math.roundToInt

@SuppressLint("AppCompatCustomView")
class RatioImageView @JvmOverloads constructor(
    context: android.content.Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var aspectRatio: Float = 1f

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RatioImageView)
        aspectRatio = typedArray.getFloat(R.styleable.RatioImageView_aspectRatio, 1f)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * aspectRatio).roundToInt()

        setMeasuredDimension(width, height)
    }

}