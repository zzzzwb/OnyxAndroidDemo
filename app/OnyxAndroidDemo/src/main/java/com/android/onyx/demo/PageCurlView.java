package com.android.onyx.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 自定义垂直边缘翻页视图
 * 
 * 实现了电子书的垂直边缘翻页效果，包括垂直边缘和阴影效果
 * 支持自动翻页动画和手势拖动翻页
 */

public class PageCurlView extends View {

    // 页面状态
    private static final int STATE_IDLE = 0;
    private static final int STATE_DRAGGING = 1;
    private static final int STATE_ANIMATING = 2;
    
    // 当前状态
    private int mState = STATE_IDLE;
    
    // 页面内容
    private Bitmap mCurrentPageBitmap;
    private Bitmap mNextPageBitmap;
    private Bitmap mTempBitmap; // 用于绘制翻页效果
    
    // 绘制工具
    private Paint mPaint;
    private Path mPath;
    private Canvas mTempCanvas;
    
    // 触摸点和边缘点
    private PointF mTouchPoint;
    private PointF mCurlPoint; // 现在用作边缘阴影的参考点
    
    // 阴影绘制
    private GradientDrawable mShadowDrawable;
    private Paint mShadowPaint;
    
    // 页面尺寸
    private int mViewWidth;
    private int mViewHeight;
    
    // 动画相关
    private boolean mIsAnimating = false;
    private float mAnimationProgress = 0f;
    private long mAnimationStartTime;
    private long mAnimationDuration = 500; // 毫秒，恢复为正常速度
    
    // 页面翻转方向（从右向左）
    private boolean mIsRightToLeft = true;
    
    // 页面翻转监听器
    private OnPageTurnListener mPageTurnListener;
    
    public interface OnPageTurnListener {
        void onPageTurned();
    }
    
    public PageCurlView(Context context) {
        this(context, null);
    }
    
    public PageCurlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public PageCurlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化绘制工具
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        
        mPath = new Path();
        
        // 初始化触摸点和卷曲点
        mTouchPoint = new PointF();
        mCurlPoint = new PointF();
        
        // 初始化阴影
        mShadowPaint = new Paint();
        mShadowPaint.setAntiAlias(true);
        mShadowPaint.setAlpha(180);
        
