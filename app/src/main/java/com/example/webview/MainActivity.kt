    package com.example.webview

    import android.annotation.SuppressLint
    import android.content.Intent
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.provider.Settings
    import android.view.View
    import android.webkit.WebView
    import android.webkit.WebViewClient
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.remember
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.viewinterop.AndroidView
    import com.example.webview.ui.theme.WebViewTheme
    import android.app.AlertDialog
    import android.widget.EditText

    class MainActivity : ComponentActivity() {
        // MQTT
        val MqttServerIP:String = "tcp://192.168.0.137:1883"  //mqtt 서버 IP
        val TOPIC:String = "did_in"	// did 토픽명

        private val handler = Handler(Looper.getMainLooper())
        // 프롬프트창이 활성화되어있는지 확인
        private var isPromptShowing = false

        companion object {
            const val PREFERENCES_NAME = "webview_app_preferences"
            const val KEY_MY_STRING = "my_string_value"
        }

        private fun saveString(value: String) {
            val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            preferences.edit().putString(KEY_MY_STRING, value).apply()
        }

        fun getString(): String? {
            val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            return preferences.getString(KEY_MY_STRING, null)
        }

        private fun promptForInput() {
            isPromptShowing = true
            val editText = EditText(this)
            val dialog = AlertDialog.Builder(this)
                .setTitle("MTStar dev Name")
                .setMessage("MTstar devname과 동일하게 설정해주세요.")
                .setView(editText)
                .setPositiveButton("저장") { _, _ ->
                    val userInput = editText.text.toString()
                    saveString(userInput)
                    isPromptShowing = false

                    // 저장된 값을 보여주는 새로운 AlertDialog를 표시
                    showSavedValueDialog(userInput)
                }
                .setOnCancelListener {
                    isPromptShowing = false
                }
                .create()
            dialog.show()
        }

        private fun showSavedValueDialog(savedValue: String) {
            val dialog = AlertDialog.Builder(this)
                .setTitle("저장된 기기명")
                .setMessage("$savedValue")
                .setPositiveButton("확인") { _, _ -> }
                .create()
            dialog.show()
        }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // 오버레이 권한 체크 및 요청
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(intent)
            }

            // Immersive 모드 설정
            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    hideSystemUI()
                }
            }

            hideSystemUI()

            setContent {
                WebViewTheme {
                    ComposeWebView(url = "https://www.naver.com", onTouch = { resetTimer() })
                }
            }
        }

        override fun onResume() {
            super.onResume()
            hideSystemUI()
            startTimerToLaunchApp()  // 타이머 시작 => MTstar로 넘어가는 타이머
            if (getString() == null && !isPromptShowing) {
                promptForInput()
            }
        }

        private fun hideSystemUI() {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        override fun onPause() {
            super.onPause()
            handler.removeCallbacksAndMessages(null)
        }

        private fun startTimerToLaunchApp() {

            handler.postDelayed({
                launchApp("com.zcadc.main") // => mtstar 앱
                startTimerToLaunchApp()
                startService(Intent(this, OverlayService::class.java))
            }, 150000) // 150000 2분 30초동안 터지가 없으면 MTstar로 넘어감
        }

        private fun launchApp(packageName: String) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        }

        fun resetTimer() {
            handler.removeCallbacksAndMessages(null)
            startTimerToLaunchApp()
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    fun ComposeWebView(url: String, onTouch: () -> Unit) {
        val context = LocalContext.current

        val webView = remember {
            WebView(context).apply {
                webViewClient = WebViewClient()
                loadUrl(url)

                setOnTouchListener { _, _ ->
                    onTouch()
                    true

                }
            }
        }

        AndroidView({ webView }) { view ->
            view.loadUrl(url)
        }
    }
