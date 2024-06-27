package meetnote3.utils

import meetnote3.model.ProcessInfo
import platform.darwin.CTL_KERN
import platform.darwin.KERN_PROC
import platform.darwin.KERN_PROC_ALL
import platform.darwin.kinfo_proc
import platform.darwin.sysctl
import platform.posix.getpid
import platform.posix.size_tVar

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
fun getProcesses(): List<ProcessInfo> {
    val mib = intArrayOf(CTL_KERN, KERN_PROC, KERN_PROC_ALL)
    val bufferSize = nativeHeap.alloc<size_tVar>()
    sysctl(mib.refTo(0), mib.size.toUInt(), null, bufferSize.ptr, null, 0u)

    val buffer = nativeHeap.allocArray<kinfo_proc>(bufferSize.value.toInt() / sizeOf<kinfo_proc>())
    sysctl(mib.refTo(0), mib.size.toUInt(), buffer, bufferSize.ptr, null, 0u)

    val processList = mutableListOf<ProcessInfo>()
    for (i in 0 until bufferSize.value.toInt() / sizeOf<kinfo_proc>()) {
        val proc = buffer[i]
        processList.add(ProcessInfo(proc.kp_proc.p_pid, proc.kp_eproc.e_ppid, proc.kp_proc.p_comm.toKString()))
    }
    return processList
}

fun getChildProcesses(parentPid: Int): List<ProcessInfo> {
    val processes = getProcesses()
    return processes.filter { it.ppid == parentPid }
}

@OptIn(ExperimentalForeignApi::class)
fun getChildProcs(): List<ProcessInfo> {
    memScoped {
        val parentPid = getpid()
        return getChildProcesses(parentPid)
    }
}
