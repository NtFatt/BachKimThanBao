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
import androidx.core.content.ContextCompat; // Import for default text color

import java.io.IOException;

public class GalleryActivity extends AppCompatActivity {

    private ImageView ivGallery;
    private TextView tvGalleryColorInfo;
    private WebView webView;
    private Button btnChooseImage;
    private Button btnRefresh;
    private ProgressBar progressBar;
    private View galleryColorDisplayView; // <- Add reference for the color swatch View

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "GalleryActivity"; // For logging

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
        galleryColorDisplayView = findViewById(R.id.gallery_color_display_view); // <- Find the new View

        // Cài đặt WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/index.html");

        // Set default text color for info TextView
        tvGalleryColorInfo.setTextColor(ContextCompat.getColor(this, android.R.color.primary_text_light)); // Or your preferred default

        // Kiểm tra xem có URI được truyền từ Intent hay không
        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            loadImageFromUri(imageUri);
        }

        // Cài đặt click listener cho các button
        btnChooseImage.setOnClickListener(v -> openImagePicker());
        btnRefresh.setOnClickListener(v -> refreshImage()); // Consider if refresh is needed

        // Cài đặt touch listener cho ImageView
        ivGallery.setOnTouchListener((v, event) -> {
            // Only process if an image is actually loaded
            if (ivGallery.getDrawable() == null || !(ivGallery.getDrawable() instanceof BitmapDrawable)) {
                return false; // No image or not a BitmapDrawable, ignore touch
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Bitmap bitmap = ((BitmapDrawable) ivGallery.getDrawable()).getBitmap();
                if (bitmap != null) {
                    try {
                        int pixelColor = getPixelColorFromImageView(bitmap, ivGallery, event.getX(), event.getY());
                        String hex = String.format("#%02X%02X%02X",
                                Color.red(pixelColor),
                                Color.green(pixelColor),
                                Color.blue(pixelColor));
                        Log.d(TAG, "Touched color hex: " + hex); // Log the picked color
                        updateColorInfoWithChroma(hex);
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting pixel color: ", e);
                        Toast.makeText(this, "Không thể lấy màu pixel", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            // Return false to allow other touch events if needed, true consumes the event
            return true; // Consume the DOWN event
        });
    }

    // Hàm tải ảnh từ URI
    private void loadImageFromUri(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        ivGallery.setImageDrawable(null); // Clear previous image
        tvGalleryColorInfo.setText("Đang tải ảnh..."); // Update status
        galleryColorDisplayView.setBackgroundColor(Color.TRANSPARENT); // Reset color view

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ivGallery.setImageBitmap(bitmap); // This uses the scaleType set in XML (fitCenter)
            tvGalleryColorInfo.setText("Chạm vào ảnh để chọn màu"); // Reset text after loading
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from URI: " + imageUri, e);
            Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            tvGalleryColorInfo.setText("Lỗi tải ảnh");
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    // Hàm mở Image Picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Optional: Add type filter if needed
        // intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng thư viện ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm làm mới ảnh (refreshes the display, doesn't reload)
    private void refreshImage() {
        Drawable drawable = ivGallery.getDrawable();
        if (drawable != null) {
            // Invalidate the view to force redraw, might help if display glitches
            ivGallery.invalidate();
            Toast.makeText(this, "Đã làm mới hiển thị ảnh", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không có ảnh để làm mới", Toast.LENGTH_SHORT).show();
        }
        // Consider removing this button if its function isn't clear or needed.
        // Reloading the same image would be done via loadImageFromUri if you kept the URI.
    }


    // Hàm xử lý kết quả chọn ảnh
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                Log.d(TAG, "Image selected: " + imageUri.toString());
                loadImageFromUri(imageUri);
            } else {
                Log.w(TAG, "Image picker returned null URI");
                Toast.makeText(this, "Không có ảnh được chọn.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Image picker cancelled or failed. ResultCode: " + resultCode);
        }
    }

    // Hàm lấy màu pixel từ ImageView (Seems correct, handles scaling)
    private int getPixelColorFromImageView(Bitmap bitmap, ImageView imageView, float touchX, float touchY) {
        if (bitmap == null || imageView.getDrawable() == null) {
            Log.w(TAG, "getPixelColorFromImageView: Bitmap or Drawable is null.");
            return Color.TRANSPARENT; // Return transparent if no image
        }

        // Get the intrinsic dimensions of the bitmap
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Get the dimensions of the ImageView
        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();

        // Get the matrix used to draw the bitmap inside the ImageView
        android.graphics.Matrix matrix = imageView.getImageMatrix();

        // Create an array to hold the matrix values
        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);

        // Extract scale and translation values from the matrix
        final float scaleX = matrixValues[android.graphics.Matrix.MSCALE_X];
        final float scaleY = matrixValues[android.graphics.Matrix.MSCALE_Y];
        final float transX = matrixValues[android.graphics.Matrix.MTRANS_X];
        final float transY = matrixValues[android.graphics.Matrix.MTRANS_Y];

        // Map the touch coordinates (view coordinates) back to the bitmap coordinates
        // Invert the matrix transformation: first subtract translation, then divide by scale
        final float bitmapX = (touchX - transX) / scaleX;
        final float bitmapY = (touchY - transY) / scaleY;

        // Clamp coordinates to be within the bitmap bounds
        int xPixel = Math.max(0, Math.min((int) bitmapX, bitmapWidth - 1));
        int yPixel = Math.max(0, Math.min((int) bitmapY, bitmapHeight - 1));

        Log.d(TAG, String.format("Touch: (%.1f, %.1f) -> Bitmap: (%d, %d) [ScaleX: %.2f, ScaleY: %.2f]",
                touchX, touchY, xPixel, yPixel, scaleX, scaleY));

        // Get the pixel color
        return bitmap.getPixel(xPixel, yPixel);
    }


    // Hàm cập nhật thông tin màu sắc với Chroma
    private void updateColorInfoWithChroma(String hexColor) {
        if (hexColor == null || !hexColor.matches("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$")) {
            Log.e(TAG, "Invalid hexColor format passed to updateColorInfoWithChroma: " + hexColor);
            return;
        }
        Log.d(TAG, "Updating UI for color: " + hexColor);

        // Update the color swatch view's background
        try {
            galleryColorDisplayView.setBackgroundColor(Color.parseColor(hexColor));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to parse hex color for background: " + hexColor, e);
            galleryColorDisplayView.setBackgroundColor(Color.LTGRAY); // Set error color
        }

        // --- Call JavaScript to get color name ---
        String script = "processColor('" + hexColor + "');";
        webView.evaluateJavascript(script, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                // value is the result from JS, often quoted JSON like "\"{\\\"name\\\":\\\"Red\\\", ...}\""
                Log.d(TAG, "JavaScript result: " + value);
                if (value != null && !value.equals("null") && value.length() > 2) {
                    // Basic cleanup: Remove surrounding quotes and unescape internal quotes
                    String cleanedValue = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                    Log.d(TAG, "Cleaned JS result: " + cleanedValue);

                    String colorName = extractJsonValue(cleanedValue, "name"); // Use cleaned value
                    String info = "Màu: " + hexColor + "\nTên màu: " + (colorName.isEmpty() ? "Không rõ" : colorName);
                    tvGalleryColorInfo.setText(info);

                    // *** REMOVED ***: Don't set text color here anymore
                    // try {
                    //    tvGalleryColorInfo.setTextColor(Color.parseColor(hexColor));
                    // } catch (Exception e) {
                    //    Log.e(TAG, "Failed to parse hex color for text", e);
                    // }

                } else {
                    Log.w(TAG, "JavaScript returned null or invalid value: " + value);
                    // Update text view even if JS fails
                    String info = "Màu: " + hexColor + "\nTên màu: Không rõ";
                    tvGalleryColorInfo.setText(info);
                }
            }
        });
    }

    // Hàm trích xuất giá trị từ JSON (Basic parsing, prone to errors if JSON is complex)
    private String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) {
            return "";
        }

        // Look for "key":"value" pattern
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            // Try looking for "key":value (numeric or boolean - less likely for 'name')
            searchKey = "\"" + key + "\":";
            start = json.indexOf(searchKey);
            if (start == -1) return ""; // Key not found at all

            start += searchKey.length();
            // Find the end - comma or closing brace
            int endComma = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);

            int end = -1;
            if (endComma != -1 && endBrace != -1) {
                end = Math.min(endComma, endBrace);
            } else if (endComma != -1) {
                end = endComma;
            } else {
                end = endBrace;
            }

            if (end == -1) return ""; // No proper end found
            String value = json.substring(start, end).trim();
            // Remove quotes if they exist around the value (e.g., for numbers passed as strings)
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;

        } else {
            // Handle "key":"value" case
            start += searchKey.length();
            int end = json.indexOf("\"", start); // Find the closing quote for the value
            if (end == -1) {
                return ""; // Malformed JSON string value
            }
            // Unescape characters like \\" inside the value if necessary (simple case)
            return json.substring(start, end).replace("\\\\", "\\").replace("\\\"", "\"");
        }
    }
}