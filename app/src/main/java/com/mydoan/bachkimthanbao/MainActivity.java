package com.mydoan.bachkimthanbao;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.utilities.Hct;

import com.google.common.util.concurrent.ListenableFuture;

import android.util.Size;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY_IMAGE = 999;

    private PreviewView previewView;
    private ImageView ivChosenImage;
    private FrameLayout cameraFrame;
    private View centerOverlay; // Overlay (crosshair) ở giữa
    private TextView tvColorInfo;
    private Button btnCapture, btnLibrary;
    private WebView webView; // WebView dùng để gọi chroma.js & ntc.js

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    // Flag xác định chế độ hiển thị: camera hay ảnh từ thư viện
    private boolean isLibraryMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ UI từ layout
        previewView = findViewById(R.id.preview_view);
        ivChosenImage = findViewById(R.id.iv_chosen_image);
        cameraFrame = findViewById(R.id.camera_frame);
        centerOverlay = findViewById(R.id.center_overlay);
        tvColorInfo = findViewById(R.id.tv_color_info);
        btnCapture = findViewById(R.id.btn_capture);
        btnLibrary = findViewById(R.id.btn_library);
        webView = findViewById(R.id.chroma_webview);

        // Cấu hình WebView cho JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Load file index.html chứa chroma.js & ntc.js từ assets
        webView.loadUrl("file:///android_asset/index.html");

        // Kiểm tra quyền Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraAndAnalysis();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        // Button chụp ảnh (capture) khi đang ở chế độ camera
        btnCapture.setOnClickListener(v -> {
            if (!isLibraryMode) {
                takePhoto();
            } else {
                Toast.makeText(this, "Chế độ thư viện: Hãy chạm vào ảnh để lấy màu", Toast.LENGTH_SHORT).show();
            }
        });

        // Button chọn ảnh từ thư viện
        btnLibrary.setOnClickListener(v -> pickImageFromGallery());

        // Thiết lập sự kiện chạm vào ảnh từ thư viện để lấy màu tại điểm chạm
        initTouchListenerForImage();
    }

    // Khởi tạo CameraX với Preview, ImageCapture, và ImageAnalysis
    private void startCameraAndAnalysis() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e("CameraX", "Lỗi khởi tạo camera: " + e.getMessage());
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                // Lấy pixel tại tâm của bitmap
                int centerX = bitmap.getWidth() / 2;
                int centerY = bitmap.getHeight() / 2;
                int centerPixel = bitmap.getPixel(centerX, centerY);
                String hex = String.format("#%02X%02X%02X", Color.red(centerPixel),
                        Color.green(centerPixel), Color.blue(centerPixel));
                runOnUiThread(() -> updateColorInfoWithChroma(hex));
            }
            imageProxy.close();
        });

        cameraProvider.unbindAll();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

        // Khi dùng camera, hiển thị preview, ẩn ảnh thư viện
        previewView.setVisibility(View.VISIBLE);
        ivChosenImage.setVisibility(View.GONE);
        isLibraryMode = false;
    }

    // Phương thức chụp ảnh (capture) từ CameraX
    private void takePhoto() {
        if (imageCapture == null) return;
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        Bitmap bitmap = imageProxyToBitmap(imageProxy);
                        imageProxy.close();
                        if (bitmap != null) {
                            ivChosenImage.setImageBitmap(bitmap);
                            ivChosenImage.setVisibility(View.VISIBLE);
                            previewView.setVisibility(View.GONE);
                            isLibraryMode = true;
                            analyzeBitmap(bitmap);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("CameraX", "Error: " + exception.getMessage());
                    }
                });
    }

    // Chuyển ImageProxy thành Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Mở gallery để chọn ảnh
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                isLibraryMode = true;
                ivChosenImage.setImageBitmap(bitmap);
                ivChosenImage.setVisibility(View.VISIBLE);
                previewView.setVisibility(View.GONE);
                analyzeBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Sự kiện chạm vào ảnh từ thư viện để lấy màu tại điểm chạm
    private void initTouchListenerForImage() {
        ivChosenImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Bitmap bitmap = ((BitmapDrawable) ivChosenImage.getDrawable()).getBitmap();
                if (bitmap != null) {
                    int pixelColor = getPixelColorFromImageView(bitmap, ivChosenImage, event.getX(), event.getY());
                    String hex = String.format("#%02X%02X%02X",
                            Color.red(pixelColor), Color.green(pixelColor), Color.blue(pixelColor));
                    updateColorInfoWithChroma(hex);
                }
            }
            return true;
        });
    }

    // Chuyển đổi tọa độ từ ImageView sang tọa độ thực của Bitmap
    private int getPixelColorFromImageView(Bitmap bitmap, ImageView imageView, float viewX, float viewY) {
        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        float scaleX = (float) bitmapWidth / imageViewWidth;
        float scaleY = (float) bitmapHeight / imageViewHeight;

        int xInBitmap = (int)(viewX * scaleX);
        int yInBitmap = (int)(viewY * scaleY);

        xInBitmap = Math.max(0, Math.min(xInBitmap, bitmapWidth - 1));
        yInBitmap = Math.max(0, Math.min(yInBitmap, bitmapHeight - 1));

        return bitmap.getPixel(xInBitmap, yInBitmap);
    }

    // Phân tích Bitmap (ảnh từ thư viện hoặc ảnh chụp) và cập nhật màu trung bình
    private void analyzeBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        int colorAvg = getAverageColor(bitmap);
        String hex = String.format("#%02X%02X%02X",
                Color.red(colorAvg), Color.green(colorAvg), Color.blue(colorAvg));
        updateColorInfoWithChroma(hex);
    }

    // Tính màu trung bình của Bitmap (lấy mẫu theo bước 10 pixel)
    private int getAverageColor(Bitmap bitmap) {
        long sumR = 0, sumG = 0, sumB = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int count = 0;
        int step = 10;
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = bitmap.getPixel(x, y);
                sumR += Color.red(pixel);
                sumG += Color.green(pixel);
                sumB += Color.blue(pixel);
                count++;
            }
        }
        int avgR = (int)(sumR / count);
        int avgG = (int)(sumG / count);
        int avgB = (int)(sumB / count);
        return Color.rgb(avgR, avgG, avgB);
    }

    // Cập nhật thông tin màu sử dụng hàm JavaScript (chroma.js & ntc.js)
    private void updateColorInfoWithChroma(String hexColor) {
        String script = "processColor('" + hexColor + "');";
        webView.evaluateJavascript(script, value -> {
            if (value != null && value.length() > 2) {
                try {
                    String json = value;
                    if (json.startsWith("\"") && json.endsWith("\"")) {
                        json = json.substring(1, json.length() - 1);
                    }
                    Log.d("UpdateColorInfo", "JSON trả về: " + json);

                    String colorName = extractJsonValue(json, "name");
                    String darkColor = extractJsonValue(json, "darkColor");
                    if (darkColor.equals("Không xác định")) {
                        darkColor = hexColor;
                    }

                    String info = "Mã màu: " + hexColor + "\nTên màu: " + colorName;
                    tvColorInfo.setText(info);

                    try {
                        int parsedColor = Color.parseColor(darkColor);
                        tvColorInfo.setTextColor(parsedColor);
                        // Đặt nền bán trong suốt dựa trên darkColor
                        tvColorInfo.setBackgroundColor(adjustAlpha(parsedColor, 0.2f));
                    } catch (Exception e) {
                        Log.e("UpdateColorInfo", "Lỗi parse darkColor: " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.e("UpdateColorInfo", "Lỗi xử lý JSON: " + e.getMessage());
                    tvColorInfo.setText("Mã màu: " + hexColor + "\nTên màu: Không xác định");
                }
            } else {
                tvColorInfo.setText("Mã màu: " + hexColor + "\nTên màu: Không xác định");
            }
        });
    }

    // Hàm trích xuất giá trị từ JSON trả về từ JavaScript sử dụng regex
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"(.*?)\"";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            Log.d("ExtractJson", "Giá trị của " + key + " là: " + value);
            return value;
        } else {
            Log.w("ExtractJson", "Không tìm thấy khóa: " + key);
            return "Không xác định";
        }
    }

    // Hàm điều chỉnh alpha của màu (tạo nền bán trong suốt)
    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraAndAnalysis();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để sử dụng", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
//1234567890