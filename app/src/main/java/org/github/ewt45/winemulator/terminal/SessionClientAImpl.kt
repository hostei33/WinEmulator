package org.github.ewt45.winemulator.terminal

import android.util.Log
import com.termux.terminal.TerminalSession
import org.github.ewt45.winemulator.MainEmuActivity

/**
 * activity的session client
 */
class SessionClientAImpl(
    val activity: MainEmuActivity,
) : SessionClientBase() {
    
    override fun onTextChanged(changedSession: TerminalSession?) {
        Log.d("SessionClientAImpl", "终端文本变化")
    }

    override fun onTitleChanged(changedSession: TerminalSession?) {
        Log.d("SessionClientAImpl", "标题变化: ${changedSession?.title}")
    }

    override fun onSessionFinished(finishedSession: TerminalSession?) {
        Log.d("SessionClientAImpl", "会话结束: ${finishedSession?.title}")
    }
}