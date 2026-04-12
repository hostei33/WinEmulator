package org.github.ewt45.winemulator.viewmodel

import android.system.OsConstants.SIGCONT
import android.system.OsConstants.SIGSTOP
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Consts.Pref.general_rootfs_lang
import org.github.ewt45.winemulator.Consts.Pref.general_shared_ext_path
import org.github.ewt45.winemulator.Consts.Pref.proot_bool_options
import org.github.ewt45.winemulator.Consts.Pref.proot_startup_cmd
import org.github.ewt45.winemulator.emu.Proot
import org.github.ewt45.winemulator.emu.ProotHelper
import org.github.ewt45.winemulator.emu.ProotRootfs
import java.io.File
import java.nio.charset.StandardCharsets

class TerminalViewModel : ViewModel() {
    private val TAG = "TerminalViewModel"

    /** TerminalSession 实例 - 使用 MutableState 以便 Compose 能观察到变化 */
    var session by mutableStateOf<TerminalSession?>(null)
        private set

    /** 终端输出 - 使用 SnapshotStateList 以便 Compose 能观察到变化 */
    val output = mutableStateListOf<String>()

    /**
     * 启动终端
     */
    suspend fun startTerminal(sessionClient: TerminalSessionClient) = withContext(Dispatchers.IO) {
        if (session != null) {
            stopTerminal()
        }
        
        val rootfs = Consts.rootfsCurrDir
        val tmpdir = Consts.tmpDir
        val lang = general_rootfs_lang.get()
        
        // 获取用户信息
        val userInfo = ProotRootfs.getPreferredUser(rootfs.canonicalFile.name)
        
        // 设置 fake data
        ProotHelper.setup_fake_data()
        
        // 构建 proot 命令
        val prootCmd = mutableListOf(
            Consts.prootBin.absolutePath,
            *proot_bool_options.get().toTypedArray(),
            "--kernel-release=${ProotHelper.DEFAULT_FAKE_KERNEL_VERSION}",
            "--rootfs=${rootfs.absolutePath}",
            "--change-id=${userInfo.uid}:${userInfo.gid}",
            "--cwd=${userInfo.home}",
            "--bind=${tmpdir.absolutePath}:/tmp",
            "--bind=${rootfs.absolutePath}/tmp:/dev/shm",
            "--bind=/sys",
            "--bind=/proc/self/fd:/dev/fd",
            "--bind=/proc",
            "--bind=/dev/urandom:/dev/random",
            "--bind=/dev",
        )

        // 添加 /dev/stderr, stdout, stdin 绑定（如果不存在）
        File("/dev/stderr").takeIf { !it.exists() }?.let {
            prootCmd.add("--bind=/proc/self/fd/2:/dev/stderr")
        }
        File("/dev/stdout").takeIf { !it.exists() }?.let {
            prootCmd.add("--bind=/proc/self/fd/1:/dev/stdout")
        }
        File("/dev/stdin").takeIf { !it.exists() }?.let {
            prootCmd.add("--bind=/proc/self/fd/0:/dev/stdin")
        }

        // 再次设置 fake data 并绑定 selinux
        ProotHelper.setup_fake_data()
        prootCmd.add("--bind=${rootfs.absolutePath}/sys/.empty:/sys/fs/selinux")
        
        // 添加 proc 文件的绑定（如果原生 /proc 无法读取）
        prootCmd.addAll(
            mapOf(
                "/proc/.loadavg" to "/proc/loadavg",
                "/proc/.stat" to "/proc/stat",
                "/proc/.uptime" to "/proc/uptime",
                "/proc/.version" to "/proc/version",
                "/proc/.vmstat" to "/proc/vmstat",
                "/proc/.sysctl_entry_cap_last_cap" to "/proc/sys/kernel/cap_last_cap",
                "/proc/.sysctl_inotify_max_user_watches" to "/proc/sys/fs/inotify/max_user_watches",
            ).mapNotNull { bindIfNotReadable(rootfs, it.key, it.value) }
        )
        
        // 添加共享文件夹绑定（清理可能存在的符号链接）
        prootCmd.addAll(general_shared_ext_path.get().map { bindPath ->
            File(rootfs, bindPath).runCatching { takeIf { FileUtils.isSymlink(it) }?.delete() }
            "--bind=$bindPath"
        })

        // 构建环境变量
        val loginEnvs = org.github.ewt45.winemulator.emu.EnvMap()
        
        // 读取 rootfs 中的 /etc/environment 文件
        readEtcEnvironment(rootfs, loginEnvs)
        
        loginEnvs.put("LANG", lang, true)
        loginEnvs.put("HOME", userInfo.home, true)
        loginEnvs.put("USER", userInfo.name, true)
        loginEnvs.put("TMPDIR", "/tmp", true)
        loginEnvs.put("DISPLAY", ":13", true)
        loginEnvs.put("PULSE_SERVER", "tcp:127.0.0.1:4713", true)
        
        // 构建最终的 shell 命令
        val shellCmd = mutableListOf(
            "/usr/bin/env",
            "-i",
            *loginEnvs.toArray(),
            userInfo.shell, "-l",
        )
        
        // 添加用户自定义启动命令
        proot_startup_cmd.get().takeIf { it.isNotBlank() }?.let { cmd ->
            shellCmd.addAll(listOf("-c", "$cmd &"))
        }
        
        prootCmd.addAll(shellCmd)
        
        val fullCommand = prootCmd.joinToString(" ")
        Log.d(TAG, "startTerminal: $fullCommand")
        
        // 环境变量（传递给 sh 进程）
        val envVars = listOf(
            "PROOT_TMP_DIR=${Consts.tmpDir.absolutePath}",
            "LD_PRELOAD=",
        )
        
        // 工作目录
        val cwd = rootfs.absolutePath
        
        // 创建 TerminalSession
        // 构造函数参数: (shellPath, cwd, args, env, transcriptRows, client)
        session = TerminalSession(
            "/system/bin/sh",  // shell 路径
            cwd,               // 工作目录
            arrayOf("-c", fullCommand),  // 参数：-c 和命令
            envVars.toTypedArray(),  // 环境变量
            2000,              // 回滚行数
            sessionClient      // TerminalSessionClient 回调
        )
        
        // 设置终端初始尺寸，触发进程启动
        session?.updateSize(120, 40)
        
        Proot.lastTimeCmd = "sh -c \\n" + prootCmd.joinToString(" \\n")
        Log.d(TAG, "使用以下参数启动proot: $fullCommand")
    }

