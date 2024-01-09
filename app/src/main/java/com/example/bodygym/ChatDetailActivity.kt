package com.example.bodygym

import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bodygym.databinding.ItemMessageBinding
import com.example.bodygym.databinding.ItemMessageMeBinding
import com.example.bodygym.databinding.ItemMessageOtherBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.net.URLConnection

data class Message(
    val content: String = "",
    val mine: Boolean = true,
    val sender: String = "",
    val timestamp: Long = 0L,
    val type: String = ""
)

var nickname: String? = null

class MessageAdapter(private val messages: ArrayList<Message>, private val currentUserName: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Chat")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for (snapshot in dataSnapshot.children) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.let { messages.add(it) }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    companion object {
        const val VIEW_TYPE_MY_MESSAGE = 1
        const val VIEW_TYPE_OTHER_MESSAGE = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MY_MESSAGE) {
            val binding =
                ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MyMessageViewHolder(binding)
        } else {
            val binding =
                ItemMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            OtherMessageViewHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender == currentUserName) VIEW_TYPE_MY_MESSAGE else VIEW_TYPE_OTHER_MESSAGE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is MyMessageViewHolder -> {
                holder.bind(message) // 나의 메시지 뷰홀더 바인딩
            }
            is OtherMessageViewHolder -> {
                holder.bind(message) // 다른 사람의 메시지 뷰홀더 바인딩

                // 이미지가 있는 경우에만 클릭 리스너 설정
                if (message.content != null && message.content.isNotEmpty() && message.type == "image") {
                    holder.binding.chatImageOther.setOnClickListener {
                        val intent = Intent(holder.itemView.context, FullScreenImageActivity::class.java)
                        intent.putExtra("imageUri", message.content)
                        holder.itemView.context.startActivity(intent)
                    }
                }

                // 동영상 클릭 리스너
                holder.binding.chatVideoOther.setOnClickListener {
                    showDialog(holder.itemView.context, message.content, isImage = false)
                }
            }
        }
    }

    // 이미지 또는 비디오를 보여주는 다이얼로그를 띄우는 함수
    private fun showDialog(context: Context, uriString: String, isImage: Boolean) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom_layout)

        if (isImage) {
            val imageView = dialog.findViewById<ImageView>(R.id.dialog_imageview)
            Glide.with(context).load(uriString).into(imageView)
            imageView.visibility = View.VISIBLE // 이미지뷰를 보이게 설정
            imageView.setOnClickListener {
                // 이미지 클릭시 확대 보기
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }
        }
        /*
        else {
            val videoView = dialog.findViewById<VideoView>(R.id.dialog_videoview)
            val mediaController = MediaController(context)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(Uri.parse(uriString))
            videoView.requestFocus()
            videoView.setOnPreparedListener { videoView.start() } // 준비가 되면 비디오 재생
            videoView.visibility = View.VISIBLE // 비디오뷰를 보이게 설정
        }
         */
        dialog.show()
    }

    override fun getItemCount() = messages.size

    class MyMessageViewHolder(private val binding: ItemMessageMeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.chatNicknameMe.text = message.sender
            when (message.type) {
                "text" -> {
                    binding.chatMessageMe.text = message.content
                    binding.chatMessageMe.visibility = View.VISIBLE
                    binding.chatImageMe.visibility = View.GONE
                    binding.chatVideoMe.visibility = View.GONE
                }

                "image" -> {
                    Glide.with(binding.chatImageMe.context).load(message.content)
                        .into(binding.chatImageMe)
                    binding.chatMessageMe.visibility = View.GONE
                    binding.chatImageMe.visibility = View.VISIBLE
                    binding.chatVideoMe.visibility = View.GONE
                }
                /*
                "video" -> {
                    val mediaController = MediaController(binding.chatVideoMe.context)
                    mediaController.setAnchorView(binding.chatVideoMe)
                    binding.chatVideoMe.setMediaController(mediaController)
                    binding.chatVideoMe.setVideoURI(Uri.parse(message.content))
                    binding.chatVideoMe.requestFocus()
                    //binding.chatVideoMe.start()

                    binding.chatMessageMe.visibility = View.GONE
                    binding.chatImageMe.visibility = View.GONE
                    binding.chatVideoMe.visibility = View.VISIBLE
                }
                */
            }
        }
    }

    class OtherMessageViewHolder(val binding: ItemMessageOtherBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.chatNicknameOther.text = message.sender
            when (message.type) {
                "text" -> {
                    binding.chatMessageOther.text = message.content
                    binding.chatMessageOther.visibility = View.VISIBLE
                    binding.chatImageOther.visibility = View.GONE
                    binding.chatVideoOther.visibility = View.GONE
                }

                "image" -> {
                    Glide.with(binding.chatImageOther.context).load(message.content)
                        .into(binding.chatImageOther)
                    binding.chatMessageOther.visibility = View.GONE
                    binding.chatImageOther.visibility = View.VISIBLE
                    binding.chatVideoOther.visibility = View.GONE
                }
                /*
                "video" -> {
                    val mediaController = MediaController(binding.chatVideoOther.context)
                    mediaController.setAnchorView(binding.chatVideoOther)
                    binding.chatVideoOther.setMediaController(mediaController)
                    binding.chatVideoOther.setVideoURI(Uri.parse(message.content))
                    binding.chatVideoOther.requestFocus()

                    // 비디오 썸네일을 로드하는 작업
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    try {
                        mediaMetadataRetriever.setDataSource(binding.chatVideoOther.context, Uri.parse(message.content))
                        val bitmap = mediaMetadataRetriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        binding.videoThumbnail.setImageBitmap(bitmap)
                        binding.videoThumbnail.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        // 에러 처리
                        e.printStackTrace()
                    } finally {
                        mediaMetadataRetriever.release()
                    }

                    // VideoView 준비가 완료되면 썸네일을 숨기고 비디오를 재생합니다.
                    binding.chatVideoOther.setOnPreparedListener {
                        binding.videoThumbnail.visibility = View.GONE
                        // 오토플레이가 필요하다면 여기에 binding.chatVideoOther.start()를 호출합니다.
                    }

                    binding.chatVideoOther.visibility = View.VISIBLE
                }
                */
            }
        }


    }

    class ChatDetailActivity : AppCompatActivity() {
        private lateinit var messageInput: EditText
        private lateinit var sendButton: Button
        private lateinit var chatDetailRecyclerView: RecyclerView
        private lateinit var messageAdapter: MessageAdapter
        private lateinit var databaseReference: DatabaseReference
        private val messages = ArrayList<Message>()
        private lateinit var textViewNickName: TextView
        private var username: String = ""
        private lateinit var deleteButton: Button
        private lateinit var chatRoomId: String
        private lateinit var author: String
        private lateinit var currentUserName: String
        private lateinit var auth: FirebaseAuth
        private lateinit var storageReference: StorageReference

        companion object {
            private const val PICK_IMAGE_VIDEO_REQUEST = 101
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.chat_detail)

            val backButton = findViewById<Button>(R.id.btn_back)
            deleteButton = findViewById(R.id.btn_delete)
            currentUserName = "unknown"
            storageReference = FirebaseStorage.getInstance().reference

            textViewNickName = findViewById(R.id.textViewNickName)
            var chatRoomId = intent.getStringExtra("chatRoomId")
            auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                fetchNickname(userId)
            }
            databaseReference = FirebaseDatabase.getInstance().getReference("Chats/$chatRoomId")

            messageInput = findViewById(R.id.et_input)
            sendButton = findViewById(R.id.btn_send)

            backButton.setOnClickListener {
                finish()
            }

            // 파일 선택 버튼
            val attachFileButton = findViewById<Button>(R.id.btn_attach)
            attachFileButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                startActivityForResult(intent, PICK_IMAGE_VIDEO_REQUEST)
            }

            // 모든 채팅방의 메시지를 가져옵니다.
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (postSnapshot in dataSnapshot.children) {
                        val sender = postSnapshot.child("sender").value.toString()
                        val content = postSnapshot.child("content").value.toString()
                        Log.d(TAG, "Sender: $sender, Content: $content")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 데이터베이스 에러 처리
                    Log.w(TAG, "loadChat:onCancelled", databaseError.toException())
                }
            })

            // userId를 재사용하여 사용자 정보 가져오기
            if (userId != null) {
                val userReference = FirebaseDatabase.getInstance().getReference("Users/$userId")

                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                        Log.d("DEBUG", "Nickname: $nickname")

                        // 채팅방의 개설자 정보를 가져옵니다.
                        databaseReference.child("author")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val author = dataSnapshot.getValue(String::class.java) ?: ""
                                    Log.d("DEBUG", "Author: $author")

                                    // 현재 로그인한 사용자의 닉네임과 채팅방 개설자의 닉네임을 비교
                                    if (nickname == author || nickname == "관리자" || nickname == "관장님") {
                                        deleteButton.visibility = View.VISIBLE  // 닉네임이 같으면 삭제 버튼 표시
                                    } else {
                                        deleteButton.visibility = View.GONE  // 닉네임이 다르면 삭제 버튼 숨김
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // DB에서 데이터 가져오기 실패
                                    Toast.makeText(
                                        this@ChatDetailActivity,
                                        "개설자 정보를 불러오는데 실패했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            this@ChatDetailActivity,
                            "닉네임을 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
            // 채팅방의 개설자 정보를 가져옵니다.
            databaseReference.child("author")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        author = dataSnapshot.getValue(String::class.java) ?: ""
                        currentUserName = username ?: "Unknown"
                        Log.d("DEBUG", "Username: $nickname")
                        Log.d("DEBUG", "Username: $author")

                        // 현재 로그인한 사용자의 닉네임과 채팅방 개설자의 닉네임을 비교
                        if (nickname == author || nickname == "관리자" || nickname == "관장님") {
                            deleteButton.visibility = View.VISIBLE  // 닉네임이 같으면 삭제 버튼 표시
                        } else {
                            deleteButton.visibility = View.GONE  // 닉네임이 다르면 삭제 버튼 숨김
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // DB에서 데이터 가져오기 실패
                        Toast.makeText(
                            this@ChatDetailActivity,
                            "개설자 정보를 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })


            // 채팅방의 이름을 가져옵니다.
            databaseReference.child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val name = dataSnapshot.getValue(String::class.java)
                        val textViewChatTitle: TextView = findViewById(R.id.textViewChatTitle)
                        textViewChatTitle.text = name // 이름 출력
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // DB에서 데이터 가져오기 실패
                        Toast.makeText(
                            this@ChatDetailActivity,
                            "이름을 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                })


            // RecyclerView 및 Adapter 초기화
            chatDetailRecyclerView = findViewById(R.id.recyclerView)
            val layoutManager = LinearLayoutManager(this)
            layoutManager.stackFromEnd = true  // 새로운 줄
            chatDetailRecyclerView.layoutManager = layoutManager
            messageAdapter = MessageAdapter(messages, username)  // currentUserName 추가
            chatDetailRecyclerView.adapter = messageAdapter

            // Firebase Database 연결
            databaseReference = FirebaseDatabase.getInstance().getReference("Chats/$chatRoomId")

            // 채팅방의 메시지들을 가져옵니다.
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    messages.clear()
                    for (snapshot in dataSnapshot.children) {
                        val message = Message(
                            content = snapshot.child("content").value.toString(),  // 문자열로 직접 가져오기
                            sender = snapshot.child("sender").value.toString(),
                            mine = snapshot.child("isMine").value.toString() == currentUserName,
                            type = snapshot.child("type").value.toString(),  // type 추가
                            timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                                ?: System.currentTimeMillis()
                        )
                        messages.add(message)
                    }
                    Log.d("ChatDetailActivity", "메시지를 불러왔습니다. 메시지 개수: ${messages.size}")
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // DB에서 데이터 가져오기 실패
                    Toast.makeText(
                        this@ChatDetailActivity,
                        "메시지를 불러오는데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Log.e("ChatDetailActivity", "메시지를 불러오는데 실패했습니다.", databaseError.toException())
                }
            })

            sendButton.setOnClickListener {
                val messageContent = messageInput.text.toString()
                if (messageContent.isNotBlank()) {
                    val message = Message(
                        content = messageContent,  // 입력한 텍스트를 직접 사용
                        sender = username,  // username 사용
                        mine = true,
                        type = "text",   // type은 항상 "text"
                        timestamp = System.currentTimeMillis()
                    )
                    Log.d("ChatDetailActivity", "username: $username")  // username 확인 로그 추가
                    databaseReference.push().setValue(message)
                    messageInput.text.clear()
                    Log.d("ChatDetailActivity", "메시지를 전송했습니다: $message")  // 메시지 전송 로그
                }
            }


            // 채팅방 ID를 저장합니다.
            chatRoomId = intent.getStringExtra("chatRoomId") ?: ""


            // 삭제 버튼 클릭 리스너 설정
            deleteButton.setOnClickListener {
                // AlertDialog 생성
                AlertDialog.Builder(this)
                    .setTitle("채팅방 삭제")  // 대화 상자 제목
                    .setMessage("정말 채팅방을 삭제하시겠습니까?")  // 대화 상자 메시지
                    .setPositiveButton("예") { _, _ ->
                        // 사용자가 '예'를 클릭했을 때의 동작
                        // Firebase에서 해당 채팅방의 데이터를 삭제
                        databaseReference.removeValue().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // 데이터 삭제 성공
                                Toast.makeText(
                                    this@ChatDetailActivity,
                                    "채팅방이 삭제되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()  // 현재 액티비티 종료 (채팅방 목록 화면으로 돌아감)
                            } else {
                                // 데이터 삭제 실패
                                Toast.makeText(
                                    this@ChatDetailActivity,
                                    "채팅방 삭제에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("아니오", null)  // 사용자가 '아니오'를 클릭했을 때의 동작, 아무것도 하지 않음
                    .show()  // AlertDialog 표시
            }
        }

        // 'fetchNickname' 함수를 정의하여 로그인된 사용자의 닉네임을 가져옵니다.
        private fun fetchNickname(userId: String) {
            val db =
                FirebaseDatabase.getInstance().getReference("Users")  // Users가 사용자 정보를 저장하는 노드라고 가정
            db.child(userId).child("nickname").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val nickname = dataSnapshot.getValue(String::class.java)
                    if (nickname != null) {
                        username = nickname
                    } // username 변수에 닉네임을 저장
                    textViewNickName.text = nickname // 닉네임 출력

                    // 여기서 닉네임과 개설자를 비교
                    if (username == author) {
                        deleteButton.visibility = View.VISIBLE  // 닉네임이 같으면 삭제 버튼 표시
                    } else {
                        deleteButton.visibility = View.GONE  // 닉네임이 다르면 삭제 버튼 숨김
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
        }

        // 채팅방을 삭제하는 함수를 정의합니다.
        private fun deleteChatRoom(chatRoomId: String) {
            if (chatRoomId.isBlank()) {
                Toast.makeText(this, "채팅방 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            val db =
                FirebaseDatabase.getInstance().getReference("Chats")  // Chats는 채팅방 정보를 저장하는 노드라고 가정
            db.child(chatRoomId).removeValue().addOnSuccessListener {
                Toast.makeText(this, "채팅방이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "채팅방을 삭제하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == PICK_IMAGE_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
                val fileUri: Uri? = data.data
                if (fileUri != null) {
                    uploadFileToFirebaseStorage(fileUri)
                } else {
                    Log.e("ChatDetailActivity", "파일을 선택하지 않았습니다.")
                }
            }
        }

        private fun uploadFileToFirebaseStorage(fileUri: Uri) {
            val fileReference = storageReference.child(System.currentTimeMillis().toString())

            val uploadTask = fileReference.putFile(fileUri)
            uploadTask.addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    saveMessageToDatabase(uri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "파일 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun saveMessageToDatabase(fileUrl: String) {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)

            storageReference.metadata.addOnSuccessListener { storageMetadata ->
                val mimeType = storageMetadata.contentType
                val fileType: String = when {
                    mimeType?.startsWith("image") == true -> "image"
                    mimeType?.startsWith("video") == true -> "video"
                    else -> "file"
                }

                val message = Message(
                    content = fileUrl,
                    sender = username,
                    mine = true,
                    type = fileType,
                    timestamp = System.currentTimeMillis()
                )

                databaseReference.push().setValue(message)
                    .addOnSuccessListener {
                        messageInput.setText("")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "메시지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(this, "파일 메타데이터를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


