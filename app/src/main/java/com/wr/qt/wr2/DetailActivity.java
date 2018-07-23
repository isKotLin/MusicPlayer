package com.wr.qt.wr2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.vigorchip.WrMusic.wr2.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.vigorchip.WrMusic.wr2.R.id.btnStartStop;


public class DetailActivity extends Activity implements OnClickListener {


    private int i = 1;

    public static final String MUSIC_LENGTH = "DetailActivity.MUSIC_LENGTH";
    public static final String CURRENT_POSITION = "DetailActivity.CURRENT_POSITION";
    public static final String CURRENT_MUSIC = "DetailActivity.CURRENT_MUSIC";



    private SeekBar pbDuration;
    private TextView tvTitle, tvTimeElapsed, tvDuration;
    private List<MusicLoader.MusicInfo> musicList = new ArrayList<>();
    private int currentMusic;

    private int currentPosition;

    private ProgressReceiver progressReceiver;


    private MyService.NatureBinder natureBinder;

    private ImageView imageView;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            natureBinder = (MyService.NatureBinder) service;
            if (natureBinder.isPlaying()) {
                pas = (ImageView) findViewById(R.id.btnStartStop);
                pas.setImageResource(R.mipmap.play);
            }
            Log.e("mode", natureBinder.getCurrentMode() + "");
            if (natureBinder.getCurrentMode() == 0) {
                i = 0;
                mode.setImageResource(R.mipmap.icon1);
            }
            if (natureBinder.getCurrentMode() == 1) {
                i = 1;
                mode.setImageResource(R.mipmap.icon2);
            }
            if (natureBinder.getCurrentMode() == 2) {
                i = 2;
                mode.setImageResource(R.mipmap.icon3);
            }
            if (natureBinder.getCurrentMode() == 3) {
                i = 3;
                mode.setImageResource(R.mipmap.icon4);
            }
        }
    };
    private Bitmap bm;
    private long songid;
    private int albumid;
    private int propro;
    private MyUsb myUsb;

    private ImageView pre, next, pas, back, mode;

    private void connectToNatureService() {
        Intent intent = new Intent(DetailActivity.this, MyService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        overridePendingTransition(R.anim.push_right_in, R.anim.hold);






        getmusic();
        setContentView(R.layout.detail_layout);
        next = (ImageView) findViewById(R.id.btnNext);
        back = (ImageView) findViewById(R.id.btnExit);
        pas = (ImageView) findViewById(R.id.btnStartStop);
        pre = (ImageView) findViewById(R.id.btnPrevious);
        mode = (ImageView) findViewById(R.id.btnMode);

        next.setEnabled(true);
        pre.setEnabled(true);

        next.setOnClickListener(this);
        back.setOnClickListener(this);
        pas.setOnClickListener(this);
        pre.setOnClickListener(this);
        mode.setOnClickListener(this);
//        mVisualizerView = new VisualizerView(this);

        connectToNatureService();
        initComponents();

        myUsb = new MyUsb();
        IntentFilter intentFil = new IntentFilter();
        intentFil.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFil.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFil.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFil.addDataScheme("file");
        registerReceiver(myUsb, intentFil);


    }

    class MyUsb extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((Intent.ACTION_MEDIA_MOUNTED).equals(intent.getAction())) {
//                Toast.makeText(DetailActivity.this, "U盘插入", Toast.LENGTH_LONG).show();
                finish();
            }
            if ((Intent.ACTION_MEDIA_EJECT).equals(intent.getAction())) {
                imageView.setImageResource(R.drawable.bgnull);
                tvTitle.setTextColor(Color.BLACK);
                tvDuration.setTextColor(Color.BLACK);
                tvTimeElapsed.setTextColor(Color.BLACK);
                next.setEnabled(false);
                pre.setEnabled(false);
                pas.setEnabled(false);
                pbDuration.setEnabled(false);
                pbDuration.setProgress(0);
                finish();
                Intent main = new Intent();
                main.setAction("main");
                sendBroadcast(main);
            }
        }
    }

    Handler han6 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 7:
                    musicList.clear();
                    getmusic();
                    freshMp3();
                    break;
            }
        }
    };

    private void freshMp3() {
        han6.sendEmptyMessageDelayed(7, 500);
    }

    private void getmusic() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int urlCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int picCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                String title = cursor.getString(displayNameCol);
                String album = cursor.getString(albumCol);
                long id = cursor.getLong(idCol);
                int duration = cursor.getInt(durationCol);
                long size = cursor.getLong(sizeCol);
                String artist = cursor.getString(artistCol);
                String url = cursor.getString(urlCol);
                int pic = cursor.getInt(picCol);

                MusicLoader.MusicInfo musicInfo = new MusicLoader.MusicInfo(id, title);
                musicInfo.setAlbum(album);
                musicInfo.setDuration(duration);
                musicInfo.setSize(size);
                musicInfo.setArtist(artist);
                musicInfo.setUrl(url);
                musicInfo.setPic(pic);

                double fileOrFilesSize = FileSizeUtil.getFileOrFilesSize(musicInfo.getUrl(), 1);
                if (fileOrFilesSize > 10) {
                    musicList.add(musicInfo);
                }

            }
            cursor.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(progressReceiver);
        overridePendingTransition(R.anim.hold, R.anim.push_right_out);
        han6.removeMessages(7);
    }

    public void onStop() {
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();

        if (natureBinder != null) {
            unbindService(serviceConnection);
        }
        unregisterReceiver(myUsb);
    }

    private void initComponents() {

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        currentMusic = getIntent().getIntExtra(CURRENT_MUSIC, 0);
        tvTitle.setText(musicList.get(currentMusic).getTitle());

        tvDuration = (TextView) findViewById(R.id.tvDuration);

        final int max = getIntent().getIntExtra(MUSIC_LENGTH, 0);
        final int pos = getIntent().getIntExtra(CURRENT_POSITION, 0);
        tvDuration.setText(FormatHelper.formatDuration(max));

        tvTimeElapsed = (TextView) findViewById(R.id.tvTimeElapsed);
        tvTimeElapsed.setText(FormatHelper.formatDuration(pos));
        pbDuration = (SeekBar) findViewById(R.id.pbDuration);
        pbDuration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (musicList.size() > 0) {
                    natureBinder.changeProgress(propro);
                    if (natureBinder != null) {
                        if (natureBinder.isPlaying()) {
                            natureBinder.notifyActivity();
                        } else {
                            natureBinder.notifyActivity();
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                pbDuration.setEnabled(true);
                propro = progress;
                tvTitle.setTextColor(Color.WHITE);
                tvDuration.setTextColor(Color.WHITE);
                tvTimeElapsed.setTextColor(Color.WHITE);
//                Log.e("tv", FormatHelper.formatDuration(progress));
                pre.setEnabled(true);
                next.setEnabled(true);
            }
        });
        pbDuration.setMax(max / 1000);

        currentPosition = getIntent().getIntExtra(CURRENT_POSITION, 0);
        pbDuration.setProgress(currentPosition / 1000);

        imageView = (ImageView) findViewById(R.id.song_image);
        songid = musicList.get(currentMusic).getId();
        albumid = musicList.get(currentMusic).getPic();
        bm = MusicUtils.getArtwork(this, songid, albumid, true);
        imageView.setImageBitmap(bm);
    }

    private void initReceiver() {
        progressReceiver = new ProgressReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(MyService.ACTION_UPDATE_DURATION);
        intentFilter.addAction(MyService.ACTION_UPDATE_CURRENT_MUSIC);
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(progressReceiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStartStop:
                if (new File(musicList.get(currentMusic).getUrl()).exists()) {
                    if (musicList.size() > 0) {
                        play(currentMusic, btnStartStop);
                    }
                }
                break;
            case R.id.btnNext:
                if (musicList.size() > 0) {
                    if ((currentMusic + 1) < musicList.size()) {
                        if (new File(musicList.get(currentMusic + 1).getUrl()).exists()) {
                            natureBinder.toNext();
                        }
                    }
                    if ((currentMusic + 1) == musicList.size()) {
                        if (new File(musicList.get(0).getUrl()).exists()) {
                            natureBinder.toNext();
                        }
                    }
                }
                pas.setImageResource(R.mipmap.play);
                break;
            case R.id.btnPrevious:
                if (musicList.size() > 0) {
                    if ((currentMusic - 1) >= 0) {
                        if (new File(musicList.get(currentMusic - 1).getUrl()).exists()) {
                            natureBinder.toPrevious();
                        }
                    }
                    if ((currentMusic - 1) < 0) {
                        if (new File(musicList.get(musicList.size() - 1).getUrl()).exists()) {
                            natureBinder.toPrevious();
                        }
                    }
                }
                pas.setImageResource(R.mipmap.play);
                break;
            case R.id.btnExit:
                Intent intent1 = new Intent("fresh");
                sendBroadcast(intent1);
                finish();
                break;
            case R.id.btnMode:
                i++;
                if (i == 4) {
                    i = 0;
                }
                if (i == 0) {
                    mode.setImageResource(R.mipmap.icon1);
                    natureBinder.changeMode();
                }
                if (i == 1) {
                    mode.setImageResource(R.mipmap.icon2);
                    natureBinder.changeMode();
                }
                if (i == 2) {
                    mode.setImageResource(R.mipmap.icon3);
                    natureBinder.changeMode();
                }
                if (i == 3) {
                    mode.setImageResource(R.mipmap.icon4);
                    natureBinder.changeMode();
                }
                break;
            default:
                break;
        }
    }

    private void play(int currentMusic, int resId) {
        if (natureBinder.isPlaying()) {
            natureBinder.stopPlay();
            pas.setImageResource(R.mipmap.pause);
        } else {
            natureBinder.startPlay(currentMusic, currentPosition);
            pas.setImageResource(R.mipmap.play);
        }
    }

    class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_UPDATE_PROGRESS.equals(action)) {
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, currentPosition);
                if (progress > 0) {
                    currentPosition = progress; // Remember the current position
                    tvTimeElapsed.setText(FormatHelper.formatDuration(progress));
                    pbDuration.setProgress(progress / 1000);
                }
            } else if (MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action)) {
                //Retrieve the current music and get the title to show on top of the screen.
                currentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                tvTitle.setText(musicList.get(currentMusic).getTitle());
                songid = musicList.get(currentMusic).getId();
                albumid = musicList.get(currentMusic).getPic();
                bm = MusicUtils.getArtwork(DetailActivity.this, songid, albumid, true);
                imageView.setImageBitmap(bm);
            } else if (MyService.ACTION_UPDATE_DURATION.equals(action)) {
                //Receive the duration and show under the progress bar
                //Why do this ? because from the ContentResolver, the duration is zero.
                int duration = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                tvDuration.setText(FormatHelper.formatDuration(duration));
                pbDuration.setMax(duration / 1000);
            } else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        natureBinder.toPrevious();
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        natureBinder.toNext();
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        play(currentMusic, btnStartStop);
                    }
                }
            }
        }
    }
}