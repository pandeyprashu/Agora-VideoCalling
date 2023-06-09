package com.example.agoravideocallingapp


import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.agoravideocallingapp.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private val appId="App id mentioned in the agora api"
    private val channelName="VideoCall"
    private val token="Token issued"
    private val uid=0

    private var isJoined=false
    private var agoraEngine:RtcEngine?=null
    private var localSurfaceView:SurfaceView?=null
    private var remoteSurfaceView:SurfaceView?=null


    private val PERMISSION_ID=1
    private val requestPermission=
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )

    private fun checkSelfPermission():Boolean{
        return !(ContextCompat.checkSelfPermission(this,
            requestPermission[0])!=PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(this,
            requestPermission[1])!=PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message:String){
        runOnUiThread {
            Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!checkSelfPermission()){
            ActivityCompat.requestPermissions(
                this,requestPermission,PERMISSION_ID
            )
        }

        setupVideoSDKEngine()

        binding.JoinButton.setOnClickListener {
            joinChannel()
        }

        binding.LeaveButton.setOnClickListener {
            leaveChannel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread{
            RtcEngine.destroy()
            agoraEngine=null
        }.start()
    }

    fun leaveChannel() {
        if(!isJoined){
            showMessage("Join a channel First")
        }else{
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if(remoteSurfaceView!=null)remoteSurfaceView!!.visibility= GONE
            if(localSurfaceView!=null)localSurfaceView!!.visibility= GONE
            isJoined=false

        }
    }
    fun joinChannel() {
        if(checkSelfPermission()){
            val option=ChannelMediaOptions()
            option.channelProfile=Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType=Constants.CLIENT_ROLE_BROADCASTER

            setupLocalVideo()
            localSurfaceView!!.visibility=VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token,channelName,uid,option)
        }else{
            showMessage("Permission Not Granted")
        }



    }
    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")

            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("User Offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = binding.remoteVideoViewContainer
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = binding.localVideoViewContainer
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }


}
