package org.github.ewt45.winemulator.terminal

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalViewClient
import org.github.ewt45.winemulator.MainEmuActivity
import java.lang.Exception

/**
 * TerminalViewClient 的实现
 * 负责处理终端视图的各种交互回调
 */
class ViewClientImpl(
    private val activity: MainEmuActivity,
    private val sessionClient: SessionClientAImpl,
) : TerminalViewClient {

    // ==================== 缩放和触摸 ====================

    override fun onScale(scale: Float): Float {
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent?) {}

    // ==================== 按键映射 ====================

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false

    override fun shouldEnforceCharBasedInput(): Boolean = false

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    // ==================== 复制模式 ====================

    override fun copyModeChanged(copyMode: Boolean) {}

    // ==================== 键盘事件 ====================

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false

    override fun onLongPress(event: MotionEvent?): Boolean = false

    // ==================== 修饰键状态 ====================

    override fun readControlKey(): Boolean = false

    override fun readAltKey(): Boolean = false

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    // ==================== 字符输入 ====================

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false

    // ==================== 其他回调 ====================

    override fun onEmulatorSet() {}

    // ==================== 日志方法 ====================

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