package org.github.ewt45.winemulator.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.terminal.ViewClientImpl
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

private val TAG = "TerminalScreen"

/**
 * 使用 termux 的 TerminalView 显示终端和交互
 */
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = (LocalActivity.current as MainEmuActivity).terminalViewModel,
) {
    val activity = LocalActivity.current as MainEmuActivity

    TerminalScreenImpl(
        getViewClient = { activity.viewClient },
        getSession = { viewModel.session },
    )
}

@Composable
private fun TerminalScreenImpl(
    getViewClient: () -> ViewClientImpl?,
    getSession: () -> com.termux.terminal.TerminalSession?,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    val session = getSession()

    Column(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewWidth = size.width
                viewHeight = size.height
            },
    ) {
        // 使用 key 确保当 session 为 null 时完全移除 AndroidView
        // 这可以避免 TerminalView 在 onSizeChanged 时访问空状态
        key(session) {
            session?.let { currentSession ->
                AndroidView(
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            getViewClient()?.let { setTerminalViewClient(it) }
                            isFocusableInTouchMode = true
                            isVerticalScrollBarEnabled = true
                            
                            // 监听视图树布局变化，自动更新终端尺寸
                            addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                                val newWidth = right - left
                                val newHeight = bottom - top
                                if (newWidth > 0 && newHeight > 0 && (newWidth != oldRight - oldLeft || newHeight != oldBottom - oldTop)) {
                                    android.util.Log.d(TAG, "布局变化，更新终端尺寸: ${newWidth}x${newHeight}")
                                    currentSession?.let {
                                        try {
                                            updateSize()
                                        } catch (e: Exception) {
                                            android.util.Log.e(TAG, "更新终端尺寸失败", e)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    update = { view ->
                        // 当 session 变化时，更新 TerminalView 的 session
                        if (view.currentSession != currentSession) {
                            android.util.Log.d(TAG, "绑定新的 TerminalSession")
                            view.attachSession(currentSession)
                            // 绑定后根据当前视图大小更新终端
                            if (viewWidth > 0 && viewHeight > 0) {
                                try {
                                    view.updateSize()
                                } catch (e: Exception) {
                                    android.util.Log.e(TAG, "更新终端尺寸失败", e)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun TerminalScreenPreview() {
    Column(Modifier.fillMaxSize()) {}
}
