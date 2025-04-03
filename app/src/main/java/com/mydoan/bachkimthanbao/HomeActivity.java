package com.mydoan.bachkimthanbao;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity {

    private CardView cardCapture, cardPalette, cardGallery, cardSavedColors, cardShare, cardSubscription;
    private static final int REQUEST_GALLERY_IMAGE = 1001; // Mã yêu cầu riêng cho việc chọn ảnh từ Gallery

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        // Tìm các CardView từ layout
        cardCapture = findViewById(R.id.card_capture);
        cardPalette = findViewById(R.id.card_palette);
        cardGallery = findViewById(R.id.card_gallery);
        cardSavedColors = findViewById(R.id.card_saved_colors);
        cardShare = findViewById(R.id.card_share);
        cardSubscription = findViewById(R.id.card_subscription);

        // Áp dụng hiệu ứng hover cho các CardView
        applyHoverAnimation(cardCapture);
        applyHoverAnimation(cardPalette);
        applyHoverAnimation(cardGallery);
        applyHoverAnimation(cardSavedColors);
        applyHoverAnimation(cardShare);
        applyHoverAnimation(cardSubscription);

        // Card Capture: Mở MainActivity (Camera)
        cardCapture.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        // Card Palette: (Chưa cập nhật)
        cardPalette.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Palette đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        // Card Gallery: Mở Image Picker
        cardGallery.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE);
        });

        // Card Saved Colors: (Chưa cập nhật)
        cardSavedColors.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Saved Colors đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        // Card Share: Chia sẻ ứng dụng
        cardShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareMsg = "Try out this Color Detector & Picker app!";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Card Subscription: (Chưa cập nhật)
        cardSubscription.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Subscription đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        // Tìm ImageView avatar người dùng và xử lý sự kiện click
        ImageView ivUserAvatar = findViewById(R.id.iv_user_avatar);
        ivUserAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    // Phương thức tiện ích để áp dụng hiệu ứng hover cho view
    private void applyHoverAnimation(View view) {
        view.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        break;
                }
                return false;
            }
        });
    }

    // Xử lý kết quả chọn ảnh từ Gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Intent intent = new Intent(this, GalleryActivity.class);
                    intent.setData(imageUri);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không có ảnh được chọn.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Không có ảnh được chọn.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}