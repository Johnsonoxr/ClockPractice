package com.johnson.sketchclock.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.johnson.sketchclock.R
import java.lang.ref.WeakReference

fun View.scaleIn(duration: Int = 200, onEnd: (() -> Unit)? = null) {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration.toLong())
        .setInterpolator(OvershootInterpolator())
        .withEndAction {
            onEnd?.invoke()
        }
        .start()
}

fun View.scaleOut(duration: Int = 200, onEnd: (() -> Unit)? = null) {
    animate()
        .scaleX(0f)
        .scaleY(0f)
        .setInterpolator(DecelerateInterpolator())
        .setDuration(duration.toLong())
        .withEndAction {
            visibility = View.GONE
            onEnd?.invoke()
        }
        .start()
}

fun View.getAttrColor(attr: Int): Int {
    val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()
    return color
}

fun Context.getAttrColor(attr: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()
    return color
}

fun View.addCancelObserverView(onCancel: () -> Unit): Boolean {
    if (((this.getTag(R.id.cancel_observer) as? WeakReference<*>)?.get() as? View)?.parent != null) {
        // already has a cancel observer view
        return false
    }

    (parent as? ViewGroup)?.let { viewGroup ->
        val cancelView = View(context)
        cancelView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        cancelView.setOnClickListener {
            viewGroup.removeView(cancelView)
            onCancel()
        }
        viewGroup.addView(cancelView, viewGroup.indexOfChild(this))
        this.setTag(R.id.cancel_observer, WeakReference(cancelView))
        return true
    }
    return false
}

fun View.removeCancelObserverView() {
    ((this.getTag(R.id.cancel_observer) as? WeakReference<*>)?.get() as? View)?.let { cancelView ->
        this.setTag(R.id.cancel_observer, null)
        (cancelView.parent as? ViewGroup)?.removeView(cancelView)
    }
}

fun View.tintBackground(color: Int) {
    background?.setTint(color)
}

fun View.tintBackgroundAttr(attr: Int) {
    background?.setTint(getAttrColor(attr))
}