    /**
     * 读取/etc/environment下的环境变量 并添加到 [envMap]
     */
    private fun readEtcEnvironment(rootfs: File, envMap: org.github.ewt45.winemulator.emu.EnvMap) {
        try {
            for (l in File(rootfs, "/etc/environment").readLines()) {
                val line = l.trim()
                line.takeIf { !line.startsWith('#') && line.contains('=') }?.let {
                    val split = line.split("=", limit = 2)
                    envMap.put(split[0], split[1].trim('\"'))
                }
            }
        } catch (e: Exception) {
            // 文件不存在或读取失败时忽略
        }
    }

    /**
     * 如果 [bindTo] 无法读取的话，绑定 File(rootfs, [bindFrom])。
     * @return --bind 的字符串，未绑定时返回null
     */
    private fun bindIfNotReadable(rootfs: File, bindFrom: String, bindTo: String): String? {
        return try {
            File(bindTo).takeUnless { it.exists() && it.canRead() }?.let {
                "--bind=${File(rootfs, bindFrom).absolutePath}:$bindTo"
            }
        } catch (e: Exception) {
            "--bind=${File(rootfs, bindFrom).absolutePath}:$bindTo"
        }
    }

    /**
     * 执行某个命令
     */
    fun runCommand(command: String) = viewModelScope.launch(Dispatchers.IO) {
        val currentSession = session ?: return@launch
        currentSession.write(command + "\n")
    }

    /**
     * 结束终端
     */
    fun stopTerminal() {
        session?.finishIfRunning()
        session = null
    }

    /**
     * viewModel销毁时结束终端
     */
    override fun onCleared() {
        super.onCleared()
        stopTerminal()
    }

    fun pauseTerminal() {
        val pid = session?.pid ?: return
        if (pid > 0) {
            android.os.Process.sendSignal(pid, SIGSTOP)
        }
    }

    fun resumeTerminal() {
        val pid = session?.pid ?: return
        if (pid > 0) {
            android.os.Process.sendSignal(pid, SIGCONT)
        }
    }
}