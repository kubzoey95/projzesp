package com.ok.wefwds;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;

/**
 * Created by kubzoey on 2018-10-23.
 */

public class Camera {
    private Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    public Camera(){
    }

    public Intent getIntent(){
        return cameraIntent;
    }

    public void setCameraIntent(Intent intent){
        cameraIntent = intent;
    }

    public Bitmap getBitmap(){
        Bundle extras = cameraIntent.getExtras();
        return (Bitmap) extras.get("data");
    }
}
