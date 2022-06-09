package com.base.image_crop.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.base.image_crop.R

/**
 * @author jiangshiyu
 * @date 2022/6/7
 * 默认方格背景drawable
 */
class TransparentClearDrawable(context: Context) : Drawable() {

    private val mTransparentPaint: Paint

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.drawRect(bounds, mTransparentPaint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        mTransparentPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mTransparentPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    init {
        // 透明背景
        val transparentBitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_shape_edit_transparent)
        val transparentShader =
            BitmapShader(transparentBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        mTransparentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTransparentPaint.isDither = true
        mTransparentPaint.isAntiAlias = true
        mTransparentPaint.shader = transparentShader

    }
}