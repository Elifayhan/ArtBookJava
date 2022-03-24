package com.elifayhan.artbookjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.elifayhan.artbookjava.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    ArrayList<Art> artArrayList;
    ArtAdapter  artAdapter;
    //MainActivity içerisinde sadece recyclerview var ama ArtActivity içerisinde çok fazla findViewById yapılacak şey var.
    //o yüzden en mantıklısı binding kullanmak.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        artArrayList = new ArrayList<>();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);
        getData();


    }

    private void getData(){
        try{
            SQLiteDatabase sqLiteDatabase = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM arts", null);
            //hem id sini  hem de name ini çekeceğiz.
            int nameIx = cursor.getColumnIndex("artname");
            int idIx = cursor.getColumnIndex("id");

            while(cursor.moveToNext()){
                String name = cursor.getString(nameIx);
                int id = cursor.getInt(idIx);
                Art art = new Art(name,id);
                artArrayList.add(art);
            }
            artAdapter.notifyDataSetChanged();
            cursor.close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //OptionsMenu ye tıkladığımızda ne olacağını yazacağız burada.
        //yani burada bir binding işlemi yapacağız. menuyü layoutu, koda bağlayacağız.
        //bunun için inflater kullanıyordu. ama menu nün kendine has bir inflaterı var adı da MenuInflater:
        MenuInflater menuInflater=getMenuInflater(); //MenuInflater ın bir tane objesi var ve onu döndürüyor.
        //bunu kullanalım:
        menuInflater.inflate(R.menu.art_menu, menu);
        return super.onCreateOptionsMenu(menu);

        //artık menümüz aktivitemize bağlandı.
    }

    //Diğer bir overrite etmemiz gereken method ise bize menüye tıklanınca ne olacağını söylüyor.


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //birden fazla aktivitemiz olabilir. böyle bir durumda tek tek if else içerisinde o item seçildiğinde ne yapacağımızı
        //yazmamız gerekir. bizim şu anda bir itemımız var ve bu itema tıklandığında yeni aktiviteye geçiş sağlanıyor.
        //aktiviteler arası geçişi de Intent ile sağlıyorduk.
        if(item.getItemId()==R.id.add_art){
            Intent intent =new Intent(MainActivity.this, ArtActivity.class);
            intent.putExtra("info", "new");
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }
}