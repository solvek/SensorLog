package com.skygolf.sensorlog;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skygolf.detector.MultiTapDetectorBuilder;
import com.skygolf.detector.MultiTapHelper;
import com.skygolf.detector.MultiTapListener;
import com.skygolf.detector.TapDetector;
import com.skygolf.detector.TapDetectorBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements SensorEventListener2 {
    private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    private static float avrFactor = 0.6f;
    private static long minDuration = 7000000;
    private static long maxDuration = 150000000;
    private static float minAccel = 1f;
    private static float maxAccel = 5f;
    private static float maxCos = -0.1f;

    /**
     * Maximal delay between taps, ms
     */
    private static long maxDelay = 500;

    SensorManager manager;
    Button buttonStart;
    Button buttonStop;
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;

    private TextView log;

    private final MultiTapListener multiTapListener = new MultiTapListener(){
        @Override
        public void onMultiTap(int tapsCount) {
            sayTaps(tapsCount);
        }
    };

    private final TapDetectorBuilder tapDetectorBuilder = new TapDetectorBuilder()
            .avrFactor(avrFactor)
            .duration(minDuration, maxDuration)
            .accelerationLimit(minAccel, maxAccel);

    private final MultiTapDetectorBuilder multiTapDetectorBuilder = new MultiTapDetectorBuilder()
            .maxDelay(maxDelay);

    private final TapDetector detector = MultiTapHelper.INSTANCE.createDetector(
            multiTapListener,
            tapDetectorBuilder,
            multiTapDetectorBuilder);

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.plant(new LogTree());

//        new TestDetectors().runTest();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Timber.tag(TAG).d("TTS initialized");
            }
        });

        isRunning = false;

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        log = findViewById(R.id.log);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                startNewFile();

                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), 0);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_PROXIMITY), 0);

                isRunning = true;
                return true;
            }
        });
        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                isRunning = false;
                manager.flush(MainActivity.this);
                manager.unregisterListener(MainActivity.this);
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void startNewFile(){
        Timber.tag(TAG).d( "Writing to %s", getStorageDir());
        try {
            writer = new FileWriter(new File(getStorageDir(), "sensors_" + System.currentTimeMillis() + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        addRow(
                0,
                "PARAMS1",
                avrFactor,
                minDuration,
                maxDuration,
                minAccel,
                maxAccel,
                maxCos);

        addRow(
                0,
                "PARAMS2",
                maxDelay,
                0,
                0,
                0,
                0,
                0);
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
        //  return "/storage/emulated/0/Android/data/com.iam360.sensorlog/";
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        if(!isRunning) return;

        addRow(evt);

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detector.registerAccel(evt.timestamp, evt.values[0], evt.values[1], evt.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void addRow(long eventTimeStamp, String event, float val1, float val2, float val3, float val4, float val5, float val6){
        String now = TIME_FORMAT.format(new Date());
        String out = String.format(Locale.US,
                "%s; %s; %f; %f; %f; %f; %f; %f; %d\n",
                now,
                event,
                val1,
                val2,
                val3,
                val4,
                val5,
                val6,
                eventTimeStamp);
        Timber.d(out);
        try {
            writer.write(out);
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Cannot add row to csv");
        }
    }

    private void addRow(SensorEvent event){
        float[] v = event.values;
        addRow(
                event.timestamp,
                eventType(event),
                nthItem(0, v),
                nthItem(1, v),
                nthItem(2, v),
                nthItem(3, v),
                nthItem(4, v),
                nthItem(5, v));
    }

    private static String eventType(SensorEvent event){
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER: return "ACC";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: return "GYRO_UN";
            case Sensor.TYPE_GYROSCOPE: return "GYRO";
            case Sensor.TYPE_MAGNETIC_FIELD: return "MAG";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "MAG_UN";
            case Sensor.TYPE_ROTATION_VECTOR: return "ROT";
            case Sensor.TYPE_GAME_ROTATION_VECTOR: return "ACC";
            case Sensor.TYPE_PROXIMITY: return "PROX";
        }

        return "UNKN";
    }

    private static float nthItem(int idx, float[] v){
        if (v == null || idx >= v.length) return 0.f;

        return v[idx];
    }

    private void sayTaps(int tapsCount) {
        String text = tapsCount+" times tap";
        switch (tapsCount){
            case 1: text = "Single tap"; break;
            case 2: text = "Double tap"; break;
            case 3: text = "Triple tap"; break;
        }
        Timber.tag(TAG).i("Saying text %s", text);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private class LogTree extends Timber.DebugTree {
        @Override
        protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
            super.log(priority, tag, message, t);
            assert tag != null;
            if (tag.startsWith("!")){
                runOnUiThread(() -> {
                    String text = log.getText().toString();
                    log.setText(message+"\r\n"+text);
                });
            }
        }
    }
}