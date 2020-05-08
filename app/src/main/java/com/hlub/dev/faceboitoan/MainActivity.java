package com.hlub.dev.faceboitoan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.face.Face;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageView faceDetectionImageView;
    private FrameLayout bottomSheetButton;
    private ImageView bottomSheetButtonImage;
    private ProgressBar bottomSheetButtonProgress;
    private RecyclerView bottomSheetRecyclerView;
    private TextView tvResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        faceDetectionImageView = findViewById(R.id.face_detection_image_view);
        bottomSheetButton = findViewById(R.id.bottom_sheet_button);
        bottomSheetButtonImage = findViewById(R.id.bottom_sheet_button_image);
        bottomSheetButtonProgress = findViewById(R.id.bottom_sheet_button_progress);
        bottomSheetRecyclerView = findViewById(R.id.bottom_sheet_recycler_view);
        tvResult = findViewById(R.id.tvResult);


        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity().start(MainActivity.this);
            }
        });

    }

    ///nhận image từ Intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = result.getUri();
                try {
                    analyzeImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "There was some error : ${result.error.message}", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage(final Bitmap image) {
        if (image == null) {
            Toast.makeText(this, "There was some error", Toast.LENGTH_SHORT).show();
            return;
        }

        faceDetectionImageView.setImageBitmap(null);
        tvResult.setText("");
        showProgress();

        //khởi tạo máy dò tìm mốc và phân loại khuôn mặt
        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();

        //khởi tạo đối tượng FirebaseVisionImage từ Bitmap
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(image);

        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        faceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Bitmap mutableImage = image.copy(Bitmap.Config.ARGB_8888, true);
                        detectFaces(firebaseVisionFaces, mutableImage);

                        faceDetectionImageView.setImageBitmap(mutableImage);
                        hideProgress();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "There was some error", Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }
                });


        // Phát hiện đường viền theo thời gian thực của nhiều khuôn mặt
        FirebaseVisionFaceDetectorOptions contourOptions = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build();

        FirebaseVisionFaceDetector faceContourDetector = FirebaseVision.getInstance().getVisionFaceDetector(contourOptions);

        faceContourDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        detectContourFaces(firebaseVisionFaces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "There was some error", Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }
                });
    }

    private void detectContourFaces(List<FirebaseVisionFace> firebaseVisionFaces) {
        for (FirebaseVisionFace face : firebaseVisionFaces) {

            //mặt
            List<FirebaseVisionPoint> faceContours = face.getContour(FirebaseVisionFaceContour.FACE).getPoints();
            float widthFace = faceContours.get(8).getX() - faceContours.get(28).getX();
            float heightFace = faceContours.get(18).getY() - faceContours.get(35).getY();
            Log.e("FACE", "Khuôn mặt " + widthFace + " - " + heightFace);
            tvResult.append("Khuôn mặt " + widthFace + " - " + heightFace + "\n");

            //mắt trái
            List<FirebaseVisionPoint> leftEyeContours = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            float widthLeftEye = leftEyeContours.get(8).getX() - leftEyeContours.get(0).getX();
            Log.e("FACE", "Mắt trái dài " + widthLeftEye);
            float heightLeftEye = leftEyeContours.get(12).getY() - leftEyeContours.get(4).getY();
            Log.e("FACE", "Mắt trái cao " + heightLeftEye);
            tvResult.append("Mắt trái dài " + widthLeftEye + "\n");
            tvResult.append("Mắt trái cao " + heightLeftEye + "\n");

            //mắt phải
            List<FirebaseVisionPoint> rightEyeContours = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
            float widthRightEye = rightEyeContours.get(8).getX() - rightEyeContours.get(0).getX();
            Log.e("FACE", "Mắt phải dài " + widthRightEye);
            float heightRightEye = rightEyeContours.get(12).getY() - rightEyeContours.get(4).getY();
            Log.e("FACE", "Mắt phải cao " + heightRightEye);
            tvResult.append("Mắt phải dài " + widthRightEye + "\n");
            tvResult.append("Mắt phải cao " + heightRightEye + "\n");


            //lông mày phải
            List<FirebaseVisionPoint> rightEyebrowContours = face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).getPoints();
            float rightEyebrow = rightEyebrowContours.get(0).getX() - rightEyebrowContours.get(4).getX();
            Log.e("FACE", "lông mày phải " + rightEyebrow);
            tvResult.append("lông mày phải " + rightEyebrow + "\n");

            //lông mày trái
            List<FirebaseVisionPoint> leftEyebrowContours = face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints();
            float leftEyebrow = leftEyebrowContours.get(4).getX() - leftEyebrowContours.get(0).getX();
            Log.e("FACE", "lông mày trái " + leftEyebrow);
            tvResult.append("lông mày trái " + leftEyebrow + "\n");

            //miệng
            List<FirebaseVisionPoint> upperLipContours = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
            float upperLip = upperLipContours.get(8).getX() - upperLipContours.get(0).getX();
            tvResult.append("Môi rộng " + upperLip + "\n");

            //sống mũi
            List<FirebaseVisionPoint> noseContours = face.getContour(FirebaseVisionFaceContour.NOSE_BRIDGE).getPoints();
            float nose = noseContours.get(1).getY() - noseContours.get(0).getY();
            tvResult.append("Mũi " + nose + "\n");

        }

    }

    //hiển thị thông tin cột mốc khuôn mặt
    private void detectFaces(List<FirebaseVisionFace> firebaseVisionFaces, Bitmap mutableImage) {
        if (firebaseVisionFaces == null || mutableImage == null) {
            Toast.makeText(this, "There was some error", Toast.LENGTH_SHORT).show();
            return;
        }

        Canvas canvas = new Canvas(mutableImage);

        //màu khung BoundingBox
        Paint facePaint = new Paint();
        facePaint.setColor(Color.YELLOW);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(8F);

        ///màu text
        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.YELLOW);
        faceTextPaint.setTextSize(40F);
        faceTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        //màu cột mốc
        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.BLUE);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(8F);

        for (int index = 0; index < firebaseVisionFaces.size(); index++) {
            FirebaseVisionFace face = firebaseVisionFaces.get(index);
            canvas.drawRect(face.getBoundingBox(), facePaint);
            canvas.drawText("Face" + index, (face.getBoundingBox().centerX() - face.getBoundingBox().width() / 2) + 8F, (face.getBoundingBox().centerY() + face.getBoundingBox().height() / 2) - 8F, faceTextPaint);

            //landmark = mốc
            //mắt trái
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null) {
                FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                canvas.drawCircle(leftEye.getPosition().getX(), leftEye.getPosition().getY(), 8F, landmarkPaint);
            }
            //mắt phải
            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null) {
                FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                canvas.drawCircle(rightEye.getPosition().getX(), rightEye.getPosition().getY(), 8F, landmarkPaint);
            }

            // trái
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK) != null) {
                FirebaseVisionFaceLandmark leftCheek = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK);
                canvas.drawCircle(leftCheek.getPosition().getX(), leftCheek.getPosition().getY(), 8F, landmarkPaint);
            }
            //mắt phải
            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK) != null) {
                FirebaseVisionFaceLandmark rightCheek = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK);
                canvas.drawCircle(rightCheek.getPosition().getX(), rightCheek.getPosition().getY(), 8F, landmarkPaint);
            }

            //mũi cơ sở
            if (face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null) {
                FirebaseVisionFaceLandmark nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                canvas.drawCircle(nose.getPosition().getX(), nose.getPosition().getY(), 8F, landmarkPaint);
            }
            //gò má trái
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR) != null) {
                FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                canvas.drawCircle(leftEar.getPosition().getX(), leftEar.getPosition().getY(), 8F, landmarkPaint);
            }
            //gò má phải
            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR) != null) {
                FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                canvas.drawCircle(rightEar.getPosition().getX(), rightEar.getPosition().getY(), 8F, landmarkPaint);
            }

            //miệng
            if (face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null) {
                FirebaseVisionFaceLandmark leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                FirebaseVisionFaceLandmark bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                FirebaseVisionFaceLandmark rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
                canvas.drawLine(leftMouth.getPosition().getX(), leftMouth.getPosition().getY(), bottomMouth.getPosition().getX(), bottomMouth.getPosition().getY(), landmarkPaint);
                canvas.drawLine(bottomMouth.getPosition().getX(), bottomMouth.getPosition().getY(), rightMouth.getPosition().getX(), rightMouth.getPosition().getY(), landmarkPaint);
            }

            //hình dạng khuôn mặt
            float r = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR).getPosition().getX();
            float l = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR).getPosition().getX();
            float height = face.getBoundingBox().height();
            float tile = height / (r - l);

//            tvResult.append("Độ rộng: " + (r - l) + "\n");
//            tvResult.append("Độ dài: " + height + "\n");
//            tvResult.append("Tỉ lệ" + tile + "\n");
//            if (tile >= 1.2) {
//                tvResult.append("Khuôn mặt dài");
//            } else {
//                tvResult.append("Khuôn mặt tròn");
//            }


        }
    }

    private void showProgress() {
        bottomSheetButtonImage.setVisibility(View.GONE);
        bottomSheetButtonProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        bottomSheetButtonImage.setVisibility(View.VISIBLE);
        bottomSheetButtonProgress.setVisibility(View.GONE);
    }
}
