package com.mydoan.bachkimthanbao;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mydoan.bachkimthanbao.databinding.ActivityEditProfileBinding;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ UserProfileActivity
        String fullName = getIntent().getStringExtra("fullName");
        String email = getIntent().getStringExtra("email");

        binding.etName.setText(fullName);
        binding.etEmail.setText(email); // Email không thể chỉnh sửa

        binding.btnSave.setOnClickListener(v -> {
            String updatedName = binding.etName.getText().toString();
            if (!updatedName.isEmpty()) {
                // Cập nhật thông tin người dùng trong Firebase
                String userId = mAuth.getCurrentUser().getUid();
                db.collection("users").document(userId)
                        .update("fullName", updatedName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditProfileActivity.this, "Thông tin đã được cập nhật", Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại màn hình trước
                        })
                        .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
