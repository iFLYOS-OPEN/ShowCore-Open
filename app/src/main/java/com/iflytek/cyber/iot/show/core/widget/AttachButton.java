package com.iflytek.cyber.iot.show.core.widget;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;

import androidx.appcompat.widget.AppCompatImageView;

import com.iflytek.cyber.iot.show.core.utils.ScreenUtils;

public class AttachButton extends AppCompatImageView {
    private final String TAG = "AttachButton";

    public interface AttachListener {
        void onAttachToLeft();
        void onAttachToRight();
    }

    private AttachListener mAttachListener;

    private float mLastRawX;
    private float mLastRawY;
    private boolean isDrug = false;

    private int mRootMeasuredWidth;
    private int mRootMeasuredHeight;

    private static final int STRIP_COUNT = 3;
    private static final int LOW_STRIP = 0;
    private static final int MID_STRIP = 1;
    private static final int HIGH_STRIP = 2;

    private static final int FRAME_COUNT = 4;
    private static final int FRAME_DURATION_MILLIS = 250;

    private int mCenterX = -1;
    private int mCenterY = -1;

    private int STRIP_WIDTH = 5;
    private int STRIP_SPACE = 7;
    private int STRIP_RADIUS = 4;

    private int[] STRIP_XS = new int[] {0, 0, 0};
    private int[] STRIP_HEIGHTS = new int[] {12, 16, 24};

    private int[][] FRAMES = new int[][] {
            {LOW_STRIP, MID_STRIP, HIGH_STRIP},
            {MID_STRIP, HIGH_STRIP, LOW_STRIP},
            {HIGH_STRIP, MID_STRIP, LOW_STRIP},
            {HIGH_STRIP, LOW_STRIP, HIGH_STRIP}
    };

    private int mCurrentFrameIndex = 0;

    private boolean mNeedDrawFrame = false;

    private boolean mIsAttachToLeft = true;

    public AttachButton(Context context) {
        this(context, null);
    }

