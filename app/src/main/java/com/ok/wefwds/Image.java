package com.ok.wefwds;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.photo.Photo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import static java.lang.Math.abs;
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

    public Image(){

    }

    public Image clone(){
        return new Image(imageMatrix.clone());
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

    public void inv(){
        Core.bitwise_not(imageMatrix, imageMatrix);
    }

    public void blur(int size){
        Imgproc.blur(imageMatrix, imageMatrix, new Size(size, size));
    }

    public void drawLines(List<Integer> lines){
        Imgproc.cvtColor(imageMatrix, imageMatrix, Imgproc.COLOR_GRAY2RGB);
        for (int i=0; i<lines.size();i++) {
            Imgproc.line(imageMatrix,new Point(0, lines.get(i)), new Point(500, lines.get(i)),new Scalar(0,0,255), 10);
        }
    }

    public void applyErosion(int erosion_size){
        Imgproc.erode(imageMatrix, imageMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*erosion_size + 1,2*erosion_size + 1)));
    }

    public void applyDilation(int dilation_size){
        Imgproc.dilate(imageMatrix, imageMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*dilation_size + 1,2*dilation_size + 1)));
    }

    public void setImageMatrix(Mat mat){
        imageMatrix = mat;
    }

    public void makeBinary(int block_size, boolean inv){
        if (inv) {
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, block_size*2 + 1, 2);
        }
        else{
            Imgproc.adaptiveThreshold(imageMatrix, imageMatrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, block_size*2 + 1, 2);
        }
    }

    public void makeBinary(double thresh, boolean inv){
        if (inv) {
            Imgproc.threshold(imageMatrix, imageMatrix, thresh, 255, Imgproc.THRESH_BINARY_INV);
        }
        else{
            Imgproc.threshold(imageMatrix, imageMatrix, thresh, 255, Imgproc.THRESH_BINARY);
        }
    }

    public void denoiseNI(){
        Photo.fastNlMeansDenoising(imageMatrix, imageMatrix);
    }
    public void reduceNoise(boolean erosionFirst){
        if (erosionFirst){
            this.applyErosion(2);
            this.applyDilation(2);
            this.applyErosion(2);
            this.applyDilation(2);
        }
        else{
            this.applyDilation(2);
            this.applyErosion(2);
            this.applyDilation(2);
            this.applyErosion(2);
        }
    }

    public void rotate(double degree){
        Mat rotation_matrix = Imgproc.getRotationMatrix2D(new Point(imageMatrix.width() / 2, imageMatrix.height() / 2), degree, 1.0);
        Imgproc.warpAffine(imageMatrix,imageMatrix,rotation_matrix, imageMatrix.size());
    }

    public double taxiMetric(double[] x,double[] y){
        return abs(x[0]  - y[0]) + abs(x[1] - y[1]);
    }

    public Mat detectLines(){
        Mat lines = new Mat();
        Imgproc.HoughLinesP(imageMatrix, lines, 2, 2*PI/180., (int)(imageMatrix.width() * 0.5), imageMatrix.width() * 0.8, imageMatrix.width() * 0.3);

        return lines;
    }

    public Mat detectLines(double threshold, double minLineLength, double maxLineGap){
        Mat lines = new Mat();
        Mat canny = new Mat();
        Imgproc.Canny(imageMatrix, canny, 75, 200);
        Imgproc.HoughLinesP(imageMatrix, lines, 1., PI/180., (int)(imageMatrix.width() * threshold), imageMatrix.width() * minLineLength, imageMatrix.width() * maxLineGap);
        return lines;
    }

    public void drawLines(Mat m, Scalar col, int thickness){
        double[] line;
        for(int i = 0; i<m.height();i++) {
            line = m.get(i,0);
            Imgproc.line(imageMatrix, new Point(line[0], line[1]), new Point(line[2], line[3]), col, thickness);
        }
    }

    public List<MatOfPoint> contourDetector(){
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imageMatrix, contours,new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    public void drawContours(List<MatOfPoint> contours, Scalar col){
        Imgproc.drawContours(imageMatrix, contours, -1, col, 50);
    }

    public static List<MatOfPoint> filterContours(List<MatOfPoint> contours, double ar, double circ){
        List<MatOfPoint> filtered = new ArrayList<>();
        for(int i = 0; i<contours.size();i++) {
            MatOfPoint2f contour = new MatOfPoint2f(contours.get(i).toArray());
            double arcLength = Imgproc.arcLength(contour, true);
            double area = Imgproc.contourArea(contour);
            if ((4. * PI * area) / pow(arcLength,2.) > circ && area > ar){
                filtered.add(new MatOfPoint(contour.toArray()));
            }
        }
        return filtered;
    }

    public MatOfKeyPoint blobDetection(Context c){
        MatOfKeyPoint kps = new MatOfKeyPoint();
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        fd.read(getPath("blob.xml", c));
        fd.detect(imageMatrix, kps);
        return kps;
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

    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i("OpenCV", "Failed to upload a file");
        }
        return "";
    }


    public void bilateralFilter(){
        Mat contoured = new Mat();
        imageMatrix.convertTo(imageMatrix, CV_8UC3);
        Imgproc.cvtColor(imageMatrix,imageMatrix,Imgproc.COLOR_RGBA2RGB);
        Imgproc.bilateralFilter(imageMatrix, contoured, 5, 175, 175);
        //Imgproc.Canny(contoured, imageMatrix, 75, 200);
        contoured.convertTo(imageMatrix, CV_8UC1);
    }

    public void canny(){
        Imgproc.Canny(imageMatrix, imageMatrix, 20, 240);
    }


    public void drawCircles(Mat circles){
        double len = circles.width();
        Point center;
        double[] circle;
        for(int i=0;i<len;i++) {
            circle = circles.get(0,i);
            center = new Point(circle[0], circle[1]);
            Imgproc.circle(imageMatrix, center, (int)circle[2], new Scalar(128));
        }
    }

    public void drawLines(double[] heights, Scalar col){
        double width = imageMatrix.width();
        for(int i=0; i<heights.length;i++) {
            Imgproc.line(imageMatrix, new Point(0, heights[i]), new Point(width - 1, heights[i]), col, 50);
        }
    }

    public static double[] distances(double[] points){
        int len = points.length;
        double[] dists = new double[len];
        for(int i = 1;i<len;i++){
            dists[i-1] = points[i] - points[i-1];
        }
        return dists;
    }

    public static double mean(double[] points){
        double sum=0;
        double len = points.length;
        for(int i=0;i<len;i++){
            sum += points[i];
        }
        return sum / len;
    }

    public static double[] clusters(double[] points, int k){
        Arrays.sort(points);
        int len = points.length;
        ArrayList<Double[]> centers;
        int actual_len;
        double[] dists = distances(points);
        centers = new ArrayList<>();
        double mean_dist = mean(dists);
        int first = 0;
        Comparator<Double[]> comp = new Comparator<Double[]>() {
            @Override
            public int compare(Double[] o1, Double[] o2) {
                return Double.compare(o1[1], o2[1]);
            }
        };
        for (int i = 1; i < len; i++) {
            if (points[i] - points[i - 1] >= mean_dist) {
                Double[] entity = new Double[2];
                entity[0] = (points[first] + points[i - 1]) / 2.;
                entity[1] = (double)(i - first);
                centers.add(entity);
                first = i;
            }
        }
        Double[] entity = new Double[2];
        entity[0] = (points[first] + points[len - 1]) / 2.;
        entity[1] = (double)((len - 1) - first);
        centers.add(entity);
        Collections.sort(centers, comp);
        actual_len = centers.size();
        len = min(k, actual_len);
        points = new double[len];
        for(int i=0;i<k;i++){
            points[len-1 - i] = centers.get(actual_len - 1 - i)[0];
        }
        return points;
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
            min_get = exp( 255 - get);
            max_get = exp( get);
            min_weights += min_get;
            max_weights += max_get;
            min += get*min_get;
            max += get*max_get;
        }
        return new int[] {(int)(sum / ((height * width) / interval)),(int)(min / min_weights), (int)(max / max_weights)};
    }
}
