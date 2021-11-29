package com.mooner.starlight.ui.debugroom

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.mooner.starlight.databinding.ActivityDebugRoomBinding
import com.mooner.starlight.models.DebugRoomMessage
import com.mooner.starlight.plugincore.config.CategoryConfigObject
import com.mooner.starlight.plugincore.config.config
import com.mooner.starlight.plugincore.core.Session
import com.mooner.starlight.plugincore.core.Session.globalConfig
import com.mooner.starlight.plugincore.core.Session.json
import com.mooner.starlight.plugincore.models.ChatSender
import com.mooner.starlight.plugincore.models.DebugChatRoom
import com.mooner.starlight.plugincore.models.Message
import com.mooner.starlight.plugincore.project.Project
import com.mooner.starlight.plugincore.utils.Icon
import com.mooner.starlight.ui.config.ParentAdapter
import com.mooner.starlight.ui.debugroom.DebugRoomChatAdapter.Companion.CHAT_SELF
import com.mooner.starlight.ui.debugroom.DebugRoomChatAdapter.Companion.CHAT_SELF_LONG
import com.mooner.starlight.utils.PACKAGE_KAKAO_TALK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.*

class DebugRoomActivity: AppCompatActivity() {

    companion object {
        private const val RESULT_BOT      = 100
        private const val RESULT_USER     = 101

        private const val CHATS_FILE_NAME = "chats.json"
    }

    private val mutex: Mutex by lazy { Mutex(locked = false) }
    private lateinit var chatList: MutableList<DebugRoomMessage>
    private var userChatAdapter: DebugRoomChatAdapter? = null
    val dir: File by lazy {
        val directory = project.directory.resolve("debugroom")
        if (!directory.exists()) directory.mkdirs()
        directory
    }

    private var roomName: String = "undefined"
    private var sender: String = "debug_sender"
    private var botName: String = "BOT"

    private lateinit var project: Project
    private lateinit var binding: ActivityDebugRoomBinding
    private var recyclerAdapter: ParentAdapter? = null
    private var imageHash: Int = 0

    private val onSend: (msg: String) -> Unit = { msg ->
        val viewType = if (msg.length >= 500)
            DebugRoomChatAdapter.CHAT_BOT_LONG
        else
            DebugRoomChatAdapter.CHAT_BOT
        addMessage(botName, msg, viewType)
    }
    private val onMarkAsRead: () -> Unit = {
        Snackbar.make(binding.root, "읽음 처리 호출됨", Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.messageInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                send(binding.messageInput.text.toString())
            }
            false
        }

        /*
        imageHash = intent.getIntExtra("imageHash", 0)
        sender = intent.getStringExtra("sender")?: "debug_sender"
         */
        imageHash = 0
        val projectName = intent.getStringExtra("projectName")?: "undefined"
        binding.roomTitle.text = roomName

        project = Session.projectManager.getProject(projectName)!!

        updateConfig()

        val listFile = dir.resolve(CHATS_FILE_NAME)
        chatList = if (listFile.exists() && listFile.isFile)
            json.decodeFromString(listFile.readText())
        else
            mutableListOf()

        userChatAdapter = DebugRoomChatAdapter(this, chatList)

        binding.chatRecyclerView.apply {
            adapter = userChatAdapter
            layoutManager = LinearLayoutManager(this@DebugRoomActivity)
            if (chatList.isNotEmpty()) {
                scrollToPosition(chatList.size - 1)
            }
        }

        binding.sendButton.setOnClickListener {
            if (binding.messageInput.text.toString().isBlank()) return@setOnClickListener
            send(binding.messageInput.text.toString())
            binding.messageInput.setText("")
        }

        recyclerAdapter = ParentAdapter(this) { parentId, id, _, data ->
            globalConfig.edit {
                getCategory(parentId)[id] = data
            }
            updateConfig()
        }.apply {
            data = getConfig()
            saved = globalConfig.getAllConfigs()
            notifyDataSetChanged()
        }

