package com.example.aop_part3_chapter03

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*

//-------------------------------------------------------------------------------------------생명주기
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //0 뷰 초기화
        clkOnOffBtn()
        clkSetAlarmTimeBtn()
        //1 데이터 가져오기
        val model = fetchDataFromSharedPreferences()
        //2 뷰에 데이터 그리기
        renderView(model)
    }

    //---------------------------------------------------------------------------------------버튼함수
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
                    cancelAlarm() //첫 클릭이라면 여기는 null 값이다.
                },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun clkOnOffBtn() {
        val onOffBtn = findViewById<Button>(R.id.on_off_btn)
        onOffBtn.setOnClickListener {
            //0. 데이터 확인
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not()) //not은 반전
            renderView(newModel)
            //1. 온오프에 따라 작업 처리
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
                    PendingIntent.FLAG_UPDATE_CURRENT
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

    //--------------------------------------------------------------------------------------기타함수
    //알람 모델 저장; sharedPreference에 사용자가 설정한 시각을 저장하고 설정 시각의 모델을 반환한다.
    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(  //모델 인스턴스 생성
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


    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
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
    //sp에서 데이터 가져와서 알람 모델 반환한다.
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE) //시간 재설정에서 입력한 값
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "09:30" //널처리
        val onOffDBValue = sharedPreferences.getBoolean(ON_OFF_KEY, false)          //널처리 필요 없음 bool은 둘중 하나,
        val alarmData = timeDBValue.split(":")  //split은 List반환
        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),        //sp에 저장한 값 꺼내와서 알람 모델 인스턴스 생성
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )
        //보정?? 왜 필요? 예외처리인데 어떤 경우에 나타나는지 알아보자.
        //sharedPreference에 저장된 값과 나타난 값이 다를 경우
        val pendingIntent = PendingIntent.getBroadcast( //알람 등록되어있는지 확인하기 위해 가져온다.
            this,                       //context
            ALARM_REQUEST_CODE,                 //상수 1000
            Intent(this, AlarmReceiver::class.java), //intent
            PendingIntent.FLAG_NO_CREATE        //이미 생성된 PendingIntent 가 있다면 재사용 (없으면 Null 리턴)
        )
        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져있는데, 데이터는 on인 경우? 불일치하므로
            alarmModel.onOff = false    //데이터를 off로 한다.
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            //알람은 켜져있는데, 데이터는 off인 경우
            pendingIntent.cancel()      //알람을 취소한다.
        }
        return alarmModel
    }


    companion object {
        const val ALARM_KEY = "alarm"
        const val ON_OFF_KEY = "onOff"
        const val SHARED_PREFERENCE_NAME = "time"
        const val ALARM_REQUEST_CODE = 1000
    }
}