package com.wr.qt.wr2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.bugly.crashreport.CrashReport;
import com.vigorchip.WrMusic.wr2.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.Color.WHITE;
import static com.vigorchip.WrMusic.wr2.R.id.seekbar_parent;
import static com.wr.qt.wr2.DetailActivity.MUSIC_LENGTH;


public class MainActivity extends Activity implements OnClickListener{


    //频谱
    private VisualizerView mVisualizerView;


    private long mStartTime;
    private BroadcastReceiver mVisualizerReceiver;

//。。
    private ListView lvSongs;
    private SeekBar pbDuration,SoundseekBar;
    private ContentObserver mVoiceObserver;
//音量进度
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;


    private TextView tvCurrentMusic;
    private TextView tvDuration1;

    private List<MusicLoader.MusicInfo> musicList = new ArrayList<>();
    private int currentMusic;
    private int currentPosition;
    private int currentMax;

    private ImageButton btnStartStop;
    private ImageButton btnNext;
    private Button btnDetail;
    private ImageButton btnLast;
    private ProgressReceiver progressReceiver;
    private MyService.NatureBinder natureBinder;

    private TextView protvc;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            natureBinder = (MyService.NatureBinder) service;
            if (natureBinder.isPlaying()) {
                btnStartStop = (ImageButton) findViewById(R.id.btnStartStop);
                btnStartStop.setImageResource(R.mipmap.play);
            }
            Log.e("mode", natureBinder.getCurrentMode() + "");
            if (natureBinder.getCurrentMode() == 0) {
                i = 0;
                btnMode.setImageResource(R.mipmap.icon1);
            }
            if (natureBinder.getCurrentMode() == 1) {
                i = 1;
                btnMode.setImageResource(R.mipmap.icon2);
            }
            if (natureBinder.getCurrentMode() == 2) {
                i = 2;
                btnMode.setImageResource(R.mipmap.icon3);
            }
            if (natureBinder.getCurrentMode() == 3) {
                i = 3;
                btnMode.setImageResource(R.mipmap.icon4);
            }
        }
    };
    private MusicAdapter adapter;
    private TextView tvFresh;
    private boolean FirstCLick = false;
    private AudioManager am;
    private Intent intent;
    private NotificationManager notiManager;
    private int propro;
    private CloseMusic closeMusic;
    private OpenMusic opneMusic;
    private List<MusicLoader.MusicInfo> temList;
    private Detail detail;
    private ImageButton btnMode;
    private Animation animation;
    private MyVolumeReceiver mVolumeReceiver;

    private void connectToNatureService() {
        intent = new Intent(MainActivity.this, MyService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    ArrayList<Integer> oldList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        oldList=new ArrayList<>();
        //dataList=new ArrayList<Map<String, Object>>();



//item列表排序
        /*simp_adapter=new SimpleAdapter(this, getData(), R.layout.music_item, new String[]{"position_icon"},new int[]{R.id.position_icon });
        lvSongs.setAdapter(simp_adapter);*/





//音量进度
        SoundseekBar=(SeekBar)findViewById(R.id.seekBar2);
        final VisualizerView newVisualizer = (VisualizerView)findViewById(R.id.mVisualizer);
        RelativeLayout soundseek = (RelativeLayout) findViewById(seekbar_parent);

        mediaPlayer=new MediaPlayer();

        audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);//获取音量服务
        int MaxSound=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//获取系统音量最大值
        SoundseekBar.setMax(MaxSound);//音量控制Bar的最大值设置为系统音量最大值
//        int currentSount=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);//获取当前音量
        SoundseekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));// 当前的媒体音量//音量控制Bar的当前值设置为系统音量当前值
        myRegisterReceiver();//注册同步更新的广播

        SoundseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                Log.v("lyj_ring", "mVoiceSeekBar max progress = "+arg1);
                //系统音量和媒体音量同时更新
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, arg1, 0);//广播给系统的
                audioManager.setStreamVolume(3, arg1, 3);//  3 代表  AudioManager.STREAM_MUSIC  系统广播给seekbar

                if (audioManager!=null&&audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)==0){
                    newVisualizer.setVisibility(View.GONE);
                }else {
                    newVisualizer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }
        });

        mVoiceObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                SoundseekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
                //或者你也可以用媒体音量来监听改变，效果都是一样的。
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                //mVoiceSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        };

