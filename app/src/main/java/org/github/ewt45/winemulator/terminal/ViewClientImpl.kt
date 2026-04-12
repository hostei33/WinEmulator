package org.github.ewt45.winemulator.terminal

import android.view.KeyEvent
import android.view.MotionEvent
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalViewClient
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * TerminalViewClient 的实现
 * 负责处理终端视图的各种交互回调
 */
class ViewClientImpl(
    private val activity: MainEmuActivity,
    private val sessionClient: SessionClientAImpl,
) : TerminalViewClient {

    private val terminalVm: TerminalViewModel
        get() = activity.terminalViewModel

    // ==================== 缩放和触摸 ====================

    override fun onScale(scale: Float): Float {
        // 返回新的字体大小比例
        // 1.0f 表示不缩放
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent?) {
        // 单击事件，通常用于显示软键盘
    }

    // ==================== 按键映射 ====================

    override fun shouldBackButtonBeMappedToEscape(): Boolean {
        // 返回 true 时，返回键映射为 Escape
        return false
    }

    override fun shouldEnforceCharBasedInput(): Boolean {
        // 是否强制使用字符输入模式
        return false
    }

    override fun shouldUseCtrlSpaceWorkaround(): Boolean {
        // 是否使用 Ctrl+Space 的变通方案
        return false
    }

    override fun isTerminalViewSelected(): Boolean {
        // 终端视图是否被选中
        return true
    }

    // ==================== 复制模式 ====================

    override fun copyModeChanged(copyMode: Boolean) {
        // 复制模式状态变化
    }

    // ==================== 键盘事件 ====================

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean {
        // 处理按键按下事件
        // 返回 true 表示已处理
        return false
    }

    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean {
        // 处理按键释放事件
        return false
    }

    override fun onLongPress(event: MotionEvent?): Boolean {
        // 长按事件
        return false
    }

    // ==================== 修饰键状态 ====================

    override fun readControlKey(): Boolean {
        // Ctrl 键是否按下
        return false
    }

    override fun readAltKey(): Boolean {
        // Alt 键是否按下
        return false
    }

    override fun readShiftKey(): Boolean {
        // Shift 键是否按下
        return false
    }

    override fun readFnKey(): Boolean {
        // Fn 键是否按下
        return false
    }

    // ==================== 字符输入 ====================

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean {
        // 处理单个字符输入
        // codePoint: Unicode 码点
        // ctrlDown: Ctrl 键是否按下
        // 返回 true 表示已处理
        return false
    }

    // ==================== 其他回调 ====================

    override fun onEmulatorSet() {
        // 终端模拟器设置完成
    }
}
