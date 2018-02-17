package pb.com.imagecropper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Uri mCropImageUri;
    private ImageView mPlaceHolderImageView;
    private ImageButton mSaveButton;
    private Bitmap bitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton GalleryButton = findViewById(R.id.GalleryButton);
        mPlaceHolderImageView = findViewById(R.id.placeHolderImage);
        mSaveButton = findViewById(R.id.saveButton);
        mSaveButton.setOnClickListener(this);

        mSaveButton.setEnabled(false);

        GalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
                CropImage.startPickImageActivity(MainActivity.this);

            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //handling the result of image chooser
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            mCropImageUri = imageUri;

            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
                }
            } else {
                // no permissions required or already granted, can start crop image activity
                startCropImageActivity(imageUri);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                CropImage.ActivityResult activityResult = CropImage.getActivityResult(data);
                Uri uri = activityResult.getUri();

                mPlaceHolderImageView.setImageURI(uri);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    mSaveButton.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
            if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // required permissions granted, start crop image activity
                startCropImageActivity(mCropImageUri);
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri)
                .setFixAspectRatio(true)
                .setAutoZoomEnabled(false)
                .setMinCropWindowSize(400,400)
                .start(this);
    }

    private void saveImage(Bitmap bitmap) {

        String savedImagePath = null;
        OutputStream outputStream = null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG" + timeStamp + ".jpg";

        File storageDir = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_PICTURES) + " CropPics");


        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
            Log.d("vivz", "success");
        }

        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Adding pics to gallery
            Intent mediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(savedImagePath);
            Uri uri = Uri.fromFile(f);
            mediaIntent.setData(uri);
            sendBroadcast(mediaIntent);

            Toast.makeText(this, "Image is saved ", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onClick(View view) {
        if (view == mSaveButton) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.save_the_image)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            saveImage(bitmap);
                            mSaveButton.setEnabled(false);
                            mPlaceHolderImageView.setImageResource(R.drawable.placeholder);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startCropImageActivity(mCropImageUri);
                        }
                    }).show();

        }

    }
}
