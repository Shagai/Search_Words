package org.opencv.samples.tutorial3;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener {

    static {
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "OCVSample::Activity";

    private MainView mOpenCvCameraView;

    private OCRSearch det = new OCRSearch();
    private int state = 0;
    private Mat prevImg = null;
    private MatOfPoint2f prevFeatures = new MatOfPoint2f();

    private String[] words = null;
    private boolean ocr = false;
    private String soup = null;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {Log.i(TAG, "Instantiated new " + this.getClass());}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);

        mOpenCvCameraView = (MainView) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        Intent intent = getIntent();
        this.words = intent.getStringExtra(StartActivity.EXTRA_MESSAGE).split("\n");
        this.ocr = intent.getBooleanExtra(StartActivity.EXTRA_BOOL, true);
        if (!this.ocr) this.soup = intent.getStringExtra(StartActivity.EXTRA_SOUP);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat rgba = inputFrame.rgba();
        final Mat gray = inputFrame.gray();

        if(this.state == 1){
            MatOfPoint2f newFeatures;
            newFeatures = FeatureTracking(gray);
            List<MatOfPoint> squares = new ArrayList<>();
            MatOfPoint quad = new MatOfPoint();
            newFeatures.convertTo(quad, CvType.CV_32S);
            squares.add(quad);
            Imgproc.drawContours(rgba, squares, -1, new Scalar(0, 0, 255), 2);
            if (!det.isBusy()) {
                for (int[] position : det.GetPositions())
                    DrawWord(rgba, squares.get(0), position);
            }
        }

        if(this.state == 0){
            List<MatOfPoint> squares = new ArrayList<>();
            if(Find_Squares(rgba, squares) == 1) {
                this.prevImg = gray.clone();
                squares.get(0).convertTo(this.prevFeatures, CvType.CV_32FC3);
                Imgproc.drawContours(rgba, squares, -1, new Scalar(0, 0, 255), 2);
                this.state = 1;

                ImgParam param = new ImgParam();
                param.SetImg(gray);
                param.SetWords(this.words);
                param.SetSquares(squares);
                param.SetContext(getApplicationContext());
                if(!this.ocr) {
                    param.SetOCR(this.ocr);
                    param.SetSoup(this.soup);
                }
                det.execute(param);
            }
        }

        return rgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mOpenCvCameraView.setupCameraFlashLight();
        return false;
    }

    private int Find_Squares(Mat img, List<MatOfPoint> quads) {
        Mat cannyDst = new Mat();

        Imgproc.Canny(img, cannyDst, 0, 50);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyDst, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint cnt : contours) {
            MatOfPoint2f cnt2f = new MatOfPoint2f(cnt.toArray());
            double cnt_len = Imgproc.arcLength(cnt2f, true);
            Imgproc.approxPolyDP(cnt2f, cnt2f, 0.02 * cnt_len, true);
            cnt2f.convertTo(cnt, CvType.CV_32S);
            if (cnt2f.height() == 4 && Imgproc.contourArea(cnt2f) > 10000){

                double[] cos = new double[4];

                for (int i = 0; i < 4; i++){
                    double[] p0 = cnt.get(i, 0);
                    double[] p1 = cnt.get((i + 1)%4, 0);
                    double[] p2 = cnt.get((i + 2)%4, 0);
                    cos[i] = angle_cos(p0, p1, p2);
                }

                if (getMax(cos) < 0.3){
                    quads.add(cnt);
                    return 1;
                }

            }
        }

        return -1;
    }

    private double angle_cos(double[] p0, double[] p1, double[] p2){
        double[] d1 = new double[2];
        double[] d2 = new double[2];

        for (int i = 0; i < 2; i++) {
            d1[i] = p0[i] - p1[i];
            d2[i] = p2[i] - p1[i];
        }

        double dot = d1[0] * d2[0] + d1[1] * d2[1];
        double dot1 = d1[0] * d1[0] + d1[1] * d1[1];
        double dot2 = d2[0] * d2[0] + d2[1] * d2[1];

        return Math.abs(dot / Math.sqrt(dot1 * dot2));
    }

    private static double getMax(double[] array){
        double max = 0;
        for (double num : array) {
            if (num > max) {
                max = num;
            }
        }

        return max;
    }

    private MatOfPoint2f FeatureTracking(Mat newImage){
        MatOfPoint2f newFeatures = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(this.prevImg, newImage, this.prevFeatures, newFeatures, status, err);
        this.prevFeatures = newFeatures;
        this.prevImg = newImage.clone();
        return newFeatures;
    }

    private void DrawWord(Mat rgba, MatOfPoint square, int[] position){

        Point point1 = new Point(square.get(0,0)[0] ,square.get(0,0)[1]);
        Point point2 = new Point(square.get(1,0)[0] ,square.get(1,0)[1]);
        Point point4 = new Point(square.get(3,0)[0] ,square.get(3,0)[1]);

        Point xRelative = new Point(point2.x - point1.x,
                                    point2.y - point1.y);
        Point yRelative = new Point(point4.x - point1.x,
                                    point4.y - point1.y);
        int indexY = position[0]/13;
        int indexX = position[0]%13;
        Point start = new Point(point1.x + indexX * xRelative.x/12 +
                                indexY * yRelative.x/12,
                                point1.y + indexX * xRelative.y/12 +
                                indexY * yRelative.y/12);
        indexY = position[position.length - 1]/13;
        indexX = position[position.length - 1]%13;
        Point finish = new Point(point1.x + indexX * xRelative.x/12 +
                                 indexY * yRelative.x/12,
                                 point1.y + (indexX + 0.5) * xRelative.y/12 +
                                 indexY * yRelative.y/12);
        Imgproc.line(rgba, start, finish, new Scalar(0, 0, 255), 10);
    }
}
