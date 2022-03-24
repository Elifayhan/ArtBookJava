package com.elifayhan.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.elifayhan.artbookjava.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    /*
    Yeni bir aktivite açıp galeriye vb. yerlere gidiğ oradan bir şey alıp onu ele mı almak istiyoruz yani görsel seçince ne olacağını yazmak mı istiyoruz
    ya da bir izin isteyip o izin verildiğinde ne olacağını yazmak istiyorsak bu gibi durumların hepsinde ActivityResultLauncher kullanılır.
    bu  yapıların şöyle bir özelliği var: bir şeyi yaptıktan sonra cevabına göre işlem yapabiliyoruz.
    İzin isteyeceğiz, izin verilirse ne yapacağız ? galeriye gideceğiz, kullanıcı bir fotoğraf seçtiğinde ne yapacağız gibi.
     */
    ActivityResultLauncher<Intent> activityResultLauncher; //galeri için
    ActivityResultLauncher<String> permissionLauncher; //izin için
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();
        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
        Intent intent = getIntent();
        String info = intent.getStringExtra("info"); //bu infoda ya old ya da new gelecek.
        //kullanıcının menuye tıkladıktan sonra mı yoksa recyclerviewa tıkladıktan sonra mı
        //diğer ekrana geçtiğini anlamaya çalışıyoruz.
        if(info.equals("new")){
            //yeni bir art yolladı demek.
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.image);

        }else {
            int artId = intent.getIntExtra("artId",1);
            binding.button.setVisibility(View.INVISIBLE);
            try{

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while(cursor.moveToNext()){

                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));
                    byte [] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);

                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }




    }



    public void save (View view){
        String name= binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();
        Bitmap smallImage = makeSmallerImage(selectedImage,300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50, outputStream);
        byte [] byteArray = outputStream.toByteArray();
        //database'i initialize edeceğiz:
        try{

            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB )");
            //değerleri girerken direkt olarak değer vermeyeceğimiz için şöyle yaptık:
            String  sqlString = "INSERT INTO arts(artname, paintername, year, image) VALUES(?, ?, ?, ?)";
            //Bağlama işlemini yapacağız:
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name); //indexler dizilerde olduğu gibi 0 dan başlamıyor.
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        //kaydı yaptıktan sonra MainActivitye geçmek için:
        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        //ve bundan önce ne kadar aktivite varsa kapat diyeceğiz:
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    //görseli veritabanına kaydetmeden önce görselin boyutunu küçültmeliyiz.
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();
        //görselin dikey mi yoksa yatak  ı olduğuna karar vermeliyiz.
        float bitmapRatio = (float) width/ (float) height;
        if(bitmapRatio>1){
            //yatay görsel demektir bu
            width = maximumSize;
            height = (int)(width/height);
        }
        else{
            //dikey görsel demektir bu.
            height = maximumSize;
            width = (int) (height*bitmapRatio);
        }
        return image.createScaledBitmap(image, width,height,true);
    }
    public void selectImage(View view){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //burada read external storage izni var mı yok mu diye kontrol ediyoruz. eğer izin verilmediyse izin isteyeceğiz.
            //ama öncesinde kullanıcı izin vermezse neden bu izni istiyoruz daha açık bir şekilde gösterelim istedik.
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                //Snackbarı, Toast mesajının üstüne tıklanabilir hali gibi düşünebilirsin.
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //tıklanınca izin isteyeceğiz.
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();

            }else{
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

        }
        else{
            //burada demek ki izin verilmiş, dolayısıyla galeriye gideceğiz.
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //Intentler ike sadece aktiviteler arası geçiş yapmakla kalmayıp bazı aksiyonlar da yapılabiliyor.
            activityResultLauncher.launch(intentToGallery);
        }
    }

    private void registerLauncher(){
        /*
        galeriye gitmek için olan launcherı yazalım.
         */
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                /*
                kullanıcının galerisine gittik amam kullanıcı galeriden bir şey seçmemiş olabilir, bunun kontrol edilmesi lazım.

                 */
                if(result.getResultCode()==RESULT_OK){
                    //kullanıcı bir şey seçtiyse
                    Intent intentFromResult=result.getData();
                    //bu intentFromResult boş olabilir. onun kontrol edilmesi lazım.
                    if(intentFromResult!=null){
                        Uri imageData = intentFromResult.getData(); //bununla birlikte kullanıcının seçtiği datanın yerini biliyoruz.
                        try {
                            if(Build.VERSION.SDK_INT>=28){
                                ImageDecoder.Source source= ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                            else{
                                selectedImage=MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

            }
        });
        /*
        ActivityResultLauncher'ın ne yapacağını burada tanımlayıp ondan sonra onCreate altında register yapmamız yani çağırmamız gerekiyor.
        OnCreate altında çağırmak demek izin istemek ya da galeriye git demek değildir, sadece bunların neler yapacağını en baştan tanımlamak demektir.

         */
        //önce izin isteme işlemini görelim
        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //izin verilirse galeriye git.
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);

                }
                else{
                    //izin verilmezse toast mesajı göster.
                    Toast.makeText(ArtActivity.this,"Permission needed!",Toast.LENGTH_LONG).show();
                }

            }
        });

    }
}