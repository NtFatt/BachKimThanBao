package com.mydoan.bachkimthanbao;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class HomeActivity extends AppCompatActivity {

    private CardView cardCapture, cardPalette, cardGallery, cardSavedColors, cardShare, cardSubscription;
    private static final int REQUEST_GALLERY_IMAGE = 1001; // mã yêu cầu riêng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        cardCapture = findViewById(R.id.card_capture);
        cardPalette = findViewById(R.id.card_palette);
        cardGallery = findViewById(R.id.card_gallery);
        cardSavedColors = findViewById(R.id.card_saved_colors);
        cardShare = findViewById(R.id.card_share);
        cardSubscription = findViewById(R.id.card_subscription);

        // Card Capture: Mở MainActivity (Camera)
        cardCapture.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        // Card Palette: (Chưa cập nhật)
        cardPalette.setOnClickListener(v -> {
            // Intent intent = new Intent(this, MyPaletteActivity.class);
            // startActivity(intent);
        });

        // Card Gallery: Mở Image Picker
        cardGallery.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE);
        });

        // Card Saved Colors: (Chưa cập nhật)
        cardSavedColors.setOnClickListener(v -> {
            // Intent intent = new Intent(this, SavedColorsActivity.class);
            // startActivity(intent);
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
            // Intent intent = new Intent(this, SubscriptionActivity.class);
            // startActivity(intent);
        });
    }

    // Xử lý kết quả chọn ảnh từ Gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            // Chuyển sang GalleryActivity, truyền URI của ảnh
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.setData(imageUri);
            startActivity(intent);
        }
    }
}
