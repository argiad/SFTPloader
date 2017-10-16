package software.kanunnikoff.sftploader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import software.kanunnikoff.sftploader.core.Core
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val serverToDeviceFragment = ServerToDeviceFragment()
    private val deviceToServerFragment = DeviceToServerFragment()
    private val serverToServerFragment = ServerToServerFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = MyPagerAdapter(supportFragmentManager)

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.server_to_device))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.device_to_server))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.server_to_server))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        viewPager.offscreenPageLimit = 3

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        MobileAds.initialize(applicationContext, resources.getString(R.string.banner_ad_unit_id))
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
    }

    private inner class MyPagerAdapter internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                SERVER_TO_DEVICE_TAB -> serverToDeviceFragment
                DEVICE_TO_SERVER_TAB -> deviceToServerFragment
                else -> serverToServerFragment
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_rate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)))
            }
            R.id.nav_share -> {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
                intent.putExtra(Intent.EXTRA_TEXT, "Google Play: https://play.google.com/store/apps/details?id=" + packageName)
                intent.type = "text/plain"
                startActivity(Intent.createChooser(intent, "Share..."))
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == ServerToDeviceFragment.WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                serverToDeviceFragment.myView!!.findViewById<EditText>(R.id.destinationFileEditText).setText(resultData.data.toString())
            }
        } else if (requestCode == DeviceToServerFragment.READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                deviceToServerFragment.myView!!.findViewById<EditText>(R.id.sourceFileEditText).setText(resultData.data.toString())
            }
        } else if (requestCode == ServerToServerFragment.WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Log.i(Core.APP_TAG, "*** uri from SAF: ${resultData.data}")
                serverToServerFragment.myView!!.findViewById<EditText>(R.id.middleLayerFileEditText).setText(resultData.data.toString())
            }
        }
    }

    companion object {
        val SERVER_TO_DEVICE_TAB = 0
        val DEVICE_TO_SERVER_TAB = 1
    }
}
