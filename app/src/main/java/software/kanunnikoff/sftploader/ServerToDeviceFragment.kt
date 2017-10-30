package software.kanunnikoff.sftploader

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import software.kanunnikoff.sftploader.core.Core
import software.kanunnikoff.sftploader.core.Sftp
import java.util.concurrent.*
import android.content.Intent
import android.net.Uri
import android.util.Log


/**
 * Created by dmitry on 12/10/2017.
 */
class ServerToDeviceFragment : Fragment() {
    private var isStarted: Boolean = false
    private var future: Future<*>? = null
    private var executor: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), ThreadPoolExecutor.DiscardPolicy())
    var myView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (myView != null) {
            return myView
        }

        myView = inflater.inflate(R.layout.fragment_server_to_device, container, false)

        myView!!.findViewById<EditText>(R.id.sourceHostEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_HOST, ""))
        myView!!.findViewById<EditText>(R.id.sourcePortEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_PORT, ""))
        myView!!.findViewById<EditText>(R.id.sourceUserEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_USER, ""))
        myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_PASSWORD, ""))
        myView!!.findViewById<EditText>(R.id.sourceFileEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_REMOTE_FILE, ""))

        myView!!.findViewById<EditText>(R.id.destinationFileEditText).setText(this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).getString(Core.S2D_LOCAL_FILE, ""))

        myView!!.findViewById<EditText>(R.id.destinationFileEditText).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_TITLE, "fileFromServer")
            this@ServerToDeviceFragment.activity!!.startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        myView!!.findViewById<Button>(R.id.controlStartStopButton).setOnClickListener { button ->
            if (!Core.isConnected(this@ServerToDeviceFragment.activity!!)) {
                Toast.makeText(this@ServerToDeviceFragment.activity!!, "Check Internet connection!", Toast.LENGTH_LONG).show()
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
                val host = myView!!.findViewById<EditText>(R.id.sourceHostEditText).text.toString()
                val port = myView!!.findViewById<EditText>(R.id.sourcePortEditText).text.toString()
                val user = myView!!.findViewById<EditText>(R.id.sourceUserEditText).text.toString()
                val password = myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).text.toString()
                val remoteFile = myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString()

                val localFile = myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString()

                if (host.isEmpty() || port.isEmpty() || user.isEmpty() || password.isEmpty() || remoteFile.isEmpty() || localFile.isEmpty()) {
                    Toast.makeText(this@ServerToDeviceFragment.activity, "Check your input data - some of them are empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                myView!!.findViewById<ProgressBar>(R.id.controlProgressBar).progress = 0
                myView!!.findViewById<TextView>(R.id.controlPercentTextView).text = "0%"
                myView!!.findViewById<TextView>(R.id.controlLogEditText).text = ""
                myView!!.findViewById<TextView>(R.id.controlSpeedTextView).text = "0 b/s"

                future = executor.submit({
                    Sftp.Downloader.download(host, Integer.valueOf(port), user, password, this@ServerToDeviceFragment.activity!!.contentResolver.openOutputStream(Uri.parse(localFile)), remoteFile,
                            onStatusChanged = { status, isError ->
                                this@ServerToDeviceFragment.activity!!.runOnUiThread {
                                    val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                    myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\n$status"

                                    if (isError) {
                                        future?.cancel(true)
                                        isStarted = false
                                        (button as Button).text = "START"
                                        button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                        val text = myView!!.findViewById<TextView>(R.id.controlLogEditText).text
                                        myView!!.findViewById<TextView>(R.id.controlLogEditText).text = "$text\nstopped"
                                    }
                                }
                            },
                            onCountChanged = { percent, speed ->
                                this@ServerToDeviceFragment.activity!!.runOnUiThread {
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
                                this@ServerToDeviceFragment.activity!!.runOnUiThread {
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

        return  myView
    }

    override fun onStop() {
        val editor = this@ServerToDeviceFragment.activity!!.getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE).edit()

        editor.putString(Core.S2D_HOST, myView!!.findViewById<EditText>(R.id.sourceHostEditText).text.toString())
        editor.putString(Core.S2D_PORT, myView!!.findViewById<EditText>(R.id.sourcePortEditText).text.toString())
        editor.putString(Core.S2D_USER, myView!!.findViewById<EditText>(R.id.sourceUserEditText).text.toString())
        editor.putString(Core.S2D_PASSWORD, myView!!.findViewById<EditText>(R.id.sourcePasswordEditText).text.toString())
        editor.putString(Core.S2D_REMOTE_FILE, myView!!.findViewById<EditText>(R.id.sourceFileEditText).text.toString())

        editor.putString(Core.S2D_LOCAL_FILE, myView!!.findViewById<EditText>(R.id.destinationFileEditText).text.toString())

        editor.apply()

        super.onStop()
    }

    companion object {
        val WRITE_REQUEST_CODE = 50
    }
}
