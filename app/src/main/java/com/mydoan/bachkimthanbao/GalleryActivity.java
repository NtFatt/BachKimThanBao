package com.mydoan.bachkimthanbao;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

public class GalleryActivity extends AppCompatActivity {

    private ImageView ivGallery;
    private TextView tvGalleryColorInfo;
    private WebView webView; // WebView để chạy index.html (chứa chroma.js & ntc.js)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        ivGallery = findViewById(R.id.iv_gallery);
        tvGalleryColorInfo = findViewById(R.id.tv_gallery_color_info);
        webView = findViewById(R.id.chroma_webview);

        // Cấu hình WebView cho JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Load file index.html chứa chroma.js và ntc.js từ thư mục assets
        webView.loadUrl("file:///android_asset/index.html");

        // Lấy URI ảnh từ Intent
        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivGallery.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show();
            }
        }

        // Thiết lập touch listener để nhận diện màu theo vị trí chạm
        ivGallery.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Bitmap bitmap = ((BitmapDrawable) ivGallery.getDrawable()).getBitmap();
                    if (bitmap != null) {
                        int pixelColor = getPixelColorFromImageView(bitmap, ivGallery, event.getX(), event.getY());
                        // Convert pixelColor -> mã hex
                        String hex = String.format("#%02X%02X%02X",
                                Color.red(pixelColor),
                                Color.green(pixelColor),
                                Color.blue(pixelColor));
                        // Gọi hàm xử lý màu qua WebView (chroma.js + ntc.js)
                        updateColorInfoWithChroma(hex);
                    }
                }
                return true;
            }
        });
    }

    // Phương thức chuyển đổi tọa độ chạm trên ImageView sang tọa độ trong Bitmap
    private int getPixelColorFromImageView(Bitmap bitmap, ImageView imageView, float x, float y) {
        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Tính tỷ lệ giữa Bitmap và ImageView
        float scaleX = (float) bitmapWidth / imageViewWidth;
        float scaleY = (float) bitmapHeight / imageViewHeight;

        int xBitmap = (int) (x * scaleX);
        int yBitmap = (int) (y * scaleY);

        // Giới hạn toạ độ để không bị vượt quá kích thước bitmap
        xBitmap = Math.max(0, Math.min(xBitmap, bitmapWidth - 1));
        yBitmap = Math.max(0, Math.min(yBitmap, bitmapHeight - 1));

        return bitmap.getPixel(xBitmap, yBitmap);
    }

    // Gọi chroma.js và ntc.js để lấy tên màu
    private void updateColorInfoWithChroma(String hexColor) {
        String script = "processColor('" + hexColor + "');";
        webView.evaluateJavascript(script, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                // value trả về dạng JSON string, ví dụ: {"darkColor":"#a1c550","name":"Red"}
                // Android WebView sẽ bọc chuỗi JSON trong dấu nháy kép, nên ta cắt bỏ
                if (value != null && value.length() > 2) {
                    String json = value.substring(1, value.length() - 1); // loại bỏ dấu " ở đầu/cuối
                    String darkColor = extractJsonValue(json, "darkColor");
                    String colorName = extractJsonValue(json, "name");

                    // Cập nhật TextView
                    // Màu: #XXXXXX
                    // Tên màu: TênMàu
                    String info = "Màu: " + hexColor + "\nTên màu: " + colorName;
                    tvGalleryColorInfo.setText(info);

                    // Thay đổi màu chữ nếu muốn
                    try {
                        tvGalleryColorInfo.setTextColor(Color.parseColor(hexColor));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Hàm đơn giản để trích xuất giá trị từ JSON string (không dùng thư viện JSON)
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
