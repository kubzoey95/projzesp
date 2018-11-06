package com.ok.wefwds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.Math.abs;
import static org.opencv.core.CvType.CV_64FC1;

/**
 * Created by kubzoey on 2018-10-23.
 *
 * Image handling and processing.
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
        Utils.bitmapToMat(bmp, imageMatrix);
    }

    public Image(Uri uri){
        Bitmap bmp = BitmapFactory.decodeFile(uri.getPath());
        imageMatrix = Mat.zeros(bmp.getHeight(), bmp.getWidth(), CV_64FC1);
        Utils.bitmapToMat(bmp, imageMatrix);
    }

    public Bitmap getBitmap(){
        Bitmap bmp = Bitmap.createBitmap(imageMatrix.width(), imageMatrix.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMatrix, bmp);
        return bmp;
    }

    static public File createImageFile(Context c) throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = c.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".bmp", storageDir);

        return image;
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
        Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 51, 2);
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
