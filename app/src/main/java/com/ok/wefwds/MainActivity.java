package com.ok.wefwds;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.provider.MediaStore.*;


public class MainActivity extends Activity {

    static Camera cam = new Camera();

    static Uri imageUri = Uri.parse("");

    static final int REQUEST_IMAGE_CAPTURE = 1;

    // Used to load the 'native-lib' library on application startup.
    static {
        OpenCVLoader.initDebug();
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = cam.getIntent();
        try {
            imageUri = Uri.fromFile(Image.createImageFile(this.getApplicationContext()));
        }
        catch(IOException e){
            return;
        }

        takePictureIntent.putExtra(EXTRA_OUTPUT, imageUri);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Image im = new Image(imageUri);
            Image im2 = im.clone();
            im.bilateralFilter();
            im.makeGray();
            int x[] = im.averageColor(500);
            boolean inv = x[2] - x[0] < x[0] - x[1];

            im.makeBinary(10, inv);

            Mat l = im.detectLines();
            double[] lines = new double[l.height()];

            for(int i=0;i<l.height();i++){
                lines[i] = l.get(i,0)[1];
            }

            double[] h = Image.clusters(lines, 5);
            im.drawLines(h, new Scalar(128));
            im2.bilateralFilter();
            im2.makeGray();
            im2.blur(15);
            im2.makeBinary(x[0], inv);
            im2.applyErosion(20);
            im2.applyDilation(15);
            List<MatOfPoint> cunt  = Image.filterContours(im2.contourDetector());
            im2.drawContours(cunt, new Scalar(255));
            cunt  = Image.filterContours(im2.contourDetector());

            im.drawContours(cunt, new Scalar(0));
            ImageView imageView = findViewById(R.id.imv);
            imageView.setImageBitmap(im.getBitmap());
        }
    }
}