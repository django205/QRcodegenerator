package com.yogeshkumar.qrcodegenerator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG=MainActivity.class.getSimpleName();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    public static final int RC_SIGN_IN=1;
    ImageView imageView;
    Button button,scan;
    EditText editText;
    String EditTextValue ;
    //Thread thread ;
    public final static int QRcodeWidth = 500 ;
    Bitmap bitmap ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        mStorage=FirebaseStorage.getInstance();
        myRef=database.getReference().child("students");
        mStorageRef=mStorage.getReference().child("QRcodes");
        myRef.setValue("hello world!");
        imageView = (ImageView)findViewById(R.id.imageView);
        editText = (EditText)findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button);
        scan=(Button)findViewById(R.id.scan);


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,scanActivity.class);
                startActivity(i);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditTextValue = editText.getText().toString();

                myRef.push().setValue(EditTextValue);
                try {
                    bitmap = TextToImageEncode(EditTextValue);

                    imageView.setImageBitmap(bitmap);

                    imageView.setDrawingCacheEnabled(true);
                    imageView.buildDrawingCache();
                    //Bitmap bitmap1=imageView.getDrawingCache();
                    ByteArrayOutputStream baos=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
                    byte[] data=baos.toByteArray();
                    String path= MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(),bitmap,"Title",null);
                    Uri imageuri=Uri.parse(path);

                  StorageReference qrcoderef=mStorageRef.child(imageuri.getLastPathSegment() );

                    UploadTask uploadTask=qrcoderef.putBytes(data);
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                           // Uri downloadUrl= taskSnapshot.getDownloadUrl();
                        }
                    });
                } catch (WriterException e) {
                    e.printStackTrace();
                }

            }
        });

mAuthStateListener=new FirebaseAuth.AuthStateListener() {
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if(user!=null){
            Log.e(LOG_TAG,"onauthstate changed:signed in"+user.getUid());
        }
        else{
            Log.e(LOG_TAG,"on authstate changed:sinngned out");

startActivityForResult(
        AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                        new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                .build(),
        RC_SIGN_IN);

        }
    }
};
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==RC_SIGN_IN){
            if(resultCode==RESULT_OK){
                Toast.makeText(this, "signed in", Toast.LENGTH_SHORT).show();
            }else if(resultCode==RESULT_CANCELED){
                Toast.makeText(this, "sign cancelled", Toast.LENGTH_SHORT).show();
            finish();
            }
        }
    }

    // method that convert text into QRcode
    Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.QRCodeBlackColor):getResources().getColor(R.color.QRCodeWhiteColor);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
                inflater.inflate(R.menu.menu_main,menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:AuthUI.getInstance().signOut(this);
      default:  return super.onOptionsItemSelected(item);

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

    if(mAuthStateListener!=null){
        mAuth.removeAuthStateListener(mAuthStateListener);
    }
    }
}

