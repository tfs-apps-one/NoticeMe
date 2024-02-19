package tfsapps.noticeme;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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

    //緯度、軽度
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
    }

    public void locationStart() {
//        Log.d("debug", "locationStart()");

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

        if (this.mainTimer1 == null) {
            return;
        }

        if (get_GPS) {
            get_GPS = false;
        } else {
            /* 取得ずみのためスキップ */
            return;
        }

        ido = location.getLatitude();
        keido = location.getLongitude();

        now_ido = location.getLatitude();
        now_keido = location.getLongitude();

/*
        Toast toast = Toast.makeText(this,
                "UPDATE="+GPS_type+"!!\n" + "緯度：" + ido + "　経度：" + keido, Toast.LENGTH_SHORT);
        toast.show();
*/
//        setContentView(R.layout.activity_sub);

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

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
}