package org.github.ewt45.winemulator.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.emu.Proot
import org.github.ewt45.winemulator.emu.ProotRootfs

class TerminalViewModel : ViewModel() {
    private val TAG = "TerminalViewModel"

    /** 当前的终端会话 */
    var session: TerminalSession? = null
        private set

    /** 会话是否运行中 */
    val isRunning: Boolean
        get() = session?.isRunning == true

    /**
     * 启动终端会话
     * @param sessionClient TerminalSessionClient 回调
     * @return 启动成功返回 session，失败返回 null
     */
    suspend fun startTerminal(sessionClient: TerminalSessionClient): TerminalSession? {
        if (session?.isRunning == true) {
            Log.w(TAG, "终端会话已在运行")
            return session
        }

        return withContext(Dispatchers.IO) {
            try {
                val prootProcessBuilder = Proot().attach()
                val envMap = prootProcessBuilder.environment()

                // 构建环境变量数组
                val env = envMap.map { "${it.key}=${it.value}" }.toTypedArray()

                // 获取工作目录
                val cwd = prootProcessBuilder.directory().absolutePath

                // 获取用户信息
                val rootfs = Consts.rootfsCurrDir
                val userInfo = ProotRootfs.getPreferredUser(rootfs.canonicalFile.name)

                // TerminalSession 构造函数:
                // (shellPath, cwd, env, transactRows, transactColumns, sessionClient)
                // 使用 proot 启动 shell
                val shellPath = userInfo.shell

                // 创建 TerminalSession
                session = TerminalSession(
                    shellPath,           // shell 路径
                    cwd,                 // 工作目录
                    env,                 // 环境变量数组
                    sessionClient        // session client 回调
                )

                Log.d(TAG, "终端会话已创建: shell=$shellPath, cwd=$cwd, running=${session?.isRunning}")
                Log.d(TAG, "proot 命令: ${Proot.lastTimeCmd}")

                session
            } catch (e: Exception) {
                Log.e(TAG, "启动终端失败", e)
                null
            }
        }
    }

    /**
     * 创建连接到指定 shell 的终端会话
     * @param sessionClient TerminalSessionClient 回调
     * @param shellPath shell 路径
     * @param env 环境变量
     * @param cwd 工作目录
     */
    fun createSession(
        sessionClient: TerminalSessionClient,
        shellPath: String,
        env: Array<String>,
        cwd: String
    ) {
        if (session?.isRunning == true) {
            Log.w(TAG, "终端会话已在运行，先停止")
            stopTerminal()
        }

        try {
            session = TerminalSession(
                shellPath,
                cwd,
                env,
                sessionClient
            )

            Log.d(TAG, "终端会话已创建: shell=$shellPath, cwd=$cwd")
        } catch (e: Exception) {
            Log.e(TAG, "创建终端会话失败", e)
        }
    }

    /**
     * 执行命令（写入到终端）
     */
    fun runCommand(command: String) {
        val s = session ?: run {
            Log.w(TAG, "终端会话未启动")
            return
        }

        if (!s.isRunning) {
            Log.w(TAG, "终端会话未运行")
            return
        }

        // 写入命令 + 换行符
        // TerminalSession.writeIfRunning 接受 String 参数
        s.write(command + "\n")
    }

    /**
     * 写入字符串到终端
     */
    fun write(text: String) {
        session?.write(text)
    }

    /**
     * 停止终端会话
     */
    fun stopTerminal() {
        session?.finishIfRunning()
        session = null
        Log.d(TAG, "终端会话已停止")
    }

    /**
     * 暂停终端
     */
    fun pauseTerminal() {
        Log.d(TAG, "pauseTerminal: 暂停功能待实现")
    }

    /**
     * 恢复终端
     */
    fun resumeTerminal() {
        Log.d(TAG, "resumeTerminal: 恢复功能待实现")
    }

    override fun onCleared() {
        super.onCleared()
        stopTerminal()
    }
}