        // 创建阴影渐变
        mShadowDrawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.TRANSPARENT});
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        
        // 如果临时位图不存在或尺寸不匹配，则创建新的
        if (mTempBitmap == null || mTempBitmap.getWidth() != w || mTempBitmap.getHeight() != h) {
            if (mTempBitmap != null) {
                mTempBitmap.recycle();
            }
            mTempBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mTempCanvas = new Canvas(mTempBitmap);
        }
    }
    
    public void setPageBitmaps(Bitmap currentPage, Bitmap nextPage) {
        this.mCurrentPageBitmap = currentPage;
        this.mNextPageBitmap = nextPage;
        invalidate();
    }
    
    public void setOnPageTurnListener(OnPageTurnListener listener) {
        this.mPageTurnListener = listener;
    }
    
    public void startPageTurnAnimation() {
        if (mState != STATE_IDLE) return;
        
        mState = STATE_ANIMATING;
        mIsAnimating = true;
        mAnimationProgress = 0f;
        mAnimationStartTime = System.currentTimeMillis();
        
        // 设置初始卷曲点在右侧中间
        if (mIsRightToLeft) {
            mTouchPoint.x = mViewWidth;
            mTouchPoint.y = mViewHeight / 2;
        } else {
            mTouchPoint.x = 0;
            mTouchPoint.y = mViewHeight / 2;
        }
        
        updateCurlPoint();
        invalidate();
    }
    
    private void updateCurlPoint() {
        // 由于我们现在使用垂直边缘，卷曲点的X坐标与触摸点相同
        // 这样阴影会沿着垂直边缘绘制
        mCurlPoint.x = mTouchPoint.x;
        mCurlPoint.y = mViewHeight / 2; // 固定在页面中间高度
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentPageBitmap == null || mNextPageBitmap == null) {
            return;
        }
        
        // 清除临时画布
        mTempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        
        // 根据状态绘制页面
        switch (mState) {
            case STATE_IDLE:
                // 静止状态，只绘制当前页
                canvas.drawBitmap(mCurrentPageBitmap, 0, 0, null);
                break;
                
            case STATE_DRAGGING:
            case STATE_ANIMATING:
                // 绘制下一页作为背景
                canvas.drawBitmap(mNextPageBitmap, 0, 0, null);
                
                // 绘制垂直边缘翻页效果
                drawPageEffect(mTempCanvas);
                canvas.drawBitmap(mTempBitmap, 0, 0, null);
                break;
        }
        
        // 如果正在动画中，继续更新
        if (mIsAnimating) {
            updateAnimation();
            invalidate();
        }
    }
    
    private void drawPageEffect(Canvas canvas) {
        // 重置路径
        mPath.reset();
        
        // 创建垂直边缘的路径（不再使用弯曲效果）
        if (mIsRightToLeft) {
            // 从右向左翻页，保持垂直边缘
            mPath.moveTo(mTouchPoint.x, 0);
            mPath.lineTo(mViewWidth, 0);
            mPath.lineTo(mViewWidth, mViewHeight);
            mPath.lineTo(mTouchPoint.x, mViewHeight);
            
            // 直接连接成垂直线，不使用曲线
            mPath.lineTo(mTouchPoint.x, 0);
        } else {
            // 从左向右翻页，保持垂直边缘
            mPath.moveTo(mTouchPoint.x, 0);
            mPath.lineTo(0, 0);
            mPath.lineTo(0, mViewHeight);
            mPath.lineTo(mTouchPoint.x, mViewHeight);
            
            // 直接连接成垂直线，不使用曲线
            mPath.lineTo(mTouchPoint.x, 0);
        }
        
        // 保存画布状态
        canvas.save();
        
        // 裁剪路径区域
        canvas.clipPath(mPath, Region.Op.INTERSECT);
        
        // 在裁剪区域内绘制当前页
        canvas.drawBitmap(mCurrentPageBitmap, 0, 0, null);
        
        // 绘制阴影
        drawShadow(canvas);
        
        // 恢复画布状态
        canvas.restore();
    }
    
    private void drawShadow(Canvas canvas) {
        // 去掉黑色边缘阴影，保持页面纯净
        // 不绘制阴影内容，直接返回
        return;
    }
    
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mAnimationStartTime;
        
        if (elapsedTime >= mAnimationDuration) {
            // 动画结束
            mIsAnimating = false;
            mState = STATE_IDLE;
            
            // 交换页面
            Bitmap temp = mCurrentPageBitmap;
            mCurrentPageBitmap = mNextPageBitmap;
            mNextPageBitmap = temp;
            
            // 通知页面已翻转
            if (mPageTurnListener != null) {
                mPageTurnListener.onPageTurned();
            }
            
            return;
        }
        
        // 计算动画进度 (0.0 - 1.0)
        float linearProgress = (float) elapsedTime / mAnimationDuration;
        
        // 应用缓动函数，使动画更加自然
        // 使用缓入缓出的插值器：先慢后快再慢
        mAnimationProgress = applyEasing(linearProgress);
        
        // 更新触摸点位置
        if (mIsRightToLeft) {
            // 从右向左翻页，触摸点从右侧移动到左侧
            mTouchPoint.x = mViewWidth * (1 - mAnimationProgress);
        } else {
            // 从左向右翻页，触摸点从左侧移动到右侧
            mTouchPoint.x = mViewWidth * mAnimationProgress;
        }
        
        // 更新卷曲点
        updateCurlPoint();
    }
    
    /**
     * 应用缓动函数，使动画更加自然
     * @param progress 线性进度 (0.0 - 1.0)
     * @return 缓动后的进度值 (0.0 - 1.0)
     */
    private float applyEasing(float progress) {
        // 使用缓入缓出的三次方缓动函数
        // 公式: t^2 * (3 - 2t) 其中t是线性进度
        // 这个函数在开始和结束时速度较慢，中间速度较快
        return progress * progress * (3 - 2 * progress);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 如果正在动画中，不处理触摸事件
        if (mIsAnimating) {
            return true;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchPoint.x = event.getX();
                mTouchPoint.y = event.getY();
                
                // 只有从边缘开始拖动才触发翻页
                if ((mIsRightToLeft && mTouchPoint.x > mViewWidth - 100) ||
                    (!mIsRightToLeft && mTouchPoint.x < 100)) {
                    mState = STATE_DRAGGING;
                    updateCurlPoint();
                    invalidate();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mState == STATE_DRAGGING) {
                    mTouchPoint.x = event.getX();
                    mTouchPoint.y = event.getY();
                    updateCurlPoint();
                    invalidate();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mState == STATE_DRAGGING) {
                    // 判断是否翻页或回弹
                    if ((mIsRightToLeft && mTouchPoint.x < mViewWidth / 2) ||
                        (!mIsRightToLeft && mTouchPoint.x > mViewWidth / 2)) {
                        // 翻到下一页
                        startPageTurnAnimation();
                    } else {
                        // 回弹到当前页
                        mState = STATE_IDLE;
                        invalidate();
                    }
                    return true;
                }
                break;
        }
        
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 释放资源
        if (mTempBitmap != null) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
    }
}