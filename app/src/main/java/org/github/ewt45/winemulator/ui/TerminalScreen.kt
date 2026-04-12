package org.github.ewt45.winemulator.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.terminal.SessionClientAImpl
import org.github.ewt45.winemulator.terminal.ViewClientImpl

/**
 * 使用 termux TerminalView 显示终端和交互
 */
@Composable
fun TerminalScreen() {
    val activity = LocalActivity.current as MainEmuActivity

    TerminalScreenImpl(
        sessionClient = activity.sessionClient,
        viewClient = activity.viewClient,
        terminalViewModel = activity.terminalViewModel
    )
}

@Composable
private fun TerminalScreenImpl(
    sessionClient: SessionClientAImpl,
    viewClient: ViewClientImpl,
    terminalViewModel: org.github.ewt45.winemulator.viewmodel.TerminalViewModel,
) {
    // 终端视图引用
    val terminalViewRef = remember { mutableMapOf<String, TerminalView>() }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TerminalView(ctx).apply {
                    // 设置 ViewClient
                    setTerminalViewClient(viewClient)

                    // 设置基本属性
                    isFocusable = true
                    isFocusableInTouchMode = true
                    isVerticalScrollBarEnabled = true

                    // 绑定到 sessionClient
                    sessionClient.terminalView = this

                    // 保存引用
                    terminalViewRef["view"] = this

                    // 绑定已有的 session
                    terminalViewModel.session?.let { session ->
                        attachSession(session)
                    }
                }
            },
            update = { view ->
                // 更新时检查 session 是否变化
                val session = terminalViewModel.session
                if (session != null && view.currentSession != session) {
                    view.attachSession(session)
                }
            },
            onRelease = { view ->
                // 清理
                sessionClient.terminalView = null
                terminalViewRef.clear()
            }
        )
    }

    // 组件销毁时清理
    DisposableEffect(Unit) {
        onDispose {
            sessionClient.terminalView = null
        }
    }
}

@Preview
@Composable
fun TerminalScreenPreview() {
    // Preview 模式不显示实际终端
    Column(Modifier.fillMaxSize()) {
        // 可以显示占位符
    }
}
