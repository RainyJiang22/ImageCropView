package com.base.imagecropview.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.base.imagecropview.AppApplication
import com.base.imagecropview.PictureCropHelper
import com.base.imagecropview.data.ImageCropResult
import com.base.imagecropview.data.TransparentResult
import com.base.imagecropview.databinding.FragmentImageCropBinding
import com.blankj.utilcode.util.ImageUtils
import java.io.File
import java.util.*

/**
 * @author jiangshiyu
 * @date 2022/6/8
 */
class ImageCropWindow(
    context: Context,
    imageCropResult: ImageCropResult
) : PopupWindow() {

    val CROP_DIR = File(AppApplication.getApplication()!!.filesDir, "crop_temp")


    private var mCropBinding: FragmentImageCropBinding? = null
    private var mOriginBitmap: Bitmap? = null
    var mCallBack: CropCallBack? = null

    init {
        width = ConstraintLayout.LayoutParams.MATCH_PARENT
        height = ConstraintLayout.LayoutParams.MATCH_PARENT
        isFocusable = true
        mCropBinding = FragmentImageCropBinding.inflate(LayoutInflater.from(context))
        contentView = mCropBinding?.root

        mCropBinding?.cropView?.let {
            mOriginBitmap = BitmapFactory.decodeFile(imageCropResult.origin)
            PictureCropHelper.startCrop(
                context,
                mOriginBitmap!!,
                imageCropResult.cropResult.rect,
                null,
                it
            )
        }
        mCropBinding?.btnCrop?.setOnClickListener {

            val cropView = mCropBinding?.cropView
            val cropResult = cropView?.let { crop -> PictureCropHelper.getCropResult(crop) }

            if (cropResult == null) {
                mCallBack?.cropFail(this)
            } else {
                val originCompress =
                    File(CROP_DIR, "${UUID.randomUUID()}.png")
                ImageUtils.save(
                    cropResult.crop,
                    originCompress,
                    Bitmap.CompressFormat.PNG,
                    false
                )

                //原图上进行裁剪，直接使用抠图区域的结果
                val cropInSource =
                    TransparentResult(originCompress.absolutePath, cropResult.rectInSource)

                val afterCrop = imageCropResult.copy(cropResult = cropInSource)
                mCallBack?.cropSuc(afterCrop, this)
            }

        }
    }


    override fun dismiss() {
        super.dismiss()
        mCropBinding = null
        if (mOriginBitmap != null && mOriginBitmap?.isRecycled == true) {
            mOriginBitmap?.recycle()
        }
    }

    interface CropCallBack {

        fun cropSuc(cropResult: ImageCropResult, popupWindow: PopupWindow)

        fun cropFail(popupWindow: PopupWindow)
    }
}