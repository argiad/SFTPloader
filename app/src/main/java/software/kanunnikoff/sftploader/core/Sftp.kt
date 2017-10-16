package software.kanunnikoff.sftploader.core

/**
 * Created by dmitry on 12/10/2017.
 */

import android.util.Log
import com.jcraft.jsch.*
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream

object Sftp {
    /**
     * Класс для скачивания файла.
     */
    object Downloader {

        fun download(host: String, port: Int, user: String, password: String, localFile: OutputStream, sourceFile: String, onStatusChanged: (status: String, isError: Boolean) -> Unit, onCountChanged: (percent: Int, speed: Int?) -> Unit, onEnd: (ms: Long) -> Unit) {
            try {
                val jsch = JSch()

                val session = jsch.getSession(user, host, port)
                session.userInfo = MyUserInfo(password)
                onStatusChanged("downloader: session connect...", false)
                session.connect()
                onStatusChanged("downloader: session connected", false)

                val channel = session.openChannel("sftp")
                onStatusChanged("downloader: channel connect...", false)
                channel.connect()
                onStatusChanged("downloader: channel connected", false)

                val channelSftp = channel as ChannelSftp

                try {
                    onStatusChanged("downloader: start downloading", false)
                    channelSftp.get(sourceFile, localFile, MyProgressMonitor(onCountChanged, onEnd), ChannelSftp.OVERWRITE, 0)
                } catch (cause: SftpException) {
                    if (cause !is InterruptedIOException && !cause.toString().contains("InterruptedIOException") && !cause.toString().contains("4:")) {
                        onStatusChanged("downloader: error ${cause}", true)
                    }

                    Log.i(Core.APP_TAG, "downloader: error", cause)
                }

                channelSftp.exit()
                session.disconnect()
            } catch (cause: Exception) {
                if (cause !is InterruptedIOException && !cause.toString().contains("InterruptedIOException") && !cause.toString().contains("4:")) {
                    onStatusChanged("downloader: error ${cause}", true)
                }

                Log.i(Core.APP_TAG, "downloader: error", cause)
            }

            Log.i(Core.APP_TAG, "exiting from download method")
        }
    }

    /**
     * Класс для закачивания файла.
     */
    object Uploader {

        fun upload(host: String, port: Int, user: String, password: String, localFile: InputStream, destinationFile: String, onStatusChanged: (status: String, isError: Boolean) -> Unit, onCountChanged: (percent: Int, speed: Int?) -> Unit, onEnd: (ms: Long) -> Unit) {
            try {
                val jsch = JSch()

                val session = jsch.getSession(user, host, port)
                session.userInfo = MyUserInfo(password)
                onStatusChanged("uploader: session connect...", false)
                session.connect()
                onStatusChanged("uploader: session connected", false)

                val channel = session.openChannel("sftp")
                onStatusChanged("uploader: channel connect...", false)
                channel.connect()
                onStatusChanged("uploader: channel connected", false)

                val channelSftp = channel as ChannelSftp

                try {
                    onStatusChanged("uploader: start uploading", false)
                    channelSftp.put(localFile, destinationFile, MyProgressMonitor(localFile.available(), onCountChanged, onEnd), ChannelSftp.OVERWRITE)
                } catch (cause: SftpException) {
                    if (cause !is InterruptedIOException && !cause.toString().contains("InterruptedIOException") && !cause.toString().contains("4:")) {
                        onStatusChanged("uploader: error ${cause}", true)
                    }

                    Log.i(Core.APP_TAG, "uploader: error", cause)
                }

                channelSftp.exit()

                session.disconnect()
            } catch (cause: Exception) {
                if (cause !is InterruptedIOException && !cause.toString().contains("InterruptedIOException") && !cause.toString().contains("4:")) {
                    onStatusChanged("uploader: error ${cause}", true)
                }

                Log.i(Core.APP_TAG, "uploader: error", cause)
            }
        }
    }

    /**
     * Класс для авторизации на SSH-сервере.
     */
    private class MyUserInfo(private val password: String) : UserInfo, UIKeyboardInteractive {

        override fun getPassword(): String {
            return password
        }

        override fun promptYesNo(str: String): Boolean {
            return true
        }

        override fun getPassphrase(): String? {
            return null
        }

        override fun promptPassphrase(message: String): Boolean {
            return true
        }

        override fun promptPassword(message: String): Boolean {
            return true
        }

        override fun showMessage(message: String) {}

        override fun promptKeyboardInteractive(destination: String, name: String, instruction: String, prompt: Array<String>, echo: BooleanArray): Array<String> {
            return arrayOf(password)
        }
    }

    /**
     * Класс для отслеживания прогресса скачивания/закачивания файла.
     */
    private class MyProgressMonitor(val onCountChanged: (percent: Int, speed: Int?) -> Unit, val onEnd: (ms: Long) -> Unit) : SftpProgressMonitor {

        constructor(fileSize: Int, onCountChanged: (percent: Int, speed: Int?) -> Unit, onEnd: (ms: Long) -> Unit) : this(onCountChanged, onEnd) {
            this.fileSize = fileSize
        }

        private var count: Long = 0
        private var max: Long = 0
        private var percent: Int = 0
        private var fileSize: Int = 0

        private var oldCount: Long = 0
        private var oldTime: Long = System.currentTimeMillis()

        private var time0: Long = 0

        override fun init(op: Int, src: String, dest: String, max: Long) {
            if (max > 0) {
                this.max = max
            } else {
                this.max = fileSize.toLong()
            }

            this.count = 0
            this.percent = -1

            time0 = System.currentTimeMillis()
        }

        override fun count(count: Long): Boolean {
            this.count += count

            if (percent >= this.count * 100 / max) {
                return true
            }

            percent = (this.count * 100 / max).toInt()



            var speed: Int? = null
            val timeDiff = (System.currentTimeMillis() - oldTime) / 1000

            if (timeDiff >= 1) {
                speed = ((this.count - oldCount) / timeDiff).toInt()
                oldCount = this.count
                oldTime = System.currentTimeMillis()
            }


            onCountChanged(percent, speed)

            return true
        }

        override fun end() {
            onEnd((System.currentTimeMillis() - time0))
        }
    }
}
