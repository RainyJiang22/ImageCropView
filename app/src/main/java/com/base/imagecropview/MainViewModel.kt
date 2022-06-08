package com.base.imagecropview

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.base.imagecropview.data.ImageCropResult
import com.blankj.utilcode.util.ImageUtils
import io.reactivex.Observable
import java.io.File
import java.util.*

/**
 * @author jiangshiyu
 * @date 2022/6/8
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val PIC_DIR =
        File(AppApplication.getApplication()!!.filesDir, "picture_temp")

    val imageCropResult = MutableLiveData<ImageCropResult>()


    fun getCropResult(@DrawableRes resId: Int): Observable<String> {
        return Observable.just(resId)
            .map {
                val originCompress = File(PIC_DIR, "${UUID.randomUUID()}.png")
                ImageUtils.save(
                    ImageUtils.getBitmap(it),
                    originCompress,
                    Bitmap.CompressFormat.PNG,
                    true
                )
                originCompress.toString()
            }
    }
}