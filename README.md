# ImageCropView

自定义裁剪view，初始化裁剪区域，支持保留上次裁剪在原图上的区域记录

### 自定义view(**ImageCropView**,**MosaicView**)

- 可自由调整裁剪框样式，宽高比
- 支持保留上次裁剪选区，第一次默认裁剪框选取整张图

### 快速开始

- Add it in your root build.gradle at the end of repositories

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

- Add the dependency

```
dependencies {
    implementation 'com.github.RainyJiang22:ImageCropView:v1.0.1'
}
```

### 使用方法

- 调用PictureCropHelper.startCrop方法进行裁剪

```kotlin
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
```

- 裁剪完成后，调用PictureCropHelper.getCropResult方法获取裁剪结果

```kotlin
  fun getCropResult(cropView: ImageCropView): ImageCropData? {
    return cropView.croppedBitmap
}
```

### 注意

**需要自定义数据，然后在每次裁剪的时候调用，初次裁剪时可使用原图路径，将原图作为区域裁剪图传入,具体可以看下面**

```kotlin
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
```

**demo使用Livedata将我们自定义的数据类持有，在每次裁剪完成后更新它在原图的裁剪区域**

```kotlin
viewModel.imageCropResult
    .distinctUntilChanged()
    .observe(this) {
        val bit = ImageUtils.getBitmap(File(it.cropResult.crop))
        binding?.ivResult?.setImageBitmap(bit)
    }
```

仅供参考，希望能您有所帮助
