package tfsapps.noticeme;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationListener {

    LocationManager locationManager;
    private boolean get_GPS = false;
    private int GPS_type = 0;
    private int GPS_poor_accuracy_count;        //GPS精度が悪い回数

    //　スレッド処理
    private Timer mainTimer1;                    //タイマー用
    private MainTimerTask mainTimerTask1;        //タイマタスククラス
    private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
    private boolean blinking = false;
    public Timer blinkTimer;					//タイマー用
    public BlinkingTask blinkTimerTask;		//タイマタスククラス
    public Handler bHandler = new Handler();   //UI Threadへのpost用ハンドラ

    //  ライト関連
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private boolean isOn = false;

    //  音量
    private AudioManager am;

    //画面パーツ
    private SeekBar seek_blinkinterval;
    private SeekBar seek_volume;

    //DB関連
    private int db_interval = 1;
    private int db_volume = 1;

    //アラーム
    private MediaPlayer alarm;
    private boolean is_set_alarm = true;        //アラーム設定

    //BlueMessage
    private String edit_str;
    private String recv_str;
    private String send_str;

    //メール　緯度、軽度
    private Intent intent;
    private String mess_mail = "";
    private double now_ido = 0.0f;         //今回の位置
    private double now_keido = 0.0f;       //今回の位置

    private final ActivityResultLauncher<String>
            requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    locationStart();
                } else {
                    Toast toast = Toast.makeText(this,
                            "これ以上なにもできません", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION);
        } else {

        }

        //  カメラ初期化
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mCameraId = cameraId;
                isOn = enabled;
            }
        }, new Handler());

        //SeekBar
        SeekSelect();
    }

    /*
        アプリスタート処理
     */
    @Override
    public void onStart() {
        super.onStart();
        //DBのロード
        /* データベース */
//        helper = new MyOpenHelper(this);
//        AppDBInitRoad();

        if (alarm == null){
            alarm = MediaPlayer.create(this, R.raw.alarm);
        }
        if (am == null) {
            am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        DisplayScreen();
    }
    @Override
    public void onResume() {
        super.onResume();
        //動画
        //mRewardedVideoAd.resume(this);
        // WakeLockを取得
//        wakeLock.acquire();
    }
    @Override
    public void onPause(){
        super.onPause();
        //  DB更新
//        AppDBUpdated();
        //mRewardedVideoAd.pause(this);
        // WakeLockを解放
//        wakeLock.release();
    }
    @Override
    public void onStop(){
        super.onStop();
        //  DB更新
//        AppDBUpdated();
    }
    /*
        アプリ終了処理
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        //カメラ
        if (mCameraManager != null)
        {
            mCameraManager = null;
        }
        //  DB更新
//        AppDBUpdated();
        //動画
        //mRewardedVideoAd.destroy(this);
    }

    /***************************************************
        シークバー　選択時の処理
    ****************************************************/
    public void SeekSelect(){
        //  点滅間隔
        seek_blinkinterval = (SeekBar)findViewById(R.id.bar_light);
        seek_blinkinterval.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミをドラッグした時
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (blinkTimer == null){
                            db_interval = seekBar.getProgress();
                        }
                        DisplayScreen();
                    }
                    //ツマミに触れた時
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(db_interval);
                    }
                    //ツマミを離した時
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(db_interval);
                    }
                }
        );
        //  点滅間隔
        seek_volume = (SeekBar)findViewById(R.id.bar_volume);
        seek_volume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミをドラッグした時
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (!alarm.isPlaying()){
                            db_volume = seekBar.getProgress();
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (db_volume*3), 0);
                        }
                        DisplayScreen();
                    }
                    //ツマミに触れた時
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(db_volume);
                    }
                    //ツマミを離した時
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(db_volume);
                    }
                }
        );

    }

    /***************************************************
         画面描画処理
     **************************************************/
    public void DisplayScreen(){
        String tmp_str = "";

        TextView txt_gps = findViewById(R.id.text_gps_status);
        TextView txt_blink = findViewById(R.id.text_light);
        TextView txt_volume = findViewById(R.id.text_volume);
        TextView txt_mess = findViewById(R.id.text_mess);

        ImageButton img_gps = findViewById(R.id.btn_img_pos);
        ImageButton img_mail = findViewById(R.id.btn_img_mail);
        ImageButton img_light = findViewById(R.id.btn_img_light);
        ImageButton img_sound = findViewById(R.id.btn_img_sound);
        ImageButton img_mess = findViewById(R.id.btn_img_mess);

        //位置情報取得ならば
        if (now_ido == 0.0f || now_keido == 0.0f){
            if (get_GPS == false) {
                tmp_str = "上図アイコンをタップすると現在位置を取得します";
            }
            else{
                tmp_str = "現在位置・・・取得中・・・";
            }
            img_gps.setImageResource(R.drawable.gps0);
            img_mail.setImageResource(R.drawable.mail0);
        }
        else{
            tmp_str = "　経度:"+now_keido+"\n　緯度:"+now_ido;
            img_gps.setImageResource(R.drawable.gps1);
            img_mail.setImageResource(R.drawable.mail1);
        }
        txt_gps.setText(tmp_str);
        txt_gps.setTextColor(Color.GRAY);

        //ライト描画
        if (blinkTimer == null){
            img_light.setImageResource(R.drawable.light0);
        }
        else{
            img_light.setImageResource(R.drawable.light1);
        }
        if (db_interval == 0) {
            txt_blink.setText("常灯:" + db_interval);
        }
        else {
            txt_blink.setText("点滅:" + db_interval);
        }
        txt_blink.setTextColor(Color.GRAY);

        //サウンド描画
        if (alarm.isPlaying() == false){
            img_sound.setImageResource(R.drawable.sound0);
        }
        else{
            img_sound.setImageResource(R.drawable.sound1);
        }
        if (db_volume == 0) {
            txt_volume.setText("消音:" + db_volume);
        }
        else{
            txt_volume.setText("音量:" + db_volume);
        }
        txt_volume.setTextColor(Color.GRAY);

        //近距離メッセージ
       img_mess.setImageResource(R.drawable.mess1);
       txt_mess.setTextColor(Color.GRAY);
    }

    /***************************************************
         各種ボタン処理
     **************************************************/
    // POS
    public void onPos(View view){
        /*
        //タイマーインスタンス生成
        this.mainTimer1 = new Timer();
        //タスククラスインスタンス生成
        this.mainTimerTask1 = new MainTimerTask();
        //タイマースケジュール設定＆開始
        this.mainTimer1.schedule(mainTimerTask1, 0, 100);
         */
        locationStart();
        DisplayScreen();
    }
    // MAIL
    public void composeEmail(String addresses, String subject, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    public void mailSend() {
        /* 送信 */
        String all_mailaddr = "";
        String message = "";
        String mailtitle = "アプリ通知メール";
        message = "" + mess_mail + "\n";
        composeEmail(all_mailaddr, mailtitle, message);
    }
    public void onMail(View view){
        mailSend();
    }
    // Light
    public void onLight(View view){
        //常灯の時
        if (db_interval == 0){
            if (blinking) {
                light_OFF();
            }
            else{
                light_ON();
            }
        }
        //点滅の時
        else {
            if (blinkTimer == null) {
                light_ON();
            } else {
                light_OFF();
            }
        }
        DisplayScreen();
    }
    // Alarm
    public void onAlarm(View view){
        if (!alarm.isPlaying()){
            soundStart();
        }
        else{
            soundStop();
        }
        DisplayScreen();
    }
    // Message
    public void onMess(View view){
        setContentView(R.layout.activity_sub);
        DisplaySubScreen();
    }


    /***************************************************
        GPS位置情報取得
     **************************************************/
    public void locationStart() {
//        Log.d("debug", "locationStart()");

        now_ido = 0.0f;
        now_keido = 0.0f;

        // LocationManager インスタンス生成
        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager != null && locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER)) {

//            Log.d("debug", "location manager Enabled");
        } else {
            // GPSを設定するように促す
            Intent settingsIntent =
                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
//            Log.d("debug", "not gpsEnable, startActivity");
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);

