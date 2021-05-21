package com.example.audioplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.voice.VoiceInteractionSession;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private static final int NOTIFY_ID = 101;
    private String CHANNEL_ID = "Player channel";
    NotificationManager notificationManager ;

    EditText editText;
    ImageButton searchbtn, acceptbtn;
    int statussearchbtn = 0, statussearchaccept = 0 ;
    String searchString;

    ArrayList<String[]> list = new ArrayList<>();
    ArrayList<ArrayList<String>> list2 = new ArrayList<ArrayList<String>>();
    ArrayList<String> arrayList;
    ArrayList<File> arrayListFile = new ArrayList<>();
    ArrayList<ArrayList<String>> searchlist2 = new ArrayList<ArrayList<String>>();
    ArrayList<File> searcharrayListFile = new ArrayList<>();

    ListView listView;

    ArrayAdapter<String> adapter;


    public boolean isAlpha(String name) {
        name.replace("_", "");
        return name.matches("[a-zA-Z]+");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getSupportActionBar().setCustomView(R.layout.title_main_bar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        editText = findViewById(R.id.txtedit);
        editText.setVisibility(View.INVISIBLE);
        acceptbtn = findViewById(R.id.acceptserch);
        acceptbtn.setBackgroundResource(R.drawable.ic_loop);
        searchbtn = findViewById(R.id.searchbtn);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
        searchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(statussearchbtn == 0)
                {
                    editText.setVisibility(View.VISIBLE);
                    acceptbtn.setBackgroundResource(R.drawable.ic_check_box);
                    statussearchbtn = 1;
                    statussearchaccept = 1;
                } else if(statussearchbtn == 1)
                {
                    editText.setVisibility(View.INVISIBLE);
                    acceptbtn.setBackgroundResource(R.drawable.ic_loop);
                    statussearchaccept = 0;
                    statussearchbtn = 0;
                }
            }
        });

        acceptbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( statussearchaccept == 1)
                {
                    searchString = editText.getText().toString();
                    searchlist2.clear();
                    searcharrayListFile.clear();
                    SearchSong();
                    listView = (ListView) findViewById(R.id.listView);
                    arrayList = new ArrayList<>();

                    customAdapter2 customAdapter2 = new customAdapter2();
                    listView.setAdapter(customAdapter2);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String songName = (String) listView.getItemAtPosition(position);
                            startActivity(new Intent(getApplicationContext(),PlayerActivity.class)
                                    .putExtra("songs", arrayList)
                                    .putExtra("list", searchlist2)
                                    .putExtra("songname", songName)
                                    .putExtra("pos", position)
                                    .putExtra("fils", searcharrayListFile));

                        }
                    });
                    acceptbtn.setBackgroundResource(R.drawable.ic_loop);
                    editText.setVisibility(View.INVISIBLE);
                    statussearchaccept = 0;
                } else if (statussearchaccept == 0) {
                    list2.clear();
                    arrayListFile.clear();
                    doStuff();
                }

            }
        });


        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION_REQUEST);
            }
        } else {
            doStuff();
        }

    }

    public void doStuff(){
        listView = (ListView) findViewById(R.id.listView);
        arrayList = new ArrayList<>();
        getMusic();

        customAdapter customAdapter = new customAdapter();
        listView.setAdapter(customAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getApplicationContext(), arrayListFile.get(position).getName(), Toast.LENGTH_LONG).show();
                String songName = (String) listView.getItemAtPosition(position);
                startActivity(new Intent(getApplicationContext(),PlayerActivity.class)
                .putExtra("songs", arrayList)
                .putExtra("list", list2)
                .putExtra("songname", songName)
                .putExtra("pos", position)
                .putExtra("fils", arrayListFile));


            }
        });
    }

    public void getMusic(){
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri,null, null, null, null);

        if( songCursor != null && songCursor.moveToFirst()){
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            int i = 0;
            do {
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                int column_index = songCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String FileName = songCursor.getString(column_index);
                File newFile = new File(FileName);
                ArrayList<String> arr = new ArrayList<String>();
                arr.add(currentTitle);
                arr.add(currentArtist);
                list2.add(arr);
                arrayList.add(currentTitle + "\n" + currentArtist);
                arrayListFile.add(newFile);

            } while (songCursor.moveToNext());
        }
    }
    // не трогать
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case  MY_PERMISSION_REQUEST: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();

                        doStuff();
                    }
                } else {
                    Toast.makeText(this, "NO Permission granted", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
        }
    }

    class customAdapter extends BaseAdapter
    {

        @Override
        public int getCount() {
            return list2.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textSong = myView.findViewById(R.id.txtsongname);
            TextView textSongArtist = myView.findViewById(R.id.txtsongnartist);
            textSong.setSelected(true);
            textSong.setText(list2.get(position).get(0));
            textSong.setTextColor(getResources().getColor(R.color.teal_2002));
            textSongArtist.setSelected(true);
            textSongArtist.setText(list2.get(position).get(1));
            textSongArtist.setTextColor(getResources().getColor(R.color.teal_2002));

            return myView;
        }
    }
    private void createChannelIfNeeded(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    private void SearchSong(){
//        int j=0;
        for(int i = 0; i<list2.size(); i++)
        {
//            Toast.makeText(this, (list2.get(i).get(0) + " " + list2.get(i).get(0)), Toast.LENGTH_LONG).show();
            if((list2.get(i).get(0) + " " + list2.get(i).get(1)).indexOf(searchString) != -1)
            {
                searchlist2.add(list2.get(i));
                searcharrayListFile.add(arrayListFile.get(i));
            }
        }

    }
    class customAdapter2 extends BaseAdapter
    {

        @Override
        public int getCount() {
            return searchlist2.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textSong = myView.findViewById(R.id.txtsongname);
            TextView textSongArtist = myView.findViewById(R.id.txtsongnartist);
            textSong.setSelected(true);
            textSong.setText(searchlist2.get(position).get(0));
            textSong.setTextColor(getResources().getColor(R.color.teal_2002));
            textSongArtist.setSelected(true);
            textSongArtist.setText(searchlist2.get(position).get(1));
            textSongArtist.setTextColor(getResources().getColor(R.color.teal_2002));

            return myView;
        }
    }
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}