//音量进度触摸区域的放大
        soundseek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                SoundseekBar.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 500)) && (event.getY() <= (seekRect.bottom + 500))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return SoundseekBar.onTouchEvent(me);
                }
                return false;
            }
        });

        btnMode = (ImageButton) findViewById(R.id.btnMode);
        btnMode.setOnClickListener(this);

        Context context = getApplicationContext();
        String packageName = context.getPackageName();
        String processName = getProcessName(android.os.Process.myPid());
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        CrashReport.initCrashReport(context, "30f72ee0ba", true, strategy);
        CrashReport.initCrashReport(getApplicationContext(), "30f72ee0ba", true);

        traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());

        connectToNatureService();

        initComponents();

        getMusic(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        adapter.notifyDataSetChanged();//进去如果扫描已经扫描了就立即显示；


        for (int i = 0; i < musicList.size(); i++) {
            MediaScannerConnection.scanFile(this, new String[]{musicList.get(i).getUrl()}, null, null);
        }

        temList = new ArrayList<>();
        temList = musicList;
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        registerReceiver();
        if (new File("/mnt/usbhost1/LOST.DIR").exists()) {
            han4.sendEmptyMessageDelayed(6, 500);
        }
        freshMp3();
    }

    private void myRegisterReceiver() {
        mVolumeReceiver = new MyVolumeReceiver() ;
        IntentFilter filter = new IntentFilter() ;
        filter.addAction("android.media.VOLUME_CHANGED_ACTION") ;
        registerReceiver(mVolumeReceiver, filter) ;
    }
    
    /*private List<Map<String,Object>> getData()
    {
        for (int i=0;i<800;i++){
            Map<String,Object>map=new HashMap<String, Object>();
            map.put("position_icon","position_icon");
            map.put("text",""+i);
            dataList.add(map);
        }
        return dataList;
    }*/

    private class MyVolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //如果音量发生变化则更改seekbar的位置
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;// 当前的媒体音量
                SoundseekBar.setProgress(currVolume) ;
            }
        }
    }


    public void traverseFolder2(String path) {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
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
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (natureBinder != null&&natureBinder.isPlaying()) {
                        natureBinder.stopPlay();
                        btnStartStop.setImageResource(R.mipmap.pause);
                        am.abandonAudioFocus(mAudioFocusChange);
                    }
                    break;
            }
        }
    };

    private void initComponents() {
        pbDuration = (SeekBar) findViewById(R.id.pbDuration);
        RelativeLayout duration_parent = (RelativeLayout) findViewById(R.id.DurationParent);
        duration_parent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                pbDuration.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 500)) && (event.getY() <= (seekRect.bottom + 500))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return pbDuration.onTouchEvent(me);
                }
                return false;
            }
        });
        pbDuration.setEnabled(false);


//获取总时长
        final int max = getIntent().getIntExtra(MUSIC_LENGTH, 0);
        tvDuration1=(TextView) findViewById(R.id.tvDuration1);
        tvDuration1.setText(FormatHelper.formatDuration(max));
        pbDuration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (FirstCLick) {
                    pbDuration.setEnabled(true);
                    if (musicList.size() > 0) {
                        natureBinder.changeProgress(propro);
                        if (natureBinder != null) {
                            if (natureBinder.isPlaying()) {
                                btnStartStop.setImageResource(R.mipmap.play);
                                natureBinder.notifyActivity();
                            } else {
                                btnStartStop.setImageResource(R.mipmap.pause);
                                natureBinder.notifyActivity();
                            }
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
                propro = progress;
                if (FirstCLick) {
                    pbDuration.setEnabled(true);
                }
                btnDetail.setEnabled(true);
                btnStartStop.setEnabled(true);
                btnNext.setEnabled(true);
//                btnStartStop.setImageResource(R.mipmap.play);
            }
        });

        tvCurrentMusic = (TextView) findViewById(R.id.tvCurrentMusic);


