package com.ok.wefwds;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.StrictMode;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.util.List;

import static android.provider.MediaStore.*;

public class MainActivity extends Activity {

    private static final double STAFF_LINE_CONSTANT = 136.375;
    private static final int RESIZE_HEIGHT = 215;
    private static final int RESIZE_WIDTH = 1110;
    private static final int RATIO_HEIGHT = 1840;
    private static final int RATIO_WIDTH = 3264;
    private static final double AVERAGE_COLOR_INTERVAL = 500.;
    private static final int BIAS_CONSTANT = 150;
    private static final double EXPECTED_CIRCULARITY = 0.7;
    private static final int CONTOUR_COLOR = 125;
    static Camera cam = new Camera();
    static Uri imageUri = Uri.parse("");
    static Piece piece = new Piece();
    Layout layout = new Layout(this);
    static Bitmap preprocessedBmp = Bitmap.createBitmap(1,1, Bitmap.Config.RGB_565);

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PICK_IMAGE = 2;

    static {
        OpenCVLoader.initDebug();
        System.loadLibrary("native-lib");
    }

    public void changeLayout(View view) {
        dispatchTakePictureIntent();
    }

    public void choosePhotoFromGallery(View view) {
        dispatchChoosePictureIntent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout.initializeButtons();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    public void playMusic(View view) {
        this.playPiece();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Image im;
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                im = new Image(imageUri);
                analyzeAndShowButtons(im);
            } else if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                try {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    im = new Image(bmp);
                    analyzeAndShowButtons(im);
                } catch (IOException e) {

                }
            }
        }
    }

    private void dispatchChoosePictureIntent() {
        Intent chooseGalleryIntent = new Intent();
        chooseGalleryIntent.setType("image/*");
        chooseGalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(chooseGalleryIntent, "Select Picture"), PICK_IMAGE);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = cam.getIntent();
        try {
            imageUri = Uri.fromFile(Image.createImageFile(this.getApplicationContext()));
        } catch (IOException e) {
            return;
        }

        takePictureIntent.putExtra(EXTRA_OUTPUT, imageUri);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void analyzeAndShowButtons(Image im) {
        preprocessedBmp = im.getBitmap();
        im.bilateralFilter();
        im.makeGray();
        double resize_ratio = Math.sqrt(RESIZE_HEIGHT * RESIZE_WIDTH) / Math.sqrt(im.imageMatrix.height() * im.imageMatrix.width());
        im.scale(resize_ratio);
        double ratio = Math.sqrt(im.imageMatrix.height() * im.imageMatrix.width()) / Math.sqrt(RATIO_HEIGHT * RATIO_WIDTH);
        int x[] = im.averageColor((int) (AVERAGE_COLOR_INTERVAL * ratio));
        boolean inv = x[2] - x[0] < x[0] - x[1];

        Mat l;
        double[] h;
        try {
            l = im.detectLinesCanny(x[1], x[2]);
            h = Helpers.getH(l);
            im.processingPhoto(ratio, inv);
        } catch (Exception e) {
            im.processingPhoto(ratio, inv);
            l = im.detectLines();
            h = Helpers.getH(l);
        }

        Staff staff = new Staff(h);
        double ratio2 = staff.line_interval / STAFF_LINE_CONSTANT;

        im.extract_notes(ratio2, staff.line_interval);

        double bias = Math.pow(BIAS_CONSTANT * ratio2, 2);

        List<MatOfPoint> cunt = Image.filterContours(im.contourDetector(), Math.max(Math.pow(staff.line_interval,2) - bias, 0), EXPECTED_CIRCULARITY);
        piece = new Piece(Note.batchOfNotes(Image.getContoursCenters(cunt)), staff);
        im.drawContours(cunt, new Scalar(CONTOUR_COLOR));
        im.drawLines(h, new Scalar(CONTOUR_COLOR));
        layout.toggleButtons();
        layout.showBitMap(im.getBitmap());
    }

    private void playPiece() {
        piece.playNotes();
    }

    public void cancelMusic(View view) {
        layout.toggleButtons();
        layout.hideBitMap();
    }
}