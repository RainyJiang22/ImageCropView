package com.base.imagecropview.ui

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.Toast
import androidx.lifecycle.distinctUntilChanged
import com.base.imagecropview.R
import com.base.imagecropview.base.BaseActivity
import com.base.imagecropview.data.ImageCropResult
import com.base.imagecropview.data.TransparentResult
import com.base.imagecropview.databinding.ActivityMainBinding
import com.blankj.utilcode.util.ImageUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {


    override fun onBundle(bundle: Bundle) {
    }

    @SuppressLint("CheckResult")
    override fun init(savedInstanceState: Bundle?) {

        val sourceBitmap = ImageUtils.getBitmap(R.drawable.picture)
        binding?.ivResult?.setImageBitmap(sourceBitmap)

        binding?.tvStartCrop?.setOnClickListener {

            viewModel.getCropResult(R.drawable.picture)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val size = ImageUtils.getSize(File(it))
                    val cropResult = ImageCropResult(
                        it, TransparentResult(it, Rect(0, 0, size[0], size[1]))
                    )
                    val result = viewModel.imageCropResult.value ?: cropResult

                    val cropWindow = ImageCropWindow(this, result)

                    cropWindow.mCallBack = object : ImageCropWindow.CropCallBack {
                        override fun cropSuc(
                            cropResult: ImageCropResult,
                            popupWindow: PopupWindow
                        ) {
                            viewModel.imageCropResult.value = cropResult
                            popupWindow.dismiss()
                        }

                        override fun cropFail(popupWindow: PopupWindow) {
                            popupWindow.dismiss()
                            Toast.makeText(this@MainActivity, "failed", Toast.LENGTH_SHORT).show()
                        }

                    }

                    cropWindow.showAtLocation(
                        binding?.llBottom,
                        Gravity.CENTER, 0, 0
                    )
                }
        }



        viewModel.imageCropResult
            .distinctUntilChanged()
            .observe(this) {
                val bit = ImageUtils.getBitmap(File(it.cropResult.crop))
                binding?.ivResult?.setImageBitmap(bit)
            }
    }

}