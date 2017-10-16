package software.kanunnikoff.sftploader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import software.kanunnikoff.sftploader.core.Core
import software.kanunnikoff.sftploader.core.Sftp
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by dmitry on 12/10/2017.
 */
class ServerToServerFragment : Fragment() {

    private var isStarted: Boolean = false
    private var future: Future<*>? = null
    private var executor: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), ThreadPoolExecutor.DiscardPolicy())
    var myView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (myView != null) {
            return myView
        }

        myView = inflater.inflate(R.layout.fragment_server_to_server, container, false)

        myView!!.findViewById<EditText>(R.id.sourceHostEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_HOST1, ""))
        myView!!.findViewById<EditText>(R.id.sourcePortEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_PORT1, ""))
        myView!!.findViewById<EditText>(R.id.sourceUserEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_USER1, ""))
        myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_PASSWORD1, ""))
        myView!!.findViewById<EditText>(R.id.sourceFileEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_REMOTE_FILE1, ""))

        myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_LOCAL_FILE, ""))

        myView!!.findViewById<EditText>(R.id.destinationHostEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_HOST2, ""))
        myView!!.findViewById<EditText>(R.id.destinationPortEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_PORT2, ""))
        myView!!.findViewById<EditText>(R.id.destinationUserEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_USER2, ""))
        myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_PASSWORD2, ""))
        myView!!.findViewById<EditText>(R.id.destinationFileEditText).setText(this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2S_REMOTE_FILE2, ""))

        myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_TITLE, "fileFromServer")
            this@ServerToServerFragment.activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        myView!!.findViewById<Button>(R.id.controlStartStopButton).setOnClickListener { button ->
            if (!Core.isConnected(this@ServerToServerFragment.activity)) {
                Toast.makeText(this@ServerToServerFragment.activity, "Check Internet connection!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (isStarted) {
                future?.cancel(true)
                isStarted = false
                (button as Button).text = "START"
                button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nstopped by user"
                myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
            } else {
                val host1 = myView!!.findViewById<EditText>(R.id.sourceHostEditText).text.toString()
                val port1 = myView!!.findViewById<EditText>(R.id.sourcePortEditText).text.toString()
                val user1 = myView!!.findViewById<EditText>(R.id.sourceUserEditText).text.toString()
                val password1 = myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).text.toString()
                val remoteFile1 = myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString()

                val localFile = myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).text.toString()

                val host2 = myView!!.findViewById<EditText>(R.id.destinationHostEditText).text.toString()
                val port2 = myView!!.findViewById<EditText>(R.id.destinationPortEditText).text.toString()
                val user2 = myView!!.findViewById<EditText>(R.id.destinationUserEditText).text.toString()
                val password2 = myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).text.toString()
                val remoteFile2 = myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString()

                if (host1.isEmpty() || port1.isEmpty() || user1.isEmpty() || password1.isEmpty() || remoteFile1.isEmpty() || localFile.isEmpty() ||
                        host2.isEmpty() || port2.isEmpty() || user2.isEmpty() || password2.isEmpty() || remoteFile2.isEmpty()) {
                    Toast.makeText(this@ServerToServerFragment.activity, "Check your input data - some of them are empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = 0
                myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "0%"
                myView!!.findViewById<TextView>(R.id.controlLogEditText).text = ""
                myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"

                future = executor.submit({
                    Sftp.Downloader.download(host1, Integer.valueOf(port1), user1, password1, this@ServerToServerFragment.activity.contentResolver.openOutputStream(Uri.parse(localFile)), remoteFile1,
                            onStatusChanged = { status, isError ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\n$status"

                                    if (isError) {
                                        future?.cancel(true)
                                        isStarted = false
                                        (button as Button).text = "START"
                                        button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                        val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                        myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nstopped"
                                        myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                    }
                                }
                            },
                            onCountChanged = { percent, speed ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = percent
                                    myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "$percent%"

                                    if (speed != null) {
                                        when {
                                            speed / 1024 / 1024 > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed mb/s"
                                            speed / 1024 > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed kb/s"
                                            speed > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed b/s"
                                            else -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                        }
                                    }
                                }
                            },
                            onEnd = { time ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nsuccessfully finished in $time ms"
                                    myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"

                                    myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = 0
                                    myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "0%"
                                }
                            }
                    )

                    Sftp.Uploader.upload(host2, Integer.valueOf(port2), user2, password2, this@ServerToServerFragment.activity.contentResolver.openInputStream(Uri.parse(localFile)), remoteFile2,
                            onStatusChanged = { status, isError ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\n$status"

                                    if (isError) {
                                        future?.cancel(true)
                                        isStarted = false
                                        (button as Button).text = "START"
                                        button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                        val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                        myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nstopped"
                                        myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                    }
                                }
                            },
                            onCountChanged = { percent, speed ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = percent
                                    myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "$percent%"

                                    if (speed != null) {
                                        when {
                                            speed / 1024 / 1024 > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed mb/s"
                                            speed / 1024 > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed kb/s"
                                            speed > 0 -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "$speed b/s"
                                            else -> myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                        }
                                    }
                                }
                            },
                            onEnd = { time ->
                                this@ServerToServerFragment.activity.runOnUiThread {
                                    isStarted = false
                                    (button as Button).text = "START"
                                    button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nsuccessfully finished in $time ms"
                                    myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                }
                            }
                    )

                    if (DocumentsContract.deleteDocument(this@ServerToServerFragment.activity.contentResolver, Uri.parse(myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).text.toString()))) {
                        this@ServerToServerFragment.activity.runOnUiThread {
                            val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                            myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nmiddle layer file has been deleted from device"

                            val editor = this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).edit()
                            editor.putString(Core.S2S_LOCAL_FILE, "")
                            editor.apply()

                            myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).setText("")
                        }
                    }
                })

                isStarted = true
                (button as Button).text = "STOP"
                button.setBackgroundColor(resources.getColor(R.color.activeTabColor))
            }
        }

        return myView
    }

    override fun onStop() {
        val editor = this@ServerToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).edit()

        editor.putString(Core.S2S_HOST1, myView!!.findViewById<EditText>(R.id.sourceHostEditText).text.toString())
        editor.putString(Core.S2S_PORT1, myView!!.findViewById<EditText>(R.id.sourcePortEditText).text.toString())
        editor.putString(Core.S2S_USER1, myView!!.findViewById<EditText>(R.id.sourceUserEditText).text.toString())
        editor.putString(Core.S2S_PASSWORD1, myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).text.toString())
        editor.putString(Core.S2S_REMOTE_FILE1, myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString())

        editor.putString(Core.S2S_LOCAL_FILE, myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).text.toString())

        editor.putString(Core.S2S_HOST2, myView!!.findViewById<EditText>(R.id.destinationHostEditText).text.toString())
        editor.putString(Core.S2S_PORT2, myView!!.findViewById<EditText>(R.id.destinationPortEditText).text.toString())
        editor.putString(Core.S2S_USER2, myView!!.findViewById<EditText>(R.id.destinationUserEditText).text.toString())
        editor.putString(Core.S2S_PASSWORD2, myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).text.toString())
        editor.putString(Core.S2S_REMOTE_FILE2, myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString())

        editor.apply()

        super.onStop()
    }

    companion object {
        val WRITE_REQUEST_CODE = 3
    }
}