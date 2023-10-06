package com.example.webview

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val handler = Handler(Looper.getMainLooper())
    val MqttServerIP: String = "tcp://192.168.0.137:1883"  //mqtt 서버 IP
    val TOPIC: String = "did_in"    // did 토픽명

    private var isScreenOff = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getString(): String? {
        val preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE)
        return preferences.getString(MainActivity.KEY_MY_STRING, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // MQTT 설정
        // 30분후에 화면종료
        handler.postDelayed({
            sendScreenOnOff(0)
            isScreenOff = true
        }, 1800000) // 1800000 30분


        // 오버레이에 사용할 뷰 설정
        overlayView = FrameLayout(this)
        overlayView.setBackgroundColor(Color.TRANSPARENT)  // 흰색 배경

        // 오버레이에 터치 이벤트
        overlayView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isScreenOff) {
                    sendScreenOnOff(1)  // 화면을 켬
                    isScreenOff = false
                    // 30분후에 화면종료
                    handler.postDelayed({
                        sendScreenOnOff(0)
                        isScreenOff = true
                    }, 1800000) // 1800000 30분
                } else {
                    // 30분 후 화면종료 이벤트 제거하고 MainActivity로 돌아감
                    handler.removeCallbacksAndMessages(null)
                    stopSelf()  // 오버레이 종료
                    // com.example.webview => MainActivity 실행
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            true
        }

        // 오버레이의 레이아웃 파라미터 설정
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 오버레이 추가
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayView)
    }

    private fun sendScreenOnOff(onOffValue: Int) {
        var mqttClient: MqttClient? = null
        mqttClient = MqttClient(MqttServerIP, MqttClient.generateClientId(), null) //연결 설정
        if (!mqttClient.isConnected) {
            mqttClient.connect()
        }
        val jsonObject = JSONObject()
        jsonObject.put("cMarksNames", getString())
        jsonObject.put("screenOnOff", onOffValue)
        val jsonString = jsonObject.toString()
        val message = MqttMessage(jsonString.toByteArray())
        message.qos = 2
        mqttClient.publish(TOPIC, message) // 메세지전송
    }
}