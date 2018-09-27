package com.cwt.liaohs.cwtdvrplus;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.view.SwitchButton;

/**
 * Created by liaohs on 2018/9/19.
 */

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener ,SwitchButton.OnCheckedChangeListener{
    private ImageButton backBut;
    private SwitchButton switchButton;
    private RadioGroup radio_double_record_setting;
    private RadioGroup radio_interval_setting;
    private RadioGroup radio_bitrate_setting;
    private RadioGroup radio_quality_setting;
    private RadioGroup radio_crash_sense_setting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();

        backBut = (ImageButton) findViewById(R.id.settings_back);
        radio_double_record_setting = (RadioGroup) findViewById(R.id.radio_double_record_setting);
        radio_interval_setting = (RadioGroup) findViewById(R.id.radio_interval_setting);
        radio_bitrate_setting = (RadioGroup) findViewById(R.id.radio_bitrate_setting);
        radio_quality_setting = (RadioGroup) findViewById(R.id.radio_quality_setting);
        radio_crash_sense_setting = (RadioGroup) findViewById(R.id.radio_crash_sense_setting);

        switchButton = (SwitchButton) findViewById(R.id.switch_button);
        switchButton.setShadowEffect(true);
        switchButton.setEnableEffect(true);

        if(RecordSettings.getRecordInterval() == 1){
            radio_interval_setting.check(R.id.radio_interval_1);
        }else if(RecordSettings.getRecordInterval() == 2){
            radio_interval_setting.check(R.id.radio_interval_2);
        }else if(RecordSettings.getRecordInterval() == 5){
            radio_interval_setting.check(R.id.radio_interval_5);
        }

        if(RecordSettings.getCrashSenseLevel() == ContextUtil.CLASH_LEVEL_LOW){
            radio_crash_sense_setting.check(R.id.radio_crash_sense_low);
        }else if(RecordSettings.getCrashSenseLevel() == ContextUtil.CLASH_LEVEL_MID){
            radio_crash_sense_setting.check(R.id.radio_crash_sense_mid);
        }else if(RecordSettings.getCrashSenseLevel() == ContextUtil.CLASH_LEVEL_HIG){
            radio_crash_sense_setting.check(R.id.radio_crash_sense_high);
        }

        if(RecordSettings.isQuality720p()){
            radio_quality_setting.check(R.id.radio_quality_720p);
        }else{
            radio_quality_setting.check(R.id.radio_quality_1080p);
        }

        switchButton.setChecked(RecordSettings.isCrashToggle());

        backBut.setOnClickListener(this);
        switchButton.setOnCheckedChangeListener(this);
        radio_double_record_setting.setOnCheckedChangeListener(this);
        radio_interval_setting.setOnCheckedChangeListener(this);
        radio_bitrate_setting.setOnCheckedChangeListener(this);
        radio_quality_setting.setOnCheckedChangeListener(this);
        radio_crash_sense_setting.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.settings_back:
                finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        switch (radioGroup.getId()) {
            case R.id.radio_double_record_setting:

                break;
            case R.id.radio_interval_setting:

                if (checkedId == R.id.radio_interval_1) {
                    RecordSettings.setRecordInterval(1);
                } else if (checkedId == R.id.radio_interval_2) {
                    RecordSettings.setRecordInterval(2);
                } else if (checkedId == R.id.radio_interval_5) {
                    RecordSettings.setRecordInterval(5);
                }

                break;
            case R.id.radio_bitrate_setting:
                if (checkedId == R.id.radio_bitrate_low) {
                    RecordSettings.setRecordBitRate(ContextUtil.BITRATE_LOW);
                } else if (checkedId == R.id.radio_bitrate_mid) {
                    RecordSettings.setRecordBitRate(ContextUtil.BITRATE_MID);
                } else if (checkedId == R.id.radio_bitrate_high) {
                    RecordSettings.setRecordBitRate(ContextUtil.BITRATE_HIG);
                }
                break;
            case R.id.radio_quality_setting:

                if(checkedId == R.id.radio_quality_720p){
                    RecordSettings.setQuality720p(true);
                }else if(checkedId == R.id.radio_quality_1080p){
                    RecordSettings.setQuality720p(false);
                }

                break;
            case R.id.radio_crash_sense_setting:

                if (checkedId == R.id.radio_crash_sense_low) {
                    RecordSettings.setCrashSenseLevel(ContextUtil.CLASH_LEVEL_LOW);
                } else if (checkedId == R.id.radio_crash_sense_mid) {
                    RecordSettings.setCrashSenseLevel(ContextUtil.CLASH_LEVEL_MID);
                } else if (checkedId == R.id.radio_crash_sense_high) {
                    RecordSettings.setCrashSenseLevel(ContextUtil.CLASH_LEVEL_HIG);
                }

                break;
        }
    }

    @Override
    public void onCheckedChanged(SwitchButton view, boolean isChecked) {
        RecordSettings.setCrashToggle(isChecked);
    }
}
