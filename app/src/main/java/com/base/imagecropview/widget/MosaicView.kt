package com.base.imagecropview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * @author jiangshiyu
 * @date 2022/6/7
 * 马赛克view
 */
class MosaicView : View {

    constructor(context: Context?) : super(context) {
        initView(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs, defStyleAttr)
    }

    private fun initView(attrs: AttributeSet?, defStyleAttr: Int) {}

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setBackgroundDrawable(TransparentClearDrawable(context))
    }
}