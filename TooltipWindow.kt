import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs


private class TooltipWindow(
    private val targetView: View,
    contentView: View,
    contentLayoutWidth: Int,
    @ColorRes anchorColor: Int? = null
) {
    companion object {
        private const val DRAW_LEFT = 1
        private const val DRAW_RIGHT = 2
        private const val DRAW_BOTTOM = 4
    }

    private val arrowSize = 33
    private val marginFromStart = 40

    private var popupWindow: PopupWindow? = null
    private var position_x: Int? = null
    private var position_y: Int? = null

    init {
        val ctx = targetView.context
        val contentRootView = LinearLayout(ctx)

        val contentRootLayoutWidth = contentLayoutWidth + arrowSize

        val anchorView = getArrowView(ctx, anchorColor)

        //Get target screen positions
        var targetRect = getTargetRect()

        //Compute direction to targetView according to the available space left for popup window in right , left or bottom of target view
        val directionToTarget = getDirectionToTargetView(ctx, targetRect, contentRootLayoutWidth)

        //Adding arrow and content view to linear layout considering the direction to targetView
        when (directionToTarget) {
            DRAW_LEFT -> {
                contentRootView.orientation = LinearLayout.HORIZONTAL
                if (LocaleHelper.isCurrentLanguageRTL(ctx)) {
                    contentRootView.addView(anchorView)
                    contentRootView.addView(contentView)
                } else {
                    contentRootView.addView(contentView)
                    contentRootView.addView(anchorView)
                }
            }

            DRAW_RIGHT -> {
                contentRootView.orientation = LinearLayout.HORIZONTAL
                if (LocaleHelper.isCurrentLanguageRTL(ctx)) {
                    contentRootView.addView(contentView)
                    contentRootView.addView(anchorView)
                } else {
                    contentRootView.addView(anchorView)
                    contentRootView.addView(contentView)
                }
            }

            DRAW_BOTTOM -> {
                contentRootView.orientation = LinearLayout.VERTICAL
                contentRootView.addView(anchorView)
                contentRootView.addView(contentView)
            }
        }

        setArrowLayoutParams(arrowSize, marginFromStart, anchorView, directionToTarget)
        setContentViewLayoutParams(contentView as ViewGroup, contentLayoutWidth)

        popupWindow = popupWindow(
            ctx,
            contentRootLayoutWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            contentRootView
        )
        popupWindow?.setBackgroundDrawable(null)

        //Get the final position for popup window based on target location and arrow margin and content view width
        computeFinalPositionXY(ctx, directionToTarget, contentRootLayoutWidth)
    }

    private fun computeFinalPositionXY(
        ctx: Context,
        directionToTarget: Int,
        contentRootLayoutWidth: Int
    ) {
        val targetRect = getTargetRect()
        when (directionToTarget) {
            DRAW_BOTTOM -> {
                if (LocaleHelper.isCurrentLanguageRTL(ctx)) {
                    position_x =
                        targetRect.centerX() - contentRootLayoutWidth + marginFromStart + arrowSize / 2
                    position_y = targetRect.bottom
                } else {
                    position_x =
                        targetRect.centerX() - marginFromStart - arrowSize / 2
                    position_y = targetRect.bottom
                }
            }
            DRAW_LEFT -> {
                position_x = targetRect.left - contentRootLayoutWidth
                position_y = targetRect.centerY() - marginFromStart - arrowSize / 2
            }
            DRAW_RIGHT -> {
                position_x = targetRect.right
                position_y = targetRect.centerY() - marginFromStart - arrowSize / 2
            }
        }
    }

    private fun getTargetRect(): Rect {
        val screenPos = IntArray(2)
        targetView.getLocationOnScreen(screenPos)
        val targetRect = Rect(
            screenPos[0], screenPos[1], screenPos[0]
                    + targetView.width, screenPos[1] + targetView.height
        )
        return targetRect
    }

    //Function to compute direction to targetView according to the available space left for popup window in right , left or bottom of target view
    private fun getDirectionToTargetView(
        ctx: Context,
        targetRect: Rect,
        contentLayoutWidth: Int
    ): Int {
        val metrics: DisplayMetrics = ctx.resources.displayMetrics
        val width = metrics.widthPixels
        val leftSpace = targetRect.left
        val rightSpace = width - targetRect.right
        return when {
            abs(rightSpace - leftSpace) < contentLayoutWidth -> DRAW_BOTTOM
            rightSpace > leftSpace -> DRAW_RIGHT
            else -> DRAW_LEFT
        }
    }

    private fun setContentViewLayoutParams(layout: ViewGroup, contentLayoutWidth: Int) {
        layout.layoutParams = LinearLayout.LayoutParams(
            contentLayoutWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun popupWindow(
        ctx: Context?,
        width: Int,
        height: Int,
        contentRootView: LinearLayout
    ): PopupWindow {
        val popupWindow = PopupWindow(ctx)
        popupWindow.isOutsideTouchable = true
        popupWindow.isTouchable = true
        popupWindow.isFocusable = false
        popupWindow.height = height
        popupWindow.width = width
        popupWindow.contentView = contentRootView
        return popupWindow
    }

    private fun getArrowView(
        ctx: Context,
        anchorColor: Int?,
    ): ImageView {
        val arrowView = ImageView(ctx)
        arrowView.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.triangle))
        if (anchorColor != null) {
            arrowView.setColorTint(anchorColor)
        }
        return arrowView
    }

    private fun setArrowLayoutParams(
        arrowSize: Int,
        marginFromStart: Int,
        arrowView: ImageView,
        directionToTargetView: Int,
    ) {
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(arrowSize, arrowSize)
        when (directionToTargetView) {
            DRAW_LEFT -> {
                layoutParams.setMargins(0, marginFromStart, 0, 0)
                arrowView.rotation = 90f
            }
            DRAW_RIGHT -> {
                layoutParams.setMargins(0, marginFromStart, 0, 0)
                arrowView.rotation = 270f
            }

            DRAW_BOTTOM ->
                if (LocaleHelper.isCurrentLanguageRTL(arrowView.context)) {
                    layoutParams.setMargins(0, 0, marginFromStart, 0)
                } else {
                    layoutParams.setMargins(marginFromStart, 0, 0, 0)
                }
        }
        arrowView.layoutParams = layoutParams
    }

    fun showPopupWindow(): PopupWindow? {
        if (position_x != null && position_y != null) {
            popupWindow?.showAtLocation(
                targetView,
                Gravity.NO_GRAVITY,
                position_x ?: 0,
                position_y ?: 0
            )
        }
        return popupWindow
    }

    fun closeWindow() {
        popupWindow?.dismiss()
    }

    fun setOnDismissListener(onDismissListener: PopupWindow.OnDismissListener) {
        popupWindow?.setOnDismissListener(onDismissListener)
    }
}

