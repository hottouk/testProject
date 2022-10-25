package com.example.aop_part3_chapter03

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*

//-----------------------------------------------------------------------------------------생명주기
class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val requiredPermissions = arrayOf(
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //0 뷰 초기화
        requirePermission()
        clkOnOffBtn()
        clkSetAlarmTimeBtn()

        //1 데이터 가져오기
        val model = fetchDataFromSharedPreferences()
        //2 뷰에 데이터 그리기
        renderView(model)
    }

    //---------------------------------------------------------------------------------------권한요청
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requirePermission() {
        requestPermissions(requiredPermissions, REQUEST_POST_NOTIFICATION_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("alarmm","${requestCode}${grantResults}")
        val isPostNotificationPermissionGranted =
            requestCode == REQUEST_POST_NOTIFICATION_PERMISSION_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if(!isPostNotificationPermissionGranted){
            Toast.makeText(this, "권한 승인이 되지 않음", Toast.LENGTH_SHORT).show()
        }
    }


    //---------------------------------------------------------------------------------------버튼함수
    @RequiresApi(Build.VERSION_CODES.M)
    private fun clkSetAlarmTimeBtn() {
        val changeAlarmBtn = findViewById<Button>(R.id.change_alarm_btn)
        changeAlarmBtn.setOnClickListener {
            //0 현재 시간을 가져온다. 캘린더를 사용한다.
            val calendar = Calendar.getInstance()
            //1 TimePicker 다이얼로그 띄운다.
            TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { view, hour, minute ->
                    //데이터 저장,
                    val model = saveAlarmModel(hour, minute, false)
                    //뷰 업데이트,
                    renderView(model)
                    //기존에 있던 알람 삭제
                    cancelAlarm()
                },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun clkOnOffBtn() {
        val onOffBtn = findViewById<Button>(R.id.on_off_btn)
        onOffBtn.setOnClickListener {
            //0 데이터 확인
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not()) //not은 반전
            renderView(newModel)
            //1 온오프에 따라 작업 처리
            Log.d("alarmm", "On : ${newModel.onOff}")
            if (newModel.onOff) {
                //켜진 경우 --> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)
                    //지나간 시간의 경우 다음날 시간으로 알람을 등록한다.
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                //꺼진 경우 --> 알람을 제거
                cancelAlarm()
            }
            //2 데이터 저장; 위에서 saveAlarmModel을 통해 처리함.
        }
    }

    //----------------------------------------------------------------------------------------기타함수

    //알람 모델 저장; sharedPreferences에 사용자가 설정한 시각을 저장하고 설정 시각의 모델을 반환한다.
    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(//모델 인스턴스
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ON_OFF_KEY, model.onOff)
            commit()
        }
        return model
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.cancel()
    }


    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampm_textview).apply {
            text = model.amPmText
        }
        findViewById<TextView>(R.id.time_textview).apply {
            text = model.timeText
        }
        findViewById<Button>(R.id.on_off_btn).apply {
            text = model.onOffText
            tag = model
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "09:30"
        val onOffDBValue = sharedPreferences.getBoolean(ON_OFF_KEY, false)
        val alarmData = timeDBValue.split(":")
        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )
        //보정?? 왜 필요? 예외처리
        //sharedPreferences에 저장된 값과 나타난 값의 간극을 줄이기 위함.
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져있는데, 데이터는 켜져있는 경우?
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            //알람은 켜져있는데, 데이터는 꺼져있는 경우
            pendingIntent.cancel()
        }
        return alarmModel
    }


    companion object {
        const val ALARM_KEY = "alarm"
        const val ON_OFF_KEY = "onOff"
        const val SHARED_PREFERENCE_NAME = "time"
        const val ALARM_REQUEST_CODE = 1000
        const val REQUEST_POST_NOTIFICATION_PERMISSION_CODE = 100
    }
}