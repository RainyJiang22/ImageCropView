package com.base.imagecropview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.ColorInt
import com.base.imagecropview.data.ImageCropData
import com.base.imagecropview.widget.ImageCropView

/**
 * @author jiangshiyu
 * @date 2022/6/7
 */
object PictureCropHelper {


    fun startCrop(
        context: Context,
        currentBitmap: Bitmap,
        rect: Rect,
        @ColorInt maskColor: Int?,
        cropView: ImageCropView,
    ) {
        val resultDrawable = BitmapDrawable(context.resources, currentBitmap)
        cropView.setImageDrawable(resultDrawable)
        cropView.maskColor = maskColor
        cropView.initRect = rect
    }


    /**
     * 获取裁剪后的图片
     */
    fun getCropResult(cropView: ImageCropView): ImageCropData? {
        return cropView.croppedBitmap
    }
}