//点击切换上一首
        btnLast =(ImageButton) findViewById(R.id.btnLast);
        btnLast.setOnClickListener(this);

        btnStartStop = (ImageButton) findViewById(R.id.btnStartStop);
        btnStartStop.setOnClickListener(this);

        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        btnDetail = (Button) findViewById(R.id.btnDetail);
        btnDetail.setOnClickListener(this);

        animation = AnimationUtils.loadAnimation(this, R.anim.big);

        adapter = new MusicAdapter();

        RelativeLayout rlnull = (RelativeLayout) findViewById(R.id.rlnull);

        lvSongs = (ListView) findViewById(R.id.lvSongs);

        lvSongs.setEmptyView(rlnull);
        lvSongs.setAdapter(adapter);
        lvSongs.getCount();
        lvSongs.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.e("position",musicList.get(position).getUrl());
                parent.getAdapter().getItem(position);
                if (new File(musicList.get(position).getUrl()).exists()){
                    protvc.setVisibility(View.VISIBLE);
                    tvCurrentMusic.setVisibility(View.GONE);
                    FirstCLick = true;
                    currentMusic = position;
                    btnDetail.setEnabled(true);
                    btnStartStop.setEnabled(true);
                    btnNext.setEnabled(true);
                    pbDuration.setEnabled(true);
                    natureBinder.startPlay(currentMusic, 0);
//                    lvSongs.requestFocusFromTouch();//获取焦点
//                    lvSongs.setSelection(currentMusic);
                    if (natureBinder.isPlaying()) {
                        btnStartStop.setImageResource(R.mipmap.play);
                    }else {
                        btnStartStop.setImageResource(R.mipmap.pause);
                    }
                    showNotification();
                }
            }
        });

        protvc = (TextView) findViewById(R.id.pro_tvC);
        tvFresh = (TextView) findViewById(R.id.tvfresh);
    }

    /**
     * 获取音乐
     *
     * @param uri
     */
    private void getMusic(Uri uri) {

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
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
                if (fileOrFilesSize>10){
                    musicList.add(musicInfo);
                }
            }
            cursor.close();
        }
    }

    int ii = 1;
    public void onResume() {
        super.onResume();
        startService(intent);
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //频谱
        Log.d("频谱","mVisualizerView:"+mVisualizerView);
//频谱
        // 频谱广播
        mStartTime = System.currentTimeMillis();
        LocalBroadcastManager.getInstance(this).registerReceiver(mVisualizerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ArrayList<Integer> list = intent.getIntegerArrayListExtra(MyService.VISUALIZER_INT_LIST);
                long end = System.currentTimeMillis();
                if (end - mStartTime >= 10) {
                    mVisualizerView = (VisualizerView) findViewById(R.id.mVisualizer);
                    Log.d("频谱广播","mVisualizerView:"+mVisualizerView);
                    int i = 0;
                    for (; i <list.size()-1 ; i++) {
                        if (list.get(i)==0){
                            continue;
                        } else{
                            oldList=list;
                            break;
                        }
                    }
                    if (i==list.size()-1){
                        mVisualizerView.updateData(oldList, intent.getIntExtra(MyService.VISUALIZER_SAMPLE_RATE_INT, 0));
                    }else {
                    mVisualizerView.updateData(list, intent.getIntExtra(MyService.VISUALIZER_SAMPLE_RATE_INT, 0));
                    }

                    mStartTime = System.currentTimeMillis();
                }
            }
        }, new IntentFilter(MyService.BROADCAST_VISUALIZER_FILTER));
        if (natureBinder != null) {
            if (natureBinder.isPlaying()) {
                btnStartStop.setImageResource(R.mipmap.play);
            } else {
                btnStartStop.setImageResource(R.mipmap.pause);
            }
        }
        ii = 1;
    }

    public void onPause() {
        super.onPause();
        Log.e("print1", "onPause");
        unregisterReceiver(progressReceiver);
        registerReceiver();
        unregisterReceiver(myUsb);
    }

    class OpenMusic extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("open")) {
                Log.e("close", "444" + currentMusic + "---" + currentPosition);
                play(currentMusic, R.id.btnStartStop);
            }
        }
    }

    class CloseMusic extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("close")) {
                Log.e("close", "222" + currentMusic + "---" + currentPosition);
                natureBinder.stopPlay();
                finish();
                System.exit(0);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
//频谱
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVisualizerReceiver);
        if (natureBinder != null) {
            unbindService(serviceConnection);
        }
        if (musicList != null) {
            musicList.clear();
        }
        han4.removeMessages(5);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            han4.removeMessages(6);
        }
        stopService(intent);
        unregisterReceiver(progressReceiver);
