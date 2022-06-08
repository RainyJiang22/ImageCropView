package com.base.imagecropview.data

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author jiangshiyu
 * @date 2022/6/7
 * 裁剪区域
 */
@Parcelize
data class ImageCropResult(
    /**
     * 原图路径
     */
    val origin: String,

    /**
     * 区域裁剪图
     */
    val cropResult: TransparentResult,
) : Parcelable


@Parcelize
data class TransparentResult(
    /**
     * 裁剪后bitmap路径
     */
    val crop: String,

    /**
     * bitmap在原图的位置
     */
    val rect: Rect
) : Parcelable