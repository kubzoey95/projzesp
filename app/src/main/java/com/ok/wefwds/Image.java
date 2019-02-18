package com.ok.wefwds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static org.opencv.core.CvType.CV_64FC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;

/**
 * Created by kubzoey on 2018-10-23.
 *
 * Image handling and processing.
 */

public class Image {

    Mat imageMatrix = new Mat();

    public Image clone(){
        return new Image(imageMatrix.clone());
    }

    public Image(Mat mat){
        imageMatrix = mat;
    }

    public Image(Uri uri){
        Bitmap bmp = BitmapFactory.decodeFile(uri.getPath());
        imageMatrix = Mat.zeros(bmp.getHeight(), bmp.getWidth(), CV_64FC1);
        Utils.bitmapToMat(bmp, imageMatrix);
    }

    public Image(Bitmap bmp){
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

    public void blur(int size){
        if(size % 2 == 0){
            size++;
        }
        Imgproc.GaussianBlur(imageMatrix, imageMatrix, new Size(size, size), 0);
    }

    public void extract_notes(double ratio2, double line_interval) {
        this.blur((int) (line_interval));
        this.cleaning(ratio2);
        this.blur((int) (line_interval));

        this.applyDilation((int) (40. * ratio2));
        this.thresh(100, false);
        this.blur((int) (line_interval));

        this.applyDilation((int) (20. * ratio2));
        this.thresh(80, false);

        this.blur((int) (line_interval));
        this.thresh(127, false);

        this.blur((int) (line_interval));
        this.thresh(100, false);
        this.blur((int) (line_interval));

        this.thresh(100, false);
        this.applyErosion((int) (line_interval / 3.));
        this.blur((int) (line_interval));

        this.thresh(170, false);
        this.blur((int) (line_interval));
        this.thresh(200, false);
    }

    public void applyErosion(int erosion_size){
        Imgproc.erode(imageMatrix, imageMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*erosion_size + 1,2*erosion_size + 1)));
    }

    public void applyDilation(int dilation_size){
        Imgproc.dilate(imageMatrix, imageMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*dilation_size + 1,2*dilation_size + 1)));
    }

    public void cleaning(double ratio2){
        this.applyDilation((int)(10. * ratio2));
        this.applyErosion((int)(60. * ratio2));
        this.applyDilation((int)(40. * ratio2));
    }

    public void makeBinary(int block_size, boolean inv){
        if (inv) {
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, block_size * 2 + 1, 2);
        }
        else{
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, block_size * 2 + 1, 2);
        }
    }

    public void thresh(double thresh, boolean inv){
        if (inv) {
            Imgproc.threshold(imageMatrix, imageMatrix, thresh, 255, Imgproc.THRESH_BINARY_INV);
        }
        else{
            Imgproc.threshold(imageMatrix, imageMatrix, thresh, 255, Imgproc.THRESH_BINARY);
        }
    }

    public Mat detectLines(){
        Mat lines = new Mat();
        Imgproc.HoughLinesP(imageMatrix, lines, 1, 1*PI/180., (int)(imageMatrix.width() * 0.5), imageMatrix.width() * 0.8, imageMatrix.width() * 0.01);
        return lines;
    }

    public Mat detectLinesCanny(double tr1, double tr2){
        Mat lines = new Mat();
        Mat canny = new Mat();
        Imgproc.Canny(imageMatrix, canny, tr1, tr2);
        Imgproc.HoughLinesP(canny, lines, 1, 1*PI/180., (int)(imageMatrix.width() * 0.5), imageMatrix.width() * 0.8 , imageMatrix.width() * 0.01);
        return lines;
    }

    public List<MatOfPoint> contourDetector(){
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imageMatrix, contours,new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    public void drawContours(List<MatOfPoint> contours, Scalar col){
        Imgproc.drawContours(imageMatrix, contours, -1, col, 5);
    }

    public static List<MatOfPoint> filterContours(List<MatOfPoint> contours, double ar_min, double circ){
        List<MatOfPoint> filtered = new ArrayList<>();
        for(int i = 0; i<contours.size();i++) {
            MatOfPoint2f contour = new MatOfPoint2f(contours.get(i).toArray());
            double arcLength = Imgproc.arcLength(contour, true);
            double area = Imgproc.contourArea(contour);
            if ((4. * PI * area) / pow(arcLength,2.) > circ && area > ar_min){
                filtered.add(new MatOfPoint(contour.toArray()));
            }
        }
        return filtered;
    }


    public static Point getContourCenter(MatOfPoint contour){
        Moments m = Imgproc.moments(contour);
        return new Point(m.get_m10() / m.get_m00(), m.get_m01() / m.get_m00());
    }

    public static List<Point> getContoursCenters(List<MatOfPoint> contours){
        List<Point> centers = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            centers.add(getContourCenter(contour));
        }
        return centers;
    }

    public void bilateralFilter(){
        Mat contoured = new Mat();
        imageMatrix.convertTo(imageMatrix, CV_8UC3);
        Imgproc.cvtColor(imageMatrix,imageMatrix,Imgproc.COLOR_RGBA2RGB);
        Imgproc.bilateralFilter(imageMatrix, contoured, 5, 175, 175);
        contoured.convertTo(imageMatrix, CV_8UC1);
    }

    public void drawLines(double[] heights, Scalar col){
        double width = imageMatrix.width();
        for(int i=0; i<heights.length;i++) {
            Imgproc.line(imageMatrix, new Point(0, heights[i]), new Point(width - 1, heights[i]), col, 2);
        }
    }

    public void scale(double factor){
        Imgproc.resize(imageMatrix, imageMatrix, new Size(imageMatrix.width() * factor, imageMatrix.height() * factor));
    }

    public void processingPhoto(double ratio, boolean inv) {
        this.makeBinary((int) (80. * ratio), inv);
        this.applyErosion((int) (5. * ratio));
        this.applyDilation((int) (5. * ratio));
    }

    public int[] averageColor(int interval){
        double sum = 0;

        interval = max(1, interval);
        Size imageMatrixSize = imageMatrix.size();
        double height = imageMatrixSize.height;
        double width = imageMatrixSize.width;
        double min = 0;
        double max = 0;
        double get;
        double min_weights = 0;
        double max_weights = 0;
        double min_get;
        double max_get;
        Mat img = imageMatrix.reshape(1,1);
        for (int i=0;i< width*height;i+=interval){
            get = img.get(0,i)[0];
            sum += get;
            min_get = exp(255 - get);
            max_get = exp(get);
            min_weights += min_get;
            max_weights += max_get;
            min += get*min_get;
            max += get*max_get;
        }
        return new int[] {(int)(sum / ((height * width) / interval)),(int)(min / min_weights), (int)(max / max_weights)};
    }
}
