package com.qrandbarcode.app;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialButton cameraBtn;
    private MaterialButton galleryBtn;
    private ImageView imageIv;

    private MaterialButton scanBtn;

    private TextView resultTv;

    //to handle the result of camera/gallery permissions in onRequestPermissionResults
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //arrays of permissions required to pick image from camera/gallery
    private String[] cameraPermissions;
    private String[] storagePermission;
    //Uri of the image that we will take from camera/gallery
    private Uri imageUri = null;

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;
    private static final String TAG = "MAIN_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI views
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        imageIv = findViewById(R.id.imageIv);
        scanBtn = findViewById(R.id.scanBtn);
        resultTv = findViewById(R.id.resultTv);


        //init the arrays of permissions requied to pick image from Camera/Gallery
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        //handle cameraBtn click check permission related to camera (i.e. WRITE STORAGE & CAMERA) and take image from Camera
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermissions()) {
                    //permission required for camera already granted launch gallery intent
                    pickImageCamera();
                } else {
                    //permission required for camera was not already granted request permissions
                    requestCameraPermission();
                }
            }
        });

        //handle galleryBtn click check permission related to gallery (i.e. WRITE STORAGE) and take image from Camera
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkStoragePermissions()) {
                    //permission required for gallery already granted launch gallery intent
                    pickImageGallery();
                } else {
                    //permission required for gallery was not already granted request permissions
                    requestStoragePermission();
                }
            }
        });

        //handle scanBtn click scan the Barcode/QR Code from picked from image picked from Camera/Gallery
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageUri == null) {
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                } else {
                    detectResultFromImage();
                }
            }
        });
    }

    private void detectResultFromImage() {
        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            //Task completed successfully we can get detailed info now
                            extractBarCodeQRCodeInfo(barcodes);
                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Task failed with an exception we cant get any detail
                            Toast.makeText(MainActivity.this, "Failed scanning due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            //failed with an exception either due to preparing InputImage or issue in BarcodeScanner init
            Toast.makeText(this, "Failed due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarCodeQRCodeInfo(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            Log.d(TAG, "extractBarCodeQRCodeInfo: rawValue: " + rawValue);


            int valueType = barcode.getValueType();

            //manage each type separately i'll do the four types to save time but you will able to manage all
            switch (valueType) {
                case Barcode.TYPE_WIFI: {
                    Barcode.WiFi typeWifi = barcode.getWifi();
                    String ssid = "" + typeWifi.getSsid();
                    String password = "" + typeWifi.getPassword();
                    String encryptionType = "" + typeWifi.getEncryptionType();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_WIFI: ");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: ssid: " + ssid);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: password: " + password);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: encryptionType: " + encryptionType);

                    resultTv.setText("TYPE: TYPE_WIFI \nssid: " + ssid + "\npassword: " + password + "\nencryptionType: " + encryptionType + "\nraw value: " + rawValue);
                }
                break;
                case Barcode.TYPE_URL: {
                    Barcode.UrlBookmark typeUrl = barcode.getUrl();

                    String title = typeUrl.getTitle();
                    String url = typeUrl.getUrl();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_WIFI: ");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: title: " + title);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: url: " + url);

                    resultTv.setText("TYPE: TYPE_URL \ntitle: " + title + "\nurl: " + url + "\nraw value: " + rawValue);
                }
                break;
                case Barcode.TYPE_EMAIL: {
                    Barcode.Email typeEmail = barcode.getEmail();

                    String address = "" + typeEmail.getAddress();
                    String body = "" + typeEmail.getBody();
                    String subject = "" + typeEmail.getSubject();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_EMAIL");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: address: " + address);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: body: " + body);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: subject: " + subject);

                    resultTv.setText("TYPE: TYPE_EMAIL \naddress: " + address + "\nbody: " + body + "\nsubject: " + subject + "\nraw value: " + rawValue);
                }
                break;
                case Barcode.TYPE_CONTACT_INFO: {
                    Barcode.ContactInfo typeContact = barcode.getContactInfo();

                    String title = "" + typeContact.getTitle();
                    String organizer = "" + typeContact.getOrganization();
                    String name = "" + typeContact.getName().getFirst() + "" + typeContact.getName().getLast();
                    String phone = "" + typeContact.getPhones().get(0).getNumber();

                    Log.d(TAG, "extractBarCodeQRCodeInfo: TYPE_CONTACT_INFO");
                    Log.d(TAG, "extractBarCodeQRCodeInfo: title: " + title);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: organizer: " + organizer);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: name: " + name);
                    Log.d(TAG, "extractBarCodeQRCodeInfo: phone: " + phone);

                    resultTv.setText("TYPE: TYPE_CONTACT_INFO \ntitle: " + title + "\norganizer: " + organizer + "\nname: " + name + "\nphone: " + phone + "\nraw value: " + rawValue);
                }
                break;
                default:{
                    resultTv.setText("raw value: "+rawValue);
                }
            }
        }
    }

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will recvice the image if picked from gallery
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image picked get the uri of the image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //set to imageview
                        imageIv.setImageURI(imageUri);
                    } else {
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera() {

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will recive the image if taken from camera
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image is taken camera
                        Intent data = result.getData();
                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //set to imageview
                        imageIv.setImageURI(imageUri);
                    } else {
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermissions() {
        //check if storage permission is allowed or not
        //return true if allowed false if not allowed
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        //return result true if permission WRITE_EXTERNAL_STORAGE granted or false if denied
        return result;
    }

    private void requestStoragePermission() {
        //request storage permission (for gallery image pick)
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions() {
        //check if camera permission allowed, true if yes false if no
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        //check if storage permission allowed, true if yes false if no
        boolean resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        //return both result as true or false
        return resultCamera && resultStorage;
    }

    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                //Check if some action from permission dialog performed or not Allow/Deny
                if (grantResults.length > 0) {
                    //Check if Camera , Storage permissions granted, contains boolean result either true or false
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    //check if both permissions are granted or not
                    if (cameraAccepted && storageAccepted) {
                        //both permissions Camera & Gallery are granted , we can launch camera intent
                        pickImageCamera();
                    } else {
                        //one or both permissions are denied can't launch camera intent
                        Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                //Check if some action from permission dialog performed or not Allow/Deny
                if (grantResults.length > 0) {
                    //Check if Storage permissions granted, contains boolean result either true or false
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    //check if both permissions are granted or not
                    if (storageAccepted) {
                        //storage permissions granted , we can launch camera intent
                        pickImageGallery();
                    } else {
                        //storage permissions are denied can't launch camera intent
                        Toast.makeText(this, "Storage permission is required...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}