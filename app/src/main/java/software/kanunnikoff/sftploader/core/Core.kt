package software.kanunnikoff.sftploader.core

import android.content.Context
import android.net.ConnectivityManager


/**
 * Created by dmitry on 13/10/2017.
 */
object Core {
    val APP_TAG = "SFTPloader"

    val S2D_HOST = "s2d_host"
    val S2D_PORT = "s2d_port"
    val S2D_USER = "s2d_user"
    val S2D_PASSWORD = "s2d_password"
    val S2D_REMOTE_FILE = "s2d_remote_file"
    val S2D_LOCAL_FILE = "s2d_local_file"

    val D2S_HOST = "d2s_host"
    val D2S_PORT = "d2s_port"
    val D2S_USER = "d2s_user"
    val D2S_PASSWORD = "d2s_password"
    val D2S_REMOTE_FILE = "d2s_remote_file"
    val D2S_LOCAL_FILE = "d2s_local_file"

    val S2S_HOST1 = "s2s_host1"
    val S2S_PORT1 = "s2s_port1"
    val S2S_USER1 = "s2s_user1"
    val S2S_PASSWORD1 = "s2s_password1"
    val S2S_REMOTE_FILE1 = "s2s_remote_file1"
    val S2S_LOCAL_FILE = "s2s_local_file"
    val S2S_HOST2 = "s2s_host2"
    val S2S_PORT2 = "s2s_port2"
    val S2S_USER2 = "s2s_user2"
    val S2S_PASSWORD2 = "s2s_password2"
    val S2S_REMOTE_FILE2 = "s2s_remote_file2"

    fun isConnected(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo

        return info != null && info.isConnected
    }
}