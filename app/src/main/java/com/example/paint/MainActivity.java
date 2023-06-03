package com.example.paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jaredrummler.android.colorpicker.ColorShape;

public class MainActivity extends AppCompatActivity implements ColorPickerDialogListener {
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private MyPaintView myView;
    private  Bitmap imageBitmap;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Рисовалка");
        myView = new MyPaintView(this);
        ((LinearLayout) findViewById(R.id.paintLayout)).addView(myView);
        myView.mPaint.setColor(Color.BLUE);
        SeekBar seekBar = findViewById(R.id.setThickness);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                myView.mPaint.setStrokeWidth(Float.parseFloat(String.valueOf(progress)));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i("onStartTrackingTouch", "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i("onStopTrackingTouch", "onStopTrackingTouch");
            }
        });
        findViewById(R.id.btnClear).setOnClickListener(view -> {
            myView.mBitmap.eraseColor(Color.TRANSPARENT);
            myView.invalidate();
        });
        Button btnColor = findViewById(R.id.btnColor);
        btnColor.setOnClickListener(view -> {
            createColorPickerDialog();
        });

        Button btnPickImage = findViewById(R.id.btnPickImage);
        btnPickImage.setOnClickListener(v -> {
            pickImageFromGallery();
        });

        Button btnSaveImage = findViewById(R.id.saveImage);
        btnSaveImage.setOnClickListener(v -> {
            saveImageToGallery();
        });
    }


    private void createColorPickerDialog() {
        ColorPickerDialog.newBuilder()
                .setColor(Color.RED)
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowCustom(true)
                .setAllowPresets(true)
                .setColorShape(ColorShape.SQUARE)
                .show(this);
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        myView.mPaint.setColor(color);
    }
    @Override
    public void onDialogDismissed(int dialogId) {
        Toast.makeText(this, "Dialog dismissed", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void pickImageFromGallery(){
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , REQUEST_CODE_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void saveImageToGallery(){
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PICK_IMAGE);
        } else {
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    myView.mBitmap,
                    "Painting",
                    "Image created by MyPaintApp"
            );

            if(savedImageURL !=null){
                Toast.makeText(this , "Image saved to Gallery" , Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this , "Failed to save image" , Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PICK_IMAGE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                pickImageFromGallery();
            } else {
                Toast.makeText(this , "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                saveImageToGallery();
            } else{
                Toast.makeText(this , "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null){
            Uri selectedImageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                myView.setBackgroundImage(bitmap);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private static class MyPaintView extends View {
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private final Path mPath;
        private final Paint mPaint;
        private Bitmap mbackgroundBitmap;
        private  Canvas mbackgroundCanvas;
        public MyPaintView(Context context) {
            super(context);
            mPath = new Path();
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(10);
            mPaint.setStyle(Paint.Style.STROKE);
        }

        public void setBackgroundImage(Bitmap bitmap){
            mbackgroundBitmap = bitmap;
            float scaleFactor = Math.min(
                    (float) getWidth() / mbackgroundBitmap.getWidth(),
                    (float) getHeight() / mbackgroundBitmap.getHeight()
            );
            int newWidth = (int) (mbackgroundBitmap.getWidth() * scaleFactor);
            int newHeight = (int) (mbackgroundBitmap.getHeight() * scaleFactor);
            mbackgroundBitmap = Bitmap.createScaledBitmap(mbackgroundBitmap, newWidth , newHeight , true);
            mCanvas.drawBitmap(mbackgroundBitmap, 0, 0, null);
            mCanvas.drawPath(mPath, mPaint);
            invalidate();
        }
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mbackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mbackgroundCanvas = new Canvas(mbackgroundBitmap);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(mbackgroundBitmap , 0, 0, null);
            canvas.drawBitmap(mBitmap, 0, 0, null);
            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPath.reset();
                    mPath.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    mPath.lineTo(x, y);
                    mCanvas.drawPath(mPath , mPaint);
                    mPath.reset();
                    break;
            }
            invalidate();
            return true;
        }
    }
}
