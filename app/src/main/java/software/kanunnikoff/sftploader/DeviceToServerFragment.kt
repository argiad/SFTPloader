package software.kanunnikoff.sftploader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
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
class DeviceToServerFragment : Fragment() {
    private var isStarted: Boolean = false
    private var future: Future<*>? = null
    private var executor: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), ThreadPoolExecutor.DiscardPolicy())
    var myView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (myView != null) {
            return myView
        }

        myView = inflater.inflate(R.layout.fragment_device_to_server, container, false)

        myView!!.findViewById<EditText>(R.id.destinationHostEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_HOST, ""))
        myView!!.findViewById<EditText>(R.id.destinationPortEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_PORT, ""))
        myView!!.findViewById<EditText>(R.id.destinationUserEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_USER, ""))
        myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_PASSWORD, ""))
        myView!!.findViewById<EditText>(R.id.destinationFileEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_REMOTE_FILE, ""))

        myView!!.findViewById<EditText>(R.id.sourceFileEditText).setText(this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.D2S_LOCAL_FILE, ""))

        myView!!.findViewById<EditText>(R.id.sourceFileEditText).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            this@DeviceToServerFragment.activity.startActivityForResult(intent, READ_REQUEST_CODE)
        }

        myView!!.findViewById<Button>(R.id.controlStartStopButton).setOnClickListener { button ->
            if (!Core.isConnected(this@DeviceToServerFragment.activity)) {
                Toast.makeText(this@DeviceToServerFragment.activity, "Check Internet connection!", Toast.LENGTH_LONG).show()
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
                val host = myView!!.findViewById<EditText>(R.id.destinationHostEditText).text.toString()
                val port = myView!!.findViewById<EditText>(R.id.destinationPortEditText).text.toString()
                val user = myView!!.findViewById<EditText>(R.id.destinationUserEditText).text.toString()
                val password = myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).text.toString()
                val remoteFile = myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString()

                val localFile = myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString()

                if (host.isEmpty() || port.isEmpty() || user.isEmpty() || password.isEmpty() || remoteFile.isEmpty() || localFile.isEmpty()) {
                    Toast.makeText(this@DeviceToServerFragment.activity, "Check your input data - some of them are empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = 0
                myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "0%"
                myView!!.findViewById<TextView>(R.id.controlLogEditText).text = ""
                myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"

                future = executor.submit({
                    Sftp.Uploader.upload(host, Integer.valueOf(port), user, password, this@DeviceToServerFragment.activity.contentResolver.openInputStream(Uri.parse(localFile)), remoteFile,
                            onStatusChanged = { status, isError ->
                                this@DeviceToServerFragment.activity.runOnUiThread {
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
                                this@DeviceToServerFragment.activity.runOnUiThread {
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
                                this@DeviceToServerFragment.activity.runOnUiThread {
                                    isStarted = false
                                    (button as Button).text = "START"
                                    button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nsuccessfully finished in $time ms"
                                    myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"
                                }
                            }
                    )
                })

                isStarted = true
                (button as Button).text = "STOP"
                button.setBackgroundColor(resources.getColor(R.color.activeTabColor))
            }
        }

        return myView
    }

    override fun onStop() {
        val editor = this@DeviceToServerFragment.activity.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).edit()

        editor.putString(Core.D2S_HOST, myView!!.findViewById<EditText>(R.id.destinationHostEditText).text.toString())
        editor.putString(Core.D2S_PORT, myView!!.findViewById<EditText>(R.id.destinationPortEditText).text.toString())
        editor.putString(Core.D2S_USER, myView!!.findViewById<EditText>(R.id.destinationUserEditText).text.toString())
        editor.putString(Core.D2S_PASSWORD, myView!!.findViewById<EditText>(R.id.destinationPasswordEditText).text.toString())
        editor.putString(Core.D2S_REMOTE_FILE, myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString())

        editor.putString(Core.D2S_LOCAL_FILE, myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString())

        editor.apply()

        super.onStop()
    }

    companion object {
        val READ_REQUEST_CODE = 2
    }
}