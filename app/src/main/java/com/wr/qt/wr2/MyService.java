package com.wr.qt.wr2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaScannerConnection;
import android.media.audiofx.Visualizer;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.vigorchip.WrMusic.wr2.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MyService extends Service {

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private List<MusicLoader.MusicInfo> musicList = new ArrayList<>();
    private Binder natureBinder = new NatureBinder();

    private int currentMusic;
    private int currentPosition = 1;

    private static final int updateProgress = 1;
    private static final int updateCurrentMusic = 2;
    private static final int updateDuration = 3;
    public static final int MPS_PREPARE = 5;
    public static final int MPS_PAUSE = 4;

    public static final String ACTION_UPDATE_PROGRESS = "UPDATE_PROGRESS";
    public static final String ACTION_UPDATE_DURATION = "UPDATE_DURATION";
    public static final String ACTION_UPDATE_CURRENT_MUSIC = "UPDATE_CURRENT_MUSIC";
    public static final String BROADCAST_VISUALIZER_FILTER = "AUDIO_PLAYER_VISUALIZER";
    public static final String VISUALIZER_INT_LIST = "visualizer_list";
    public static final String  VISUALIZER_SAMPLE_RATE_INT = "visualizer_sample";




    private int currentMode = 1; //default sequence playing


    //    public static final String[] MODE_DESC = {"单曲循环", "全部循环", "随机播放", "顺序播放"};
    public String[] MODE_DESC = null;

    public static final int MODE_ONE_LOOP = 0;
    public static final int MODE_ALL_LOOP = 1;
    public static final int MODE_RANDOM = 2;
    public static final int MODE_SEQUENCE = 3;

    private Notification notification;

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case updateProgress:
                    toUpdateProgress();
                    break;
                case updateDuration:
                    toUpdateDuration();
                    break;
                case updateCurrentMusic:
                    toUpdateCurrentMusic();
                    break;
            }
        }
    };
    private NotificationManager notiManage;
    private MySound mySound;
    private MySounds mySounds;
    private MyUsb myUsb;
    private List<MusicLoader.MusicInfo> temList;
    private int coreDure;
    private Visualizer mVisualizer;

    private void toUpdateProgress() {
        if (mediaPlayer != null && isPlaying) {
            int progress = mediaPlayer.getCurrentPosition();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_PROGRESS);
            intent.putExtra(ACTION_UPDATE_PROGRESS, progress);
            sendBroadcast(intent);
            handler.sendEmptyMessageDelayed(updateProgress, 1000);
        }
    }

    private void toUpdateDuration() {
        if (mediaPlayer != null) {
//            int duration = mediaPlayer.getDuration();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_DURATION);
            intent.putExtra(ACTION_UPDATE_DURATION, coreDure);
            sendBroadcast(intent);
        }
    }

    private void toUpdateCurrentMusic() {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_CURRENT_MUSIC);
        intent.putExtra(ACTION_UPDATE_CURRENT_MUSIC, currentMusic);
        sendBroadcast(intent);
    }

    public void onCreate() {
        initMediaPlayer();
        initVisualizer();
        getmusic();
        temList = new ArrayList<>();
        temList = musicList;
        super.onCreate();
        notiManage = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNoti();
        freshMp3();
        MODE_DESC = getResources().getStringArray(R.array.music_mode);
        registerMy();
    }

    private void initVisualizer() {
        Log.d("player-service", "create");
        Log.d("id", mediaPlayer.getAudioSessionId() + "");
//        int audioId = mediaPlayer.getAudioSessionId();

        mediaPlayer.start();
        mediaPlayer.setLooping(true);
        mVisualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()){
                    return;
                }
                Intent intent = new Intent(BROADCAST_VISUALIZER_FILTER);
                ArrayList<Integer> list = new ArrayList<>(fft.length);
                for (int i = 0; i < fft.length; ++i) {
                    list.add((int) fft[i]);
                }
                intent.putIntegerArrayListExtra(VISUALIZER_INT_LIST, list);
                intent.putExtra(VISUALIZER_SAMPLE_RATE_INT, samplingRate);
                LocalBroadcastManager.getInstance(MyService.this).sendBroadcast(intent);
            }
        },Visualizer.getMaxCaptureRate()/2,true,true);
        mVisualizer.setEnabled(true);
    }

    private void registerMy() {
        mySound = new MySound();
        IntentFilter intentFilter = new IntentFilter("closesound");
        registerReceiver(mySound, intentFilter);
        mySounds = new MySounds();
        IntentFilter intentFilter1 = new IntentFilter("opensound");
        registerReceiver(mySounds, intentFilter1);

        myUsb = new MyUsb();
        IntentFilter intentFil = new IntentFilter();
        intentFil.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFil.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFil.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFil.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFil.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFil.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFil.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);

        intentFil.addDataScheme("file");
        registerReceiver(myUsb, intentFil);
    }

    class MyUsb extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((Intent.ACTION_MEDIA_UNMOUNTED).equals(intent.getAction())) {
//                if (mediaPlayer.isPlaying()) {
//                    Log.e("size", "--------");
//                    mediaPlayer.stop();
//                    mediaPlayer = null;
//                }
            }
            if ((Intent.ACTION_MEDIA_EJECT).equals(intent.getAction())) {
//                mediaPlayer.stop();
                mediaPlayer.reset();
                Log.e("MyService", "u盘 ACTION_MEDIA_EJECT");
            }
        }
    }

    private class MySounds extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("opensound")) {
                Log.e("close", "333");
                Intent intent3 = new Intent("open");
                sendBroadcast(intent3);
                new NatureBinder().notifyActivity();
            }
        }
    }

    private class MySound extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("closesound")) {
                Log.e("close", "111");
                Intent intent2 = new Intent("close");
                sendBroadcast(intent2);
            }
        }
    }

    private void showNoti() {
        if (musicList.size() == 0) {

        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            notification = new Notification.Builder(this)
                    .setTicker("Music")
                    .setSmallIcon(R.drawable.audio)
                    .setContentTitle("Music")
                    .setContentText(musicList.get(currentMusic).getTitle())
                    .setContentIntent(pendingIntent)
                    .getNotification();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            if (notiManage!=null) {
                notiManage.notify(1, notification);
            }
        }
    }

    Handler han5 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 6:
                    musicList.clear();
                    getmusic();
                    freshMp3();
                    break;
            }
        }
    };

    private void freshMp3() {
        han5.sendEmptyMessageDelayed(6, 500);
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

    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mVisualizer != null) {
            mVisualizer.release();
        }
        unregisterReceiver(mySound);
        unregisterReceiver(mySounds);
        unregisterReceiver(myUsb);
    }

    public void traverseFolder2(String path) {

        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length == 0) {
                System.out.println("文件夹是空的!");
                return;
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        System.out.println("文件夹:" + file2.getAbsolutePath());
                        traverseFolder2(file2.getAbsolutePath());
                    } else {
                        if (file2.toString().endsWith(".mp3") || file2.toString().endsWith(".flac")) {
                            String mPath = file2.getAbsolutePath();
                            Log.e("music", mPath);
                            MediaScannerConnection.scanFile(this, new String[]{mPath}, null, null);
                        }
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    /**
     * initialize the MediaPlayer
     */
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                coreDure = mediaPlayer.getDuration();
                mediaPlayer.start();
                mediaPlayer.seekTo(currentPosition);
                handler.sendEmptyMessage(updateDuration);
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                musicList.clear();
                traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());
                getmusic();
                Log.e("size", musicList.size() + "");
                for (int i = 0; i < musicList.size(); i++) {
                    MediaScannerConnection.scanFile(MyService.this, new String[]{musicList.get(i).getUrl()}, null, null);
                }
                if (isPlaying) {
                    if (musicList.size() > 0) {
                        switch (currentMode) {
                            case MODE_ONE_LOOP:
                                if (new File(musicList.get(currentMusic).getUrl()).exists()) {
                                    mediaPlayer.start();
                                }
                                break;
                            case MODE_ALL_LOOP:
                                if (currentMusic < musicList.size() - 1) {
                                    if (new File(musicList.get(currentMusic + 1).getUrl()).exists()) {
                                        play((currentMusic + 1) % musicList.size(), 0);
                                    }
                                } else {
                                    play(0, 0);
                                }

                                break;
                            case MODE_RANDOM:
                                //拔掉u盘还会有问题
                                if (new File(musicList.get(currentMusic).getUrl()).exists()) {
                                    play(getRandomPosition(), 0);
                                }
                                break;
                            case MODE_SEQUENCE:
                                //拔掉u盘还会有问题
                                if (currentMusic < musicList.size() - 1) {
                                    playNext();
//                                    play((currentMusic + icon1) % musicList.size(), 0);
                                }
                                break;
                            default:
                                break;
                        }
                        showNoti();
                    }
                }
            }
        });

    }

    private void setCurrentMusic(int pCurrentMusic) {
        currentMusic = pCurrentMusic;
        handler.sendEmptyMessage(updateCurrentMusic);
    }

    private int getRandomPosition() {
        int random = (int) (Math.random() * (musicList.size()));
        return random;
    }

    private void play(int currentMusic, int pCurrentPosition) {
        if (currentMusic < musicList.size()) {

            currentPosition = pCurrentPosition;
            setCurrentMusic(currentMusic);
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(musicList.get(currentMusic).getUrl());
            } catch (Exception e) {
                e.printStackTrace();
            }
           /* 避免0b文件的音乐*/
            mediaPlayer.prepareAsync();
            handler.sendEmptyMessage(updateProgress);
            isPlaying = true;
        }
    }

    private void stop() {
//        mediaPlayer.pause();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
        }
    }

    private void start() {
        mediaPlayer.start();
    }

    private void playNext() {
        switch (currentMode) {
            case MODE_ONE_LOOP:
                if (currentMusic == musicList.size() - 1) {
                    play(0, 0);
                } else {
                    play(currentMusic + 1, 0);
                    showNoti();
                }
                break;
            case MODE_ALL_LOOP:
                if (currentMusic + 1 == musicList.size()) {
                    play(0, 0);
                    showNoti();
                } else {
                    play(currentMusic + 1, 0);
                    showNoti();
                }
                break;
            case MODE_SEQUENCE:
                if (currentMusic + 1 == musicList.size()) {
//                    Toast.makeText(this, "已经到最后一首了", Toast.LENGTH_SHORT).show();
                } else {
                    play(currentMusic + 1, 0);
                    showNoti();
                }
                break;
            case MODE_RANDOM:
                int pos = getRandomPosition();
                play(pos, 0);
                currentMusic = pos;
                showNoti();
                break;
        }
    }

    private void playPrevious() {
        switch (currentMode) {
            case MODE_ONE_LOOP:
                if (currentMusic == 0) {
                    play(musicList.size() - 1, 0);
                } else {
                    play(currentMusic - 1, 0);
                    showNoti();
                }
                break;
            case MODE_ALL_LOOP:
                if (currentMusic == 0) {
                    play(musicList.size() - 1, 0);
                    showNoti();
                } else {
                    play(currentMusic - 1, 0);
                    showNoti();
                }
                break;
            case MODE_SEQUENCE:
                if (currentMusic == 0) {
//                    Toast.makeText(this, "已经到第一首了", Toast.LENGTH_SHORT).show();
                } else {
                    play(currentMusic - 1, 0);
                    showNoti();
                }
                break;
            case MODE_RANDOM:
                int pos = getRandomPosition();
                play(pos, 0);
                currentMusic = pos;
                showNoti();
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return natureBinder;
    }

    class NatureBinder extends Binder {

        public void startPlay(int currentMusic, int currentPosition) {
            play(currentMusic, currentPosition);
        }

        public void startPlay() {
            start();
        }

        public void stopPlay() {
            stop();
        }

        public void toNext() {
            playNext();
        }

        public void toPrevious() {
            playPrevious();
        }

        /**
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        public void changeMode() {
            currentMode = (currentMode + 1) % 4;

//            Toast.makeText(MyService.this, MODE_DESC[currentMode], Toast.LENGTH_SHORT).show();

            Toast toast = Toast.makeText(MyService.this, MODE_DESC[currentMode], Toast.LENGTH_LONG);
            showMyToast(toast, 500);
        }


        public void showMyToast(final Toast toast, final int cnt) {
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    toast.show();
                }
            }, 0, 3000);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    toast.cancel();
                    timer.cancel();
                }
            }, cnt);
        }

        /**
         * return the current mode
         * MODE_ONE_LOOP = icon1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         *
         * @return
         */
        public int getCurrentMode() {
            return currentMode;
        }

        /**
         * The service is playing the music
         *
         * @return
         */
        public boolean isPlaying() {
            return isPlaying;
        }

        /**
         * Notify Activities to update the current music and duration when current activity changes.
         */
        public void notifyActivity() {
            toUpdateCurrentMusic();
            toUpdateDuration();
        }

        /**
         * Seekbar changes
         *
         * @param progress
         */
        public void changeProgress(int progress) {
            if (mediaPlayer != null) {
                currentPosition = progress * 1000;
                if (isPlaying) {
                    mediaPlayer.seekTo(currentPosition);
                } else {
//                    Toast.makeText(MyService.this,currentPosition+"",Toast.LENGTH_SHORT).show();
                    int pos = progress * 1000;
                    Intent intent = new Intent();
                    intent.setAction(ACTION_UPDATE_PROGRESS);
                    intent.putExtra(ACTION_UPDATE_PROGRESS, pos);
                    sendBroadcast(intent);
                    toUpdateDuration();
                }
            }
        }
    }


}