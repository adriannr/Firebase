package com.example.firebase;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText mResultEt;
    //EditText mResultEtTranslate;
    ImageView mPreviewIv;
    Button mSaveOcrBtn;
    private MenuAdapter mMenuAdapter;
    private ListView mMenuListView;
    private ChildEventListener mChildEventListener;



    private static final String TAG = "MainActivity";

    public static final int RC_SIGN_IN = 1;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;
    public static final String ANONYMOUS = "anonymous";


    String cameraPermission[];
    String storagePermission[];

    private String mUsername;


    Uri image_uri;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMenuesDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mMenuPhotosStorageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();

        mResultEt = findViewById(R.id.resultEt);
        //mResultEtTranslate = findViewById(R.id.resultEtTranslate);
        mPreviewIv = findViewById(R.id.imageIv);
        mSaveOcrBtn = findViewById(R.id.save_ocr_btn);
        mMenuListView = (ListView) findViewById(R.id.messageListView);


        List<MenuModel> menues = new ArrayList<>();
        mMenuAdapter = new MenuAdapter(this, R.layout.item_menu, menues);
        mMenuListView.setAdapter(mMenuAdapter);

        cameraPermission = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user!=null){
                    //user is signed in
                    onSignedInInitialize(user.getDisplayName());
                }
                else {
                    //user is signed out
                    onSignedInCleanup();
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    // Create and launch sign-in intent
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }

            }
        };

        // Firebase Database & Storage
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mMenuesDatabaseReference = mFirebaseDatabase.getReference().child(
                "menues");
        mMenuPhotosStorageReference = mFirebaseStorage.getReference().child(
                "menu_photos");

        mSaveOcrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    private void onSignedInCleanup() {
        mUsername = ANONYMOUS;
        mMenuAdapter.clear();

        detachDatabaseReadListener();

    }

    private void detachDatabaseReadListener(){
        if (mChildEventListener!=null){
            mMenuesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }
    private void onSignedInInitialize(String username) {
        mUsername = username;

        attachDatabaseReadListener();

    }

    private void attachDatabaseReadListener(){
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    MenuModel friendlyMessage =
                            snapshot.getValue(MenuModel.class);
                    mMenuAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            mMenuesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        //detachDatabaseReadListener();
        mMenuAdapter.clear();
    };

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //handle actionbar item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.addImage) {
            showImageImportDialog();
        }
        if (id == R.id.sign_out_menu) {
            AuthUI.getInstance().signOut(MainActivity.this);
            return true;        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        String[] items = {"Camera", "Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder((this));
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    //camera option clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        //permission allowed, take picture
                        pickCamera();
                    }
                }
                if (which == 1) {
                    //gallery option clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        //permission allowed, take picture
                        pickStorage();
                    }
                }
            }
        });
        dialog.create().show(); //show dialog
    }

    private void pickCamera() {
        //intent to pick image from camera and save to storage for high quality

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic");//picture title
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To Text");
        //picture description
        image_uri =
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickStorage() {
        //intent to pick from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }


    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return result;

    }

    private boolean checkCameraPermission() {
        /*Check camera permission and return result
        In order to get high quality image we have to save image to external storage first
        before inserting to image view hence storage permission check
         */
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return result && result1;
    }

    private void processDocumentTextBlock(FirebaseVisionDocumentText result) {
        // [START mlkit_process_document_text_block]
        String resultText = result.getText();
        for (FirebaseVisionDocumentText.Block block : result.getBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockRecognizedLanguages = block.getRecognizedLanguages();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionDocumentText.Paragraph paragraph : block.getParagraphs()) {
                String paragraphText = paragraph.getText();
                Float paragraphConfidence = paragraph.getConfidence();
                List<RecognizedLanguage> paragraphRecognizedLanguages = paragraph.getRecognizedLanguages();
                Rect paragraphFrame = paragraph.getBoundingBox();
                for (FirebaseVisionDocumentText.Word word : paragraph.getWords()) {
                    String wordText = word.getText();
                    Float wordConfidence = word.getConfidence();
                    List<RecognizedLanguage> wordRecognizedLanguages = word.getRecognizedLanguages();
                    Rect wordFrame = word.getBoundingBox();
                    for (FirebaseVisionDocumentText.Symbol symbol : word.getSymbols()) {
                        String symbolText = symbol.getText();
                        Float symbolConfidence = symbol.getConfidence();
                        List<RecognizedLanguage> symbolRecognizedLanguages = symbol.getRecognizedLanguages();
                        Rect symbolFrame = symbol.getBoundingBox();
                    }
                }
            }
        }
    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraAccepted =
                            grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted =
                            grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }
                    else {
                        Toast.makeText(this,"permission denied",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length>0){

                    boolean writeStorageAccepted =
                            grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickStorage();
                    }
                    else {
                        Toast.makeText(this,"permission denied",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

    }


    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //got image from gallery -> crop

                CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON).start(this); //enable guidlines
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //got image from camera -> crop
                CropImage.activity(image_uri).setGuidelines(CropImageView.Guidelines.ON).start(this); //enable guidlines
            }
        }
        //get cropped image

        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                final Uri resultUri = result.getUri(); //get image uri
                // set image to image view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable =
                        (BitmapDrawable)mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                        .getCloudDocumentTextRecognizer();

                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText result) {
                                // Task completed successfully
                                // ...
                                final String originalText = result.getText();
                                mResultEt.setText(originalText);

                                /* TODO add translator back
                                TranslatorOptions options =
                                        new TranslatorOptions.Builder()
                                                .setSourceLanguage(TranslateLanguage.SPANISH)
                                                .setTargetLanguage(TranslateLanguage.ENGLISH)
                                                .build();
                                final Translator translator =
                                        Translation.getClient(options);
                                DownloadConditions conditions = new DownloadConditions.Builder()
                                        .requireWifi()
                                        .build();
                                translator.downloadModelIfNeeded(conditions)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void v) {
                                                        // Model downloaded successfully. Okay to start translating.
                                                        // (Set a flag, unhide the translation UI, etc.)
                                                        translator.translate(originalText)
                                                                .addOnSuccessListener(
                                                                        new OnSuccessListener<String>() {
                                                                            @Override
                                                                            public void onSuccess(@NonNull String translatedText) {
                                                                                // Translation successful.
                                                                                mResultEtTranslate.setText(translatedText);
                                                                            }
                                                                        })
                                                                .addOnFailureListener(
                                                                        new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                // Error.
                                                                                e.printStackTrace();
                                                                            }
                                                                        });
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Model couldn’t be downloaded or other internal error.
                                                        e.printStackTrace();
                                                    }
                                                });*/


                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                                // Task failed with an exception
                                // ...
                            }
                        });


                /*TextRecognizer recognizer =
                        new TextRecognizer.Builder(getApplicationContext()).build();

                if(!recognizer.isOperational()){
                    Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
                }
                else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    //get text from sb until there is no text
                    for (int i = 0; i<items.size(); i++){
                        TextBlock myItem = items.valueAt(i);
                        Log.d(TAG,items.valueAt(i).getValue());
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    mResultEt.setText(sb.toString());

                }*/
                mSaveOcrBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final StorageReference photoRef =
                                mMenuPhotosStorageReference.child(resultUri.getLastPathSegment());

                        UploadTask uploadTask = photoRef.putFile(resultUri);
                        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                // Continue with the task to get the download URL
                                return photoRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloadUri = task.getResult();
                                    MenuModel menuItem =
                                            new MenuModel(mResultEt.getText().toString(),
                                                    downloadUri.toString(),
                                                    mUsername);
                                    mMenuesDatabaseReference.push().setValue(menuItem);

                                } else {
                                    // Handle failures
                                    // ...
                                }
                            }
                        });

                    }
                });

            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //show error if exists
                Exception error = result.getError();
                Toast.makeText(this,""+error, Toast.LENGTH_SHORT).show();
            }
        }
    }

}