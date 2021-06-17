package com.example.firebasetest;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import io.opencensus.tags.Tag;

import static android.widget.Toast.*;

public class SignUp extends Fragment {

    EditText name,mobile,email,password;
    Button button;
    ProgressBar progressBar;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view=inflater.inflate(R.layout.fragment_sign_up, container, false);
        name=view.findViewById(R.id.name);
        mobile=view.findViewById(R.id.mobile);
        email=view.findViewById(R.id.login_email);
        password=view.findViewById(R.id.password);
        button=view.findViewById(R.id.signup_btn);
        progressBar=view.findViewById(R.id.login_progressBar);
        firebaseAuth=FirebaseAuth.getInstance();
        firestore=FirebaseFirestore.getInstance();

        if(firebaseAuth.getCurrentUser()!=null)
        {
            startActivity(new Intent(getActivity().getApplicationContext(),Home.class));
            getActivity().finish();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(name.getText().toString().isEmpty())
                    name.setError("Name is Required");
                else if(mobile.getText().toString().isEmpty())
                    mobile.setError("Email is Required");
                else if(email.getText().toString().isEmpty())
                    email.setError("Mobile Number is Required");
                else if(password.getText().toString().length()<6)
                    password.setError("Password should be greater then 6 character");
                else
                {
                    progressBar.setVisibility(View.VISIBLE);

                    firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(),password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful())
                            {
                                DocumentReference documentReference=firestore.collection("user").document(firebaseAuth.getUid());
                                Map<String,Object> user=new HashMap<>();
                                user.put("name",name.getText().toString());
                                user.put("email",email.getText().toString());
                                user.put("mobile",mobile.getText().toString());
                                user.put("password",password.getText().toString());
                                documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("TAG","User profile is created for"+firebaseAuth.getUid());
                                    }
                                });

                                Toast.makeText(getActivity().getApplicationContext(), "Account created Successful", LENGTH_LONG).show();
                                startActivity(new Intent(getActivity().getApplicationContext(),Home.class));
                                getActivity().finish();
                            }
                            else
                            {
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(getActivity().getApplicationContext(), "Error : "+task.getException().getMessage()   , LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });

        return view;
    }
}
