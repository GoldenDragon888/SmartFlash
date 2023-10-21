package au.smartflash.smartflash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import au.smartflash.smartflash.model.Word;

public class ChooseCardFaceActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private Word currentWord;

    private File externalDir;

    private GestureDetector gestureDetector;

    private ImageView imageView;

    private ScaleGestureDetector scaleGestureDetector;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_card_face);  // Use your actual layout resource name

        Button chooseImageButton = findViewById(R.id.chooseImageButton);
        chooseImageButton.setOnClickListener(v -> onChooseImageButtonClicked());

        Button cutIntoFourButton = findViewById(R.id.cutIntoFourButton);
        cutIntoFourButton.setOnClickListener(v -> onCutIntoFourButtonClicked());

        Button cardfaceHomeButton = findViewById(R.id.cardface_home_button);
        cardfaceHomeButton.setOnClickListener(v -> finish());

        imageView = findViewById(R.id.imageForCut);
        ImageView imageViewPart1 = findViewById(R.id.imageViewPart1);
        ImageView imageViewPart2 = findViewById(R.id.imageViewPart2);
        ImageView imageViewPart3 = findViewById(R.id.imageViewPart3);
        ImageView imageViewPart4 = findViewById(R.id.imageViewPart4);

    }
    public static int calculateInSampleSize(BitmapFactory.Options paramOptions, int paramInt1, int paramInt2) {
        int k = paramOptions.outHeight;
        int m = paramOptions.outWidth;
        int j = 1;
        int i = 1;
        if (k > paramInt2 || m > paramInt1) {
            k /= 2;
            m /= 2;
            while (true) {
                j = i;
                if (k / i >= paramInt2) {
                    j = i;
                    if (m / i >= paramInt1) {
                        i *= 2;
                        continue;
                    }
                }
                break;
            }
        }
        return j;
    }

    private void displayWord() {}

    private int getImageViewIdForPart(int paramInt) {
        switch (paramInt) {
            default:
                throw new IllegalArgumentException("Invalid part index");
            case 3:
                return R.id.imageViewPart4;
            case 2:
                return R.id.imageViewPart3;
            case 1:
                return R.id.imageViewPart2;
            case 0:
                return R.id.imageViewPart1;
        }
    }

    private MatrixValues getMatrixValuesFromPhotoView(PhotoView paramPhotoView) {
        Matrix matrix = new Matrix();
        paramPhotoView.getAttacher().getSuppMatrix(matrix);
        float[] arrayOfFloat = new float[9];
        matrix.getValues(arrayOfFloat);
        MatrixValues matrixValues = new MatrixValues();
        matrixValues.scaleX = arrayOfFloat[0];
        matrixValues.scaleY = arrayOfFloat[4];
        matrixValues.transX = arrayOfFloat[2];
        matrixValues.transY = arrayOfFloat[5];
        return matrixValues;
    }

    private Bitmap getPartOfBitmap(Bitmap paramBitmap, PhotoView paramPhotoView, MatrixValues paramMatrixValues, int paramInt1, int paramInt2) {
        int i = (int)(-paramMatrixValues.transX / paramMatrixValues.scaleX);
        paramInt1 = (int)(-paramMatrixValues.transY / paramMatrixValues.scaleY + paramInt1 / paramMatrixValues.scaleY);
        return Bitmap.createBitmap(paramBitmap, i, paramInt1, Math.min(paramBitmap.getWidth() - i, paramPhotoView.getWidth()), Math.min(paramBitmap.getHeight() - paramInt1, paramInt2));
    }

    private int getRotationAngle(Uri paramUri) {
        try {
            ExifInterface exifInterface = new ExifInterface(getContentResolver().openInputStream(paramUri));
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            Log.e("TAG", "Error getting Exif data", e);
            return 0;
        }
    }

    private Bitmap loadImage(String filename) {
        File directory = getExternalFilesDir(null);
        if (directory == null) {
            Log.e("FLAG", "Error: External storage directory not accessible.");
            return null;
        }

        File file = new File(directory, filename);
        if (file.exists())
            return BitmapFactory.decodeFile(file.getAbsolutePath());

        int resourceId = getResources().getIdentifier(filename.replace(".png", ""), "drawable", getPackageName());
        return BitmapFactory.decodeResource(getResources(), resourceId);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    private void saveBitmapToFile(Bitmap bitmap, String fileName) {
        File directory = getExternalFilesDir(null);
        if (directory == null) {
            Log.e("FLAG", "Error: External storage directory not accessible.");
            return;
        }

        File file = new File(directory, fileName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (IOException e) {
            Log.e("FLAG", "Error saving bitmap to file.", e);
        }
    }

    private void splitAndSaveBitmap(Bitmap paramBitmap) {
        PhotoView photoView = (PhotoView) findViewById(R.id.imageForCut);
        paramBitmap = getBitmapFromView((View) photoView);
        getMatrixValuesFromPhotoView(photoView);
        int[] sections = new int[]{170, 170, 380, 710};
        int y = 0;
        int width = paramBitmap.getWidth();
        for (int i = 0; i < 4; i++) {
            Log.d("FLAG", "Processing part " + (i + 1));

            Bitmap bitmap = Bitmap.createBitmap(paramBitmap, 0, y, width, Math.min(paramBitmap.getHeight() - y, sections[i]));
            Log.d("FLAG", "Bitmap dimensions for part " + (i + 1) + ": " + bitmap.getWidth() + "x" + bitmap.getHeight());

            y += sections[i];
            String filename = "part" + (i + 1) + ".png";
            saveBitmapToFile(bitmap, filename);

            ImageView imageView = (ImageView) findViewById(getImageViewIdForPart(i));
            if (imageView != null) {
                imageView.setImageBitmap(loadImage(filename));
            } else {
                Log.e("FLAG", "Error: ImageView not found for part " + (i + 1));
            }
        }
    }

    public Bitmap getBitmapFromView(View paramView) {
        Bitmap bitmap = Bitmap.createBitmap(paramView.getWidth(), paramView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable drawable = paramView.getBackground();
        if (drawable != null) {
            drawable.draw(canvas);
        } else {
            canvas.drawColor(-1);
        }
        paramView.draw(canvas);
        return bitmap;
    }

    public void handleImageSplit() {
        splitAndSaveBitmap(((BitmapDrawable)((ImageView)findViewById(R.id.imageForCut)).getDrawable()).getBitmap());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            // Now you can use the Uri to load the image or process it.
        }
        if (isImageSelectedSuccessfully(requestCode, resultCode, data)) {
            this.selectedImageUri = data.getData();
            displaySelectedImage(this.selectedImageUri);
        }

    }

    private boolean isImageSelectedSuccessfully(int requestCode, int resultCode, Intent data) {
        return requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null;
    }

    private void displaySelectedImage(Uri imageUri) {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));

            Bitmap scaledBitmap = scaleBitmapToScreenWidth(originalBitmap);

            int rotationAngle = getRotationAngle(imageUri);
            if (rotationAngle != 0) {
                scaledBitmap = rotateBitmap(scaledBitmap, rotationAngle);
            }

            PhotoView photoView = findViewById(R.id.imageForCut);  // replace 2131296618 with the actual ID
            photoView.setImageBitmap(scaledBitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Bitmap scaleBitmapToScreenWidth(Bitmap originalBitmap) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        float aspectRatio = (float) originalHeight / originalWidth;

        int newHeight = Math.round(screenWidth * aspectRatio);
        return Bitmap.createScaledBitmap(originalBitmap, screenWidth, newHeight, true);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationAngle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void onChooseImageButtonClicked() {
        // Logic for when choose image button is clicked
        // Implement this if needed, or replace with your actual callback logic
        pickImageFromGallery();
    }
    private void onCutIntoFourButtonClicked() {
        // Logic for when cut into four button is clicked
        // Implement this if needed, or replace with your actual callback logic
        handleImageSplit();

    }

    private class MatrixValues {
        float scaleX;
        float scaleY;
        float transX;
        float transY;

        private MatrixValues() {}
    }

}