//        unregisterReceiver(myUsb);
        unregisterReceiver(closeMusic);
        unregisterReceiver(opneMusic);
        unregisterReceiver(detail);
        unregisterReceiver(mVolumeReceiver);
    }



    /**
     * 显示通知栏
     */
    private void showNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification.Builder(this)
                .setTicker("Music")
                .setSmallIcon(R.drawable.audio)
                .setContentTitle("Music")
                .setContentText(musicList.get(currentMusic).getTitle())
                .setContentIntent(pendingIntent)
                .getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notiManager.notify(1, notification);
    }

    int i = 0;
    int a = 1;

    private void freshMp3() {
        han4.sendEmptyMessageDelayed(5, 500);

    }

    @Override
    public void onClick(View v) {
        String usb_noMusic=getResources().getString(R.string.usb_noMusic);
        switch (v.getId()) {
//点击切换上一首
            case R.id.btnLast:
                if (mediaPlayer != null) {
                    btnLast.startAnimation(animation);
                }
                if (musicList.size() > 0) {
                    if ((currentMusic - 1) >= 0) {
                        if (new File(musicList.get(currentMusic - 1).getUrl()).exists()) {
                            natureBinder.toPrevious();
                            lvSongs.setSelection(currentMusic-1);
                            btnStartStop.setImageResource(R.mipmap.play);
                        }
                    }
                    if ((currentMusic - 1) < 0) {
                        if (new File(musicList.get(musicList.size() - 1).getUrl()).exists()) {
                            natureBinder.toPrevious();
                            //返回底部
                            lvSongs.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Select the last row so it will scroll into view...
                                    adapter.notifyDataSetInvalidated();
                                    lvSongs.setSelection(adapter.getCount());
                                    btnStartStop.setImageResource(R.mipmap.play);
                                }
                            });
                        }
                    }
                }
//                Toast.makeText(this, "定位上一首:"+currentMusic,Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnStartStop:
                if (mediaPlayer != null) {
                    btnStartStop.startAnimation(animation);
                }
                if (musicList.size() == 0) {
                    Toast.makeText(this, usb_noMusic, Toast.LENGTH_SHORT).show();
                }
                if (musicList.size()>0){
                    if (new File(musicList.get(currentMusic).getUrl()).exists()) {
                        if (musicList.size() > 0) {
                            play(currentMusic, R.id.btnStartStop);
                            FirstCLick = true;
                        }
                    }
                }
                break;
            case R.id.btnNext:
                if (mediaPlayer != null) {
                    btnNext.startAnimation(animation);
                }
                if (musicList.size() > 0) {
                    if ((currentMusic+1)<musicList.size()){
                        if (new File(musicList.get(currentMusic+1).getUrl()).exists()){
                            lvSongs.setSelection(currentMusic);
//                            Toast.makeText(this, "定位下一首:"+currentMusic,Toast.LENGTH_SHORT).show();
                            natureBinder.toNext();
                            FirstCLick = true;
                            btnStartStop.setImageResource(R.mipmap.play);
                        }
                    }
                    if ((currentMusic+1)==musicList.size()){
                        if (new File(musicList.get(0).getUrl()).exists()){
                            lvSongs.setSelection(0);
//                            Toast.makeText(this, "定位下一首:"+currentMusic,Toast.LENGTH_SHORT).show();
                            natureBinder.toNext();
                            btnStartStop.setImageResource(R.mipmap.play);
                        }
                    }
                }
                if (musicList.size() == 0) {
                    Toast.makeText(this, usb_noMusic, Toast.LENGTH_SHORT).show();
                }
