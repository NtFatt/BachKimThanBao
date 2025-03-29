package com.mydoan.bachkimthanbao;

import android.os.Bundle;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.PreviewView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.camera.core.ImageProxy;
import androidx.annotation.NonNull;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;

import java.nio.ByteBuffer;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageView capturedImageView; // ImageView để hiển thị ảnh chụp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.camera_preview);
        Button takePictureButton = findViewById(R.id.btn_capture); // Nút chụp ảnh
        Button chooseImageButton = findViewById(R.id.btn_library);  // Nút chọn ảnh từ thư viện
        capturedImageView = findViewById(R.id.iv_captured_image); // ImageView hiển thị ảnh

        // Xử lý sự kiện chọn ảnh từ thư viện
        chooseImageButton.setOnClickListener(view -> openGallery());

        // Kiểm tra và yêu cầu quyền truy cập camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Xử lý sự kiện nhấn nút chụp ảnh
        takePictureButton.setOnClickListener(view -> takePicture());
    }

    // Mở thư viện ảnh để người dùng chọn
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");  // Chỉ chọn ảnh
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Xử lý kết quả từ việc chọn ảnh từ thư viện
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();  // Lấy URI của ảnh đã chọn
            if (selectedImageUri != null) {
                try {
                    // Lấy ảnh từ URI
                    Bitmap selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    capturedImageView.setImageBitmap(selectedImage);  // Hiển thị ảnh lên ImageView
                    // Phân tích màu sắc từ ảnh đã chọn
                    analyzeImageColor(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Khởi động camera và cấu hình CameraX
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Lấy ProcessCameraProvider
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Cấu hình Preview để hiển thị hình ảnh từ camera
                Preview preview = new Preview.Builder().build();

                // Cấu hình ImageCapture để chụp ảnh
                imageCapture = new ImageCapture.Builder().build();

                // Cấu hình CameraSelector để chọn camera trước (front camera)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Chọn camera trước
                        .build();

                // Kiểm tra xem camera trước có sẵn không
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    Log.e("CameraX", "Front camera not available. Switching to back camera.");
                    // Nếu camera trước không có sẵn, sử dụng camera sau
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Camera sau
                            .build();
                }

                // Liên kết camera vào lifecycle của Activity và set Preview vào PreviewView
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Liên kết Preview với SurfaceProvider của PreviewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (Exception e) {
                Log.e("CameraX", "Camera setup failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Chụp ảnh khi người dùng nhấn nút
    private void takePicture() {
        if (imageCapture == null) {
            return;
        }

        // Chụp ảnh
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {

            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                // Chuyển đổi ImageProxy thành Bitmap
                Bitmap bitmap = imageProxyToBitmap(image);
                // Hiển thị ảnh trong ImageView
                capturedImageView.setImageBitmap(bitmap);
                // Phân tích màu sắc từ ảnh chụp được
                analyzeImageColor(bitmap);
                image.close();  // Đảm bảo đóng ImageProxy sau khi xử lý
            }

            @Override
            public void onError(@NonNull ImageCaptureException exc) {
                super.onError(exc);
                // Hiển thị Toast khi có lỗi
                Toast.makeText(MainActivity.this, "Error capturing image: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CameraX", "Error capturing image: " + exc.getMessage());
            }
        });
    }

    // Chuyển đổi ImageProxy thành Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // Phân tích màu sắc từ Bitmap
    private void analyzeImageColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Biến để tính tổng giá trị RGB và HSV
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int pixelCount = 0;

        // Biến để tính tổng giá trị HSV
        float totalHue = 0;
        float totalSaturation = 0;
        float totalValue = 0;

        boolean isBlack = true;
        boolean isWhite = true;

        // Lấy pixel từ toàn bộ ảnh để nhận diện chính xác
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixelColor = bitmap.getPixel(x, y);

                // Chuyển đổi từ RGB sang HSV
                float[] hsv = new float[3];
                Color.colorToHSV(pixelColor, hsv);

                // Kiểm tra điều kiện cho màu đen và trắng
                if (hsv[1] > 0.1 || hsv[2] > 0.1) {
                    isBlack = false;
                }

                if (hsv[1] > 0.1 || hsv[2] < 0.9) {
                    isWhite = false;
                }

                // Cộng dồn giá trị RGB và HSV
                totalRed += Color.red(pixelColor);
                totalGreen += Color.green(pixelColor);
                totalBlue += Color.blue(pixelColor);

                totalHue += hsv[0];
                totalSaturation += hsv[1];
                totalValue += hsv[2];

                pixelCount++;
            }
        }

        // Kiểm tra nếu ảnh gần như là màu trắng hoặc đen
        if (isBlack && isWhite) {
            TextView colorTextView = findViewById(R.id.color_text);
            colorTextView.setText("Detected Color: White");
            Toast.makeText(this, "Detected Color: White", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tính trung bình giá trị RGB và HSV
        int averageRed = totalRed / pixelCount;
        int averageGreen = totalGreen / pixelCount;
        int averageBlue = totalBlue / pixelCount;

        float averageHue = totalHue / pixelCount;
        float averageSaturation = totalSaturation / pixelCount;
        float averageValue = totalValue / pixelCount;

        // Nhận diện màu sắc từ giá trị Hue trung bình
        String color = detectColorHSV(averageHue, averageSaturation, averageValue, averageRed, averageGreen, averageBlue);

        // Hiển thị kết quả màu sắc nhận diện
        TextView colorTextView = findViewById(R.id.color_text);
        colorTextView.setText("Detected Color: " + color);

        // Hoặc hiển thị màu sắc thông qua Toast
        Toast.makeText(this, "Detected Color: " + color, Toast.LENGTH_SHORT).show();
    }

    private String detectColorHSV(float hue, float saturation, float value, int red, int green, int blue) {
        // Kiểm tra màu đen: Sự bão hòa rất thấp và giá trị sáng rất thấp
        if (saturation < 0.1 && value < 0.1) {
            return "Black"; // Màu đen
        }

        // Kiểm tra màu trắng: Sự bão hòa thấp và giá trị sáng gần 1
        if (saturation < 0.1 && value > 0.9) {
            return "White"; // Màu trắng
        }

        // Dựa vào giá trị Hue để phân loại màu sắc
        // Dựa vào giá trị Hue để phân loại màu sắc
        if (hue >= 0 && hue <= 15) {
            return "Red"; // Đỏ
        } else if (hue > 15 && hue <= 30) {
            return "Orange"; // Cam
        } else if (hue > 30 && hue <= 45) {
            return "Yellow-Orange"; // Vàng cam
        } else if (hue > 45 && hue <= 60) {
            return "Yellow"; // Vàng
        } else if (hue > 60 && hue <= 75) {
            return "Yellow-Green"; // Vàng xanh
        } else if (hue > 75 && hue <= 90) {
            return "Lime Green"; // Xanh chanh
        } else if (hue > 90 && hue <= 105) {
            return "Green"; // Xanh lá
        } else if (hue > 105 && hue <= 120) {
            return "Emerald Green"; // Xanh ngọc
        } else if (hue > 120 && hue <= 135) {
            return "Spring Green"; // Xanh xuân
        } else if (hue > 135 && hue <= 150) {
            return "Sea Green"; // Xanh biển
        } else if (hue > 150 && hue <= 165) {
            return "Teal"; // Xanh ngọc
        } else if (hue > 165 && hue <= 180) {
            return "Turquoise"; // Xanh lam
        } else if (hue > 180 && hue <= 195) {
            return "Cyan"; // Cyan
        } else if (hue > 195 && hue <= 210) {
            return "Sky Blue"; // Xanh da trời
        } else if (hue > 210 && hue <= 225) {
            return "Azure"; // Xanh lam nhạt
        } else if (hue > 225 && hue <= 240) {
            return "Blue"; // Xanh dương
        } else if (hue > 240 && hue <= 255) {
            return "Royal Blue"; // Xanh hoàng gia
        } else if (hue > 255 && hue <= 270) {
            return "Purple"; // Tím
        } else if (hue > 270 && hue <= 285) {
            return "Magenta"; // Hồng đậm
        } else if (hue > 285 && hue <= 300) {
            return "Violet"; // Tím violet
        } else if (hue > 300 && hue <= 315) {
            return "Fuchsia"; // Hồng fuchsia
        } else {
            return "Pink"; // Hồng
        }
    }

    // Xử lý kết quả trả về từ việc yêu cầu quyền camera
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Nếu quyền camera được cấp, khởi động camera
                startCamera();
            } else {
                Log.e("Permission", "Camera permission denied.");
            }
        }
    }
}
