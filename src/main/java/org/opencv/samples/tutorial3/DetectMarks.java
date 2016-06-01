package org.opencv.samples.tutorial3;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jcama on 29/12/2015.
 */

public class DetectMarks extends AsyncTask<ImgParam, Void, Void> {

    private static final String TAG = "OCR::WordSearch";

    private boolean busy = true;

    // OCR
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/WordSearch/";
    public static final String lang = "spa";
    protected String _path;

    //Position
    private List<int[]> positions = new ArrayList<>();

    public DetectMarks() {

    }

    public List<int[]> GetPositions(){
        return this.positions;
    }

    public Void doInBackground(ImgParam... params) {
        String[] words = params[0].GetWords();
        String recognizedText = ReadLetters(params[0].GetImg(), params[0].GetSquares(), params[0].GetContext());
        for (String word : words) {
            this.positions.add(SearchWord(recognizedText, word));
        }
        this.busy = false;
        return null;
    }

    public boolean isBusy() {return this.busy;}

    private String ReadLetters(Mat img, List<MatOfPoint> squares, Context myContext){
        Mat res = new Mat();
        Mat mask = Mat.zeros(new Size(img.cols(), img.rows()), CvType.CV_8UC1);
        Imgproc.drawContours(mask, squares, -1, new Scalar(255, 255, 255), -1);
        img.copyTo(res, mask);
        double clipLimit = 1.2;
        Size tileGridSize = new Size(8, 8);
        CLAHE clahe = Imgproc.createCLAHE(clipLimit, tileGridSize);
        clahe.apply(res, res);


        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return "ERROR: Creation of directory " + path + " on sdcard failed";
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = myContext.getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

        // Store Image
        _path = DATA_PATH + "wordsearch.png";
        boolean status = Imgcodecs.imwrite( _path, res);
        Log.v(TAG, "Status: " + status);

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(new File(DATA_PATH, "wordsearch.png"));

        String recognizedText = baseApi.getUTF8Text();
        Log.v(TAG, "Text: " + recognizedText);
        recognizedText = recognizedText.replace("*", "");
        recognizedText = recognizedText.replace("]", "J");
        recognizedText = recognizedText.replace("'I'", "T");
        recognizedText = recognizedText.replace("I)", "D");
        recognizedText = recognizedText.replace("(Z", "C");
        recognizedText = recognizedText.replace("I.", "L");
        recognizedText = recognizedText.replace("l—'", "F");
        recognizedText = recognizedText.replace("I(", "K");
        recognizedText = recognizedText.replace("I—I", "H");
        recognizedText = recognizedText.replace("1", "I");
        recognizedText = recognizedText.replace("2", "Z");
        recognizedText = recognizedText.replace("¿", "");
        recognizedText = recognizedText.replace("...", "");
        recognizedText = recognizedText.replace(" ", "");
        recognizedText = recognizedText.replace("\n\n", "\n");
        recognizedText = recognizedText.replace("'", "");
        recognizedText = recognizedText.replace("(¡", "G");
        Log.v(TAG, "Text: " + recognizedText);

        baseApi.end();

        recognizedText = recognizedText.trim();
        return recognizedText;
    }

    private int[] SearchWord(String recognizedText, String word){
        char[] words = word.toCharArray();
        boolean[] direction = new boolean[8];
        boolean stop = false;
        for (int i = 0; i < direction.length; i++){
            direction[i] = true;
        }
        int[] indexes = new int[words.length];
        char[] text = recognizedText.toCharArray();
        for (int j = 0; j < text.length; j++){
            if (text[j] == '\n') continue;
            if (text[j] == words[0]) {
                indexes[0] = j;
                for (int i = 1; i < words.length; i++) {
                    if (j - i*14 > 0 && direction[0]) {
                        if (text[j - i*14] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[0] = true;
                            }
                            indexes[i] = j - i*14;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j - i*14] != words[i] && i > 1 && direction[0]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 1; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j - i*13 > 0 && direction[1]) {
                        if (text[j - i*13] == words[i]) {
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++) {
                                    direction[k] = false;
                                }
                                direction[1] = true;
                            }
                            indexes[i] = j - i*13;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j - i*13] != words[i] && i > 1 && direction[1]) {
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 2; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j - i*12 > 0 && direction[2]) {
                        if (text[j - i*12] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[2] = true;
                            }
                            indexes[i] = j - i*12;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j - i*12] != words[i] && i > 1 && direction[2]) {
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 3; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j - i*1 > 0 && direction[3]) {
                        if (text[j - i*1] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[3] = true;
                            }
                            indexes[i] = j - i*1;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j - i*1] != words[i] && i > 1 && direction[3]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 4; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j + i*1 > 0 && direction[4]) {
                        if (text[j + i*1] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[4] = true;
                            }
                            indexes[i] = j + i*1;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j + i*1] != words[i] && i > 1 && direction[4]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 5; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j + i*12 > 0 && direction[5]) {
                        if (text[j + i*12] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[5] = true;
                            }
                            indexes[i] = j + i*12;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j + i*12] != words[i] && i > 1 && direction[5]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 6; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j + i*13 > 0 && direction[6]) {
                        if (text[j + i*13] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[6] = true;
                            }
                            indexes[i] = j + i*13;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j + i*13] != words[i] && i > 1 && direction[6]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 7; k++){
                                direction[k] = false;
                            }
                        }
                    }
                    if (j + i*14 > 0 && direction[7]) {
                        if (text[j + i*14] == words[i]){
                            if (i == 1) {
                                for (int k = 0; k < direction.length; k++){
                                    direction[k] = false;
                                }
                                direction[7] = true;
                            }
                            indexes[i] = j + i*14;
                            if (i == (indexes.length - 1)) stop = true;
                            continue;
                        }
                        if (text[j + i*14] != words[i] && i > 1 && direction[7]){
                            for (int k = 0; k < direction.length; k++){
                                direction[k] = true;
                            }
                            for (int k = 0; k < 8; k++){
                                direction[k] = false;
                            }
                        }
                    }

                    break;
                }
            }
            if (stop) break;

            for (int k = 0; k < direction.length; k++){
                direction[k] = true;
            }
        }

        return indexes;
    }

}

