package server

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.SocketException


class SpawnServerThread(private val listener: Listener) : Thread() {
    interface Listener {
        fun mobileConnected(name: String?, androidVersion: String?, SDKVersion: String?, port: String?)
    }

    private var isStopping = false
    private var ds: DatagramSocket? = null

    init {
        try {
            ds = DatagramSocket(5000)
        } catch (e: SocketException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        while (!isStopping) {
            val buffer = ByteArray(1024)
            var dp = DatagramPacket(buffer, buffer.size)
            try {
                ds!!.receive(dp)
                val localPort = randomFreePort
                val data = String(dp.data, 0, dp.length).split(",".toRegex()).toTypedArray()

                listener.mobileConnected(data[0], data[1], data[2], localPort)
                dp = DatagramPacket(localPort!!.toByteArray(), localPort.length, dp.address, dp.port)
                ds!!.send(dp)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        isStopping = true
        ds!!.close()
    }

    private val randomFreePort: String?
        private get() {
            var localPort: String? = null
            try {
                ServerSocket(0).use { s -> localPort = s.localPort.toString() + "" }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return localPort
        }
}
