package com.mydoan.bachkimthanbao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mydoan.bachkimthanbao.databinding.ActivityUserProfileBinding;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            binding.tvEmail.setText(user.getEmail());
            DocumentReference userRef = db.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    binding.tvName.setText(documentSnapshot.getString("fullName"));
                    binding.tvDob.setText(documentSnapshot.getString("dob"));
                    binding.tvGender.setText(documentSnapshot.getString("gender"));
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).into(binding.ivAvatar);
                    } else {
                        binding.ivAvatar.setImageResource(R.drawable.default_avatar);
                    }
                } else {
                    Toast.makeText(UserProfileActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(UserProfileActivity.this, "Lỗi kết nối đến Firebase", Toast.LENGTH_SHORT).show();
            });
        }

        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, EditProfileActivity.class);
            // Truyền dữ liệu để chỉnh sửa thông tin vào Activity tiếp theo
            intent.putExtra("fullName", binding.tvName.getText().toString());
            intent.putExtra("email", binding.tvEmail.getText().toString());
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(UserProfileActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(UserProfileActivity.this, LoginActivity.class));
            finish();
        });
    }
}