//                Log.d("定位下一首","currentMusic:"+currentMusic);
                break;

            case R.id.btnDetail:
                if (natureBinder.isPlaying()) {
                    FirstCLick = true;
                }
                if (musicList.size() > 0) {
                    if (new File(musicList.get(currentMusic).getUrl()).exists()) {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        Log.e("max", currentMax + "");
                        intent.putExtra(MUSIC_LENGTH, currentMax);
                        intent.putExtra(DetailActivity.CURRENT_MUSIC, currentMusic);
                        intent.putExtra(DetailActivity.CURRENT_POSITION, currentPosition);
                        startActivity(intent);
                    }
                }
                if (musicList.size() == 0) {
                    Toast.makeText(this, usb_noMusic, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnMode:
                a++;
                if (a == 4) {
                    a = 0;
                }
                if (a == 0) {
                    btnMode.setImageResource(R.mipmap.icon1);
                    natureBinder.changeMode();
                }
                if (a == 1) {
                    btnMode.setImageResource(R.mipmap.icon2);
                    natureBinder.changeMode();
                }
                if (a == 2) {
                    btnMode.setImageResource(R.mipmap.icon3);
                    natureBinder.changeMode();
                }
                if (a == 3) {
                    btnMode.setImageResource(R.mipmap.icon4);
                    natureBinder.changeMode();
                }
                break;
            default:
                break;
        }
    }

    private void play(int position, int resId) {
        Log.e("position", currentPosition + "=====" + position);
        if (natureBinder.isPlaying()) {
            natureBinder.stopPlay();
            btnStartStop.setImageResource(R.mipmap.pause);
        } else {
            natureBinder.startPlay(position, currentPosition);
            btnStartStop.setImageResource(R.mipmap.play);
        }
    }

    private MyUsb myUsb;

    private void registerReceiver() {
        progressReceiver = new ProgressReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(MyService.ACTION_UPDATE_DURATION);
        intentFilter.addAction(MyService.ACTION_UPDATE_CURRENT_MUSIC);
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        intentFilter.addAction("fresh");
        registerReceiver(progressReceiver, intentFilter);
        myUsb = new MyUsb();
        IntentFilter intentFil = new IntentFilter();
        intentFil.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFil.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFil.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFil.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFil.addDataScheme("file");
        registerReceiver(myUsb, intentFil);
        closeMusic = new CloseMusic();
        IntentFilter intentFilter22 = new IntentFilter("close");
        registerReceiver(closeMusic, intentFilter22);
        opneMusic = new OpenMusic();
        IntentFilter intentFilter111 = new IntentFilter("open");
        registerReceiver(opneMusic, intentFilter111);

        detail = new Detail();
        IntentFilter deFil=new IntentFilter("main");
        registerReceiver(detail,deFil);
    }

    private class Detail extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            pbDuration.setEnabled(false);
            btnDetail.setEnabled(false);
            btnNext.setEnabled(false);
            btnStartStop.setEnabled(false);
            pbDuration.setProgress(0);
        }
    }

    class MusicAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return musicList.size();
        }

        @Override
        public Object getItem(int position) {
            if (musicList.size() > 0) {
                return musicList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return musicList.get(position).getId();
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.music_item, null);
                ImageView pImageView = (ImageView) convertView.findViewById(R.id.albumPhoto);
                TextView pArtist = (TextView) convertView.findViewById(R.id.artist);
                viewHolder = new ViewHolder(pImageView, pArtist);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            showPlayStateIcon(convertView,position);

            /*
            final MusicLoader.MusicInfo song = musicList.get(position);
            if (song.getId()==playerInfo[0]) {
                viewHolder.sort.setText("");
                if (playerInfo[1] == MyService.STATE_PAUSE) {
                    viewHolder.durationtext.setVisibility(TextView.VISIBLE);
                    viewHolder.durationtext.setBackgroundResource(R.drawable.list_pause_indicator);
                } else if (playerInfo[1] == MyService.STATE_OVER) {
                    viewHolder.durationtext.setText(TextFormatter.getMusicTime(musicList.get(position).getDuration()));
                    viewHolder.durationtext.setBackgroundResource(0);
                }
            }else{
                viewHolder.durationtext.setText(TextFormatter.getMusicTime(musicList.get(position).getDuration()));
                viewHolder.durationtext.    setBackgroundResource(0);
            }*/


            //viewHolder.playView.setBackgroundResource(R.drawable.list_pause_indicator);
            //viewHolder.imageView.setImageResource(R.drawable.logo);

//            lvSongs.smoothScrollToPosition(currentMusic);

            return convertView;
        }


