package com.example.audioplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.Environment.getExternalStorageDirectory;


public class PlayerActivity extends AppCompatActivity {
    public Button playbtn, nextbtn, prevbtn, forwbtn, revbtn, loopbtn, timerbtn;
    TextView txtsname, txtsstart, txtsstop, txtsartist;
    SeekBar seekmusic;

    private NotificationCompat.Builder notificationBuilder;
    private static MediaSessionCompat mediaSessionCompat;
    private PlaybackStateCompat.Builder stateBuilder;
    private NotificationManager notificationManager;
    private String CHANNEL_ID = "channelId";

    String sname;
    public static final String EXTRA_NAME = "song_name";
    static MediaPlayer mediaPlayer;
    int position;
    static int oldPosition = -1;
    ImageView imageView;
    ArrayList<File> mySongs;
    ArrayList<ArrayList<String>> list2 = new ArrayList<ArrayList<String>>();

    private static final int NOTIFY_ID = 101;
    RemoteViews remoteViews;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        CreateNotificationChannal();
        cretMadiaSession();

        playbtn = findViewById(R.id.playbtn);
        nextbtn = findViewById(R.id.nextbtn);
        prevbtn = findViewById(R.id.prevbtn);
        loopbtn = findViewById(R.id.loopbtn);
        timerbtn = findViewById(R.id.timerbtn);
        txtsname = findViewById(R.id.txtsname);
        txtsartist = findViewById(R.id.txtsartist);
        txtsstart = findViewById(R.id.txtsstart);
        txtsstop =  findViewById(R.id.txtsstop);
        seekmusic = findViewById(R.id.seekbar);
        imageView = findViewById(R.id.imgView);
        Thread updateseekbar;

        // анимации вращения пластинки
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(10000);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setRepeatMode(Animation.RESTART);
        rotate.setInterpolator(new LinearInterpolator());

        RotateAnimation rotateBack = new RotateAnimation(180,360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateBack.setDuration(500);
        rotateBack.setInterpolator(new LinearInterpolator());
        
        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        mySongs = (ArrayList) bundle.getParcelableArrayList("fils");
        String songName = i.getStringExtra("songname");
        list2 = (ArrayList) bundle.getParcelableArrayList("list");
        position = bundle.getInt("pos", 0);
        txtsname.setSelected(true);

        if( position != oldPosition) {
            Uri uri = Uri.parse(mySongs.get(position).getPath());

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = MediaPlayer.create(this, uri);
            mediaPlayer.start();
        }

        imageView.startAnimation(rotate);

        txtsname.setText(list2.get(position).get(0));
        txtsartist.setText(list2.get(position).get(1));

            updateseekbar = new Thread() {
                @Override
                public void run() {
                    int totalDiretion = mediaPlayer.getDuration();
                    int currentposition = 0;

                    while (currentposition < totalDiretion) {
                        try {
                            sleep(500);
                            currentposition = mediaPlayer.getCurrentPosition();
                            seekmusic.setProgress(currentposition);
                        } catch (InterruptedException | IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            seekmusic.setMax(mediaPlayer.getDuration());
            updateseekbar.start();

            seekmusic.getProgressDrawable().setColorFilter(getResources().getColor(R.color.teal_200), PorterDuff.Mode.MULTIPLY);
            seekmusic.getThumb().setColorFilter(getResources().getColor(R.color.teal_200), PorterDuff.Mode.SRC_IN);

            seekmusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            });

            String endTime = createTime(mediaPlayer.getDuration());
            txtsstop.setText(endTime);

            final Handler handler = new Handler();
            final int delay = 1000;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String currenttime = createTime(mediaPlayer.getCurrentPosition());
                    txtsstart.setText(currenttime);
                    handler.postDelayed(this, delay);
                }
            }, delay);
        oldPosition = position;


