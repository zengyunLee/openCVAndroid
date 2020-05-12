package com.feiyu.aaron.autocrop;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
public class MainActivity extends AppCompatActivity {
    public static String TAG="aaron";
    private String mPath;
    private ImageView mImageView;
    private Bitmap mBitmap;
    private TextView mTextView;

    static {
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            Log.i(TAG, "static loadLibrary success ");
        } else {
            Log.i(TAG, "static loadLibrary fail");
        }
    }

/*    //openCV4Android 需要加载用到
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };*/

    private void convertGray() {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Bitmap   grayBitmap = Bitmap.createBitmap( mBitmap .getWidth(),  mBitmap .getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat( mBitmap , rgbMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Utils.matToBitmap(grayMat, grayBitmap); //convert mat to bitmap
        mImageView.setImageBitmap(grayBitmap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image1);
//        mPath = getIntent().getStringExtra("picpath");
        mBitmap =((BitmapDrawable) ((ImageView) mImageView).getDrawable()).getBitmap();
        mTextView = (TextView) findViewById(R.id.Text1);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                convertGray();
            }
        });
    }

/*    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }*/
}