//改变当前播放状态，currentMusic是获取当前的位置
        private void showPlayStateIcon(View convertView, int position) {
            ImageView playerImage = (ImageView) convertView.findViewById(R.id.play_view);
            TextView pTitle = (TextView) convertView.findViewById(R.id.title);
            TextView sort = (TextView) convertView.findViewById(R.id.rank);
            TextView pduration = (TextView) convertView.findViewById(R.id.tv_item_duration);

            pduration.setText(TextFormatter.getMusicTime(musicList.get(position).getDuration()));

            sort.setText(String.valueOf(position+1)+"");
            pTitle.setText(musicList.get(position).getTitle());
            playerImage.setImageResource(R.drawable.list_pause_indicator);
            if (position != currentMusic){
                playerImage.setVisibility(View.GONE);
                pduration.setVisibility(View.VISIBLE);
                pTitle.setTextColor(WHITE);
                sort.setTextColor(WHITE);
            }else{//正在播放
            playerImage.setVisibility(View.VISIBLE);
                pduration.setVisibility(View.GONE);
                pTitle.setTextColor(Color.rgb(177,10,31));
                sort.setTextColor(Color.rgb(177,10,31));
            }
        }
    }


    class ViewHolder {public ViewHolder(ImageView pImageView,TextView pArtist) {
            imageView = pImageView;
            artist = pArtist;
        }

        ImageView imageView;
        TextView artist;

    }

    class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_UPDATE_PROGRESS.equals(action)) {
                int progress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS, 0);
                if (progress > 0) {
                    FirstCLick = true;
                    currentPosition = progress;
                    pbDuration.setProgress(progress / 1000);
                    protvc.setText(FormatHelper.formatDuration(progress));
                }
            } else if (MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(action)) {
                currentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                if (currentMusic < musicList.size()) {
                    tvCurrentMusic.setText(musicList.get(currentMusic).getTitle());
                    //lvSongs.setSelection(0);
                }//lvSongs.setSelection(currentMusic-1);

            } else if (MyService.ACTION_UPDATE_DURATION.equals(action)) {
                currentMax = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION, 0);
                int max = currentMax / 1000;
//获取总时长
                tvDuration1.setText(FormatHelper.formatDuration(currentMax));
                pbDuration.setMax(currentMax / 1000);

            } else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        protvc.setVisibility(View.VISIBLE);
                        tvCurrentMusic.setVisibility(View.VISIBLE);
                        natureBinder.toPrevious();
                        if (musicList.size() > 0) {
                            if ((currentMusic-1)>=0){
                                if (new File(musicList.get(currentMusic-1).getUrl()).exists()) {
                                    natureBinder.toPrevious();
                                    btnStartStop.setImageResource(R.mipmap.play);
                                    am.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                                }
                            }
                            if ((currentMusic-1)<0){
                                if (new File(musicList.get(musicList.size()-1).getUrl()).exists()) {
                                    natureBinder.toPrevious();
                                    btnStartStop.setImageResource(R.mipmap.play);
                                    am.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                                }
                            }
                        }
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        protvc.setVisibility(View.VISIBLE);
                        tvCurrentMusic.setVisibility(View.VISIBLE);
                        if (musicList.size() > 0) {
                            if ((currentMusic+1)<musicList.size()){
                                if (new File(musicList.get(currentMusic+1).getUrl()).exists()){
                                    natureBinder.toNext();
                                    FirstCLick = true;
                                    btnStartStop.setImageResource(R.mipmap.play);
                                    am.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                                }
                            }
                            if ((currentMusic+1)==musicList.size()){
                                if (new File(musicList.get(0).getUrl()).exists()){
                                    natureBinder.toNext();
                                    btnStartStop.setImageResource(R.mipmap.play);
                                    am.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                                }
                            }
                        }
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        play(currentMusic, R.id.btnStartStop);
                    }
                }
            }
        }
    }

    Handler han4 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 5:
                    musicList.clear();
                    getMusic(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                        adapter.notifyDataSetChanged();
                    i++;
                    freshMp3();
                    break;
                case 6:
                    String scan_start=getResources().getString(R.string.scan_start);
                    String scan_second=getResources().getString(R.string.scan_second);
                    tvFresh.setText(scan_start + ii+scan_second);
                    if (ii > 68) {
                        String scan_end=getResources().getString(R.string.scan_end);
                        tvFresh.setText(scan_end);
                        break;
                    }
                    ii++;
                    han4.sendEmptyMessageDelayed(6, 1000);
                    break;
            }
        }
    };

    class MyUsb extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((Intent.ACTION_MEDIA_MOUNTED).equals(intent.getAction())) {
                final AlertDialog alertDialog=new AlertDialog.Builder(MainActivity.this).setView(getLayoutInflater().inflate(R.layout.usb_out,null)).setCancelable(false).show();

                han4.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pbDuration.setEnabled(false);
                        btnDetail.setEnabled(false);
                        btnStartStop.setEnabled(true);
                        pbDuration.setProgress(0);
                        btnStartStop.setImageResource(R.mipmap.pause);
                        alertDialog.dismiss();
                    }
                },6000);

                traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());
                //改动
                ii = 1;
                for (int i = 0; i < musicList.size(); i++) {
                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{musicList.get(i).getUrl()}, null, null);
                }
