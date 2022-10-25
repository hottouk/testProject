package com.example.aop_part3_chapter03

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        createNotificationChannel(context)
        notifyNotification(context)
    }

    //일정 버젼 이상에서 notification하려면 체널을 만들어야 한다.
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "기상 알람", NotificationManager.IMPORTANCE_HIGH
            ) //체널 설정!! 인자(ID, 이름, 중요도)
            NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
            //체널 생성!! 인자(설정한 체널)
        }
    }


    private fun notifyNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            val build = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("알람")
                .setContentText("일어날 시간입니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            notify(NOTIFICATION_ID, build.build())
        }
    }


    companion object {
        const val NOTIFICATION_CHANNEL_ID = "1000"
        const val NOTIFICATION_ID  = 100
    }
}