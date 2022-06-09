package com.base.image_crop.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import com.base.image_crop.R
import com.base.image_crop.bean.ImageCropData
import com.blankj.utilcode.util.SizeUtils
import java.lang.Exception
import kotlin.math.roundToInt

/**
 * 裁剪的View类
 */
class ImageCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // view的宽高
    private var mViewWidth = 0
    private var mViewHeight = 0

    // 原图缩放至 View 显示的比例
    private var mCropScale = 1.0f

    // 旋转角度
    private var mImgAngle = 0.0f

    // 图片宽度
    private var mImgWidth = 0.0f

    // 图片高度
    private var mImgHeight = 0.0f

    // 四角线的长度值
    private var mHandleSize: Int

    // 四角的线宽度
    private var mHandleWidth = 0

    // 触摸可处理的宽度值
    private var mTouchPadding = 0

    // 裁剪框最小值
    private var mFrameMinSize: Int

    // 裁剪外框线框宽度
    private var mFrameStrokeWeight = 2f

    // 裁剪指导线宽度
    private var mGuideStrokeWeight = 2f

    // 外框画笔
    private val mFramePaint: Paint

    // 半透明区域画笔
    private val mTranslucentPaint: Paint

    // bitmap画笔
    private val mBitmapPaint: Paint

    // 背景的颜色
    private var mBackgroundColor: Int

    // 半透明框的颜色
    private var mOverlayColor: Int

    // 外框的颜色
    private var mFrameColor: Int

    // 四边角点的颜色
    private var mHandleColor: Int

    // 指导线颜色
    private var mGuideColor: Int

    // 裁剪模式
    private var mCropMode: CropModeEnum? = CropModeEnum.FREE

    // 指导线的显示模式
    private var mGuideShowMode: ShowModeEnum? = ShowModeEnum.SHOW_ALWAYS

    // 处理四个角的显示模式
    private var mHandleShowMode: ShowModeEnum? = ShowModeEnum.SHOW_ALWAYS

    // 触摸的情况
    private var mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS

    // 居中中点的矩型区域
    private var mCenter = PointF()

    // 图形的矩阵类
    private var mMatrix: Matrix? = null

    // 图片的rectf区域
    private var mImageRectF: RectF? = null

    // 裁剪框的RectF
    private var mFrameRectF: RectF? = null

    // 初始化的比例大小0.01到1.0，默认情况为0.75
    private var mInitialFrameScale = 0f

    // 是否初始化
    private var mIsInitialized = false

    // 是否开始裁剪
    private var mIsCropEnabled = true

    // 是否显示指导线
    private var mShowGuide = true

    // 是否显示处理线
    private var mShowHandle = true
    private var mLastX = 0f
    private var mLastY = 0f

    // 旋转的动画
    private var mValueAnimator: ValueAnimator? = null

    // 裁剪框放大的动画
    private var mFrameValueAnimator: ValueAnimator? = null
    private val mAnimationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS
    private var mIsAnimating = false
    private var mOnCropListener: OnCropListener? = null


    /**
     * 初始化裁剪区域，针对原图（未缩放）
     */
    var initRect: Rect? = null
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
        get


    fun setBgColor(@ColorInt backgroundColor: Int) {
        mBackgroundColor = backgroundColor
        invalidate()
    }

    /**
     * 加载Style自定义属性数据
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    @SuppressLint("CustomViewStyleable")
    private fun loadStyleable(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CropView, defStyleAttr, 0)
        val drawable: Drawable?
        try {
            drawable = ta.getDrawable(R.styleable.CropView_img_src)
            drawable?.let { setImageDrawable(it) }
            for (mode in CropModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_crop_mode, CropModeEnum.FREE.iD) == mode.iD) {
                    mCropMode = mode
                    break
                }
            }
            mBackgroundColor = ta.getColor(R.styleable.CropView_background_color, TRANSPARENT)
            mOverlayColor = ta.getColor(R.styleable.CropView_overlay_color, TRANSLUCENT_BLACK)
            mFrameColor = ta.getColor(R.styleable.CropView_frame_color, WHITE)
            mHandleColor = ta.getColor(R.styleable.CropView_handle_color, WHITE)
            mGuideColor = ta.getColor(R.styleable.CropView_guide_color, TRANSLUCENT_WHITE)
            for (mode in ShowModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_guide_show_mode, 1) == mode.id) {
                    mGuideShowMode = mode
                    break
                }
            }
            for (mode in ShowModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_handle_show_mode, 1) == mode.id) {
                    mHandleShowMode = mode
                    break
                }
            }
            setGuideShowMode(mGuideShowMode)
            setHandleShowMode(mHandleShowMode)
            mHandleSize = ta.getDimensionPixelSize(
                R.styleable.CropView_handle_size, SizeUtils.dp2px(
                    HANDLE_SIZE.toFloat()
                )
            )
            mHandleWidth = ta.getDimensionPixelSize(
                R.styleable.CropView_handle_width, SizeUtils.dp2px(
                    HANDLE_WIDTH.toFloat()
                )
            )
            mTouchPadding = ta.getDimensionPixelSize(R.styleable.CropView_touch_padding, 0)
            mFrameMinSize = ta.getDimensionPixelSize(
                R.styleable.CropView_min_frame_size, SizeUtils.dp2px(
                    FRAME_MIN_SIZE.toFloat()
                )
            )
            mFrameStrokeWeight = ta.getDimensionPixelSize(
                R.styleable.CropView_frame_stroke_weight, SizeUtils.dp2px(
                    FRAME_STROKE_WEIGHT.toFloat()
                )
            ).toFloat()
            mGuideStrokeWeight = ta.getDimensionPixelSize(
                R.styleable.CropView_guide_stroke_weight, SizeUtils.dp2px(
                    GUIDE_STROKE_WEIGHT.toFloat()
                )
            ).toFloat()
            mIsCropEnabled = ta.getBoolean(R.styleable.CropView_crop_enabled, true)
            mInitialFrameScale = constrain(
                ta.getFloat(R.styleable.CropView_initial_frame_scale, DEFAULT_INITIAL_SCALE),
                0.01f,
                1.0f,
                DEFAULT_INITIAL_SCALE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ta.recycle()
        }
    }

    /**
     * 设置指导线的显示模式
     *
     * @param guideShowMode
     */
    fun setGuideShowMode(guideShowMode: ShowModeEnum?) {
        mGuideShowMode = guideShowMode
        mShowGuide = when (guideShowMode) {
            ShowModeEnum.SHOW_ALWAYS -> true
            ShowModeEnum.NOT_SHOW, ShowModeEnum.SHOW_ON_TOUCH -> false
            else -> true
        }
        invalidate()
    }

    /**
     * 设置处理线的显示模式
     *
     * @param mode
     */
    fun setHandleShowMode(mode: ShowModeEnum?) {
        mHandleShowMode = mode
        mShowHandle = when (mode) {
            ShowModeEnum.SHOW_ALWAYS -> true
            ShowModeEnum.NOT_SHOW, ShowModeEnum.SHOW_ON_TOUCH -> false
            else -> true
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(viewWidth, viewHeight)
        mViewWidth = viewWidth - paddingLeft - paddingRight
        mViewHeight = viewHeight - paddingTop - paddingBottom
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        doLayout(mViewWidth, mViewHeight)
    }

    val paint = Paint()

    /**
     * 原图上的蒙层
     */
    var maskColor: Int? = null


    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        if (mIsInitialized && bitmap != null) {
            // setMatrix()
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), paint)
            canvas.drawBitmap(bitmap!!, mMatrix!!, mBitmapPaint)
            maskColor?.let {
                //蒙层绘制
                canvas.drawColor(it, PorterDuff.Mode.SRC_ATOP)
            }
            canvas.restore()
            // 画裁剪框
            drawCropFrame(canvas)
        }
    }

    /**
     * 画裁剪框
     *
     * @param canvas
     */
    private fun drawCropFrame(canvas: Canvas) {
        if (!mIsCropEnabled) {
            // 如果不是裁剪模式，直接就不画裁剪框
            return
        }
        drawOverlay(canvas)
        drawFrame(canvas)
        if (mShowHandle) {
            drawHandleLines(canvas)
        }
    }

    /**
     * 画四角的线
     *
     * @param canvas
     */
    private fun drawHandleLines(canvas: Canvas) {
        mFramePaint.color = mHandleColor
        mFramePaint.style = Paint.Style.FILL
        // 指导线最边界（最左/最右/最下/最上）的x和y
        val handleLineLeftX = mFrameRectF!!.left - mHandleWidth / 2
        val handleLineRightX = mFrameRectF!!.right + mHandleWidth / 2
        val handleLineTopY = mFrameRectF!!.top - mHandleWidth / 2
        val handleLineBottomY = mFrameRectF!!.bottom + mHandleWidth / 2


        val halfCenterX = (mFrameRectF!!.left + mFrameRectF!!.right) / 2
        val halfCenterY = (mFrameRectF!!.top + mFrameRectF!!.bottom) / 2
        //上面横线
        val topLineHorizontal =
            RectF(
                halfCenterX - mHandleSize / 2,
                handleLineTopY,
                halfCenterX + mHandleSize / 2,
                mFrameRectF!!.top + mHandleWidth / 2
            )

        val bottomLineHorizontal =
            RectF(
                halfCenterX - mHandleSize / 2,
                mFrameRectF!!.bottom - mHandleWidth / 2,
                halfCenterX + mHandleSize / 2,
                handleLineBottomY
            )

        val leftLineVertical =
            RectF(
                handleLineLeftX,
                halfCenterY - mHandleSize / 2,
                mFrameRectF!!.left + mHandleWidth / 2,
                halfCenterY + mHandleSize / 2
            )

        val rightLineVertical =
            RectF(
                mFrameRectF!!.right - mHandleWidth / 2,
                halfCenterY - mHandleSize / 2,
                handleLineRightX,
                halfCenterY + mHandleSize / 2
            )

        // 左上竖向

        val ltRectFVertical =
            RectF(
                handleLineLeftX,
                handleLineTopY,
                mFrameRectF!!.left + mHandleWidth / 2,
                handleLineTopY + mHandleSize
            )
        // 左上横向
        val ltRectFHorizontal =
            RectF(
                handleLineLeftX,
                handleLineTopY,
                handleLineLeftX + mHandleSize,
                mFrameRectF!!.top + mHandleWidth / 2
            )

        //右上横向
        val rtRectFHorizontal = RectF(
            handleLineRightX - mHandleSize,
            handleLineTopY,
            handleLineRightX,
            mFrameRectF!!.top + mHandleWidth / 2
        )
        //右上竖向
        val rtRectFVertical = RectF(
            mFrameRectF!!.right - mHandleWidth / 2,
            handleLineTopY,
            handleLineRightX,
            handleLineTopY + mHandleSize
        )
        //左下竖向
        val lbRectFVertical = RectF(
            handleLineLeftX,
            handleLineBottomY - mHandleSize,
            mFrameRectF!!.left + mHandleWidth / 2,
            mFrameRectF!!.bottom
        )
        //左下横向
        val lbRectFHorizontal = RectF(
            handleLineLeftX,
            mFrameRectF!!.bottom - mHandleWidth / 2,
            handleLineLeftX + mHandleSize,
            handleLineBottomY
        )
        //右下竖向
        val rbRectFVertical = RectF(
            mFrameRectF!!.right - mHandleWidth / 2,
            handleLineBottomY - mHandleSize,
            handleLineRightX,
            handleLineBottomY
        )
        //右上横向
        val rbRectFHorizontal = RectF(
            handleLineRightX - mHandleSize,
            mFrameRectF!!.bottom - mHandleWidth / 2,
            handleLineRightX,
            handleLineBottomY
        )
        canvas.drawRect(ltRectFVertical, mFramePaint)
        canvas.drawRect(ltRectFHorizontal, mFramePaint)
        canvas.drawRect(rtRectFVertical, mFramePaint)
        canvas.drawRect(rtRectFHorizontal, mFramePaint)
        canvas.drawRect(lbRectFVertical, mFramePaint)
        canvas.drawRect(lbRectFHorizontal, mFramePaint)
        canvas.drawRect(rbRectFVertical, mFramePaint)
        canvas.drawRect(rbRectFHorizontal, mFramePaint)
        canvas.drawRect(topLineHorizontal, mFramePaint)
        canvas.drawRect(bottomLineHorizontal, mFramePaint)
        canvas.drawRect(leftLineVertical, mFramePaint)
        canvas.drawRect(rightLineVertical, mFramePaint)
    }


    /**
     * 画裁剪框边界线
     *
     * @param canvas
     */
    private fun drawFrame(canvas: Canvas) {
        mFramePaint.style = Paint.Style.STROKE
        mFramePaint.isFilterBitmap = true
        mFramePaint.color = mFrameColor
        mFramePaint.strokeWidth = mFrameStrokeWeight
        canvas.drawRect(mFrameRectF!!, mFramePaint)
    }

    /**
     * 画裁剪框的半透明覆盖层
     *
     * @param canvas
     */
    private fun drawOverlay(canvas: Canvas) {
        mTranslucentPaint.style = Paint.Style.FILL
        mTranslucentPaint.isFilterBitmap = true
        mTranslucentPaint.color = mOverlayColor
        val path = Path()
        val overlayRectF = RectF()
        overlayRectF.set(mImageRectF!!)
        path.addRect(mFrameRectF!!, Path.Direction.CW)
        path.addRect(overlayRectF, Path.Direction.CCW)
        canvas.drawPath(path, mTranslucentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 如果还没初始化，直接跳过触摸事件
        if (!mIsInitialized) return false
        // 如果没有打开裁剪功能，那么也直接跳过触摸事件
        if (!mIsCropEnabled) return false
        // 如果是在切换裁剪模式，跳过触摸事件
        if (mIsAnimating) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onActionDown(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                onActionMove(event)
                if (mTouchArea != TouchAreaEnum.OUT_OF_BOUNDS) {
                    // 阻止父view拦截点击事件
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(true)
                onActionCancel()
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(true)
                onActionUp()
                return true
            }
            else -> {
            }
        }
        return false
    }

    private fun onActionUp() {
        if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH) mShowGuide = false
        if (mHandleShowMode == ShowModeEnum.SHOW_ON_TOUCH) mShowHandle = false
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS
        invalidate()
    }

    private fun onActionCancel() {
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS
        invalidate()
    }

    private fun onActionMove(event: MotionEvent) {
        val diffX = event.x - mLastX
        val diffY = event.y - mLastY
        when (mTouchArea) {
            TouchAreaEnum.CENTER -> {
                moveFrame(diffX, diffY)
            }
            TouchAreaEnum.LEFT_TOP -> {
                moveHandleLeftTop(diffX, diffY)
            }
            TouchAreaEnum.RIGHT_TOP -> {
                moveHandleRightTop(diffX, diffY)
            }
            TouchAreaEnum.LEFT_BOTTOM -> {
                moveHandleLeftBottom(diffX, diffY)
            }
            TouchAreaEnum.RIGHT_BOTTOM -> {
                moveHandleRightBottom(diffX, diffY)
            }
            TouchAreaEnum.CENTER_LEFT -> {
                moveHandleCenterLeft(diffX)
            }
            TouchAreaEnum.CENTER_TOP -> {
                moveHandleCenterTop(diffY)
            }
            TouchAreaEnum.CENTER_RIGHT -> {
                moveHandleCenterRight(diffX)
            }
            TouchAreaEnum.CENTER_BOTTOM -> {
                moveHandleCenterBottom(diffY)
            }
            TouchAreaEnum.OUT_OF_BOUNDS -> {
            }
        }
        invalidate()
        mLastX = event.x
        mLastY = event.y
    }

    private fun moveHandleCenterBottom(diffY: Float) {
        mFrameRectF!!.bottom += diffY
        if (isHeightTooSmall) {
            val offsetY = mFrameMinSize - mFrameRectF!!.height()
            mFrameRectF!!.bottom += offsetY
        }
        checkScaleBounds()

    }

    private fun moveHandleCenterRight(diffX: Float) {
        mFrameRectF!!.right += diffX
        if (isWidthTooSmall) {
            val offsetX = mFrameMinSize - mFrameRectF!!.width()
            mFrameRectF!!.right += offsetX
        }
        checkScaleBounds()
    }

    private fun moveHandleCenterTop(diffY: Float) {
        mFrameRectF!!.top += diffY
        if (isHeightTooSmall) {
            val offsetY = mFrameMinSize - mFrameRectF!!.height()
            mFrameRectF!!.top -= offsetY
        }
        checkScaleBounds()
    }

    private fun moveHandleCenterLeft(diffX: Float) {
        mFrameRectF!!.left += diffX
        if (isWidthTooSmall) {
            val offsetX = mFrameMinSize - mFrameRectF!!.width()
            mFrameRectF!!.left -= offsetX
        }
        checkScaleBounds()

    }

    private fun moveLineLeft(diffX: Float) {
        mFrameRectF!!.left += diffX
        if (isWidthTooSmall) {
            val offsetX = mFrameMinSize - mFrameRectF!!.width()
            mFrameRectF!!.left -= offsetX
        }
        checkScaleBounds()
    }


    private fun moveHandleRightTop(diffX: Float, diffY: Float) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF!!.right += diffX
            mFrameRectF!!.top += diffY
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.right += offsetX
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.top -= offsetY
            }
            checkScaleBounds()
        } else {
            val dy = diffX * ratioY / ratioX
            mFrameRectF!!.right += diffX
            mFrameRectF!!.top -= dy
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.right += offsetX
                val offsetY = offsetX * ratioY / ratioX
                mFrameRectF!!.top -= offsetY
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.top -= offsetY
                val offsetX = offsetY * ratioX / ratioY
                mFrameRectF!!.right += offsetX
            }
            var ox: Float
            var oy: Float
            if (!isInsideX(mFrameRectF!!.right)) {
                ox = mFrameRectF!!.right - mImageRectF!!.right
                mFrameRectF!!.right -= ox
                oy = ox * ratioY / ratioX
                mFrameRectF!!.top += oy
            }
            if (!isInsideY(mFrameRectF!!.top)) {
                oy = mImageRectF!!.top - mFrameRectF!!.top
                mFrameRectF!!.top += oy
                ox = oy * ratioX / ratioY
                mFrameRectF!!.right -= ox
            }
        }
    }

    private fun moveHandleLeftBottom(diffX: Float, diffY: Float) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF!!.left += diffX
            mFrameRectF!!.bottom += diffY
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.left -= offsetX
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.bottom += offsetY
            }
            checkScaleBounds()
        } else {
            val dy = diffX * ratioY / ratioX
            mFrameRectF!!.left += diffX
            mFrameRectF!!.bottom -= dy
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.left -= offsetX
                val offsetY = offsetX * ratioY / ratioX
                mFrameRectF!!.bottom += offsetY
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.bottom += offsetY
                val offsetX = offsetY * ratioX / ratioY
                mFrameRectF!!.left -= offsetX
            }
            var ox: Float
            var oy: Float
            if (!isInsideX(mFrameRectF!!.left)) {
                ox = mImageRectF!!.left - mFrameRectF!!.left
                mFrameRectF!!.left += ox
                oy = ox * ratioY / ratioX
                mFrameRectF!!.bottom -= oy
            }
            if (!isInsideY(mFrameRectF!!.bottom)) {
                oy = mFrameRectF!!.bottom - mImageRectF!!.bottom
                mFrameRectF!!.bottom -= oy
                ox = oy * ratioX / ratioY
                mFrameRectF!!.left += ox
            }
        }
    }

    private fun moveHandleRightBottom(diffX: Float, diffY: Float) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF!!.right += diffX
            mFrameRectF!!.bottom += diffY
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.right += offsetX
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.bottom += offsetY
            }
            checkScaleBounds()
        } else {
            val dy = diffX * ratioY / ratioX
            mFrameRectF!!.right += diffX
            mFrameRectF!!.bottom += dy
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.right += offsetX
                val offsetY = offsetX * ratioY / ratioX
                mFrameRectF!!.bottom += offsetY
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.bottom += offsetY
                val offsetX = offsetY * ratioX / ratioY
                mFrameRectF!!.right += offsetX
            }
            var ox: Float
            var oy: Float
            if (!isInsideX(mFrameRectF!!.right)) {
                ox = mFrameRectF!!.right - mImageRectF!!.right
                mFrameRectF!!.right -= ox
                oy = ox * ratioY / ratioX
                mFrameRectF!!.bottom -= oy
            }
            if (!isInsideY(mFrameRectF!!.bottom)) {
                oy = mFrameRectF!!.bottom - mImageRectF!!.bottom
                mFrameRectF!!.bottom -= oy
                ox = oy * ratioX / ratioY
                mFrameRectF!!.right -= ox
            }
        }
    }

    private fun moveHandleLeftTop(diffX: Float, diffY: Float) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF!!.left += diffX
            mFrameRectF!!.top += diffY
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.left -= offsetX
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.top -= offsetY
            }
            checkScaleBounds()
        } else {
            val dy = diffX * ratioY / ratioX
            mFrameRectF!!.left += diffX
            mFrameRectF!!.top += dy
            // 控制缩放边界
            if (isWidthTooSmall) {
                val offsetX = mFrameMinSize - mFrameRectF!!.width()
                mFrameRectF!!.left -= offsetX
                val offsetY = offsetX * ratioY / ratioX
                mFrameRectF!!.top -= offsetY
            }
            if (isHeightTooSmall) {
                val offsetY = mFrameMinSize - mFrameRectF!!.height()
                mFrameRectF!!.top -= offsetY
                val offsetX = offsetY * ratioX / ratioY
                mFrameRectF!!.left -= offsetX
                mFrameRectF!!.left -= offsetX
            }

            //限制不能让裁剪框超出图片边界
            var ox: Float
            var oy: Float
            if (!isInsideX(mFrameRectF!!.left)) {
                ox = mImageRectF!!.left - mFrameRectF!!.left
                mFrameRectF!!.left += ox
                oy = ox * ratioY / ratioX
                mFrameRectF!!.top += oy
            }
            if (!isInsideY(mFrameRectF!!.top)) {
                oy = mImageRectF!!.top - mFrameRectF!!.top
                mFrameRectF!!.top += oy
                ox = oy * ratioX / ratioY
                mFrameRectF!!.left += ox
            }
        }
    }

    /**
     * 检查缩放边界
     */
    private fun checkScaleBounds() {
        val lDiff = mFrameRectF!!.left - mImageRectF!!.left
        val rDiff = mFrameRectF!!.right - mImageRectF!!.right
        val tDiff = mFrameRectF!!.top - mImageRectF!!.top
        val bDiff = mFrameRectF!!.bottom - mImageRectF!!.bottom
        if (lDiff < 0) {
            mFrameRectF!!.left -= lDiff
        }
        if (rDiff > 0) {
            mFrameRectF!!.right -= rDiff
        }
        if (tDiff < 0) {
            mFrameRectF!!.top -= tDiff
        }
        if (bDiff > 0) {
            mFrameRectF!!.bottom -= bDiff
        }
    }

    /**
     * x 是否在图片内
     *
     * @param x
     * @return
     */
    private fun isInsideX(x: Float): Boolean {
        return mImageRectF!!.left <= x && mImageRectF!!.right >= x
    }

    private fun isInsideY(y: Float): Boolean {
        return mImageRectF!!.top <= y && mImageRectF!!.bottom >= y
    }

    private val isHeightTooSmall: Boolean
        get() = mFrameRectF!!.height() < mFrameMinSize
    private val isWidthTooSmall: Boolean
        get() = mFrameRectF!!.width() < mFrameMinSize

    private fun moveFrame(x: Float, y: Float) {
        // 1.先平移
        mFrameRectF!!.left += x
        mFrameRectF!!.right += x
        mFrameRectF!!.top += y
        mFrameRectF!!.bottom += y
        // 2.判断有没有超出界外，如果超出则后退
        handleMoveBounds()
    }

    /**
     * 控制不超出界外
     */
    private fun handleMoveBounds() {
        var diff = mFrameRectF!!.left - mImageRectF!!.left
        if (diff < 0) {
            mFrameRectF!!.left -= diff
            mFrameRectF!!.right -= diff
        }
        diff = mFrameRectF!!.right - mImageRectF!!.right
        if (diff > 0) {
            mFrameRectF!!.left -= diff
            mFrameRectF!!.right -= diff
        }
        diff = mFrameRectF!!.top - mImageRectF!!.top
        if (diff < 0) {
            mFrameRectF!!.top -= diff
            mFrameRectF!!.bottom -= diff
        }
        diff = mFrameRectF!!.bottom - mImageRectF!!.bottom
        if (diff > 0) {
            mFrameRectF!!.top -= diff
            mFrameRectF!!.bottom -= diff
        }
    }

    private fun onActionDown(event: MotionEvent) {
        invalidate()
        mLastX = event.x
        mLastY = event.y
        handleTouchArea(mLastX, mLastY)
    }

    /**
     * <Strong>控制指导线或者边框线的显示</Strong>
     *
     *
     * 处理触摸的边界来控制指导线或者边框线的显示，共5种，四个角和里面的中心部分
     *
     * @param x
     * @param y
     */
    private fun handleTouchArea(x: Float, y: Float) {
        if (isInLeftTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.LEFT_TOP
            handleGuideAndHandleMode()
            return
        }
        if (isInRightTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.RIGHT_TOP
            handleGuideAndHandleMode()
            return
        }
        if (isInLeftBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.LEFT_BOTTOM
            handleGuideAndHandleMode()
            return
        }
        if (isInRightBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.RIGHT_BOTTOM
            handleGuideAndHandleMode()
            return
        }
        if (isInCenterLeftCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_LEFT
            handleGuideAndHandleMode()
            return
        }
        if (isInCenterTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_TOP
            handleGuideAndHandleMode()
            return
        }
        if (isInCenterRightCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_RIGHT
            handleGuideAndHandleMode()
            return
        }
        if (isInCenterBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_BOTTOM
            handleGuideAndHandleMode()
            return
        }
        if (isInFrameCenter(x, y)) {
            if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH) mShowGuide = true
            mTouchArea = TouchAreaEnum.CENTER
            return
        }
        // 默认情况
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS
    }

    private fun isInCenterBottomCorner(x: Float, y: Float): Boolean {
        val dx = x - (mFrameRectF!!.left + mFrameRectF!!.width() / 2)
        val dy = y - mFrameRectF!!.bottom
        return isInsideBound(dx, dy)
    }

    private fun isInCenterRightCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.right
        val dy = y - (mFrameRectF!!.top + mFrameRectF!!.height() / 2)
        return isInsideBound(dx, dy)
    }

    private fun isInCenterTopCorner(x: Float, y: Float): Boolean {
        val dx = x - (mFrameRectF!!.left + mFrameRectF!!.width() / 2)
        val dy = y - mFrameRectF!!.top
        return isInsideBound(dx, dy)
    }

    private fun isInCenterLeftCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.left
        val dy = y - (mFrameRectF!!.top + mFrameRectF!!.height() / 2)
        return isInsideBound(dx, dy)
    }

    /**
     * 处理指导线和四角线的显示模式
     */
    private fun handleGuideAndHandleMode() {
        if (mHandleShowMode == ShowModeEnum.SHOW_ON_TOUCH) {
            mShowHandle = true
        }
        if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH) {
            mShowGuide = true
        }
    }

    private fun isInLeftTopCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.left
        val dy = y - mFrameRectF!!.top
        return isInsideBound(dx, dy)
    }

    private fun isInRightTopCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.right
        val dy = y - mFrameRectF!!.top
        return isInsideBound(dx, dy)
    }

    private fun isInLeftBottomCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.left
        val dy = y - mFrameRectF!!.bottom
        return isInsideBound(dx, dy)
    }

    private fun isInRightBottomCorner(x: Float, y: Float): Boolean {
        val dx = x - mFrameRectF!!.right
        val dy = y - mFrameRectF!!.bottom
        return isInsideBound(dx, dy)
    }

    private fun isInsideBound(dx: Float, dy: Float): Boolean {
        val d = (Math.pow(dx.toDouble(), 2.0) + Math.pow(dy.toDouble(), 2.0)).toFloat()
        return Math.pow((mHandleSize + mTouchPadding).toDouble(), 2.0) >= d
    }

    private fun isInFrameCenter(x: Float, y: Float): Boolean {
        if (mFrameRectF!!.left <= x && mFrameRectF!!.right >= x) {
            if (mFrameRectF!!.top <= y && mFrameRectF!!.bottom >= y) {
                mTouchArea = TouchAreaEnum.CENTER
                return true
            }
        }
        return false
    }

    /**
     * 限制范围
     *
     * @param value
     * @param min
     * @param max
     * @param defaultValue
     * @return
     */
    private fun constrain(value: Float, min: Float, max: Float, defaultValue: Float): Float {
        return if (value < min || value > max) defaultValue else value
    }

    /**
     * 获取当前View显示的的bitmap
     *
     * @return
     */
    private val bitmap: Bitmap?
        get() {
            var bm: Bitmap? = null
            val d = drawable
            if (d != null && d is BitmapDrawable) bm = d.bitmap
            return bm
        }

    private fun setBitmap(bitmap: Bitmap?): Bitmap? {
        var bm: Bitmap? = null
        if (bitmap != null) {
            bm = bitmap
        }
        return bm
    }

    /**
     * 对图片进行布局
     *
     * @param viewWidth
     * @param viewHeight
     */
    private fun doLayout(viewWidth: Int, viewHeight: Int) {
        if (viewHeight == 0 || viewWidth == 0) {
            return
        }
        val pointF = PointF(paddingLeft + viewWidth * 0.5f, paddingTop + viewHeight * 0.5f)
        setCenter(pointF)
        setScale(calcScale(viewWidth, viewHeight, mImgAngle))
        setMatrix()
        val rectF = RectF(0f, 0f, mImgWidth, mImgHeight)
        mImageRectF = calcImageRect(rectF, mMatrix)
        if (initRect != null) {
            //根据原图缩放比例缩放选框区域
            val offsetX = mImageRectF!!.left
            val offsetY = mImageRectF!!.top
            mFrameRectF = RectF(
                mCropScale * initRect!!.left + offsetX,
                mCropScale * initRect!!.top + offsetY,
                mCropScale * initRect!!.right + offsetX,
                mCropScale * initRect!!.bottom + offsetY
            )
        } else {
            mFrameRectF = calculateFrameRect(mImageRectF)
        }
        mIsInitialized = true
        invalidate()
    }

    /**
     * 重设矩阵，平移缩放旋转操作
     */
    private fun setMatrix() {
        mMatrix!!.reset()
        mMatrix!!.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f)
        mMatrix!!.postScale(mCropScale, mCropScale, mCenter.x, mCenter.y)
    }


    //计算裁剪框大小
    private fun calculateFrameRect(imageRect: RectF?): RectF {
        val frameW = getRatioX(imageRect!!.width())
        val frameH = getRatioY(imageRect.height())
        val frameRatio = frameW / frameH
        val imgRatio = imageRect.width() / imageRect.height()
        val l: Float
        val t: Float
        val r: Float
        val b: Float
        if (frameRatio >= imgRatio) {
            //宽比长比例大于img图宽高比的情况，宽撑满，缩高度
            l = imageRect.left
            r = imageRect.right
            //图的中点
            val hy = (imageRect.top + imageRect.bottom) * 0.5f
            //中点到上下顶点坐标的距离
            val hh = imageRect.height() / frameRatio * 0.5f
            t = hy - hh
            b = hy + hh
        } else {
            //宽比长比例大于img图宽高比的情况，高撑满，缩宽度
            t = imageRect.top
            b = imageRect.bottom
            val hx = (imageRect.left + imageRect.right) * 0.5f
            val hw = imageRect.width() * frameRatio * 0.5f
            l = hx - hw
            r = hx + hw
        }
        //裁剪框宽度
        val fW = r - l
        //高度
        val fH = b - t
        //中心点
        val cx = l + fW / 2
        val cy = t + fH / 2
        //放大后的裁剪框的宽高
        val sw = fW * mInitialFrameScale
        val sh = fH * mInitialFrameScale
        return RectF(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2)
    }


    //当前裁剪框高比
    private fun getRatioX(w: Float): Float {
        return when (mCropMode) {
            CropModeEnum.FIT_IMAGE -> mImageRectF!!.width()
            CropModeEnum.FREE -> w
            CropModeEnum.RATIO_2_3 -> 2f
            CropModeEnum.RATIO_3_2 -> 3f
            CropModeEnum.RATIO_4_3 -> 4f
            CropModeEnum.RATIO_3_4 -> 3f
            CropModeEnum.RATIO_16_9 -> 16f
            CropModeEnum.RATIO_9_16 -> 9f
            CropModeEnum.SQUARE -> 1f
            else -> w
        }
    }

    private fun getRatioY(h: Float): Float {
        return when (mCropMode) {
            CropModeEnum.FIT_IMAGE -> mImageRectF!!.height()
            CropModeEnum.FREE -> h
            CropModeEnum.RATIO_2_3 -> 3f
            CropModeEnum.RATIO_3_2 -> 2f
            CropModeEnum.RATIO_4_3 -> 3f
            CropModeEnum.RATIO_3_4 -> 4f
            CropModeEnum.RATIO_16_9 -> 9f
            CropModeEnum.RATIO_9_16 -> 16f
            CropModeEnum.SQUARE -> 1f
            else -> h
        }
    }

    private val ratioX: Float
        private get() = when (mCropMode) {
            CropModeEnum.FIT_IMAGE -> mImageRectF!!.width()
            CropModeEnum.RATIO_2_3 -> 2f
            CropModeEnum.RATIO_3_2 -> 3f
            CropModeEnum.RATIO_4_3 -> 4f
            CropModeEnum.RATIO_3_4 -> 3f
            CropModeEnum.RATIO_16_9 -> 16f
            CropModeEnum.RATIO_9_16 -> 9f
            CropModeEnum.SQUARE -> 1f
            else -> 1f
        }
    private val ratioY: Float
        private get() = when (mCropMode) {
            CropModeEnum.FIT_IMAGE -> mImageRectF!!.height()
            CropModeEnum.RATIO_2_3 -> 3f
            CropModeEnum.RATIO_3_2 -> 2f
            CropModeEnum.RATIO_4_3 -> 3f
            CropModeEnum.RATIO_3_4 -> 4f
            CropModeEnum.RATIO_16_9 -> 9f
            CropModeEnum.RATIO_9_16 -> 16f
            CropModeEnum.SQUARE -> 1f
            else -> 1f
        }

    /**
     * 将Matrix映射到rect
     *
     * @param rect
     * @param matrix
     * @return
     */
    private fun calcImageRect(rect: RectF, matrix: Matrix?): RectF {
        val applied = RectF()
        matrix!!.mapRect(applied, rect)
        return applied
    }

    private fun calcScale(viewWidth: Int, viewHeight: Int, angle: Float): Float {
        mImgWidth = drawable.intrinsicWidth.toFloat()
        mImgHeight = drawable.intrinsicHeight.toFloat()
        if (mImgWidth <= 0) mImgWidth = viewWidth.toFloat()
        if (mImgHeight <= 0) mImgHeight = viewHeight.toFloat()
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val imgRatio = getRotatedWidth(angle) / getRotatedHeight(angle)
        var scale = 1.0f
        if (imgRatio >= viewRatio) {
            scale = viewWidth / getRotatedWidth(angle)
        } else if (imgRatio < viewRatio) {
            scale = viewHeight / getRotatedHeight(angle)
        }
        return scale
    }

    private fun getRotatedWidth(angle: Float): Float {
        return getRotatedWidth(angle, mImgWidth, mImgHeight)
    }

    private fun getRotatedWidth(angle: Float, width: Float, height: Float): Float {
        return if (angle % 180 == 0f) width else height
    }

    private fun getRotatedHeight(angle: Float): Float {
        return getRotatedHeight(angle, mImgWidth, mImgHeight)
    }

    private fun getRotatedHeight(angle: Float, width: Float, height: Float): Float {
        return if (angle % 180 == 0f) height else width
    }

    /**
     * 保存中心矩阵
     *
     * @param center
     */
    private fun setCenter(center: PointF) {
        mCenter = center
    }

    /**
     * 保存比例
     *
     * @param scale
     */
    private fun setScale(scale: Float) {
        mCropScale = scale
    }


    /**
     * 裁剪图像返回裁剪后的bitmap
     *
     *
     * <Strong>NOTICE:工作在异步线程</Strong>
     *
     * @return
     */
    fun cropImage(onCropListener: OnCropListener) {
        addOnCropListener(onCropListener)
        ThreadPoolManagerUtils.instance?.execute {
            val cropped = croppedBitmap
            if (mOnCropListener != null) {
                mOnCropListener!!.onCropFinished(cropped)
            }
        }
    }

    /**
     * @return 裁剪后结果
     */
    val croppedBitmap: ImageCropData?
        get() {
            val source = bitmap ?: return null
            val rotated = getRotatedBitmap(source)
            val cropRect = calcCropRect(source.width, source.height)
            val cropped = Bitmap.createBitmap(
                rotated,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height(),
                null,
                false
            )
            if (rotated != cropped && rotated != source) {
                rotated.recycle()
            }
            return ImageCropData(cropped, cropRect)
        }


    /**
     * 将当前的原图bitmap返回出来
     */
    val currentBitmap: Bitmap?
        get() {
            return bitmap
        }


    /**
     * 计算裁剪区域
     * @param originalImageWidth 原图宽
     * @param originalImageHeight 原图高
     */
    private fun calcCropRect(originalImageWidth: Int, originalImageHeight: Int): Rect {
        //mImgAngle 固定 0，不考虑了
        val rotatedWidth =
            getRotatedWidth(mImgAngle, originalImageWidth.toFloat(), originalImageHeight.toFloat())
        val rotatedHeight =
            getRotatedHeight(mImgAngle, originalImageWidth.toFloat(), originalImageHeight.toFloat())
        //mImgAngle 固定 0，视为等于 mCropScale，原图缩放显示比例
        val scaleForOriginal = rotatedWidth / mImageRectF!!.width()
        //缩放至原图裁剪区域
        val offsetX = mImageRectF!!.left * scaleForOriginal
        val offsetY = mImageRectF!!.top * scaleForOriginal
        val left = (mFrameRectF!!.left * scaleForOriginal - offsetX).roundToInt()
        val top = (mFrameRectF!!.top * scaleForOriginal - offsetY).roundToInt()
        val right = (mFrameRectF!!.right * scaleForOriginal - offsetX).roundToInt()
        val bottom = (mFrameRectF!!.bottom * scaleForOriginal - offsetY).roundToInt()
        val imageW = rotatedWidth.roundToInt()
        val imageH = rotatedHeight.roundToInt()
        return Rect(
            left.coerceAtLeast(0),
            top.coerceAtLeast(0),
            right.coerceAtMost(imageW),
            bottom.coerceAtMost(imageH)
        )
    }

    /**
     * 得到旋转后的Bitmap
     *
     * @param bitmap
     * @return
     */
    private fun getRotatedBitmap(bitmap: Bitmap): Bitmap {
        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(
            mImgAngle,
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat()
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotateMatrix, true)
    }

    private fun addOnCropListener(onCropListener: OnCropListener) {
        mOnCropListener = onCropListener
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mValueAnimator != null) {
            mValueAnimator!!.cancel()
            mValueAnimator = null
        }
        if (mFrameValueAnimator != null) {
            mFrameValueAnimator!!.cancel()
            mFrameValueAnimator = null
        }
        if (mOnCropListener != null) {
            mOnCropListener = null
        }
    }


    /**
     * 手指点击的区域枚举类
     */
    enum class TouchAreaEnum {
        CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, OUT_OF_BOUNDS, CENTER_LEFT, CENTER_TOP, CENTER_RIGHT, CENTER_BOTTOM
    }

    /**
     * 裁剪框的比例模式
     */
    enum class CropModeEnum(val iD: Int) {
        FIT_IMAGE(0), RATIO_2_3(1), RATIO_3_2(2), RATIO_4_3(3), RATIO_3_4(4), SQUARE(5), RATIO_16_9(
            6
        ),
        RATIO_9_16(7), FREE(
            8
        );

    }

    /**
     * 展示模式，控制裁剪框显示的时机
     */
    enum class ShowModeEnum(val id: Int) {
        SHOW_ALWAYS(1), SHOW_ON_TOUCH(2), NOT_SHOW(3);

    }

    interface OnCropListener {
        fun onCropFinished(bitmap: ImageCropData?)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        val ss = state as SavedState
        mCropMode = ss.mCropMode
        mImgAngle = ss.mImgAngle
        mImgWidth = ss.mImgWidth
        mImgHeight = ss.mImgHeight
        mHandleSize = ss.mHandleSize
        mHandleWidth = ss.mHandleWidth
        mTouchPadding = ss.mTouchPadding
        mFrameMinSize = ss.mFrameMinSize
        mFrameStrokeWeight = ss.mFrameStrokeWeight
        mGuideStrokeWeight = ss.mGuideStrokeWeight
        mBackgroundColor = ss.mBackgroundColor
        mOverlayColor = ss.mOverlayColor
        mFrameColor = ss.mFrameColor
        mHandleColor = ss.mHandleColor
        mGuideColor = ss.mGuideColor
        mCropMode = ss.mCropMode
        mGuideShowMode = ss.mGuideShowMode
        mHandleShowMode = ss.mHandleShowMode
        mInitialFrameScale = ss.mInitialFrameScale
        mIsInitialized = ss.mIsInitialized
        mIsCropEnabled = ss.mIsCropEnabled
        mShowGuide = ss.mShowGuide
        mShowHandle = ss.mShowHandle
        mIsAnimating = ss.mIsAnimating
    }

    override fun onSaveInstanceState(): Parcelable {
        val superParcelable = super.onSaveInstanceState()
        val ss = SavedState(superParcelable)
        ss.mCropMode = mCropMode
        ss.mImgAngle = mImgAngle
        ss.mImgWidth = mImgWidth
        ss.mImgHeight = mImgHeight
        ss.mHandleSize = mHandleSize
        ss.mHandleWidth = mHandleWidth
        ss.mTouchPadding = mTouchPadding
        ss.mFrameMinSize = mFrameMinSize
        ss.mFrameStrokeWeight = mFrameStrokeWeight
        ss.mGuideStrokeWeight = mGuideStrokeWeight
        ss.mBackgroundColor = mBackgroundColor
        ss.mOverlayColor = mOverlayColor
        ss.mFrameColor = mFrameColor
        ss.mHandleColor = mHandleColor
        ss.mGuideColor = mGuideColor
        ss.mCropMode = mCropMode
        ss.mGuideShowMode = mGuideShowMode
        ss.mHandleShowMode = mHandleShowMode
        ss.mInitialFrameScale = mInitialFrameScale
        ss.mIsInitialized = mIsInitialized
        ss.mIsCropEnabled = mIsCropEnabled
        ss.mShowGuide = mShowGuide
        ss.mShowHandle = mShowHandle
        ss.mIsAnimating = mIsAnimating
        return ss
    }

    class SavedState : BaseSavedState {
        // 放大比例
        private var mCropScale = 1.0f

        // 旋转角度
        var mImgAngle = 0.0f

        // 图片宽度
        var mImgWidth = 0.0f

        // 图片高度
        var mImgHeight = 0.0f

        // 四角线的长度值
        var mHandleSize = 0

        // 四角的线宽度
        var mHandleWidth = 0

        // 触摸可处理的宽度值
        var mTouchPadding = 0

        // 裁剪框最小值
        var mFrameMinSize = 0

        // 裁剪外框线框宽度
        var mFrameStrokeWeight = 2f

        // 裁剪指导线宽度
        var mGuideStrokeWeight = 2f

        // 背景的颜色
        var mBackgroundColor = 0

        // 半透明框的颜色
        var mOverlayColor = 0

        // 外框的颜色
        var mFrameColor = 0

        // 四边角点的颜色
        var mHandleColor = 0

        // 指导线颜色
        var mGuideColor = 0

        // 裁剪模式
        var mCropMode: CropModeEnum? = null

        // 指导线的显示模式
        var mGuideShowMode: ShowModeEnum? = null

        // 处理四个角的显示模式
        var mHandleShowMode: ShowModeEnum? = null

        // 初始化的比例大小0.01到1.0，默认情况为0.75
        var mInitialFrameScale = 0f

        // 是否初始化
        var mIsInitialized = false

        // 是否开始裁剪
        var mIsCropEnabled = true

        // 是否显示指导线
        var mShowGuide = true

        // 是否显示处理线
        var mShowHandle = true
        var mIsRotating = false
        var mIsAnimating = false

        // 是否已经翻转了
        var mIsReverseY = false

        // 是否有旋转操作
        var mIsRotated = false

        // 旋转操作的开关
        var mRotateSwitch = true

        internal constructor(superState: Parcelable?) : super(superState) {}
        private constructor(source: Parcel) : super(source) {
            mGuideColor = source.readInt()
            mHandleColor = source.readInt()
            mFrameColor = source.readInt()
            mOverlayColor = source.readInt()
            mBackgroundColor = source.readInt()
            mFrameMinSize = source.readInt()
            mTouchPadding = source.readInt()
            mHandleWidth = source.readInt()
            mHandleSize = source.readInt()
            mImgHeight = source.readFloat()
            mImgWidth = source.readFloat()
            mImgAngle = source.readFloat()
            mCropScale = source.readFloat()
            mFrameStrokeWeight = source.readFloat()
            mGuideStrokeWeight = source.readFloat()
            mHandleShowMode = source.readSerializable() as ShowModeEnum?
            mGuideShowMode = source.readSerializable() as ShowModeEnum?
            mCropMode = source.readSerializable() as CropModeEnum?
            mInitialFrameScale = source.readFloat()
            mRotateSwitch = source.readInt() != 0
            mIsRotated = source.readInt() != 0
            mIsReverseY = source.readInt() != 0
            mIsAnimating = source.readInt() != 0
            mIsRotating = source.readInt() != 0
            mShowHandle = source.readInt() != 0
            mShowGuide = source.readInt() != 0
            mIsCropEnabled = source.readInt() != 0
            mIsInitialized = source.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(mGuideColor)
            out.writeInt(mHandleColor)
            out.writeInt(mFrameColor)
            out.writeInt(mOverlayColor)
            out.writeInt(mBackgroundColor)
            out.writeInt(mFrameMinSize)
            out.writeInt(mTouchPadding)
            out.writeInt(mHandleWidth)
            out.writeInt(mHandleSize)
            out.writeFloat(mImgHeight)
            out.writeFloat(mImgWidth)
            out.writeFloat(mImgAngle)
            out.writeFloat(mCropScale)
            out.writeFloat(mFrameStrokeWeight)
            out.writeFloat(mGuideStrokeWeight)
            out.writeSerializable(mHandleShowMode)
            out.writeSerializable(mGuideShowMode)
            out.writeSerializable(mCropMode)
            out.writeFloat(mInitialFrameScale)
            out.writeInt(if (mRotateSwitch) 1 else 0)
            out.writeInt(if (mIsRotated) 1 else 0)
            out.writeInt(if (mIsReverseY) 1 else 0)
            out.writeInt(if (mIsAnimating) 1 else 0)
            out.writeInt(if (mIsRotating) 1 else 0)
            out.writeInt(if (mShowHandle) 1 else 0)
            out.writeInt(if (mShowGuide) 1 else 0)
            out.writeInt(if (mIsCropEnabled) 1 else 0)
            out.writeInt(if (mIsInitialized) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<Any?> {
                override fun createFromParcel(parcel: Parcel): SavedState? {
                    return SavedState(parcel)
                }

                override fun newArray(i: Int): Array<SavedState?> {
                    return arrayOfNulls(i)
                }
            }
        }
    }

    companion object {
        const val TAG = "ImageCropView"

        private const val HANDLE_SIZE = 24
        private const val HANDLE_WIDTH = 2
        private const val FRAME_MIN_SIZE = 50
        private const val FRAME_STROKE_WEIGHT = 1
        private const val GUIDE_STROKE_WEIGHT = 1
        private const val DEFAULT_INITIAL_SCALE = 1f

        // 颜色值，包含透明度
        private const val TRANSPARENT = 0x00000000
        private const val TRANSLUCENT_WHITE = -0x44000001
        private const val WHITE = -0x1
        private const val TRANSLUCENT_BLACK = -0x45000000

        // 默认的动画时长
        private const val DEFAULT_ANIMATION_DURATION_MILLIS = 100
    }

    init {
        mHandleSize = SizeUtils.dp2px(HANDLE_SIZE.toFloat())
        mFrameMinSize = SizeUtils.dp2px(FRAME_MIN_SIZE.toFloat())
        mFrameStrokeWeight = SizeUtils.dp2px(FRAME_STROKE_WEIGHT.toFloat()).toFloat()
        mGuideStrokeWeight = SizeUtils.dp2px(GUIDE_STROKE_WEIGHT.toFloat()).toFloat()
        mFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTranslucentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 抗锯齿处理
        mBitmapPaint.isFilterBitmap = true
        mMatrix = Matrix()
        mCropScale = 1.0f
        mBackgroundColor = TRANSPARENT
        mFrameColor = WHITE
        mOverlayColor = TRANSLUCENT_BLACK
        mHandleColor = WHITE
        mGuideColor = TRANSLUCENT_WHITE
        loadStyleable(context, attrs, defStyleAttr)
        mValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        mValueAnimator?.interpolator = DecelerateInterpolator()
        mValueAnimator?.duration = mAnimationDurationMillis.toLong()
        mFrameValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        mFrameValueAnimator?.interpolator = DecelerateInterpolator()
        mFrameValueAnimator?.duration = mAnimationDurationMillis.toLong()
    }
}