        binding.bottomSheet.configRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DebugRoomActivity)
            adapter = recyclerAdapter
        }

        binding.leave.setOnClickListener {
            finish()
        }
    }

    private fun send(message: String) {
        val viewType = if (message.length >= 500) CHAT_SELF_LONG else CHAT_SELF
        addMessage(sender, message, viewType)
        val data = Message(
            message = message,
            sender = ChatSender(
                name = sender,
                profileBase64 = "",
                profileHash = imageHash
            ),
            room = DebugChatRoom(
                name = roomName,
                isGroupChat = false,
                onSend = onSend,
                onMarkAsRead = onMarkAsRead
            ),
            packageName = "com.kakao.talk",
            hasMention = false
        )
        project.callEvent(
            name = "onMessage",
            args = arrayOf(data)
        )
    }

    private fun addMessage(sender: String, msg: String, viewType: Int) {
        chatList += if (msg.length >= 500) {
            val fileName = UUID.randomUUID().toString() + ".chat"
            File(dir, "chats").apply {
                mkdirs()
                resolve(fileName).writeText(msg)
            }
            DebugRoomMessage(
                sender = sender,
                message = msg.substring(0..500).replace("\u200b", ""),
                fileName = fileName,
                viewType = viewType
            )
        } else {
            DebugRoomMessage(
                sender = sender,
                message = msg,
                viewType = viewType
            )
        }
        runOnUiThread {
            userChatAdapter?.notifyItemInserted(chatList.size)
            binding.messageInput.setText("")
            binding.chatRecyclerView.post {
                binding.chatRecyclerView.smoothScrollToPosition(chatList.size - 1)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val encoded = mutex.withLock {
                json.encodeToString(chatList.toList())
            }
            File(dir, CHATS_FILE_NAME).writeText(encoded)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userChatAdapter = null
        recyclerAdapter = null
    }

    private fun updateConfig() {
        roomName = globalConfig.getCategory("d_room").getString("name", "undefined")
        sender = globalConfig.getCategory("d_user").getString("name", "debug_sender")
        botName = globalConfig.getCategory("d_bot").getString("name", "BOT")

        runOnUiThread {
            binding.roomTitle.text = roomName
        }
    }

    private fun openImagePicker(resultCode: Int) {
        ImagePicker.with(this@DebugRoomActivity).apply {
            galleryOnly()
            crop(1f, 1f)
            maxResultSize(512, 512)
            saveDir(cacheDir.resolve("DebugRoom"))
            galleryMimeTypes(
                mimeTypes = arrayOf(
                    "image/png",
                    "image/jpg",
                    "image/jpeg"
                )
            )
        }.start(resultCode)
    }

    private fun reloadConfig() {
        if (recyclerAdapter != null) {
            with(recyclerAdapter!!) {
                val preSize = data.size
                data = listOf()
                notifyItemRangeRemoved(0, preSize)
                data = getConfig()
                notifyItemRangeRemoved(0, data.size)
            }
        }
    }

    private fun getConfig(): List<CategoryConfigObject> {
        return config {
            category {
                id = "d_room"
                textColor = color { "#706EB9" }
                items = items {
                    string {
                        id = "name"
                        title = "방 이름"
                        icon = Icon.BOOKMARK
                        iconTintColor = color { "#706EB9" }
                        require = { text ->
                            if (text.isBlank()) "이름을 입력해주세요."
                            else null
                        }
                    }
                    string {
                        id = "package"
                        title = "앱 패키지"
                        icon = Icon.LIST_BULLETED
                        hint = "ex) $PACKAGE_KAKAO_TALK"
                        defaultValue = PACKAGE_KAKAO_TALK
                        iconTintColor = color { "#706EB9" }
                    }
                    toggle {
                        id = "is_group_chat"
                        title = "isGroupChat"
                        icon = Icon.MARK_CHAT_UNREAD
                        iconTintColor = color { "#706EB9" }
                        defaultValue = false
                    }
                }
            }
            category {
                id = "d_bot"
                title = "봇"
                textColor = color { "#706EB9" }
                items = items {
                    string {
                        id = "name"
                        title = "이름"
                        icon = Icon.BOOKMARK
                        iconTintColor = color { "#706EB9" }
                        require = { text ->
                            if (text.isBlank()) "이름을 입력해주세요."
                            else null
                        }
                    }
                    button {
                        id = "set_profile_image"
                        title = "프로필 이미지 설정"
                        val profileImagePath = globalConfig.getCategory("d_bot").getString("profile_image_path")
                        if (profileImagePath == null) {
                            setIcon(icon = Icon.ACCOUNT_BOX)
                            iconTintColor = color { "#706EB9" }
                        } else {
                            val file = File(profileImagePath)
                            if (file.exists() && file.isFile) {
                                setIcon(iconFile = file)
                                iconTintColor = null
                            } else {
                                globalConfig.edit {
                                    getCategory("d_bot") -= "profile_image_path"
                                }
                                setIcon(icon = Icon.ACCOUNT_BOX)
                                iconTintColor = color { "#706EB9" }
                            }
                        }
                        onClickListener = {
                            openImagePicker(RESULT_BOT)
                        }
                    }
                }
            }
            category {
                id = "d_user"
                title = "전송자"
                textColor = color { "#706EB9" }
                items = items {
                    string {
                        id = "name"
                        title = "이름"
                        icon = Icon.BOOKMARK
                        iconTintColor = color { "#706EB9" }
                        require = { text ->
                            if (text.isBlank()) "이름을 입력해주세요."
                            else null
                        }
                    }
                    button {
                        id = "set_profile_image"
                        title = "프로필 이미지 설정"
                        val profileImagePath = globalConfig.getCategory("d_user").getString("profile_image_path")
                        if (profileImagePath == null) {
                            setIcon(icon = Icon.ACCOUNT_BOX)
                            iconTintColor = color { "#706EB9" }
                        } else {
                            val file = File(profileImagePath)
                            if (file.exists() && file.isFile) {
                                setIcon(iconFile = file)
                                iconTintColor = null
                            } else {
                                globalConfig.edit {
                                    getCategory("d_user") -= "profile_image_path"
                                }
                                setIcon(icon = Icon.ACCOUNT_BOX)
                                iconTintColor = color { "#706EB9" }
                            }
                        }
                        onClickListener = {
                            openImagePicker(RESULT_USER)
                        }
                    }
                }
            }
            category {
                id = "d_cautious"
                title = "위험"
                textColor = color { "#FF865E" }
                items = items {
                    button {
                        id = "clear_chat"
                        title = "대화 기록 초기화"
                        onClickListener = {
                            dir.deleteRecursively()
                            chatList.clear()
                            userChatAdapter?.notifyDataSetChanged()
                        }
                        icon = Icon.LAYERS_CLEAR
                        //backgroundColor = Color.parseColor("#B8DFD8")
                        iconTintColor = color { "#FF5C58" }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                val uri: Uri = data?.data!!
                globalConfig.edit {
                    val category = when(requestCode) {
                        RESULT_BOT -> getCategory("d_bot")
                        RESULT_USER -> getCategory("d_user")
                        else -> return@edit
                    }
                    category["profile_image_path"] = uri.path!!
                }
                reloadConfig()
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}