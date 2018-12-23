package com.ok.wefwds;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.util.List;

import static android.provider.MediaStore.*;


public class MainActivity extends Activity {

    static Camera cam = new Camera();

    static Uri imageUri = Uri.parse("");

    static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int PICK_IMAGE = 1;


    // Used to load the 'native-lib' library on application startup.
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

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    private void dispatchChoosePictureIntent() {
        Intent chooseGalleryIntent = new Intent();
        chooseGalleryIntent.setType("image/*");
        chooseGalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            imageUri = Uri.fromFile(Image.createImageFile(this.getApplicationContext()));
        } catch (IOException e) {
            return;
        }
        chooseGalleryIntent.putExtra(EXTRA_OUTPUT, imageUri);

        if (chooseGalleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooseGalleryIntent, PICK_IMAGE);
        }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == PICK_IMAGE ) && resultCode == RESULT_OK) {
            Image im = new Image(imageUri);
            im.bilateralFilter();
            im.makeGray();
            int x[] = im.averageColor(500);
            boolean inv = x[2] - x[0] < x[0] - x[1];

            im.makeBinary(80, inv);
            im.applyErosion(5);
            im.applyDilation(5);
            Mat l = im.detectLines();
            double[] lines = new double[l.height()];

            for (int i = 0; i < l.height(); i++) {
                lines[i] = l.get(i, 0)[1];
            }

            double[] h = Image.clusters(lines, 5);
            //im.drawLines(h, new Scalar(128));
            im.applyDilation(20);
            im.applyErosion(40);
            List<MatOfPoint> cunt = Image.filterContours(im.contourDetector(), 3000, 0.5);
            im.drawContours(cunt, new Scalar(255));
            cunt = Image.filterContours(im.contourDetector(), 3000, 0.5);
            Piece piece = new Piece(Note.batchOfNotes(Image.getContoursCenters(cunt)), new Staff(h));
            piece.playNotes();
            im.drawContours(cunt, new Scalar(125));
            im.drawLines(h, new Scalar(125));
            ImageView imageView = findViewById(R.id.imv);
            imageView.setImageBitmap(im.getBitmap());
        }
    }
}