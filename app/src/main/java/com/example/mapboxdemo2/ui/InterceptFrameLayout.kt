package com.example.mapboxdemo2.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class InterceptFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var initialX = 0f
    private var initialY = 0f
    private val touchSlop = 30 // スワイプ認定する距離（ピクセルで調整）

    private var isFirstMoveAfterIntercept = false
    private var interceptOffsetAngle = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                return false // まず子に渡す（ドラッグやタップを優先）
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(ev.x - initialX)
                val dy = Math.abs(ev.y - initialY)
                // 一定距離以上スワイプされたら親で奪う（ホイール回転用）
                if (dx > touchSlop || dy > touchSlop) {
                    isFirstMoveAfterIntercept = true
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 必要に応じて初期化
            }
            MotionEvent.ACTION_MOVE -> {
                if (isFirstMoveAfterIntercept) {
                    // ★ここで回転の基準値などの補正処理を今後追加（サンプル：補正フラグリセットのみ）
                    // 例：interceptOffsetAngle = ...（子View/外部から値を取得し補正する処理を入れる）
                    isFirstMoveAfterIntercept = false
                }
                // 以降は通常の回転やドラッグ処理を実装
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isFirstMoveAfterIntercept = false
            }
        }
        return true
    }
}