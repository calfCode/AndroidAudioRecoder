package com.example.androidaudiorecoder;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import android.Manifest;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("androidaudiorecoder");
    }
    private TextView recorder_time_tip;
    private Button recorder_btn;
    private static final int DISPLAY_RECORDING_TIME_FLAG = 100000;
    private int record = R.string.record;
    private int stop = R.string.stop;

    private boolean isRecording = false;
    private AudioRecordRecorderService recorderService;
    private String outputPCMPath = "/mnt/sdcard/vocal.pcm";
    private String outputAACPath;
    private Timer timer;
    private int recordingTimeInSecs = 0;
    private TimerTask displayRecordingTimeTask;
    private static final String TAG = "MainActivity2";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String appFilePath =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+File.separator;
        Log.d(TAG,"appFilePath="+appFilePath);
        outputPCMPath = appFilePath+"2.pcm";
        Log.d(TAG,"outputPath="+ outputPCMPath);
        recorder_time_tip = (TextView) findViewById(R.id.recorder_time_tip);
        recorder_btn = (Button) findViewById(R.id.recorder_btn);
        String timeTip = "00:00";
        recorder_time_tip.setText(timeTip);
        verifyAudioPermissions(this);
        bindListener();
        outputAACPath = appFilePath+"2.aac";

    }

    private void bindListener() {
        recorder_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    isRecording = false;
                    recorder_btn.setText(getString(record));
                    recordingTimeInSecs = 0;
                    recorderService.stop();
                    AudioEncoder audioEncoder = new AudioEncoder();
                    audioEncoder.encode(outputPCMPath,2,128 * 1024,48000,outputAACPath);
                    mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
                    displayRecordingTimeTask.cancel();
                    timer.cancel();
                } else {
                    isRecording = true;
                    recorder_btn.setText(getString(stop));
                    //启动AudioRecorder来录音
                    recorderService = AudioRecordRecorderService.getInstance();
                    try {
                        recorderService.initMetaData();
                        recorderService.start(outputPCMPath);
                        //启动一个定时器来监测时间
                        recordingTimeInSecs = 0;
                        timer = new Timer();
                        displayRecordingTimeTask = new TimerTask() {
                            @Override
                            public void run() {
                                mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
                                recordingTimeInSecs++;
                            }
                        };
                        timer.schedule(displayRecordingTimeTask, 0, 1000);
                    } catch (AudioConfigurationException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();;
                    } catch (StartRecordingException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();;
                    }
                }
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISPLAY_RECORDING_TIME_FLAG:
                    int minutes = recordingTimeInSecs / 60;
                    int seconds = recordingTimeInSecs % 60;
                    String timeTip = String.format("%02d:%02d", minutes, seconds);
                    recorder_time_tip.setText(timeTip);
                    break;
                default:
                    break;
            }
        }
    };

    //申请录音权限
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };

    /*
     * 申请录音权限*/
    public static void verifyAudioPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO,
                    GET_RECODE_AUDIO);
        }
    }
}