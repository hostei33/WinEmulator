package org.github.ewt45.winemulator.terminal

import android.util.Log
import org.github.ewt45.winemulator.MainEmuActivity

class ViewClientImpl(
    val activity: MainEmuActivity,
    val sessionClient: SessionClientAImpl,
) : ViewClientBase() {
    
    override fun onEmulatorSet() {
        Log.d("ViewClientImpl", "Emulator 已设置")
    }
    
    override fun isTerminalViewSelected() = true
    
    override fun shouldBackButtonBeMappedToEscape() = false
    
    override fun shouldEnforceCharBasedInput() = false
    
    override fun shouldUseCtrlSpaceWorkaround() = false
}