//            Log.d("debug", "checkSelfPermission false");
            return;
        }

        List<String> providers = locationManager.getProviders(true);
        // 並行して、３のプロバイダーを起動する
        for (String provider : providers){
            locationManager.requestLocationUpdates(provider,
                    1000, 3, this);
        }
        get_GPS = true;
    }

    @Override
    public void onLocationChanged(Location location) {

        double ido = 0.0f;
        double keido = 0.0f;
        String tmp = "";
        int id = 0;

        /*
        //連続して取得する場合は、mainTimer1を使って制御すること
        if (this.mainTimer1 == null) {
            return;
        }
         */

        if (get_GPS) {
            get_GPS = false;
        } else {
            /* 取得ずみのためスキップ */
            return;
        }

        now_ido = location.getLatitude();
        now_keido = location.getLongitude();
        mess_mail = "現在の緯度,経度\n" + "http://maps.apple.com/?q=" + location.getLatitude() + "," + location.getLongitude();
        DisplayScreen();
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    /***************************************************
        ライト処理
    ****************************************************/
    /*************************************
     *   ライトＯＮ
     *********************************** */
    public void light_on_exec() {
        if(mCameraId == null){
            return;
        }
        try {
            mCameraManager.setTorchMode(mCameraId, blinking);
        } catch (CameraAccessException e) {
            //エラー処理
            e.printStackTrace();
        }
    }
    public void light_ON() {

        if (db_interval == 0){
            blinking = true;    //常時点灯
            light_on_exec();
        }
        else{
            this.blinkTimer = new Timer();
            this.blinkTimerTask = new BlinkingTask();
            this.blinkTimer.schedule(blinkTimerTask, (db_interval*100), (db_interval*100));
        }
    }
    /*************************************
     *   ライトＯＦＦ
     *********************************** */
    public void light_OFF() {
        if(mCameraId == null){
            return;
        }
        try {
            blinking = false;
            mCameraManager.setTorchMode(mCameraId, false);
        } catch (CameraAccessException e) {
            //エラー処理
            e.printStackTrace();
        }

        // スレッド停止
        if (this.blinkTimer != null) {
            this.blinkTimer.cancel();
            this.blinkTimer = null;
        }
    }

    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class MainTimerTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            mHandler.post(new Runnable() {
                public void run() {
                    if (get_GPS == false) {
                        locationStart();
                    }
//                    SubShow();
                }
            });
        }
    }
    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class BlinkingTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            bHandler.post( new Runnable() {
                public void run() {
                    light_on_exec();
                    if (blinking){
                        blinking = false;
                    }
                    else{
                        blinking = true;
                    }
                }
            });
        }
    }

    /***************************************************
         アラーム処理
     ****************************************************/
    /* 効果音スタート */
    public void soundStart(){

        if (!alarm.isPlaying()) {
            alarm.setLooping(true);
            alarm.start();
        }
    }

    /* 効果音ストップ */
    public void soundStop(){
        if (alarm != null) {
            //アラーム
            alarm.pause();
        }
    }


    /***************************************************
        サブ画面処理
     ****************************************************/
    public void DisplaySubScreen(){
        String tmp_text = "";

        TextView txt_blue_status = findViewById(R.id.text_blue_status);
        ImageButton img_blue = findViewById(R.id.btn_img_blue);

        if (true){

        }

    }

    // メイン画面へ遷移
    public void onMainSub(View view) {
        setContentView(R.layout.activity_main);
        DisplayScreen();
    }
    // ペアリング
    public void onBlueSub(View view) {
        DisplaySubScreen();

    }
    // テキスト
    public void onSendSub(View view) {
        DisplaySubScreen();
    }

}