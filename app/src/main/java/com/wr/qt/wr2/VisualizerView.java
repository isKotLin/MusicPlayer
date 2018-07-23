package com.wr.qt.wr2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wr-app1 on 2017/8/5.
 */

public class VisualizerView extends View {
    private List<Integer> mFFT;
    private Rect mRect;

    private float mPoints[] = new float[4];
    private float upsetPoints[] = new float[4];// 倒影
    private Paint mMainPaint = new Paint();
    private Paint upsetPaint = new Paint();// 倒影
    private int mBlockWidth = 4;

    LinearGradient mLinearGradient;

    private static final int[] SAMPLE_HZ = new int[]{
            0, 0, 50, 80, 120, 176, 190, 200, 206, 241, 287, 331, 353, 394, 453, 353, 394, 353, 284, 176, 206, 241, 353, 480, 500, 570,
            680, 800, 600, 540, 600, 700, 800, 990, 900, 600, 512, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900
    };
    private float[] mLastHeight = new float[SAMPLE_HZ.length];
    private int[] mLastCnt = new int[SAMPLE_HZ.length];

    private int[] mColorGradient = new int[]{
            Color.rgb(177, 10, 31), Color.rgb(177, 10, 31), Color.rgb(177, 10, 31),
            Color.rgb(177, 10, 31), Color.rgb(177, 10, 31), Color.rgb(177, 10, 31)
    };

    private int mSampleRate;

    public VisualizerView(Context context) {
        super(context);
        mRect = new Rect();
        mMainPaint.setStyle(Paint.Style.STROKE);
        mMainPaint.setAntiAlias(true);
        mMainPaint.setDither(true);

        upsetPaint.setStrokeWidth(mBlockWidth);
        upsetPaint.setAntiAlias(true);
        upsetPaint.setShader(mLinearGradient);
        upsetPaint.setAlpha(60);
        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);//关闭硬件加速，启用软件加速

        Arrays.fill(mLastHeight, -1);

        mLinearGradient = new LinearGradient(0, 0, 0, mRect.bottom, mColorGradient, null, LinearGradient.TileMode.CLAMP);
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRect = new Rect();
    }

    public void updateData(List<Integer> fft, int sampleRate) {
        mFFT = fft;
        mSampleRate = sampleRate / 1000;
        invalidate();
    }

    private static boolean indexRangeInFFT(List<Integer> fft, int pos) {
        return (pos + 1) << 1 > 0 && ((pos + 1) << 1 | 1) < fft.size();
    }

    private static double getSwingFromFFT(List<Integer> fft, int pos) {
        return Math.hypot(fft.get((pos + 1) << 1), fft.get((pos + 1) << 1 | 1));
    }

    private static final double[] AVG_WEIGHT = new double[]{0.5, 0.2, 0.1};

    private static double getAverageFromFFT(List<Integer> fft, int pos) {
        double sum = 0;
        double sumWeight = 0;

        for (int i = 0; i < AVG_WEIGHT.length && indexRangeInFFT(fft, pos + i); ++i) {
            sum += getSwingFromFFT(fft, pos + i);
            sumWeight += AVG_WEIGHT[i];
        }

        for (int i = 1; i < AVG_WEIGHT.length && indexRangeInFFT(fft, pos - i); ++i) {
            sum += getSwingFromFFT(fft, pos - i);
            sumWeight += AVG_WEIGHT[i];
        }

        return sum / sumWeight;
    }
