package com.ok.wefwds;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class Layout extends Activity {
    Context context;

    public Layout(Context context) {
        this.context = context;
    }

    Button playButton, cancelButton;
    ImageView cameraButton, galleryButton, analyzedBitMap;

    public void showBitMap(Bitmap im) {
        analyzedBitMap.setVisibility(View.VISIBLE);
        analyzedBitMap.setImageBitmap(im);
    }

    public void hideBitMap() {
        analyzedBitMap.setImageResource(0);
    }

    public void initializeButtons() {
        cameraButton = ((Activity) context).findViewById(R.id.camera);
        galleryButton = ((Activity) context).findViewById(R.id.gallery);
        analyzedBitMap = ((Activity) context).findViewById(R.id.bitMap);
        playButton = ((Activity) context).findViewById(R.id.play);
        cancelButton = ((Activity) context).findViewById(R.id.cancel);
        this.setPlayButtonStartOption();
        this.setCancelButtonStartOption();
    }

    public void toggleButtons() {
        if (playButton.getVisibility() == View.VISIBLE) {
            playButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            cameraButton.setVisibility(View.VISIBLE);
            galleryButton.setVisibility(View.VISIBLE);
        } else {
            playButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            cameraButton.setVisibility(View.GONE);
            galleryButton.setVisibility(View.GONE);
        }
    }

    private void setPlayButtonStartOption() {
        playButton.setVisibility(View.GONE);
        playButton.setText("Play music");
    }

    private void setCancelButtonStartOption() {
        cancelButton.setVisibility(View.GONE);
        cancelButton.setText("Cancel");
    }
}
