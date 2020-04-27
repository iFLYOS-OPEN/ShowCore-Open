package com.iflytek.cyber.iot.show.core.demo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core.action"
        private const val PERMISSION_PREFIX = "com.iflytek.cyber.iot.show.core.permission"

        const val ACTION_INTERCEPTOR_RESULT = "$ACTION_PREFIX.INTERCEPTOR_RESULT"
        const val ACTION_PLAY_TTS = "$ACTION_PREFIX.PLAY_TTS"
        const val PERMISSION_RECEIVE_BROADCAST = "$PERMISSION_PREFIX.RECEIVE_BROADCAST"

        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_TYPE = "type"
        const val EXTRA_TTS_TEXT = "tts_text"
        const val EXTRA_TTS_PATH = "tts_path"

        const val FILE_CHOOSE_CODE = 10010
        const val REQUEST_PERMISSION_CODE = 10011

        private val TTS_DIR = "${Environment.getExternalStorageDirectory()}/ShowCoreDemo"
        private val TTS_FILE_PATH =
            "$TTS_DIR/tts.mp3"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_INTERCEPTOR_RESULT -> {
                    Log.d("Receiver", intent.action.toString())
                    broadcast_action_name.text = intent.action
                    broadcast_type_value.text = intent.getStringExtra(EXTRA_TYPE)
                    broadcast_extra_value.text = intent.getStringExtra(EXTRA_PAYLOAD)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_INTERCEPTOR_RESULT)
        registerReceiver(receiver, intentFilter)

        tts_send_text.isChecked = true
        tts_send_type_group.setOnCheckedChangeListener { group, checkedId ->
            et_tts_text.isVisible = checkedId == R.id.tts_send_text
            tts_path_container.isVisible = checkedId == R.id.tts_send_path
        }

        btn_choose_tts_path.setOnClickListener {
            if (PermissionChecker.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "audio/mpeg"
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                try {
                    startActivityForResult(
                        Intent.createChooser(intent, "选择一个可播放的 TTS 文件"),
                        FILE_CHOOSE_CODE
                    )
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
                Toast.makeText(this, "请先授予读取外置存储权限", Toast.LENGTH_SHORT).show()
            }
        }
        tts_send.setOnClickListener {
            try {
                val intent = Intent(ACTION_PLAY_TTS)
                intent.setClassName(
                    "com.iflytek.cyber.iot.show.core",
                    "com.iflytek.cyber.iot.show.core.EngineService"
                )
                when (tts_send_type_group.checkedRadioButtonId) {
                    R.id.tts_send_path -> {
                        val file = File(TTS_FILE_PATH)
                        if (!file.exists()) {
                            Toast.makeText(this, "请先选择 TTS 文件", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        } else {
                            intent.putExtra(EXTRA_TTS_PATH, TTS_FILE_PATH)
                        }
                    }
                    R.id.tts_send_text -> {
                        val text = et_tts_text.text.toString()
                        if (text.isEmpty()) {
                            Toast.makeText(this, "请先选择 TTS 文件", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        } else {
                            intent.putExtra(EXTRA_TTS_TEXT, et_tts_text.text.toString())
                        }
                    }
                }
                startService(intent)
                Toast.makeText(this, "已发送", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this, "请求失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_CHOOSE_CODE -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data ?: return
                    Log.d("FileChooser", uri.toString())

                    try {
                        contentResolver.query(uri, null, null, null, null)?.let { cursor ->
                            cursor.moveToFirst()

                            val name =
                                cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                            tv_tts_file_name.text = name

                            cursor.close()
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }

                    try {
                        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return

                        val fd = fileDescriptor.fileDescriptor

                        val fis = FileInputStream(fd)

                        val ttsFileDir = File(TTS_DIR)
                        if (ttsFileDir.exists()) {
                            if (!ttsFileDir.isDirectory) {
                                ttsFileDir.mkdirs()
                            }
                        } else {
                            ttsFileDir.mkdirs()
                        }
                        val ttsFile = File(TTS_FILE_PATH)
                        if (ttsFile.exists()) {
                            ttsFile.delete()
                        }
                        ttsFile.createNewFile()

                        val fos = FileOutputStream(ttsFile)

                        val byteArray = ByteArray(1024)
                        var readSize = fis.read(byteArray)
                        while (readSize != -1) {
                            fos.write(byteArray, 0, readSize)
                            readSize = fis.read(byteArray)
                        }

                        fos.flush()

                        fis.close()
                        fos.close()

                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            }
        }
    }
}
