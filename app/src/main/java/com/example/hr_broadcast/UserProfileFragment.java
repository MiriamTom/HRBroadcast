package com.example.hr_broadcast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileFragment extends Fragment {

    private TextView textViewUsername, textViewEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fetchUserData();

        return view;
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Update UI with user data
                                String username = document.getString("username");
                                String email = document.getString("email");

                                textViewUsername.setText(username != null ? username : "N/A");
                                textViewEmail.setText(email != null ? email : "N/A");
                            } else {
                                textViewUsername.setText("User data not found");
                                textViewEmail.setText("");
                            }
                        } else {
                            textViewUsername.setText("Failed to fetch user data");
                            textViewEmail.setText("");
                        }
                    });
        } else {
            textViewUsername.setText("User not logged in");
            textViewEmail.setText("");
        }
    }
}