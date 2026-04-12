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
import java.io.File

class TerminalViewModel : ViewModel() {
    private val TAG = "TerminalViewModel"

    /** 当前的终端会话 */
    var session: TerminalSession? = null
        private set

    /** SessionClient 回调，由外部设置 */
    var sessionClient: TerminalSessionClient? = null

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

        this.sessionClient = sessionClient

        return withContext(Dispatchers.IO) {
            try {
                val prootProcessBuilder = Proot().attach()
                val envMap = prootProcessBuilder.environment()

                // 构建环境变量数组
                val env = envMap.map { "${it.key}=${it.value}" }.toTypedArray()

                // 获取工作目录
                val cwd = prootProcessBuilder.directory().absolutePath

                // 构建命令 - 使用 proot 启动 shell
                val prootCmd = mutableListOf(
                    Consts.prootBin.absolutePath,
                    *Consts.Pref.proot_bool_options.get().toTypedArray(),
                )

                // 从原始命令中提取 proot 参数
                val originalCmd = prootProcessBuilder.command()
                if (originalCmd.size >= 3 && originalCmd[0] == "sh" && originalCmd[1] == "-c") {
                    // 解析 proot 命令
                    val cmdStr = originalCmd[2]
                    Log.d(TAG, "startTerminal: proot 命令: $cmdStr")
                }

                // 直接使用 ProcessBuilder 的命令
                val process = prootProcessBuilder.start()

                // 创建 TerminalSession，连接到 proot 进程
                // 注意：TerminalSession 需要一个可执行的 shell 路径
                // 我们这里使用 proot 启动的进程
                val rootfs = Consts.rootfsCurrDir
                val userInfo = org.github.ewt45.winemulator.emu.ProotRootfs.getPreferredUser(rootfs.canonicalFile.name)

                // 使用 proot 命令作为 shell
                val shellPath = userInfo.shell

                session = TerminalSession(
                    shellPath,           // executablePath: shell 路径
                    cwd,                 // workingPath: 工作目录
                    env,                 // env: 环境变量
                    sessionClient,       // sessionClient: 回调
                    Proot.lastTimeCmd    // initialCommand: 初始命令（用于显示）
                ).also {
                    Log.d(TAG, "终端会话已创建: ${it.isRunning}")
                }

                session
            } catch (e: Exception) {
                Log.e(TAG, "启动终端失败", e)
                null
            }
        }
    }

    /**
     * 创建连接到 proot 进程的终端会话
     * @param sessionClient TerminalSessionClient 回调
     * @param prootCmd proot 完整命令
     * @param env 环境变量
     * @param cwd 工作目录
     */
    fun createSession(
        sessionClient: TerminalSessionClient,
        prootCmd: Array<String>,
        env: Array<String>,
        cwd: String
    ) {
        if (session?.isRunning == true) {
            Log.w(TAG, "终端会话已在运行，先停止")
            stopTerminal()
        }

        this.sessionClient = sessionClient

        try {
            // 使用第一个命令作为 shell (通常是 proot)
            val shellPath = prootCmd.firstOrNull() ?: "/system/bin/sh"

            session = TerminalSession(
                shellPath,
                cwd,
                env,
                sessionClient,
                null // initialCommand
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
        command.forEach { char ->
            s.write(char.code)
        }
        s.write('\n'.code)
    }

    /**
     * 写入单个字符到终端
     */
    fun write(codePoint: Int) {
        session?.write(codePoint)
    }

    /**
     * 写入字符串到终端
     */
    fun write(text: String) {
        text.forEach { char ->
            session?.write(char.code)
        }
    }

    /**
     * 停止终端会话
     */
    fun stopTerminal() {
        session?.finishIfRunning()
        session = null
        sessionClient = null
        Log.d(TAG, "终端会话已停止")
    }

    /**
     * 暂停终端
     */
    fun pauseTerminal() {
        // TerminalSession 没有直接的暂停方法
        // 可以通过发送 SIGSTOP 信号实现
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