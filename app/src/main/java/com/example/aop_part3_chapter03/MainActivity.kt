package com.example.aop_part3_chapter03

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


//-----------------------------------------------------------------------------------------생명주기
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}