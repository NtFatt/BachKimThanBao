package com.mydoan.bachkimthanbao;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PaletteActivity extends AppCompatActivity {

    private View colorView;
    private SeekBar seekRed, seekGreen, seekBlue;
    private TextView textR, textG, textB;
    private Button btnMinusR, btnPlusR, btnMinusG, btnPlusG, btnMinusB, btnPlusB;
    private WebView webView;

    private int red = 0, green = 0, blue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palette);

        // Ánh xạ view
        colorView = findViewById(R.id.colorView);
        seekRed = findViewById(R.id.seekRed);
        seekGreen = findViewById(R.id.seekGreen);
        seekBlue = findViewById(R.id.seekBlue);
        textR = findViewById(R.id.textR);
        textG = findViewById(R.id.textG);
        textB = findViewById(R.id.textB);
        btnMinusR = findViewById(R.id.btnMinusR);
        btnPlusR = findViewById(R.id.btnPlusR);
        btnMinusG = findViewById(R.id.btnMinusG);
        btnPlusG = findViewById(R.id.btnPlusG);
        btnMinusB = findViewById(R.id.btnMinusB);
        btnPlusB = findViewById(R.id.btnPlusB);
        webView = new WebView(this);


        // Thêm WebView vào layout
        LinearLayout layout = findViewById(R.id.activity_palette);
        layout.addView(webView);

        // Cấu hình WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        String html = "<html><head>" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/chroma-js/2.1.0/chroma.min.js\"></script>" +
                "<script>" +
                "function updateColor(r, g, b) {" +
                "   var color = chroma(r, g, b);" +
                "   var hex = color.hex();" +
                "   document.body.style.backgroundColor = hex;" +
                "   var textColor = chroma.contrast(color, 'white') > chroma.contrast(color, 'black') ? 'white' : 'black';" +
                "   document.getElementById('hex').style.color = textColor;" +
                "   document.getElementById('hex').innerText = 'HEX: ' + hex;" +
                "}" +
                "</script>" +
                "</head><body style='text-align:center; font-size:24px; margin:30px;'>" +
                "<div id='hex'>HEX: #000000</div>" +
                "</body></html>";

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        // Gán max cho SeekBar
        seekRed.setMax(255);
        seekGreen.setMax(255);
        seekBlue.setMax(255);

        // Lắng nghe thay đổi SeekBar
        seekRed.setOnSeekBarChangeListener(colorChangeListener);
        seekGreen.setOnSeekBarChangeListener(colorChangeListener);
        seekBlue.setOnSeekBarChangeListener(colorChangeListener);

        // Xử lý nút +
        btnPlusR.setOnClickListener(v -> adjustColor("R", 1));
        btnPlusG.setOnClickListener(v -> adjustColor("G", 1));
        btnPlusB.setOnClickListener(v -> adjustColor("B", 1));

        // Xử lý nút -
        btnMinusR.setOnClickListener(v -> adjustColor("R", -1));
        btnMinusG.setOnClickListener(v -> adjustColor("G", -1));
        btnMinusB.setOnClickListener(v -> adjustColor("B", -1));

        // Cập nhật ban đầu
        updateColor();
    }

    private final SeekBar.OnSeekBarChangeListener colorChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == seekRed) red = progress;
            else if (seekBar == seekGreen) green = progress;
            else if (seekBar == seekBlue) blue = progress;
            updateColor();
        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    private void adjustColor(String channel, int delta) {
        switch (channel) {
            case "R":
                red = clamp(red + delta);
                seekRed.setProgress(red);
                break;
            case "G":
                green = clamp(green + delta);
                seekGreen.setProgress(green);
                break;
            case "B":
                blue = clamp(blue + delta);
                seekBlue.setProgress(blue);
                break;
        }
        updateColor();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }


    private void updateColor() {
        // Tính màu nền
        int color = Color.rgb(red, green, blue);
        colorView.setBackgroundColor(color);


        // Tính độ sáng của màu nền theo công thức luminance
        float luminance = (0.2126f * red + 0.7152f * green + 0.0722f * blue);

        // Nếu luminance lớn hơn 128, chọn chữ đen, ngược lại chọn chữ trắng
        int textColor = (luminance > 128) ? Color.BLACK : Color.WHITE;

        // Cập nhật màu cho WebView
        String js = String.format("updateColor(%d, %d, %d)", red, green, blue);
        webView.evaluateJavascript(js, null);
        // Cập nhật màu và text
            colorView.setBackgroundColor(color);
            textR.setText("R: " + red);
            textG.setText("G: " + green);
            textB.setText("B: " + blue);
    }
}
