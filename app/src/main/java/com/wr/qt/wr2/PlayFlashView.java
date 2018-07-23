package com.wr.qt.wr2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.Random;

/**
 * Created by wr-app1 on 2017/8/4.
 */

public class PlayFlashView extends View{
    private int picthNum = 0;
    private int index=0;
    int[] nowIndex= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    int[] lastIndex= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    final int COUNT=30;
    int columnWidth=0;

    Paint p = new Paint();
    Paint p2 = new Paint();
    Bitmap mSCBitmap = null;
    DisplayMetrics dm;
    int width=0;
    int height=0;

    public PlayFlashView(Context context) {
        super(context);
        init();

    }

    public PlayFlashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        p.setColor(Color.WHITE);
        p2.setColor(Color.GRAY);
        p.setStyle(Paint.Style.FILL);
        p2.setStyle(Paint.Style.FILL);
        p.setAntiAlias(true);
        p.setDither(true);
        p2.setAntiAlias(true);
        p2.setDither(true);
        p.setAlpha(255);
        p2.setAlpha(80);
        picthNum=0;
        dm=new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        width=dm.widthPixels;
        height=dm.heightPixels;
        columnWidth=(width-30)/COUNT-5;

        initScreen();
    }

    private void initScreen(){
        Canvas canvas=new Canvas();
        mSCBitmap=Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(mSCBitmap);
        setIndexList();
        int everyWidth=columnWidth+5;
        int Yheight=height/2-height/12;
        if(picthNum>0){
            for(int i=0;i<COUNT;i++){
                if(nowIndex[i]>0){
                    canvas.drawRect(everyWidth*i+15, Yheight-nowIndex[i], 15+everyWidth*i+columnWidth, Yheight, p);
                    canvas.drawRect(everyWidth*i+15, Yheight, everyWidth*i+columnWidth+15, Yheight+(nowIndex[i]*60)/100, p2);
                    if(lastIndex[i]<nowIndex[i]){
                        canvas.drawLine(everyWidth*i+15, Yheight-nowIndex[i]-5, everyWidth*i+columnWidth+15, Yheight-nowIndex[i]-5, p);
                        lastIndex[i]=nowIndex[i];
                    }else if(lastIndex[i]-nowIndex[i]>7){
                        lastIndex[i]=lastIndex[i]-2;
                        canvas.drawLine(everyWidth*i+15, Yheight-lastIndex[i]-5, everyWidth*i+columnWidth+15, Yheight-lastIndex[i]-5, p);
                    }
                }else if(nowIndex[i]<=0){
                    if(lastIndex[i]>2){
                        lastIndex[i]=lastIndex[i]-2;
                        canvas.drawLine(everyWidth*i+15, Yheight-lastIndex[i], everyWidth*i+columnWidth+15, Yheight-lastIndex[i], p);
                    }else{
                        lastIndex[i]=0;
                        canvas.drawLine(everyWidth*i+15, Yheight, everyWidth*i+columnWidth+15, Yheight, p);
                    }
                }
            }
        }else if(picthNum<=0){
            for(int i=0;i<COUNT;i++){
                if(lastIndex[i]>2){
                    lastIndex[i]=lastIndex[i]-2;
                    canvas.drawLine(everyWidth*i+15, Yheight-lastIndex[i], everyWidth*i+columnWidth+15, Yheight-lastIndex[i], p);
                }else{
                    lastIndex[i]=0;
                    canvas.drawLine(everyWidth*i+15, Yheight, everyWidth*i+columnWidth+15, Yheight, p);
                }
            }
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);
        initScreen();
        canvas.drawBitmap(mSCBitmap, 0, 0, new Paint());

    }
    private void setIndexList(){
        index++;
        if(index>=2){
            index=0;
            Random r=new Random();
            for(int i=0;i<COUNT;i++){
                int num=picthNum+r.nextInt(100)-50;
                if(num>0){
                    nowIndex[i]=num;
                }else{
                    nowIndex[i]=0;
                }
            }
        }
    }
    public void  updatePicthNum(int picthNum){
        this.picthNum=picthNum;

        new Thread(new puThread()).start();
    }



    class puThread implements Runnable{

        @Override
        public void run() {

            try {
                Thread.sleep(300);
                if(picthNum>1){
                    picthNum=picthNum-1;

                }else{
                    picthNum=0;
                }
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }

    }

}
