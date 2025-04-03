package com.mydoan.bachkimthanbao;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button captureButton;
    private TextView colorInfoTextView;
    private WebView chromaWebView;
    private ImageCapture imageCapture;
    private View centerOverlay;
    private View colorDisplayView; // Ô màu sắc

    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private Handler handler = new Handler();
    private Runnable colorDetectionRunnable;
    private String lastHexColor = "";

    // --- Zoom Variables ---
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ScaleGestureDetector scaleGestureDetector;
    private float currentZoomRatio = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        captureButton = findViewById(R.id.btn_capture);
        colorInfoTextView = findViewById(R.id.tv_color_info);
        chromaWebView = findViewById(R.id.chroma_webview);
        centerOverlay = findViewById(R.id.center_overlay);
        colorDisplayView = findViewById(R.id.color_display_view);

        // Yêu cầu cấp quyền camera nếu chưa được cấp
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        chromaWebView.getSettings().setJavaScriptEnabled(true);
        chromaWebView.addJavascriptInterface(new JavaScriptInterface(), "Android");
        chromaWebView.loadUrl("file:///android_asset/chroma.html");

        captureButton.setOnClickListener(v -> capturePhoto());

        // Thêm hiệu ứng hover cho nút capture
        captureButton.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        // Hiệu ứng phóng to nhẹ khi hover
                        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start();
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        // Trở lại kích thước ban đầu khi hover thoát
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        break;
                }
                return false;
            }
        });

        colorDetectionRunnable = new Runnable() {
            @Override
            public void run() {
                extractColorFromCenter();
                handler.postDelayed(this, 100);
            }
        };

        setupPinchToZoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(colorDetectionRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(colorDetectionRunnable);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Camera provider future failed.", e);
            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupPinchToZoom() {
        ScaleGestureDetector.SimpleOnScaleGestureListener listener =
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (camera == null) return true;

                        CameraInfo cameraInfo = camera.getCameraInfo();
                        LiveData<ZoomState> zoomState = cameraInfo.getZoomState();
                        ZoomState currentZoomState = zoomState.getValue();

                        if (currentZoomState != null) {
                            float delta = detector.getScaleFactor();
                            currentZoomRatio = currentZoomState.getZoomRatio() * delta;

                            currentZoomRatio = Math.max(currentZoomState.getMinZoomRatio(),
                                    Math.min(currentZoomRatio, currentZoomState.getMaxZoomRatio()));

                            camera.getCameraControl().setZoomRatio(currentZoomRatio)
                                    .addListener(() -> {}, ContextCompat.getMainExecutor(MainActivity.this));
                        }
                        return true;
                    }
                };

        scaleGestureDetector = new ScaleGestureDetector(this, listener);

        previewView.setOnTouchListener((view, motionEvent) -> scaleGestureDetector.onTouchEvent(motionEvent));
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Log.w("CameraX", "ImageCapture use case is null. Cannot capture photo.");
            Toast.makeText(this, "Camera not ready.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "IMG_" + sdf.format(new Date()) + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ColorApp");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues)
                        .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo saved: " + outputFileResults.getSavedUri();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.d("CameraX", msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(MainActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void extractColorFromCenter() {
        previewView.post(() -> {
            Bitmap bitmap = previewView.getBitmap();

            if (bitmap != null && !bitmap.isRecycled()) {
                int sampleSize = 5;
                int centerX = bitmap.getWidth() / 2;
                int centerY = bitmap.getHeight() / 2;

                int redSum = 0, greenSum = 0, blueSum = 0, sampleCount = 0;

                for (int x = centerX - sampleSize / 2; x <= centerX + sampleSize / 2; x++) {
                    for (int y = centerY - sampleSize / 2; y <= centerY + sampleSize / 2; y++) {
                        if (x >= 0 && x < bitmap.getWidth() && y >= 0 && y < bitmap.getHeight()) {
                            try {
                                int pixel = bitmap.getPixel(x, y);
                                redSum += Color.red(pixel);
                                greenSum += Color.green(pixel);
                                blueSum += Color.blue(pixel);
                                sampleCount++;
                            } catch (Exception e) {
                                Log.e("ColorExtraction", "Error accessing pixel: ", e);
                            }
                        }
                    }
                }

                if (sampleCount > 0) {
                    int red = redSum / sampleCount;
                    int green = greenSum / sampleCount;
                    int blue = blueSum / sampleCount;
                    String hexColor = String.format("#%02X%02X%02X", red, green, blue);

                    if (!hexColor.equals(lastHexColor)) {
                        if (centerOverlay != null) {
                            GradientDrawable border = new GradientDrawable();
                            border.setShape(GradientDrawable.RECTANGLE);
                            border.setStroke(6, Color.rgb(red, green, blue));
                            border.setColor(Color.TRANSPARENT);
                            centerOverlay.setBackground(border);
                        }

                        // Cập nhật ô màu sắc
                        colorDisplayView.setBackgroundColor(Color.rgb(red, green, blue));

                        chromaWebView.evaluateJavascript("javascript:getColorName('" + hexColor + "')", colorNameJson -> {
                            String colorName = colorNameJson.replace("\"", "");
                            if (colorName.equals("null") || colorName.isEmpty() || colorName.startsWith("Unknown")) {
                                colorName = "Unknown (" + hexColor + ")";
                            }
                            colorInfoTextView.setText(colorName);
                            colorInfoTextView.setTextColor(Color.BLACK);
                        });

                        lastHexColor = hexColor;
                    }
                }
            }
        });
    }

    public class JavaScriptInterface {
        @JavascriptInterface
        public void setColorName(String colorName) {
            runOnUiThread(() -> Log.d("JSInterface", "Received color name: " + colorName));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
