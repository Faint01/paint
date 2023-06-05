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
import android.graphics.PointF;
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

    private float mScaleFactor = 1.0f;
    private boolean isScalingEnabled = false;

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

        Button btnScale =findViewById(R.id.btnScale);
        btnScale.setOnClickListener(view ->{
            if(myView.isZoomEnabled){
                myView.setZoomEnabled(false);
                btnScale.setText("Включить масштабирование");
            } else {
                myView.setZoomEnabled(true);
                btnScale.setText("Выключить масштабирование");
            }
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
        if (myView != null) {
            myView.saveCanvasWithImage();
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

    private void scaleImage(){

    }

    private static class MyPaintView extends View {

        private float mInitialDistance = 0f;
        private float mScaleFactor = 1f;
        private float mFocusX;
        private float mFocusY;

        private Bitmap mBitmap;
        private Canvas mCanvas;
        private final Path mPath;
        private final Paint mPaint;

        private float mPosX;
        private float mPosY;
        private float mLastTouchX;
        private float mLastTouchY;

        private PointF mMiddlePoint;

        private float mLastScaleFactor = 1f;

        private static final float TOUCH_TOLERANCE = 4;
        private static final float MIN_SCALE_FACTOR = 0.1f;
        private static final float MAX_SCALE_FACTOR = 2f;

        private boolean isDrawingEnabled = true;
        private boolean isZoomEnabled = false;
        private boolean isMoving = false;
        private Bitmap mbackgroundBitmap;
        private  Canvas mbackgroundCanvas;

        private float initialDistance;
        private float initialImageDistance;




        public MyPaintView(Context context) {
            super(context);
            mPath = new Path();
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(10);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStyle(Paint.Style.STROKE);

            mMiddlePoint = new PointF();
        }

        public void setZoomEnabled(Boolean enabled){
            isZoomEnabled = enabled;
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
            mbackgroundCanvas.drawBitmap(mbackgroundBitmap, 0, 0, null);
            mbackgroundCanvas.drawPath(mPath, mPaint);
            invalidate();
        }

        public void saveCanvasWithImage() {
            Bitmap combinedBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas combinedCanvas = new Canvas(combinedBitmap);
            combinedCanvas.drawBitmap(mbackgroundBitmap, 0, 0, null);
            combinedCanvas.drawBitmap(mBitmap, 0, 0, null);

            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContext().getContentResolver(),
                    combinedBitmap,
                    "MyPaintImage",
                    "Combined image of canvas and background image"
            );

            if (savedImageURL != null) {
                Toast.makeText(getContext(), "Изображение сохранено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Не удалось сохранить изображение", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mbackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mbackgroundCanvas = new Canvas(mbackgroundBitmap);

            if (mbackgroundBitmap != null) {
                float scaleFactor = Math.min(
                        (float) w / mbackgroundBitmap.getWidth(),
                        (float) h / mbackgroundBitmap.getHeight()
                );
                int newWidth = (int) (mbackgroundBitmap.getWidth() * scaleFactor);
                int newHeight = (int) (mbackgroundBitmap.getHeight() * scaleFactor);
                mbackgroundBitmap = Bitmap.createScaledBitmap(mbackgroundBitmap, newWidth, newHeight, true);
                mbackgroundCanvas.drawBitmap(mbackgroundBitmap, 0, 0, null);
                mbackgroundCanvas.drawPath(mPath, mPaint);
                mLastScaleFactor = mScaleFactor;
            }
        }

        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.scale(mScaleFactor, mScaleFactor, mMiddlePoint.x, mMiddlePoint.y);
            canvas.translate(mPosX, mPosY);
            canvas.drawBitmap(mbackgroundBitmap, 0, 0, null);
            canvas.drawBitmap(mBitmap, 0, 0, null);
            canvas.drawPath(mPath, mPaint);
            canvas.restore();
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isDrawingEnabled && !isZoomEnabled) {
                return super.onTouchEvent(event);
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    isMoving = false;
                    mLastTouchX = x;
                    mLastTouchY = y;
                    if (isDrawingEnabled) {
                        mPath.reset();
                        mPath.moveTo(x, y);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (isZoomEnabled) {
                        mInitialDistance = calculateDistance(event);
                        midPoint(mMiddlePoint, event);
                    }
                    isDrawingEnabled = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isZoomEnabled && event.getPointerCount() == 2) {
                        // Масштабирование
                        float newDistance = calculateDistance(event);
                        float scaleFactor = newDistance / mInitialDistance;

                        // Применение ограничений на масштабирование
                        if (scaleFactor < MIN_SCALE_FACTOR) {
                            scaleFactor = MIN_SCALE_FACTOR;
                        } else if (scaleFactor > MAX_SCALE_FACTOR) {
                            scaleFactor = MAX_SCALE_FACTOR;
                        }

                        mScaleFactor *= scaleFactor;
                        mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, MAX_SCALE_FACTOR));

                        mLastTouchX = x;
                        mLastTouchY = y;

                        invalidate();
                    } else if (isDrawingEnabled) {
                        // Рисование
                        float dx = Math.abs(x - mLastTouchX);
                        float dy = Math.abs(y - mLastTouchY);
                        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                            mPath.quadTo(mLastTouchX, mLastTouchY, (x + mLastTouchX) / 2, (y + mLastTouchY) / 2);
                            mCanvas.drawPath(mPath, mPaint);
                            mPath.reset();
                            mPath.moveTo(mLastTouchX, mLastTouchY);
                            mLastTouchX = x;
                            mLastTouchY = y;
                        }
                        invalidate();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (!isMoving && isDrawingEnabled) {
                        mPath.lineTo(x, y);
                        mCanvas.drawPath(mPath, mPaint);
                        mPath.reset();
                        invalidate();
                    }
                    isMoving = false;
                    isDrawingEnabled = true; // Включаем режим рисования
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
            }
            return true;
        }

        private float calculateDistance(MotionEvent event) {
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }
}
