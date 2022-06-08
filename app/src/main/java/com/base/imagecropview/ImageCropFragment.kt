package com.base.imagecropview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.base.imagecropview.base.BaseFragment
import com.base.imagecropview.base.EmptyViewModel
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
class ImageCropFragment(val imageCropResult: ImageCropResult) :
    BaseFragment<FragmentImageCropBinding, EmptyViewModel>() {

    companion object {
        val CROP_DIR = File(AppApplication.getApplication()!!.filesDir, "crop_temp")
    }

    private var mOriginBitmap: Bitmap? = null

    var mCallBack: CropCallBack? = null

    override fun onBundle(bundle: Bundle) {

    }

    override fun init(savedInstanceState: Bundle?) {
        binding?.cropView?.let {
            mOriginBitmap = BitmapFactory.decodeFile(imageCropResult.origin)
            PictureCropHelper.startCrop(
                requireContext(),
                mOriginBitmap!!,
                imageCropResult.cropResult.rect,
                null,
                it
            )
        }

        binding?.btnCrop?.setOnClickListener {

            val cropView = binding?.cropView
            val cropResult = cropView?.let { crop -> PictureCropHelper.getCropResult(crop) }

            if (cropResult == null) {
                mCallBack?.cropFail()
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
                mCallBack?.cropSuc(afterCrop)
            }

        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (mOriginBitmap != null && mOriginBitmap?.isRecycled == true) {
            mOriginBitmap?.recycle()
        }
    }


    interface CropCallBack {

        fun cropSuc(cropResult: ImageCropResult)

        fun cropFail()
    }
}