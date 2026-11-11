package rs.zzijad.boxoffz

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Mali pomoćnici za UI pravljen iz koda (bez XML layouta). */
fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.col(id: Int) = getColor(id)

fun Context.text(s: String, sp: Float, color: Int = R.color.text, bold: Boolean = false) =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(col(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

fun Context.button(s: String, onClick: () -> Unit) = Button(this).apply {
    text = s
    isAllCaps = false
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    setTextColor(getColorStateList(R.color.btn_text))
    setBackgroundResource(R.drawable.bg_btn)
    stateListAnimator = null
    isFocusable = true
    setPadding(dp(28), 0, dp(28), 0)
    minWidth = dp(180)
    setOnClickListener { onClick() }
}

fun LinearLayout.add(v: android.view.View, w: Int = WRAP_CONTENT, h: Int = WRAP_CONTENT, top: Int = 0, side: Int = 0) {
    addView(v, LinearLayout.LayoutParams(w, h).apply {
        topMargin = context.dp(top); marginStart = context.dp(side); marginEnd = context.dp(side)
    })
}

fun Context.column() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    setBackgroundColor(col(R.color.bg))
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
}