//This handler works on the top of ToolTipWindow object , Because ToolTipWindow , for multiple pop up on same page 
// they can overlap , so it takes request one at at time and schedules the last one once current window dismissed
object ToolTipWindowHandler {

    private var popupWindow: WeakReference<PopupWindow>? = null
    private val requestQueue: Queue<TooltipWindowRequest> = LinkedList()
    private val lock = Any()

    data class TooltipWindowRequest(
        val targetView: WeakReference<View>,
        val contentView: WeakReference<View>,
        val contentLayoutWidth: Int,
        @ColorRes val anchorColor: Int? = null
    )

    fun dismissWindow() {
        popupWindow?.get()?.apply {
            dismiss()
        }
    }

    fun showToolTipWindow(
        targetView: View,
        contentView: View,
        contentLayoutWidthInDp: Int,
        @ColorRes anchorColor: Int? = null,
    ) {
        synchronized(lock) {
            try {
                if (popupWindow == null) {
                    TooltipWindow(
                        targetView,
                        contentView,
                        CommonUtils.dpToPx(targetView.context, contentLayoutWidthInDp).toInt(),
                    ).apply {
                        val popup = showPopupWindow()
                        popupWindow = WeakReference(popup)
                        setOnDismissListener {
                            popup?.setOnDismissListener(null)
                            popupWindow = null
                            runOnMainThread {
                                checkQueueForPendingRequestIfAny()
                            }
                        }
                    }

                } else {
                    addToRequestQueue(
                        TooltipWindowRequest(
                            WeakReference(targetView),
                            WeakReference(contentView),
                            contentLayoutWidthInDp,
                            anchorColor
                        )
                    )
                }
            } catch (e: Exception) {
                //Just to fail safe , till stable , as crashing because of coach mark not make sense
            }
        }
    }

    private fun checkQueueForPendingRequestIfAny() {
        while (requestQueue.isNotEmpty()) {
            val peek = requestQueue.peek()
            var isPeekConsumed = false
            peek?.apply {
                val targetView = targetView.get()
                val contentView = contentView.get()
                if (targetView?.isAttachedToWindow == true && contentView != null) {
                    showToolTipWindow(
                        targetView,
                        contentView,
                        contentLayoutWidth,
                        anchorColor
                    )
                    isPeekConsumed = true
                }
            }
            requestQueue.poll()
            if (isPeekConsumed)
                break
        }
    }

    private fun addToRequestQueue(tooltipWindowRequest: TooltipWindowRequest) {
        requestQueue.add(tooltipWindowRequest)
    }
}
