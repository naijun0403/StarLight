package dev.mooner.starlight.utils

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import coil.ImageLoader
import coil.imageLoader
import coil.load
import coil.request.Disposable
import coil.request.ImageRequest
import dev.mooner.starlight.plugincore.logger.LoggerFactory
import kotlin.reflect.full.primaryConstructor

private val LOG = LoggerFactory.logger {  }

fun NestedScrollView.bindFadeImage(imageView: ImageView) {
    setOnScrollChangeListener { _, _, scrollY, _, _ ->
        if (scrollY in 0..200) {
            imageView.alpha = 1f - (scrollY / 200.0f)
        } else {
            imageView.alpha = 0f
        }
    }
}

fun dpToPx(context: Context, dp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
}

inline fun ImageView.loadWithTint(
    @DrawableRes drawableResId: Int,
    @ColorRes tintColor: Int?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    ImageViewCompat.setImageTintList(this, tintColor?.let { color -> ColorStateList.valueOf(context.getColor(color)) })
    return load(drawableResId, imageLoader, builder)
}

inline fun ImageView.loadWithTint(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    @ColorRes
    tintColor: Int?,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    ImageViewCompat.setImageTintList(this, if (tintColor == null) null else ColorStateList.valueOf(context.getColor(tintColor)))
    return load(data, imageLoader, builder)
}

private fun ProgressBar.setProgressGraceful(to: Int) {
    max = width

    val start = progress
    val end = ((to.toFloat() / 100f) * width).toInt()
    LOG.verbose { "Animating progressbar to= $to, start= $start, end= $end" }
    val animator = ValueAnimator.ofInt(start, end).apply {
        interpolator = AccelerateDecelerateInterpolator()
        startDelay = 0
        duration = 200
        addUpdateListener { valueAnimator ->
            progress = valueAnimator.animatedValue as Int
        }
    }

    animator.start()
}

var ProgressBar.graceProgress: Int
    get() = this.progress / width
    set(value) {
        setProgressGraceful(value)
    }

private val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

fun dp(value: Int): Int =
    value.toPx.toInt()
fun dp(value: Float): Float =
    value.toPx

inline fun View.applyLayoutParams(block: ViewGroup.LayoutParams.() -> Unit) {
    applyLayoutParams<ViewGroup.LayoutParams>(block)
}

@JvmName("applyLayoutParamsTyped")
inline fun <reified T : ViewGroup.LayoutParams> View.applyLayoutParams(
    block: T.() -> Unit
) {
    val params = (layoutParams ?: T::class.primaryConstructor!!.call(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)) as T
    block(params)
    layoutParams = params
}

fun getScreenSizeDp(context: Context): Pair<Float, Float> {
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val dpHeight: Float = displayMetrics.heightPixels / displayMetrics.density
    val dpWidth: Float = displayMetrics.widthPixels / displayMetrics.density
    return dpWidth to dpHeight
}