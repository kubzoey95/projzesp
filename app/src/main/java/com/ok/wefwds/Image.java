package com.ok.wefwds;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static java.lang.Math.abs;
import static org.opencv.core.CvType.CV_64FC1;

/**
 * Created by kubzoey on 2018-10-23.
 */

public class Image {

    Mat imageMatrix = new Mat();

    public Image(){

    }

    public Image(Mat mat){
        imageMatrix = mat;
    }

    public Image(Camera cam){
        Bitmap bmp = cam.getBitmap();
        imageMatrix = Mat.zeros(bmp.getHeight(), bmp.getWidth(), CV_64FC1);
        Utils.bitmapToMat(cam.getBitmap(), imageMatrix);
    }

    public Bitmap getBitmap(){
        Bitmap bmp = Bitmap.createBitmap(imageMatrix.width(), imageMatrix.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMatrix, bmp);
        return bmp;
    }

    public void makeGray(){
        Imgproc.cvtColor(imageMatrix, imageMatrix, Imgproc.COLOR_RGB2GRAY);
    }

    public void applyErosion(int erosion_size){
        Imgproc.erode(imageMatrix, imageMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*erosion_size + 1,2*erosion_size + 1)));
    }

    public void setImageMatrix(Mat mat){
        imageMatrix = mat;
    }

    public void makeBinary(){
        int avg = highestContrastAverage();
        if (avg > 127){
            //Imgproc.threshold(imageMatrix,imageMatrix, highestContrastAverage(), 255, Imgproc.THRESH_BINARY_INV);
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        }
        else {
            //Imgproc.threshold(imageMatrix,imageMatrix, highestContrastAverage(), 255, Imgproc.THRESH_BINARY_INV);
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        }
    }

    public int averageColor(){
        double sum = 0;
        Size imageMatrixSize = imageMatrix.size();
        for (int i=0;i< imageMatrixSize.width;i++){
            for(int n=0;n<imageMatrixSize.height;n++){
                sum += imageMatrix.get(n,i)[0];
            }
        }
        return (int)(sum / (imageMatrixSize.height * imageMatrixSize.width));
    }

    public int highestContrastAverage(){
        double sum = 0;
        int avg = averageColor();
        double contrast_sum = 0;
        double contrast;
        double color;
        Size imageMatrixSize = imageMatrix.size();
        for (int i=0;i< imageMatrixSize.width;i++){
            for(int n=0;n<imageMatrixSize.height;n++){
                color = imageMatrix.get(n,i)[0];
                contrast = abs(color - avg);
                sum += color * contrast;
                contrast_sum += contrast;
            }
        }
        return (int)(sum / contrast_sum);
    }

    public void equalizeHist(){
        Imgproc.equalizeHist(imageMatrix, imageMatrix);
    }
}
