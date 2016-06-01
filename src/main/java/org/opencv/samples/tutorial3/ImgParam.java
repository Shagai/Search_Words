package org.opencv.samples.tutorial3;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by Jcama on 31/05/2016.
 */
public class ImgParam {

    private Mat img = null;
    private String[] words = null;
    private List<MatOfPoint> squares = null;
    private Context myContext = null;

    public ImgParam(){

    }

    public void SetImg(Mat img){ this.img = img;}
    public Mat GetImg(){return this.img;}

    public void SetWords(String[] words){ this.words = words;}
    public String[] GetWords(){return this.words;}

    public void SetSquares(List<MatOfPoint> squares){ this.squares = squares;}
    public List<MatOfPoint> GetSquares(){return this.squares;}

    public void SetContext(Context myContext){ this.myContext = myContext;}
    public Context GetContext(){return this.myContext;}
}