    public AttachButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttachButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initMetrics();
    }

    public boolean isAttachToLeft() {
        return mIsAttachToLeft;
    }

    public void setAttachListener(AttachListener listener) {
        mAttachListener = listener;
    }

    private void getRootSize() {
        ViewGroup group = (ViewGroup) getParent();
        if (group != null) {
            int[] location = new int[2];
            group.getLocationInWindow(location);
            //获取父布局的高度
            mRootMeasuredHeight = group.getMeasuredHeight();
            mRootMeasuredWidth = group.getMeasuredWidth();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //当前手指的坐标
        float rawX = ev.getRawX();
        float rawY = ev.getRawY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN://手指按下
                getRootSize();

                isDrug = false;
                //记录按下的位置
                mLastRawX = rawX;
                mLastRawY = rawY;
                break;
            case MotionEvent.ACTION_MOVE://手指滑动
                ViewGroup group = (ViewGroup) getParent();
                if (group != null) {
                    int[] location = new int[2];
                    group.getLocationInWindow(location);

                    //获取父布局顶点的坐标
                    int rootTopY = location[1];
                    if (rawX >= 0 && rawX <= mRootMeasuredWidth && rawY >= rootTopY && rawY <= (mRootMeasuredHeight + rootTopY)) {
                        //手指X轴滑动距离
                        float differenceValueX = rawX - mLastRawX;
                        //手指Y轴滑动距离
                        float differenceValueY = rawY - mLastRawY;
                        //判断是否为拖动操作
                        if (!isDrug) {
                            if (Math.sqrt(differenceValueX * differenceValueX + differenceValueY * differenceValueY) < 2) {
                                isDrug = false;
                            } else {
                                isDrug = true;
                            }
                        }
                        //获取手指按下的距离与控件本身X轴的距离
                        float ownX = getX();
                        //获取手指按下的距离与控件本身Y轴的距离
                        float ownY = getY();
                        //理论中X轴拖动的距离
                        float endX = ownX + differenceValueX;
                        //理论中Y轴拖动的距离
                        float endY = ownY + differenceValueY;
                        //X轴可以拖动的最大距离
                        float maxX = mRootMeasuredWidth - getWidth();
                        //Y轴可以拖动的最大距离
                        float maxY = mRootMeasuredHeight - getHeight();
                        //X轴边界限制
                        endX = endX < 0 ? 0 : endX > maxX ? maxX : endX;
                        //Y轴边界限制
                        endY = endY < 0 ? 0 : endY > maxY ? maxY : endY;
                        //开始移动
                        setX(endX);
                        setY(endY);
                        //记录位置
                        mLastRawX = rawX;
                        mLastRawY = rawY;
                    }
                }
                break;
            case MotionEvent.ACTION_UP://手指离开
                float center = mRootMeasuredWidth / 2;
                //自动贴边
                if (mLastRawX <= center) {
                    //向左贴边
                    AttachButton.this.animate()
                            .setInterpolator(new BounceInterpolator())
                            .setDuration(500)
                            .x(ScreenUtils.dpToPx(20))
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mIsAttachToLeft = true;

                                    if (mAttachListener != null) {
                                        mAttachListener.onAttachToLeft();
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            })
                            .start();
                } else {
                    //向右贴边
                    AttachButton.this.animate()
                            .setInterpolator(new BounceInterpolator())
                            .setDuration(500)
                            .x(mRootMeasuredWidth - getWidth() - ScreenUtils.dpToPx(20))
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mIsAttachToLeft = false;

                                    if (mAttachListener != null) {
                                        mAttachListener.onAttachToRight();
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            })
                            .start();
                }
                break;
        }

        if (isDrug) {
            setPressed(false);
        }

        //是否拦截事件
        return isDrug ? isDrug : super.onTouchEvent(ev);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mNeedDrawFrame) {
            drawFrame(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mCenterX == -1) {
            mCenterX = (right - left) / 2;
            mCenterY = (bottom - top) / 2;

            int strip0_x = mCenterX - STRIP_WIDTH / 2 - STRIP_SPACE - STRIP_WIDTH;
            for (int i = 0; i < STRIP_COUNT; i++) {
                STRIP_XS[i] = strip0_x + (STRIP_WIDTH + STRIP_SPACE) * i;
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(Color.BLACK);

        int[] curFrame = FRAMES[mCurrentFrameIndex];
        for (int i = 0; i < STRIP_COUNT; i++) {
            drawStrip(canvas, p, STRIP_XS[i], STRIP_HEIGHTS[curFrame[i]]);
        }
    }

    private void drawStrip(Canvas canvas, Paint p, int x, int height) {
        canvas.drawRoundRect(new RectF(x, mCenterY - height /2,
                x + STRIP_WIDTH, mCenterY + height / 2), STRIP_RADIUS, STRIP_RADIUS, p);
    }

    private void initMetrics() {
        STRIP_WIDTH = ScreenUtils.dpToPx(STRIP_WIDTH);
        STRIP_SPACE = ScreenUtils.dpToPx(STRIP_SPACE);
        STRIP_RADIUS = ScreenUtils.dpToPx(STRIP_RADIUS);

        for (int i = 0; i < STRIP_COUNT; i++) {
            STRIP_HEIGHTS[i] = ScreenUtils.dpToPx(STRIP_HEIGHTS[i]);
        }
    }

    public void startAnim() {
        mNeedDrawFrame = true;
        mCurrentFrameIndex = 0;

        if (mPulseThread == null) {
            mPulseThread = new PulseThread();
            mPulseThread.start();
        }
    }

    public void stopAnim() {
        mNeedDrawFrame = false;

        if (mPulseThread != null) {
            mPulseThread.stopRun();
            mPulseThread = null;
        }
    }

    private PulseThread mPulseThread;

    private class PulseThread extends Thread {
        private boolean mStopRun = false;

        void stopRun() {
            mStopRun = true;
            interrupt();
        }

        @Override
        public void run() {
            super.run();

            while (!mStopRun) {
                try {
                    postInvalidate();

                    sleep(FRAME_DURATION_MILLIS);

                    mCurrentFrameIndex = (mCurrentFrameIndex + 1) % FRAME_COUNT;
                } catch (InterruptedException e) {

                }
            }
        }
    }
}