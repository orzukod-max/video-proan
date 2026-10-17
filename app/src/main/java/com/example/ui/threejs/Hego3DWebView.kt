package com.example.ui.threejs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CameraState

class HegoWebInterface(
    private val onSceneReady: () -> Unit,
    private val onCameraStateChanged: (CameraState) -> Unit,
    private val onObjectClicked: (String) -> Unit
) {
    @JavascriptInterface
    fun onSceneReady() {
        Log.d("HegoWebInterface", "3D Scene is ready")
        onSceneReady()
    }

    @JavascriptInterface
    fun onCameraStateChanged(stateStr: String) {
        Log.d("HegoWebInterface", "Camera state changed: $stateStr")
        val state = when (stateStr) {
            "HEGO_HAND_HELD" -> CameraState.HEGO_HAND_HELD
            "DESK_STAND" -> CameraState.DESK_STAND
            else -> CameraState.TRANSITIONING
        }
        onCameraStateChanged(state)
    }

    @JavascriptInterface
    fun onObjectClicked(objectName: String) {
        Log.d("HegoWebInterface", "Object clicked: $objectName")
        onObjectClicked(objectName)
    }
}

class Hego3DController(private val webView: WebView?) {
    fun executeAction(
        action: String,
        targetObject: String? = null,
        mood: String = "happy",
        actionForce: Float = 5.0f,
        speech: String = ""
    ) {
        val targetArg = if (targetObject != null) "'$targetObject'" else "null"
        val escapedSpeech = speech.replace("'", "\\'").replace("\n", " ")
        val script = "window.hegoExecuteAction('$action', $targetArg, '$mood', $actionForce, '$escapedSpeech');"
        webView?.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun setSpeaking(isSpeaking: Boolean) {
        val script = "window.hegoSetSpeaking($isSpeaking);"
        webView?.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun setCameraMode(mode: String) {
        val script = "window.hegoSetCameraMode('$mode');"
        webView?.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun setMood(mood: String) {
        val script = "window.hegoSetMood('$mood');"
        webView?.post {
            webView.evaluateJavascript(script, null)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Hego3DWebView(
    modifier: Modifier = Modifier,
    onControllerReady: (Hego3DController) -> Unit,
    onSceneReady: () -> Unit = {},
    onCameraStateChanged: (CameraState) -> Unit = {},
    onObjectClicked: (String) -> Unit = {}
) {
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = when {
            result.resultCode == Activity.RESULT_OK && result.data != null -> {
                val data = result.data
                val clipData = data?.clipData
                when {
                    clipData != null -> {
                        val count = clipData.itemCount
                        Array(count) { i -> clipData.getItemAt(i).uri }
                    }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
            }
            else -> null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(Color.TRANSPARENT)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let {
                            try {
                                it.grant(it.resources)
                            } catch (e: Exception) {
                                Log.e("Hego3DWebView", "Error granting permission request", e)
                            }
                        }
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallbackParam: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = filePathCallbackParam

                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        return try {
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            Log.e("Hego3DWebView", "Failed to launch file chooser", e)
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                            false
                        }
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("Hego3DWebView", "Page loaded: $url")
                    }
                }

                addJavascriptInterface(
                    HegoWebInterface(
                        onSceneReady = onSceneReady,
                        onCameraStateChanged = onCameraStateChanged,
                        onObjectClicked = onObjectClicked
                    ),
                    "HegoAndroidBridge"
                )

                loadUrl("file:///android_asset/hego_scene.html")
                onControllerReady(Hego3DController(this))
            }
        }
    )
}
