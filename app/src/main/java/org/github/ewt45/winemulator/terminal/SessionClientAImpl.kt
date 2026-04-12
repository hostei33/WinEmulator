package org.github.ewt45.winemulator.terminal

import android.util.Log
import com.termux.shared.settings.properties.TermuxPropertyConstants
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.MainEmuActivity
import java.lang.Exception

/**
 * TerminalSessionClient 的 Activity 实现
 * 负责处理终端会话的各种回调
 */
class SessionClientAImpl(
    private val activity: MainEmuActivity,
) : TerminalSessionClient {

    private val TAG = "SessionClientAImpl"

    /** 关联的 TerminalView */
    var terminalView: TerminalView? = null

    // ==================== 会话状态回调 ====================

    override fun onTextChanged(changedSession: TerminalSession?) {
        // 终端内容变化时，通知 TerminalView 重绘
        terminalView?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession?) {
        // 终端标题变化
        val title = changedSession?.title ?: "Terminal"
        Log.d(TAG, "终端标题变化: $title")
        // 可以在这里更新 Activity 标题
    }

    override fun onSessionFinished(finishedSession: TerminalSession?) {
        Log.i(TAG, "终端会话结束")
        // 会话结束时可以通知用户或自动重启
    }

    // ==================== 剪贴板操作 ====================

    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {
        text?.let {
            val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("terminal", it)
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "已复制到剪贴板: ${it.take(50)}...")
        }
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
            session?.write(text.toString())
        }
    }

    // ==================== 终端事件 ====================

    override fun onBell(session: TerminalSession?) {
        // 终端响铃，可以震动或播放声音
        Log.d(TAG, "终端响铃")
    }

    override fun onColorsChanged(session: TerminalSession?) {
        // 终端颜色变化
        terminalView?.onScreenUpdated()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        // 光标状态变化
        Log.d(TAG, "光标状态: $state")
    }

    // ==================== 样式设置 ====================

    override fun getTerminalCursorStyle(): Int {
        return TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_STYLE
    }

    // ==================== 日志回调 ====================

    override fun logError(tag: String?, message: String?) {
        Log.e(tag, message ?: "")
    }

    override fun logWarn(tag: String?, message: String?) {
        Log.w(tag, message ?: "")
    }

    override fun logInfo(tag: String?, message: String?) {
        Log.i(tag, message ?: "")
    }

    override fun logDebug(tag: String?, message: String?) {
        Log.d(tag, message ?: "")
    }

    override fun logVerbose(tag: String?, message: String?) {
        Log.v(tag, message ?: "")
    }

    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        Log.e(tag, message ?: "", e)
    }

    override fun logStackTrace(tag: String?, e: Exception?) {
        Log.e(tag, "", e)
    }
}
