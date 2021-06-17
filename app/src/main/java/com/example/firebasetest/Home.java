package com.example.firebasetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firestore.admin.v1beta1.Progress;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nullable;

public class Home extends AppCompatActivity {
    TextView name,email,mobile,progressText;
    ProgressBar progressBar;
    ImageView recordingBtn,callBtn;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    Boolean isRecording;
    MediaRecorder mediaRecorder;
    String recordFileName;
    StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        name=findViewById(R.id.home_name);
        email=findViewById(R.id.home_email);
        mobile=findViewById(R.id.home_mobile);
        progressText=findViewById(R.id.progress_text);
        progressBar=findViewById(R.id.progress_bar);
        recordingBtn=findViewById(R.id.mic_btn);
        callBtn=findViewById(R.id.call_btn);
        firebaseAuth=FirebaseAuth.getInstance();
        firestore=FirebaseFirestore.getInstance();
        isRecording=false;

        final DocumentReference documentReference=firestore.collection("user").document(firebaseAuth.getCurrentUser().getUid());
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                name.setText(documentSnapshot.getString("name"));
                email.setText(documentSnapshot.getString("email"));
                mobile.setText(documentSnapshot.getString("mobile"));
            }
        });

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobile="198";
                Intent call = new Intent(Intent.ACTION_CALL);
                call.setData(Uri.parse("tel:"+mobile));
                startActivity(call);
                //Log.i("Mittu","Service Called");
                Intent intent=new Intent(getApplicationContext(),MyService.class);
                Bundle extra =new Bundle();
                extra.putString("mobile",mobile);
                intent.putExtras(extra);
                startService(intent);
            }
        });

        recordingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording)
                {
                    stopRecording();
                    recordingBtn.setImageDrawable(getResources().getDrawable(R.drawable.mic_off,null));
                    isRecording=false;
                }
                else
                {
                    if(checkPermissions())
                    {
                        startRecording();
                        recordingBtn.setImageDrawable(getResources().getDrawable(R.drawable.mic_on,null));
                        isRecording=true;
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_items,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.logout_btn:
            {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                finish();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private boolean checkPermissions() {
            if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO )== PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else
        {
            ActivityCompat.requestPermissions(Home.this,new String[]{Manifest.permission.RECORD_AUDIO},21);
            return false;
        }
    }
    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder=null;
        sendRecordingToFirebase();
    }
    private void startRecording() {
        String filePath=getExternalFilesDir("/").getAbsolutePath();
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        Date date=new Date();
        recordFileName="Recording "+ dateFormat.format(date)+".3gp";

        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filePath+"/"+recordFileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
        progressText.setText("Recording...");
        progressText.setVisibility(View.VISIBLE);
    }
    private void sendRecordingToFirebase() {
        mStorageRef = FirebaseStorage.getInstance().getReference();
        String filePath=getExternalFilesDir("/").getAbsolutePath()+"/"+recordFileName;

        Uri uri=Uri.fromFile(new File(filePath));
        StorageReference riversRef = mStorageRef.child("recordings/"+firebaseAuth.getUid()+"/"+recordFileName);
        riversRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressText.setText("Uploaded Succsessfull");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
                progressText.setVisibility(View.INVISIBLE);
                Toast.makeText(Home.this, "Something Wrong", Toast.LENGTH_LONG).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress=(100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                progressText.setText("Uploading...");
                progressBar.setProgress((int) progress);
            }
        });
    }
}
