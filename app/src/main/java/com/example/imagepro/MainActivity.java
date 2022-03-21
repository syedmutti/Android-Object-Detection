package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    private AppUpdateManager mAppUpdateManager;
    private static  final int RC_APP_UPDATE = 100;
    private Button camera_button;
    private Button storage_prediction, btnDemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        // select device and run
        // we successfully loaded model
        // before next tutorial
        // as we are going to predict in Camera Activity
        // Next tutorial will be about predicting using Interpreter

        camera_button=findViewById(R.id.camera_button);
        btnDemo = findViewById(R.id.btnDemo);

        btnDemo.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this,SampleImagesActivity.class));
        });
        camera_button.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,CameraActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        storage_prediction=findViewById(R.id.storage_prediction);
        storage_prediction.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,StoragePredictionActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(result -> {

            if(result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
            {

                try {
                    mAppUpdateManager.startUpdateFlowForResult(result, AppUpdateType.FLEXIBLE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
        mAppUpdateManager.registerListener(installStateUpdatedListener);
    }

    private InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(@NonNull InstallState State) {
            if(State.installStatus() == InstallStatus.DOWNLOADED){
                showCompletedUpdate();
            }
        }


        private void showCompletedUpdate() {

            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "New App is ready! ",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("Install", view -> mAppUpdateManager.completeUpdate());
            snackbar.show();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == RC_APP_UPDATE && resultCode != RESULT_OK) {
            Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        if(mAppUpdateManager!=null) mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void Open_Website(View view) {

        startActivity(new Intent(MainActivity.this,Web_Inside_App_Activity.class));

    }

    public void OpenSocialMEdia(View view) {

        final Dialog builder1 = new Dialog(MainActivity.this);
        builder1.setCancelable(false);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        final View ReferSubmitPageView = inflater.inflate(R.layout.view_soicial_link,null );
        builder1.setContentView(ReferSubmitPageView);
        builder1.show();
        builder1.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        CardView Youtube_Button = ReferSubmitPageView.findViewById(R.id.Youtube_Button);
        CardView Linkdin_Button = ReferSubmitPageView.findViewById(R.id.Linkdin_Button);
        CardView Twitter_Button = ReferSubmitPageView.findViewById(R.id.Twitter_Button);
        CardView Insta_Button = ReferSubmitPageView.findViewById(R.id.Insta_Button);
        CardView Facebook_Button = ReferSubmitPageView.findViewById(R.id.Facebook_Button);

        Youtube_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://www.youtube.com/"+"xraylabor"));
                    startActivity(intent);
                }
                catch (Exception e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.youtube.com/"+"xraylabor")));
                }

            }
        });

        Linkdin_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.linkedin.com/company/xray-lab")));
            }
        });

        Twitter_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("twitter://user?screen_name=[XRAYLAB3]"));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://twitter.com/#!/[XRAYLAB3]")));
                }

            }
        });

        Insta_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Uri uri = Uri.parse("http://instagram.com/_u/xray_lab1");
                Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

                likeIng.setPackage("com.instagram.android");

                try {
                    startActivity(likeIng);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/xray_lab1")));
                }


            }
        });

        Facebook_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/xraylab.auburnhills.7")));

            }
        });



        TextView Cancle = ReferSubmitPageView.findViewById(R.id.Cancle);
        Cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                builder1.dismiss();
            }
        });



    }


}