//                Toast.makeText(MainActivity.this, "U盘插入", Toast.LENGTH_LONG).show();
                han4.removeMessages(6);
                han4.sendEmptyMessageDelayed(6, 1000);
            }
            if ((Intent.ACTION_MEDIA_UNMOUNTED).equals(intent.getAction())) {

                Log.e("USB","U盘拔出 UNMOUNTED");
//                final AlertDialog alertDialog=new AlertDialog.Builder(MainActivity.this).setTitle("U盘拔出").setMessage("正在刷新歌曲").show();
                final AlertDialog alertDialog=new AlertDialog.Builder(MainActivity.this).setView(getLayoutInflater().inflate(R.layout.usb_out,null)).setCancelable(false).show();
                han4.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pbDuration.setEnabled(false);
                        btnDetail.setEnabled(false);
                        btnNext.setEnabled(false);
                        btnStartStop.setEnabled(false);
                        pbDuration.setProgress(0);
                        btnStartStop.setImageResource(R.mipmap.pause);
                        alertDialog.dismiss();
//                        mVisualizerView.setVisibility(View.GONE);
                    }
                },6000);

                for (int i = 0; i < musicList.size(); i++) {
                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{musicList.get(i).getUrl()}, null, null);
                }
                traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());
                String usb_noMusic=getResources().getString(R.string.usb_noMusic);
                tvFresh.setText(usb_noMusic);
                han4.removeMessages(6);
//                Toast.makeText(MainActivity.this, "U盘拔出 ", Toast.LENGTH_SHORT).show();
                musicList.clear();
                adapter.notifyDataSetChanged();
                natureBinder.stopPlay();
                btnStartStop.setImageResource(R.mipmap.pause);
                pbDuration.setEnabled(false);
                btnDetail.setEnabled(false);
                btnNext.setEnabled(false);
                btnStartStop.setEnabled(false);
                pbDuration.setProgress(0);
                protvc.setVisibility(View.GONE);
                tvCurrentMusic.setVisibility(View.GONE);
                mVisualizerView.setVisibility(View.GONE);
            }

            if ((Intent.ACTION_MEDIA_EJECT).equals(intent.getAction())) {
                Log.e("USB","U盘拔出 EJECT");
                btnDetail.setEnabled(false);
                btnStartStop.setEnabled(false);
                btnNext.setEnabled(false);
                System.exit(0);
                pbDuration.setEnabled(false);
                pbDuration.setProgress(0);
                natureBinder.stopPlay();
                mVisualizerView.setVisibility(View.GONE);

                final AlertDialog alertDialog1=new AlertDialog.Builder(MainActivity.this).setView(getLayoutInflater().inflate(R.layout.usb_out,null)).setCancelable(false).show();
//                tvFresh.setText("音乐为空");
                String usb_noMusic=getResources().getString(R.string.usb_noMusic);
                tvFresh.setText(usb_noMusic);

                han4.removeMessages(6);
                btnStartStop.setImageResource(R.mipmap.pause);

                ii = 1;
                musicList.clear();
                han4.sendEmptyMessage(5);
                han4.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnDetail.setEnabled(false);
                        btnStartStop.setEnabled(false);
                        btnNext.setEnabled(false);
                        pbDuration.setEnabled(false);
                        alertDialog1.dismiss();
                        btnStartStop.setImageResource(R.mipmap.pause);
                        pbDuration.setProgress(0);
                        mVisualizerView.setVisibility(View.GONE);
                    }
                },3000);
                protvc.setText("00:00");
                tvDuration1.setText("00:00");
                tvCurrentMusic.setVisibility(View.GONE);
                tvCurrentMusic.setText("");
                System.exit(0);
            }

        }
    }

    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                String exitMsg=getResources().getString(R.string.app_exit);
                Toast.makeText(this, exitMsg, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                if (musicList != null) {
                    musicList.clear();
                }
                stopService(intent);
                notiManager.cancelAll();
                finish();
                System.exit(0);
            }
            return true;
        }

//        switch (keyCode) {
//            case KeyEvent.KEYCODE_VOLUME_UP:
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
//                return true;
//            case KeyEvent.KEYCODE_VOLUME_DOWN:
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
//                return true;
//            default:
//                break;
//        }
        return super.onKeyDown(keyCode, event);
    }


}
