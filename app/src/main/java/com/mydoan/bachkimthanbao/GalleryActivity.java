package com.mydoan.bachkimthanbao;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class GalleryActivity extends AppCompatActivity {

    private ImageView ivGallery;
    private TextView tvGalleryColorInfo;
    private WebView webView;
    private Button btnChooseImage;
    private Button btnRefresh;
    private ProgressBar progressBar;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Tìm các view từ layout
        ivGallery = findViewById(R.id.iv_gallery);
        tvGalleryColorInfo = findViewById(R.id.tv_gallery_color_info);
        webView = findViewById(R.id.chroma_webview);
        btnChooseImage = findViewById(R.id.btn_choose_image);
        btnRefresh = findViewById(R.id.btn_refresh);
        progressBar = findViewById(R.id.progress_bar);

        // Cài đặt WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/index.html");

        // Kiểm tra xem có URI được truyền từ Intent hay không
        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            loadImageFromUri(imageUri);
        }

        // Cài đặt click listener cho các button
        btnChooseImage.setOnClickListener(v -> openImagePicker());
        btnRefresh.setOnClickListener(v -> refreshImage());

        // Cài đặt touch listener cho ImageView
        ivGallery.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Drawable drawable = ivGallery.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null) {
                        int pixelColor = getPixelColorFromImageView(bitmap, ivGallery, event.getX(), event.getY());
                        String hex = String.format("#%02X%02X%02X",
                                Color.red(pixelColor),
                                Color.green(pixelColor),
                                Color.blue(pixelColor));
                        updateColorInfoWithChroma(hex);
                    }
                }
            }
            return true;
        });
    }

    // Hàm tải ảnh từ URI
    private void loadImageFromUri(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ivGallery.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    // Hàm mở Image Picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Hàm làm mới ảnh
    private void refreshImage() {
        Drawable drawable = ivGallery.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                ivGallery.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "Không có ảnh để làm mới", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không có ảnh để làm mới", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm xử lý kết quả chọn ảnh
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                loadImageFromUri(imageUri);
            } else {
                Toast.makeText(this, "Không có ảnh được chọn.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hàm lấy màu pixel từ ImageView
    private int getPixelColorFromImageView(Bitmap bitmap, ImageView imageView, float x, float y) {
        if (bitmap == null) {
            return 0;
        }

        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        float scaleX = (float) bitmapWidth / imageViewWidth;
        float scaleY = (float) bitmapHeight / imageViewHeight;

        int xBitmap = (int) (x * scaleX);
        int yBitmap = (int) (y * scaleY);

        xBitmap = Math.max(0, Math.min(xBitmap, bitmapWidth - 1));
        yBitmap = Math.max(0, Math.min(yBitmap, bitmapHeight - 1));

        return bitmap.getPixel(xBitmap, yBitmap);
    }

    // Hàm cập nhật thông tin màu sắc với Chroma
    private void updateColorInfoWithChroma(String hexColor) {
        String script = "processColor('" + hexColor + "');";
        webView.evaluateJavascript(script, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (value != null && value.length() > 2) {
                    String json = value.substring(1, value.length() - 1);
                    String darkColor = extractJsonValue(json, "darkColor");
                    String colorName = extractJsonValue(json, "name");

                    String info = "Màu: " + hexColor + "\nTên màu: " + colorName;
                    tvGalleryColorInfo.setText(info);

                    try {
                        tvGalleryColorInfo.setTextColor(Color.parseColor(hexColor));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Hàm trích xuất giá trị từ JSON
    private String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty()) {
            return "";
        }

        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            return "";
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            return "";
        }
        return json.substring(start, end);
    }
}