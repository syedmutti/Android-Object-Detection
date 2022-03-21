package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class SampleImagePredictionActivity extends AppCompatActivity {

    ImageView predictionImage;
    private objectDetectorClass objectDetectorClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_image_prediction);

        predictionImage = findViewById(R.id.prediction_image);

        int imageId = getIntent().getIntExtra("image", 0);

        try{

            objectDetectorClass=new objectDetectorClass(getAssets(),"detect.tflite","labelmap.txt",320);
            Log.d("PredictionActivity","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("PredictionActivity","Getting some error");
            e.printStackTrace();
        }


        if(imageId != 0) {
            predictionImage.setImageResource(imageId);
            predictInfo(imageId);
        }
    }


    void predictInfo(int resource) {

        runOnUiThread(() -> {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),resource);
            Mat selected_image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bitmap, selected_image);
            selected_image=objectDetectorClass.recognizePhoto(selected_image);
            Bitmap bitmap1=null;
            bitmap1=Bitmap.createBitmap(selected_image.cols(), selected_image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(selected_image, bitmap1);
            predictionImage.setImageBitmap(bitmap1);
        });

    }
}