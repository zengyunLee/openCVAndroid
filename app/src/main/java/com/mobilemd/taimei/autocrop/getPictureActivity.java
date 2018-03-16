package com.mobilemd.taimei.autocrop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.mobilemd.taimei.autocrop.Utils.ImageUtils;
import com.soundcloud.android.crop.Crop;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class getPictureActivity extends AppCompatActivity {
    public static final String TAG = "AutoCrop";
    public static final int TAKE_PHOTO = 1;
    public static final int PICK_PHOTO = 0;
    public static final int CROP_PHOTO = 2;

    private Button TakePhoto;
    private Button Transform;
    private Button PickPhoto;
    private ImageView picture;
    private Uri imageUri;
    private Bitmap mBitmap;
    private Uri pendingCrop;
    private int cropWidth = 108 * 8;
    private int cropHeight = 192 * 8;


    static {
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            Log.i(TAG, "static loadLibrary success ");
        } else {
            Log.i(TAG, "static loadLibrary fail");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_picture);
        picture =  findViewById(R.id.Img1);
        TakePhoto = findViewById(R.id.btn_take_photo);
        Transform=  findViewById(R.id.btn_choose_pic);
        PickPhoto = findViewById(R.id.btn_Pick);
        TakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
                try{
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e){
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT >=24) {
                    imageUri = FileProvider.getUriForFile(getPictureActivity.this,"com.mobilemd.taimei.autocrop",outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });
        Transform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBitmap =((BitmapDrawable) picture.getDrawable()).getBitmap();
                if(null==mBitmap) {
                    return;
                }
                /*Mat srcmat = convertGray(mBitmap);
                Log.i(TAG, "onClick: done");
                List<Point> begin = getCornersByContour(binaryzation(srcmat));
                Toast.makeText(getPictureActivity.this, "你好!", Toast.LENGTH_LONG).show();
                Mat desmat = findAndDrawContours(binaryzation(srcmat));
                Utils.matToBitmap(desmat,mBitmap);
                picture.setImageBitmap(mBitmap);*/
                transform(mBitmap);
                beginCrop(pendingCrop,cropWidth,cropHeight);
            }
        });
        PickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickPhoto();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " );
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(mBitmap);
                        Log.d(TAG, "onActivityResult: ");
                    }catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case Crop.REQUEST_PICK:
                if(resultCode == RESULT_OK) {
                    beginCrop(data.getData(),cropWidth,cropHeight);
                }
                break;
            case Crop.REQUEST_CROP:
                    handleCrop(resultCode,data);
                break;
            case PICK_PHOTO:
                if (resultCode == RESULT_OK && null != data) {
                    Uri selectedImg = data.getData();
                    picture.setImageURI(selectedImg);
                    mBitmap =((BitmapDrawable) picture.getDrawable()).getBitmap();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 高斯滤波
     * @param src 待滤波的灰度图
     * @return 滤波后的灰度图
     */

    public static Mat removeGaussianNoise(Mat src) {
        Mat des = new Mat();
        Size size = new Size(7,7);
        Imgproc.GaussianBlur(src,des,size,0,0);
        return  des;
    }

    /**
     * @param imgsource 经过预处理（高斯滤波，灰度处理，二值化）之后的Mat
     * @return 找到最大轮廓之后，最大轮廓拟合四边形的顶点
     */
    public static List<Point> getCornersByContour(Mat imgsource){
        List<MatOfPoint> contours=new ArrayList<>();
        //轮廓检测
        Imgproc.findContours(imgsource,contours,new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea=-1;
        int maxAreaIdx=-1;
        MatOfPoint temp_contour;
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int idx = 0; idx < contours.size(); idx++){
            temp_contour = contours.get(idx);
            double contourarea=Imgproc.contourArea(temp_contour);
            //当前轮廓面积比最大的区域面积大就检测是否为四边形
            if (contourarea > maxArea){
                //检测contour是否是四边形
                MatOfPoint2f new_mat=new MatOfPoint2f(temp_contour.toArray());
                int contourSize= (int) temp_contour.total();
                MatOfPoint2f approxCurve_temp=new MatOfPoint2f();
                //对图像轮廓点进行多边形拟合
                Imgproc.approxPolyDP(new_mat,approxCurve_temp,contourSize*0.05,true);
                if (approxCurve_temp.total()==4){
                    maxArea=contourarea;
                    maxAreaIdx=idx;
                    approxCurve=approxCurve_temp;
                }
            }
        }
        double[] temp_double=approxCurve.get(0,0);
        Point point1=new Point((int) temp_double[0],(int) temp_double[1]);

        temp_double=approxCurve.get(1,0);
        Point point2=new Point((int) temp_double[0],(int) temp_double[1]);

        temp_double=approxCurve.get(2,0);
        Point point3=new Point((int) temp_double[0],(int) temp_double[1]);
        temp_double=approxCurve.get(3,0);

        Point point4=new Point((int) temp_double[0],(int) temp_double[1]);

        List<Point> source=new ArrayList<>();
        source.add(point1);
        source.add(point2);
        source.add(point3);
        source.add(point4);
        //对4个点进行排序
        Point centerPoint=new Point(0,0);//质心
        for (Point corner:source){
            centerPoint.x += corner.x / 4;
            centerPoint.y += corner.y / 4;
        }
        Point lefttop = new Point();
        Point righttop = new Point();
        Point leftbottom = new Point();
        Point rightbottom = new Point();
        for (int i = 0;i < source.size(); i++) {
            if (source.get(i).x < centerPoint.x && source.get(i).y < centerPoint.y) {
                lefttop=source.get(i);
            } else if (source.get(i).x > centerPoint.x && source.get(i).y < centerPoint.y) {
                righttop=source.get(i);
            } else if (source.get(i).x < centerPoint.x && source.get(i).y > centerPoint.y) {
                leftbottom=source.get(i);
            } else if (source.get(i).x > centerPoint.x && source.get(i).y > centerPoint.y) {
                rightbottom=source.get(i);
            }
        }
        source.clear();
        source.add(lefttop);
        source.add(righttop);
        source.add(leftbottom);
        source.add(rightbottom);
        return source;
    }

    /**
     * 轮廓提取并显示在原图上，
     * 输入的Mat必须是二值化之后的灰度图
     */
    public Mat findAndDrawContours(Mat blurredImage) {
        List<MatOfPoint> contours=new ArrayList<>();
        Imgproc.findContours(blurredImage,contours,new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        Mat mRgba=new Mat();
        mRgba.create(blurredImage.rows(), blurredImage.cols(), CvType.CV_8UC3);
        //绘制检测到的轮廓
        Imgproc.drawContours(mRgba, contours, maxValIdx, new Scalar(0,255,0), 5);
        Log.d(TAG, "findAndDrawContours: done");
        return mRgba;
    }

    private static Mat convertGray(Bitmap mBitmap) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Utils.bitmapToMat( mBitmap , rgbMat);//convert original bitmap to Mat, R G B.
        rgbMat= removeGaussianNoise(rgbMat);
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
//        Utils.matToBitmap(grayMat, grayBitmap); //convert mat to bitmap
        Log.d(TAG, "convertGray: done");
        return grayMat;
    }

    /**
     * 将灰度图二值化
     */
    private Mat binaryzation(Mat mat) {
        Mat m = new Mat();
        //Imgproc.threshold(mat,m,180,255,Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(mat, m, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 0);
        return m;
    }

    /**
     * 根据轮廓检测得到的四个顶点，计算得到包含该四边形（与该四边形相切）的矩形顶点坐标
     * @param list
     * @return
     */
    private List<Point> getReatAnglesPoint(List<Point> list) {
        List<Point> des = new ArrayList<>();
        Point point = list.get(0);
        double xMax = point.x;
        double xMin = point.x;
        double yMax = point.y;
        double yMin = point.y;
        for(Point p :list) {
            xMax = xMax > p.x ? xMax : p.x;
            xMin = xMin < p.x ? xMin : p.x;
            yMax = yMax > p.y ? yMax : p.y;
            yMin = yMin < p.y ? yMin : p.y;
        }
        cropWidth = (int) (xMax - xMin);
        cropHeight = (int) (yMax - yMin);
        des.add(new Point(xMin,yMin));
        des.add(new Point(xMax,yMin));
        des.add(new Point(xMin,yMax));
        des.add(new Point(xMax,yMax));
        return des;
    }

    public void transform(Bitmap bitmap) {
        Bitmap rgbBitmap =  bitmap;
        Mat rgbSrcMat = ImageUtils.bitmapToMat(rgbBitmap);
        Mat correctedImage = new Mat(rgbSrcMat.rows(),rgbSrcMat.cols(),rgbSrcMat.type());
        Mat sampledImage = convertGray(bitmap);
        /**
         * 根据轮廓检测获取顶点传入的图像必须是二值化之后的图像，否则只能检测一个轮廓（外边框）
         */
        List<Point> srcCorners = getCornersByContour(binaryzation(sampledImage));
        List<Point> desCorners = getReatAnglesPoint(srcCorners);
        Mat srcPoints = Converters.vector_Point2f_to_Mat(srcCorners);
        Mat desPoints = Converters.vector_Point2f_to_Mat(desCorners);
        //求出变换矩阵
        Mat transformation = Imgproc.getPerspectiveTransform(srcPoints,desPoints);
        //进行透视变换
        Imgproc.warpPerspective(rgbSrcMat,correctedImage,transformation,correctedImage.size());
        Bitmap rgbDesBitmap = ImageUtils.matToBitmap(correctedImage);
        picture.setImageBitmap(rgbDesBitmap);
        adapt(rgbDesBitmap);
    }

    private void adapt(Bitmap rgbDesBitmap) {
        try {
            pendingCrop = saveBitmap(rgbDesBitmap,"transformedImg");
//            beginCrop(pendingCrop);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(getPictureActivity.this,"transform done!",Toast.LENGTH_SHORT).show();
    }

    private Uri saveBitmap(Bitmap bitmap,String bitName) throws IOException
    {
        File file = new File("/data/data/com.mobilemd.taimei.autocrop/"+bitName);
        Uri dstUri = Uri.fromFile(file);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 90, out))
            {
                out.flush();
                out.close();
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return dstUri;
    }

    private void beginCrop(Uri source,int x,int y) {
        Uri dstUri = Uri.fromFile(new File(getCacheDir(),"cropped"));
        Crop.of(source,dstUri).withAspect(x, y).start(this);
    }

    private void handleCrop(int resultCode,Intent result) {
        if (resultCode == RESULT_OK) {
            picture.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this,Crop.getError(result).getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto () {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, PICK_PHOTO);
    }
}
