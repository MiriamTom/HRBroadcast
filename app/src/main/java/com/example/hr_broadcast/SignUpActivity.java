package com.example.hr_broadcast;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextEmail, editTextPassword;
    private Button buttonSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString();
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Všetky polia musia byť vyplnené!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignUpActivity.this, "Registrácia úspešná!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("SignUp", "User registered: " + user.getEmail());

                            // Save additional user data in Firestore
                            saveUserToFirestore(user);
                        }
                    } else {
                        Log.e("SignUp", "Registration failed", task.getException());
                        Toast.makeText(SignUpActivity.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a map for user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("createdAt", System.currentTimeMillis());

        // Store user data in Firestore (you can create a 'users' collection)
        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User data saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving user data", e));
    }

}
