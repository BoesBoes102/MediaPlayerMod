package dev.boes.mediaplayer.media.windows

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

internal class ComException(val hresult: Int, operation: String) :
    RuntimeException("$operation failed with HRESULT 0x%08X".format(hresult))

@Suppress("FunctionName")
internal interface Combase : Library {
    fun RoInitialize(initType: Int): Int
    fun WindowsCreateString(source: WString, length: Int, string: PointerByReference): Int
    fun WindowsDeleteString(string: Pointer?): Int
    fun WindowsGetStringRawBuffer(string: Pointer?, length: IntByReference): Pointer?
    fun RoGetActivationFactory(activatableClassId: Pointer, iid: GUID, factory: PointerByReference): Int

    companion object {
        val INSTANCE: Combase by lazy { Native.load("combase", Combase::class.java) }
    }
}

@Suppress("FunctionName")
internal interface Shcore : Library {
    fun CreateStreamOverRandomAccessStream(randomAccessStream: Pointer, iid: GUID, stream: PointerByReference): Int

    companion object {
        val INSTANCE: Shcore by lazy { Native.load("shcore", Shcore::class.java) }
    }
}

internal fun checkHr(hresult: Int, operation: String) {
    if (hresult < 0) throw ComException(hresult, operation)
}

internal class ComObject(val pointer: Pointer) : AutoCloseable {
    private var released = false

    private fun function(index: Int): Function {
        val vtable = pointer.getPointer(0)
        return Function.getFunction(vtable.getPointer(index.toLong() * Native.POINTER_SIZE))
    }

    fun invoke(index: Int, vararg args: Any?): Int = function(index).invokeInt(arrayOf(pointer, *args))

    fun call(index: Int, vararg args: Any?) = checkHr(invoke(index, *args), "vtable[$index]")

    fun getObject(index: Int, vararg args: Any?): ComObject? {
        val result = PointerByReference()
        call(index, *args, result)
        return result.value?.let(::ComObject)
    }

    fun getString(index: Int): String {
        val result = PointerByReference()
        call(index, result)
        return HString.consume(result.value)
    }

    fun getInt(index: Int): Int {
        val result = IntByReference()
        call(index, result)
        return result.value
    }

    fun getLong(index: Int): Long {
        val result = LongByReference()
        call(index, result)
        return result.value
    }

    fun getFloat(index: Int): Float {
        val result = FloatByReference()
        call(index, result)
        return result.value
    }

    fun queryInterface(iid: GUID): ComObject? {
        val result = PointerByReference()
        val hr = invoke(0, iid, result)
        return if (hr < 0) null else result.value?.let(::ComObject)
    }

    override fun close() {
        if (released) return
        released = true
        invoke(2)
    }
}

internal object HString {
    fun create(value: String): Pointer {
        val result = PointerByReference()
        checkHr(Combase.INSTANCE.WindowsCreateString(WString(value), value.length, result), "WindowsCreateString")
        return result.value
    }

    fun consume(handle: Pointer?): String {
        if (handle == null) return ""
        return try {
            val length = IntByReference()
            val buffer = Combase.INSTANCE.WindowsGetStringRawBuffer(handle, length)
            if (buffer == null || length.value == 0) "" else String(buffer.getCharArray(0, length.value))
        } finally {
            Combase.INSTANCE.WindowsDeleteString(handle)
        }
    }
}

internal object WinRt {
    private const val RO_INIT_MULTITHREADED = 1
    private const val RPC_E_CHANGED_MODE = 0x80010106.toInt()

    private const val ASYNC_STARTED = 0
    private const val ASYNC_COMPLETED = 1
    private const val ASYNC_CANCELED = 2

    private const val ASYNC_INFO_STATUS = 7
    private const val ASYNC_INFO_ERROR_CODE = 8
    private const val ASYNC_INFO_CANCEL = 9
    private const val ASYNC_OPERATION_GET_RESULTS = 8

    private val IID_ASYNC_INFO = GUID("{00000036-0000-0000-C000-000000000046}")

    fun initializeThread() {
        val hr = Combase.INSTANCE.RoInitialize(RO_INIT_MULTITHREADED)
        if (hr != RPC_E_CHANGED_MODE) checkHr(hr, "RoInitialize")
    }

    fun activationFactory(className: String, iid: GUID): ComObject {
        val classId = HString.create(className)
        try {
            val result = PointerByReference()
            checkHr(Combase.INSTANCE.RoGetActivationFactory(classId, iid, result), "RoGetActivationFactory($className)")
            return ComObject(result.value)
        } finally {
            Combase.INSTANCE.WindowsDeleteString(classId)
        }
    }

    fun await(operation: ComObject, timeoutMillis: Long) {
        val info = operation.queryInterface(IID_ASYNC_INFO) ?: throw IllegalStateException("Object is not an async operation")
        info.use {
            val deadline = System.nanoTime() + timeoutMillis * 1_000_000
            while (true) {
                when (it.getInt(ASYNC_INFO_STATUS)) {
                    ASYNC_COMPLETED -> return
                    ASYNC_CANCELED -> throw IllegalStateException("Async operation was canceled")
                    ASYNC_STARTED -> {
                        if (System.nanoTime() > deadline) {
                            it.invoke(ASYNC_INFO_CANCEL)
                            throw IllegalStateException("Async operation timed out")
                        }
                        Thread.sleep(2)
                    }

                    else -> throw ComException(it.getInt(ASYNC_INFO_ERROR_CODE), "Async operation")
                }
            }
        }
    }

    fun awaitObject(operation: ComObject, timeoutMillis: Long): ComObject? {
        await(operation, timeoutMillis)
        return operation.getObject(ASYNC_OPERATION_GET_RESULTS)
    }
}
