package com.base.imagecropview

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import cn.nekocode.rxlifecycle.LifecycleEvent
import cn.nekocode.rxlifecycle.compact.RxLifecycleCompact
import com.base.imagecropview.base.BaseActivity
import com.base.imagecropview.base.EmptyViewModel
import com.base.imagecropview.data.ImageCropResult
import com.base.imagecropview.data.TransparentResult
import com.base.imagecropview.databinding.ActivityMainBinding
import com.blankj.utilcode.util.ImageUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    private var currentShowFragment: Fragment? = null

    private var imageCropFragment: ImageCropFragment? = null

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
                    Log.d("crop", "init: $it")
                    val size = ImageUtils.getSize(File(it))
                    val cropResult = ImageCropResult(
                        it, TransparentResult(it, Rect(0, 0, size[0], size[1]))
                    )
                    val result = viewModel.imageCropResult.value ?: cropResult
                    imageCropFragment = ImageCropFragment(result)
                    binding?.flFragment?.visibility = View.VISIBLE
                    showFragment(imageCropFragment!!)
                }
        }

        imageCropFragment?.mCallBack = object : ImageCropFragment.CropCallBack {
            override fun cropSuc(cropResult: ImageCropResult) {
                viewModel.imageCropResult.value = cropResult
                binding?.flFragment?.visibility = View.INVISIBLE
                closeFragment()
            }

            override fun cropFail() {
                Toast.makeText(this@MainActivity, "failed", Toast.LENGTH_SHORT).show()
            }

        }

        viewModel.viewModelScope
            .launch {
                viewModel.imageCropResult
                    .distinctUntilChanged()
                    .asFlow()
                    .collect {
                        val bit = ImageUtils.getBitmap(File(it.cropResult.crop))
                        binding?.ivResult?.setImageBitmap(bit)
                    }
            }
    }

    private fun showFragment(fragment: Fragment) {
        closeFragment()
        currentShowFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, currentShowFragment!!)
            .commitAllowingStateLoss()
    }

    /**
     * 返回是否有fragment被remove了
     */
    private fun closeFragment(): Boolean {
        currentShowFragment?.let {
            if (!it.isHidden) {
                supportFragmentManager.beginTransaction()
                    .remove(it).commitAllowingStateLoss()
                currentShowFragment = null
                return true
            } else {
                return false
            }
        }
        return false
    }

}