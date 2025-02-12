package com.example.hr_broadcast;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        mAuth = FirebaseAuth.getInstance();

        Button loginButton = view.findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> loginUser());

        TextView signUpTextView = view.findViewById(R.id.signUpTextView);
        signUpTextView.setOnClickListener(v -> onSignUpClick());

        return view;
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("Login", "User logged in: " + user.getEmail());
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        if (getActivity() != null) {
                            getActivity().finish(); // Finish the parent activity to avoid going back
                        }
                    } else {
                        Log.e("Login", "Authentication failed", task.getException());
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onSignUpClick() {
        startActivity(new Intent(getActivity(), SignUpActivity.class));
    }
}
