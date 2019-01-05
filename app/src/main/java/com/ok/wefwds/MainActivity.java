package com.ok.wefwds;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.StrictMode;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;

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
    static Piece piece = new Piece();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PICK_IMAGE = 2;
    Button playButton;
    ImageView cameraButton, galleryButton;

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
        this.initializeButtons();

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
                analyzeAndPlay(im);
            } else if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                try {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    im = new Image(bmp);
                    analyzeAndPlay(im);
                } catch (IOException e) {
                    return;
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

    private void analyzeAndPlay(Image im) {
        im.bilateralFilter();
        im.makeGray();
        double resize_ratio = Math.sqrt(215 * 1110) / Math.sqrt(im.imageMatrix.height() * im.imageMatrix.width());
        im.scale(resize_ratio);
        double ratio = Math.sqrt(im.imageMatrix.height() * im.imageMatrix.width()) / Math.sqrt(1840 * 3264);
        int x[] = im.averageColor((int) (500. * ratio));
        boolean inv = x[2] - x[0] < x[0] - x[1];

        Mat l;
        double[] h;
        try {
            l = im.detectLinesCanny(x[1], x[2]);
            double[] lines = new double[l.height()];

            for (int i = 0; i < l.height(); i++) {
                lines[i] = l.get(i, 0)[1];
            }

            h = Image.clusters(lines, 5);
            im.makeBinary((int) (80. * ratio), inv);
            im.applyErosion((int) (5. * ratio));
            im.applyDilation((int) (5. * ratio));
        } catch (Exception e) {
            im.makeBinary((int) (80. * ratio), inv);
            im.applyErosion((int) (5. * ratio));
            im.applyDilation((int) (5. * ratio));
            l = im.detectLines();
            double[] lines = new double[l.height()];

            for (int i = 0; i < l.height(); i++) {
                lines[i] = l.get(i, 0)[1];
            }

            h = Image.clusters(lines, 5);
        }

        Staff staff = new Staff(h);
        double ratio2 = staff.line_interval / 136.375;
        im.blur((int) (staff.line_interval));
        im.cleaning(ratio2);
        im.blur((int) (staff.line_interval));

        im.applyDilation((int) (40. * ratio2));
        im.thresh(100, false);
        //im.cleaning(ratio2 / 3);
        im.blur((int) (staff.line_interval));

        im.applyDilation((int) (20. * ratio2));
        im.thresh(80, false);

        im.blur((int) (staff.line_interval));
        //im.applyDilation((int)(40. * ratio2));
        im.thresh(127, false);
        im.blur((int) (staff.line_interval));
        //im.applyDilation((int)(40. * ratio2));
        im.thresh(100, false);
        im.blur((int) (staff.line_interval));
        //im.applyDilation((int)(40. * ratio2));
        im.thresh(100, false);
        im.applyErosion((int) (staff.line_interval / 3.));
        im.blur((int) (staff.line_interval));
        im.thresh(170, false);
        im.blur((int) (staff.line_interval));
        im.thresh(200, false);


        double bias = (150 * ratio2) * (150 * ratio2);

        List<MatOfPoint> cunt = Image.filterContours(im.contourDetector(), Math.max(staff.line_interval * staff.line_interval - bias, 0), staff.line_interval * staff.line_interval + bias, 0.7);
        piece = new Piece(Note.batchOfNotes(Image.getContoursCenters(cunt)), staff);
        im.drawContours(cunt, new Scalar(125));
        im.drawLines(h, new Scalar(125));
        ImageView imageView = findViewById(R.id.imageView3);
        imageView.setImageBitmap(im.getBitmap());

        this.toggleButtons();
    }

    private void playPiece() {
        piece.playNotes();
        this.toggleButtons();
    }

    private void initializeButtons() {
        playButton = findViewById(R.id.play);
        cameraButton = findViewById(R.id.camera);
        galleryButton = findViewById(R.id.gallery);

        playButton.setVisibility(View.GONE);
        playButton.setText("Play music");

    }

    private void toggleButtons() {
        if (playButton.getVisibility() == View.VISIBLE) {
            playButton.setVisibility(View.GONE);
            cameraButton.setVisibility(View.VISIBLE);
            galleryButton.setVisibility(View.VISIBLE);
        } else {
            playButton.setVisibility(View.VISIBLE);
            playButton.setText("Play music");
            cameraButton.setVisibility(View.GONE);
            galleryButton.setVisibility(View.GONE);
        }
    }


}