/*
    protected void drawCylinder(Canvas canvas){
        if (mFFT == null) {
            return;
        }
        if (mRect.width() != getWidth() || mRect.height() != getHeight()) {
            mRect.set(0, 0, getWidth(), getHeight());
            mLinearGradient = new LinearGradient(0, 0, 0, mRect.bottom,
                    mColorGradient,
                    null, LinearGradient.TileMode.CLAMP);
        }

        float width = mRect.width() * 1.0f / SAMPLE_HZ.length;

        mMainPaint.setStrokeWidth(width / 2);
        mMainPaint.setShader(mLinearGradient);
        mMainPaint.setAntiAlias(true);
//频块圆角
        mMainPaint.setStrokeJoin(Paint.Join.ROUND);
        mMainPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < SAMPLE_HZ.length; ++i) {
            double val = getAverageFromFFT(mFFT, (int) Math.round(SAMPLE_HZ[i] * 1.0 / mSampleRate * mFFT.size()));
            //32
            val = 24 * Math.log10(val / 8);
            float left = i * width;
            float right = i * width + width;
            float bottom = mRect.bottom;
            float top = mRect.height() - (float) (val * mRect.height() / 64.0);

            mPoints[0] = (right - left) / 2 + left;
            mPoints[1] = bottom;
            mPoints[2] = mPoints[0];
            mPoints[3] = top;

            canvas.drawLines(mPoints, mMainPaint);

        }
    }*/


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFFT == null) {
            return;
        }
        if (mRect.width() != getWidth() || mRect.height() != getHeight()) {
            mRect.set(0, 0, getWidth(), getHeight() / 2);
            mLinearGradient = new LinearGradient(0, 0, 0, mRect.bottom,
                    mColorGradient,
                    null, LinearGradient.TileMode.CLAMP);
        }
        float width = mRect.width() * 1.0f / SAMPLE_HZ.length;

        mMainPaint.setStrokeWidth(width / 2);//每条能量柱的间隔
        mMainPaint.setAntiAlias(true);
        mMainPaint.setStrokeJoin(Paint.Join.ROUND);//频块圆角
        mMainPaint.setStrokeCap(Paint.Cap.ROUND);//频块圆角

        upsetPaint.setStrokeWidth(mBlockWidth);
        upsetPaint.setAntiAlias(true);
        upsetPaint.setAlpha(60);
        upsetPaint.setStrokeJoin(Paint.Join.ROUND);
        upsetPaint.setStrokeCap(Paint.Cap.ROUND);
        upsetPaint.setStrokeWidth(width / 2);
        upsetPaint.setAntiAlias(true);
        for (int i = 0; i < SAMPLE_HZ.length; ++i) {
            double val = getAverageFromFFT(mFFT, (int) Math.round(SAMPLE_HZ[i] * 1.0 / mSampleRate * mFFT.size()));
            //32
            val = 28 * Math.log10(val / 16);
            float left = i * width;
            float right = i * width + width;
            float bottom = mRect.bottom;//跳动
            float top = mRect.height() - (float) (val * mRect.height() / 58.0);//能量柱高度


            if (val<0){
                 top = mRect.height() - (float) (val * mRect.height() / 58.0)/12;
            }
            else {
                top = mRect.height() - (float) (val * mRect.height() / 48.0);
            }
            //bottom * 2 可以去掉圆头
            mPoints[0] = (right - left) / 2 + left;
            mPoints[1] = bottom;
            //mPoints[1] = bottom /2;// 倒立
            mPoints[2] = mPoints[0];//能量柱网上跳动
            mPoints[3] = top;
            //mPoints[3] = top /2 -top; //往上

            upsetPoints[0] = (right - left) / 2 + left;
            upsetPoints[1] = bottom;
            //upsetPoints[1] = bottom / 2;
            upsetPoints[2] = upsetPoints[0];//能量柱往上跳动,去掉则往左跳动
            upsetPoints[3] = getHeight() - top;


                if (i > SAMPLE_HZ.length / 2.6) {
                    mMainPaint.setColor(Color.rgb(128, 128, 128));
                    if (mPoints[3] > getHeight() / 2) {
                        mMainPaint.setAlpha(60);
                    }
                } else {
                    mMainPaint.setColor(Color.rgb(177, 10, 31));
                    if (mPoints[3] > getHeight() / 2) {
                        mMainPaint.setAlpha(60);
                    }
                }

                if (i > SAMPLE_HZ.length / 2.6) {
                    upsetPaint.setColor(Color.rgb(128, 128, 128));
                    if (upsetPoints[3] < getHeight() / 2) {
                        upsetPaint.setAlpha(255);
                    } else {
                        upsetPaint.setAlpha(60);
                    }
                } else {
                    upsetPaint.setColor(Color.rgb(177, 10, 31));
                    upsetPaint.setAlpha(60);

                    if (upsetPoints[3] < getHeight() / 2) {
                        upsetPaint.setAlpha(255);
                    }
                }

                if (top < mLastHeight[i]) {
                    mMainPaint.setAlpha(0);
                }
            /*if (top < mLastHeight[i] || mLastHeight[i] < 0) {
                mLastHeight[i] = top;
                mLastCnt[i] = 2;
                canvas.drawLine(left + (right - left) / 4, top - mBlockWidth / 2, right - (right - left) / 4,
                        top - mBlockWidth / 2, mBlockPaint);
            } else {
                if (mLastHeight[i] + (mLastCnt[i] + 1) * 0.5f <= mRect.bottom - mBlockWidth) {
                    ++mLastCnt[i];
                    mLastHeight[i] = mLastHeight[i] + mLastCnt[i] * 0.5f;
                } else {
                    mLastHeight[i] = mRect.bottom - mBlockWidth;
                }
            }*/
            canvas.drawLines(mPoints, mMainPaint);
            canvas.drawLines(upsetPoints, upsetPaint);
        }

//        for (int i = 0; i < SAMPLE_HZ.length; ++i) {
//            double val = getAverageFromFFT(mFFT, (int) Math.round(SAMPLE_HZ[i] * 1.0 / mSampleRate * mFFT.size()));
//            //32
//            val = 24 * Math.log10(val / 8);
//            float left = i * width;
//            float right = i * width + width;
//            float bottom = mRect.bottom;
//            float top = mRect.height() - (float) (val * mRect.height() / 64.0);//能量柱高度
//
//            upsetPoints[0] = (right - left) / 2 + left;
//            upsetPoints[1] = bottom;
//            //upsetPoints[1] = bottom / 2;
//            upsetPoints[2] = upsetPoints[0];//能量柱往上跳动,去掉则往左跳动
//            upsetPoints[3] = top;
//
//            if (i>SAMPLE_HZ.length/2.6){
//                upsetPaint.setColor(Color.rgb(128,128,128));
//                upsetPaint.setAlpha(80);
//            }else
//                upsetPaint.setColor(Color.rgb(177,10,31));
//                upsetPaint.setAlpha(80);
//            canvas.drawLines(upsetPoints, upsetPaint);
//        }

            /*
            if (mPoints2[3] > 0 && mPoints[3] > mPoints2[3]) {
                mPoints[3] = mPoints2[3];
                upsetPoints[3] = (getHeight() * 2 - mPoints[3]) / 2;
            }else {
                mPoints2[3] = mPoints[3];}
*/

            /* if (mPoints2[i * 4 + 3] > 0 && mPoints[i * 4 + 3] > mPoints2[i * 4 + 3] + downspeed) {
            mPoints[i * 4 + 3] = mPoints2[i * 4 + 3] + downspeed;
            upsetPoints[i * 4 + 3] = (getHeight() * 2 - mPoints[i * 4 + 3]) / 2 - downspeed / 2;
        }
        mPoints2[i * 4 + 3] = mPoints[i * 4 + 3];
    }*/
    }
}
