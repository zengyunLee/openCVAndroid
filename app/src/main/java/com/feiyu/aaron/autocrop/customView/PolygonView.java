package com.feiyu.aaron.autocrop.customView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.feiyu.aaron.autocrop.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lizengyun on 2018/3/14.
 */

public class PolygonView extends FrameLayout {

    private Context mContext;
    private Paint mPaint;
    private ImageView mPointer1;
    private ImageView mPointer2;
    private ImageView mPointer3;
    private ImageView mPointer4;
    private PolygonView mPolygonView;

    public PolygonView(@NonNull Context context) {
        this(context, null);

    }

    public PolygonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPolygonView = this;
        mPointer1 = getImageView(0, 0);
        mPointer2 = getImageView(getWidth(), 0);
        mPointer3 = getImageView(0, getHeight());
        mPointer4 = getImageView(getWidth(), getHeight());
        addView(mPointer1);
        addView(mPointer2);
        addView(mPointer3);
        addView(mPointer4);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(getResources().getColor(R.color.blue));
        mPaint.setStrokeWidth(2);
    }

    private ImageView getImageView(int x, int y) {
        ImageView imageview = new ImageView(mContext);
        LayoutParams layoutparams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageview.setLayoutParams(layoutparams);
        imageview.setImageResource(R.drawable.circle);
        imageview.setX(x);
        imageview.setY(y);
        imageview.setOnTouchListener(new TouchListaner());
        return imageview;
    }

    private class TouchListaner implements OnTouchListener {

        PointF downPoint = new PointF();
        PointF startPoint = new PointF();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downPoint.x = event.getX();
                    downPoint.y = event.getY();
                    startPoint = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - downPoint.x, event.getY() - downPoint.y);
                    if ((startPoint.x + mv.x + v.getWidth()) < mPolygonView.getWidth() &&
                            (startPoint.y + mv.y + v.getHeight()) < mPolygonView.getHeight() &&
                            (startPoint.x + mv.x) > 0 && (startPoint.y + mv.y) > 0
                            ) {
                        v.setX(startPoint.x + mv.x);
                        v.setY(startPoint.y + mv.y);
                        startPoint = new PointF(v.getX(), v.getY());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(R.color.blue);
                    } else {
                        color = getResources().getColor(R.color.orange);
                    }
                    mPaint.setColor(color);
                    break;
            }
            mPolygonView.invalidate();
            return true;
        }

    }

    public Map<Integer, PointF> getPoints() {
        List<PointF> points = new ArrayList<>();
        points.add(new PointF(mPointer1.getX(), mPointer1.getY()));
        points.add(new PointF(mPointer2.getX(), mPointer2.getY()));
        points.add(new PointF(mPointer3.getX(), mPointer3.getY()));
        points.add(new PointF(mPointer4.getX(), mPointer4.getY()));
        return getOrderedPoints(points);
    }

    /**
     * 为各个点重新排序
     * @param points
     * @return
     */
    public Map<Integer,PointF> getOrderedPoints(List<PointF> points){
        PointF centerPoint=new PointF();
        int size=points.size();
        for(PointF point:points){
            centerPoint.x+=point.x/size;
            centerPoint.y+=point.y/size;
        }
        Map<Integer,PointF> orderedPoints=new HashMap<>();
        for (PointF point:points){
            int index=-1;
            if (point.x<centerPoint.x&&point.y<centerPoint.y){
                index=0;
            }else if (point.x>centerPoint.x&&point.y<centerPoint.y){
                index=1;
            }else if (point.x<centerPoint.x&& point.y>centerPoint.y){
                index=2;
            }else if (point.x>centerPoint.x&&point.y>centerPoint.y){
                index=3;
            }
            orderedPoints.put(index,point);
        }

        return orderedPoints;
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawLine(mPointer1.getX() + (mPointer1.getWidth() / 2), mPointer1.getY() + (mPointer1.getHeight() / 2), mPointer3.getX() + (mPointer3.getWidth() / 2), mPointer3.getY() + (mPointer3.getHeight() / 2), mPaint);
        canvas.drawLine(mPointer1.getX() + (mPointer1.getWidth() / 2), mPointer1.getY() + (mPointer1.getHeight() / 2), mPointer2.getX() + (mPointer2.getWidth() / 2), mPointer2.getY() + (mPointer2.getHeight() / 2), mPaint);
        canvas.drawLine(mPointer2.getX() + (mPointer2.getWidth() / 2), mPointer2.getY() + (mPointer2.getHeight() / 2), mPointer4.getX() + (mPointer4.getWidth() / 2), mPointer4.getY() + (mPointer4.getHeight() / 2), mPaint);
        canvas.drawLine(mPointer3.getX() + (mPointer3.getWidth() / 2), mPointer3.getY() + (mPointer3.getHeight() / 2), mPointer4.getX() + (mPointer4.getWidth() / 2), mPointer4.getY() + (mPointer4.getHeight() / 2), mPaint);

    }
    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        mPointer1.setX(pointFMap.get(0).x);
        mPointer1.setY(pointFMap.get(0).y);

        mPointer2.setX(pointFMap.get(1).x);
        mPointer2.setY(pointFMap.get(1).y);

        mPointer3.setX(pointFMap.get(2).x);
        mPointer3.setY(pointFMap.get(2).y);

        mPointer4.setX(pointFMap.get(3).x);
        mPointer4.setY(pointFMap.get(3).y);
    }


}