        showNotification();


        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying())
                {
                    playbtn.setBackgroundResource(R.drawable.ic_play);
                    mediaPlayer.pause();
                    imageView.startAnimation(rotateBack);
                }
                else
                {
                    playbtn.setBackgroundResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                    imageView.startAnimation(rotate);
                }

            }
        });
        nextbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = ((position+1)%mySongs.size());
                oldPosition = position;
                Uri u = Uri.parse(mySongs.get(position).getPath());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                txtsname.setText(list2.get(position).get(0));
                txtsartist.setText(list2.get(position).get(1));
                mediaPlayer.start();
                playbtn.setBackgroundResource(R.drawable.ic_pause);
                String endTime = createTime(mediaPlayer.getDuration());
                txtsstop.setText(endTime);
                showNotification();
            }
        });
        prevbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = ((position-1)<0)?(mySongs.size()-1):(position-1);
                oldPosition = position;
                Uri u = Uri.parse(mySongs.get(position).getPath());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                txtsname.setText(list2.get(position).get(0));
                txtsartist.setText(list2.get(position).get(1));
                mediaPlayer.start();
                playbtn.setBackgroundResource(R.drawable.ic_pause);
                showNotification();
            }
        });

        loopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isLooping())
                {
                    mediaPlayer.setLooping(false);
                    Toast.makeText(getApplicationContext(), "Повтор выключен", Toast.LENGTH_SHORT).show();

                } else{
                    mediaPlayer.setLooping(true);
                    Toast.makeText(getApplicationContext(), "Повтор включен", Toast.LENGTH_SHORT).show();
                }
            }
        });

        timerbtn.setOnClickListener(new View.OnClickListener() {
            private myTimerTask myTimerTask;

            @Override
            public void onClick(View v) {
                Timer timer = new Timer();
                myTimerTask timerTask = new myTimerTask();
                timer.schedule(timerTask, 60000);
                Toast.makeText(getApplicationContext(), "Песня выключится через минуту", Toast.LENGTH_SHORT).show();
            }
        });


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextbtn.callOnClick();
            }
        });
    }
    public String createTime(int duration)
    {
        String time = "";
        int min = duration/1000/60;
        int sec = duration/1000%60;


        time+=min+":";

        if(sec<10)
        {
            time+="0";
        }
        time+=sec;

        return time;
    }
    class myTimerTask extends TimerTask{
        @Override
        public void run(){
            if(mediaPlayer.isPlaying())
            playbtn.callOnClick();
        }
    }

    private void createChannelIfNeeded(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    private void CreateNotificationChannal()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT

            );
            channel.enableVibration(false);
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void cretMadiaSession(){
        mediaSessionCompat = new MediaSessionCompat(this, "simplplaye sessin");
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                );
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setPlaybackState(stateBuilder.build());
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();

                playbtn.callOnClick();
                showNotification();
            }

            @Override
            public void onPause() {
                super.onPause();

                playbtn.callOnClick();
                showNotification();
            }
            @Override
            public void onSkipToNext() {
                super.onSkipToNext();

                nextbtn.callOnClick();
                showNotification();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();

                prevbtn.callOnClick();
                showNotification();
            }

        });

        mediaSessionCompat.setActive(true);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showNotification(){
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        int icon;
        String playPause;
        if(mediaPlayer.isPlaying()){
            icon = R.drawable.ic_pause;
            playPause = "Pause ";
        } else {
            icon = R.drawable.ic_play;
            playPause = "Play ";
        }

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                icon, playPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE)
        );
        NotificationCompat.Action nextAction = new NotificationCompat.Action(
                R.drawable.ic_previous, playPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        );
        NotificationCompat.Action prevAction = new NotificationCompat.Action(
                R.drawable.ic_next, playPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        );
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this,
                0, new Intent(this, PlayerActivity.class), 0);

        int id = (int) System.currentTimeMillis();
        Intent play_intent = new Intent("play_clicked");
        play_intent.putExtra("id", id);
//        play_intent.setAction()

        PendingIntent p_play_intent = PendingIntent.getBroadcast(PlayerActivity.this, 123, play_intent, 0);
        remoteViews = new RemoteViews(getPackageName(), R.layout.not_item);
        remoteViews.setOnClickPendingIntent(R.id.playbtn2,p_play_intent);
        remoteViews.setTextViewText(R.id.txtSongName,list2.get(position).get(0));
        remoteViews.setTextViewText(R.id.txtSongArtist,list2.get(position).get(1));



        notificationBuilder.setContentTitle(list2.get(position).get(0))
                .setContentText(list2.get(position).get(1))
                .setSmallIcon(R.drawable.diskyre)
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(nextAction)
                .addAction(playPauseAction)
                .addAction(prevAction)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken())
                .setShowActionsInCompactView(0))
                .setAutoCancel(false);
        notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());

    }

    public static class MyReciver extends BroadcastReceiver{

        public MyReciver(){

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        }
    }

    public static class Play_listner extends BroadcastReceiver
    {


        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(intent.getExtras().getInt("id"));
        }
    }
}