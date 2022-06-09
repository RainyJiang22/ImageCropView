package com.base.image_crop.bean

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * @author jiangshiyu
 * @date 2022/6/7
 */
data class ImageCropData(val crop: Bitmap, val rectInSource: Rect)