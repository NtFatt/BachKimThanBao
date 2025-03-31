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
import android.view.MotionEvent; // <-- Added Import
import android.view.ScaleGestureDetector; // <-- Added Import
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera; // <-- Added Import
import androidx.camera.core.CameraInfo; // <-- Added Import (Potentially needed for accurate zoom limits)
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState; // <-- Added Import
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData; // <-- Added Import (If observing ZoomState LiveData)

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException; // <-- Added Import for future.get()

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button captureButton;
    private TextView colorInfoTextView;
    private WebView chromaWebView;
    private ImageCapture imageCapture;
    private View centerOverlay;

    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private Handler handler = new Handler();
    private Runnable colorDetectionRunnable;
    private String lastHexColor = "";

    // --- Zoom Variables ---
    private ProcessCameraProvider cameraProvider; // Store CameraProvider
    private Camera camera; // Store Camera instance
    private ScaleGestureDetector scaleGestureDetector;
    private float currentZoomRatio = 1.0f; // Start with no zoom
    // --- End Zoom Variables ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        captureButton = findViewById(R.id.btn_capture);
        colorInfoTextView = findViewById(R.id.tv_color_info);
        chromaWebView = findViewById(R.id.chroma_webview);
        centerOverlay = findViewById(R.id.center_overlay);

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

        colorDetectionRunnable = new Runnable() {
            @Override
            public void run() {
                extractColorFromCenter();
                handler.postDelayed(this, 100); // Continue detecting color
            }
        };

        // --- Initialize ScaleGestureDetector ---
        setupPinchToZoom();
        // --- End Initialization ---
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(colorDetectionRunnable); // Start color detection when resumed
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(colorDetectionRunnable); // Stop color detection when paused
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Store the CameraProvider
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        // Optionally set capture mode, target resolution etc. here
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera and store the Camera instance
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

                // Apply initial zoom state if necessary (e.g., if app was backgrounded)
                // updateCameraZoom(); // You might call this if you want to restore zoom

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
                        if (camera == null) return true; // Camera not ready yet

                        // Get current zoom state
                        CameraInfo cameraInfo = camera.getCameraInfo();
                        LiveData<ZoomState> zoomState = cameraInfo.getZoomState();
                        ZoomState currentZoomState = zoomState.getValue(); // Get current value (might be null initially)

                        if (currentZoomState != null) {
                            // Calculate the new desired zoom ratio based on the gesture
                            float delta = detector.getScaleFactor();
                            currentZoomRatio = currentZoomState.getZoomRatio() * delta;

                            // Clamp the zoom ratio within the limits supported by the camera
                            currentZoomRatio = Math.max(currentZoomState.getMinZoomRatio(),
                                    Math.min(currentZoomRatio, currentZoomState.getMaxZoomRatio()));

                            // Set the zoom ratio on the camera
                            Log.d("CameraX", "Setting Zoom Ratio: " + currentZoomRatio);
                            camera.getCameraControl().setZoomRatio(currentZoomRatio)
                                    .addListener(() -> {}, ContextCompat.getMainExecutor(MainActivity.this)); // Optional: Add listener for completion/failure
                        } else {
                            Log.w("CameraX", "ZoomState is null, cannot process scale.");
                        }
                        return true; // Indicate the event was handled
                    }
                };

        scaleGestureDetector = new ScaleGestureDetector(this, listener);

        // Attach the detector to the preview view for touch events
        previewView.setOnTouchListener((view, motionEvent) -> {
            // Pass touch events to the scale gesture detector
            boolean handled = scaleGestureDetector.onTouchEvent(motionEvent);
            // You might want to handle other touch events here if needed
            // Returning true consumes the event, false allows it to propagate
            // For zooming, consuming it is usually desired.
            return handled;
            // Alternatively, if you want activity-wide gestures:
            // return scaleGestureDetector.onTouchEvent(motionEvent);
            // And use the onTouchEvent override below instead of setting listener on previewView
        });
    }


    /*
    // --- Alternative: Activity-level touch handling ---
    // If you prefer handling touch events for the whole activity screen,
    // uncomment this method and REMOVE the previewView.setOnTouchListener(...) call above.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass activity touch events to the scale gesture detector
        if (scaleGestureDetector != null) {
             scaleGestureDetector.onTouchEvent(event);
        }
        // Return true if you want to indicate the event was handled here,
        // or call super.onTouchEvent(event) if you want default Activity handling as well.
        return true;
    }
    // --- End Alternative ---
    */


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
        // Consider adding WRITE_EXTERNAL_STORAGE permission handling for older Android versions if needed
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ColorApp"); // Saves to Pictures/ColorApp

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Use MediaStore for saving
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
        // Use post to ensure the view is laid out and bitmap is available
        previewView.post(() -> {
            Bitmap bitmap = previewView.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) { // Check if bitmap is valid
                // Calculate center based on bitmap dimensions (more reliable)
                int centerX = bitmap.getWidth() / 2;
                int centerY = bitmap.getHeight() / 2;

                // Ensure coordinates are within bitmap bounds
                if (centerX >= 0 && centerX < bitmap.getWidth() && centerY >= 0 && centerY < bitmap.getHeight()) {
                    try {
                        int pixel = bitmap.getPixel(centerX, centerY); // Get pixel from the exact center

                        int red = Color.red(pixel);
                        int green = Color.green(pixel);
                        int blue = Color.blue(pixel);
                        String hexColor = String.format("#%02X%02X%02X", red, green, blue);

                        // Update only if color changes to avoid unnecessary UI updates and JS calls
                        if (!hexColor.equals(lastHexColor)) {
                            // Update the border color of the center overlay
                            if (centerOverlay != null) {
                                GradientDrawable border = new GradientDrawable();
                                border.setShape(GradientDrawable.RECTANGLE); // Define shape
                                border.setStroke(6, pixel); // Stroke width and color
                                border.setColor(Color.TRANSPARENT); // Make the inside transparent
                                centerOverlay.setBackground(border);
                            }

                            // Call JavaScript to get the color name and update TextView
                            // Ensure the WebView is loaded before calling evaluateJavascript
                            chromaWebView.evaluateJavascript("javascript:getColorName('" + hexColor + "')", colorNameJson -> {
                                // The result from JS might be JSON string (e.g., "\"Lime\""), remove quotes if necessary
                                String colorName = colorNameJson.replace("\"", "");
                                if (colorName.equals("null") || colorName.isEmpty()) {
                                    colorName = "Unknown"; // Handle cases where JS returns null or empty
                                }
                                String colorText = colorName + " (" + hexColor + ")";
                                colorInfoTextView.setText(colorText);
                                colorInfoTextView.setTextColor(pixel); // Set text color to the detected color
                            });

                            lastHexColor = hexColor; // Update the last detected color
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e("ColorExtraction", "Invalid coordinates for getPixel: " + centerX + "," + centerY + " in bitmap " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    } catch (IllegalStateException e) {
                        Log.e("ColorExtraction", "Could not get pixel, bitmap might be recycled.", e);
                    }

                } else {
                    Log.w("ColorExtraction", "Calculated center (" + centerX + "," + centerY + ") is outside bitmap bounds (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
                }

                // Note: It's generally not recommended to recycle the bitmap obtained from PreviewView#getBitmap()
                // as the PreviewView manages its lifecycle. Let CameraX handle it.
                // if (bitmap != null && !bitmap.isRecycled()) {
                //     bitmap.recycle(); // Avoid this unless you are sure you manage the bitmap copy
                // }

            } else {
                // Log.v("ColorExtraction", "Bitmap from PreviewView is null or recycled."); // Verbose logging if needed
            }
        });
    }


    // JavaScriptInterface remains the same
    public class JavaScriptInterface {
        @JavascriptInterface
        public void setColorName(String colorName) {
            // This method might not be strictly needed if the result is handled
            // directly in the evaluateJavascript callback, but can be kept for flexibility.
            runOnUiThread(() -> {
                // Example: Log the name received from JS if needed for debugging
                // Log.d("JSInterface", "Received color name: " + colorName);
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); // Permission granted, start camera
            } else {
                Toast.makeText(this, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show();
                // Optionally, disable camera-related features or close the app
            }
        }
    }
}