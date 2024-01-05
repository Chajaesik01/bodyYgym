package com.example.bodygym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Message(val sender: String, val content: String)

class MessageAdapter(private val messages: ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val senderTextView: TextView = view.findViewById(R.id.sender) // 실제 레이아웃에 맞게 수정 필요
        val contentTextView: TextView = view.findViewById(R.id.content) // 실제 레이아웃에 맞게 수정 필요
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderTextView.text = message.sender
        holder.contentTextView.text = message.content
    }

    override fun getItemCount() = messages.size
}


class ChatDetailActivity : AppCompatActivity() {
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatDetailRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var databaseReference: DatabaseReference
    private val messages = ArrayList<Message>()
    private lateinit var textViewNickName: TextView
    private var username: String? = null

    private lateinit var auth: FirebaseAuth  // FirebaseAuth 인스턴스 추가



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_detail)
        val backButton = findViewById<Button>(R.id.btn_back) // 이 부분을 추가하세요.

        textViewNickName = findViewById(R.id.textViewNickName)
        val chatRoomId = intent.getStringExtra("chatRoomId")
        auth = FirebaseAuth.getInstance()  // FirebaseAuth 인스턴스 초기화
        val userId = auth.currentUser?.uid  // 현재 로그인된 사용자의 UID를 가져옵니다.
        if (userId != null) {
            fetchNickname(userId)  // 사용자의 닉네임을 가져옵니다.
        }
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats/$chatRoomId")

        messageInput = findViewById(R.id.et_input)
        sendButton = findViewById(R.id.btn_send)
        backButton.setOnClickListener {
            finish()
        }

        // 채팅방의 이름을 가져옵니다.
        databaseReference.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val name = dataSnapshot.getValue(String::class.java)
                val textViewChatTitle: TextView = findViewById(R.id.textViewChatTitle)
                textViewChatTitle.text = name // 이름 출력
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // DB에서 데이터 가져오기 실패
                Toast.makeText(this@ChatDetailActivity, "이름을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })

        // RecyclerView 및 Adapter 초기화
        chatDetailRecyclerView = findViewById(R.id.recyclerView)
        chatDetailRecyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messages)
        chatDetailRecyclerView.adapter = messageAdapter

        // Firebase Database 연결
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats/$chatRoomId")

        // 채팅방의 메시지들을 가져옵니다.
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for (snapshot in dataSnapshot.children) {
                    val message = Message(
                        sender = snapshot.child("sender").value.toString(),
                        content = snapshot.child("content").value.toString()
                    )
                    messages.add(message)
                }
                messageAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // DB에서 데이터 가져오기 실패
                Toast.makeText(this@ChatDetailActivity, "메시지를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })

        sendButton.setOnClickListener {
            val messageContent = messageInput.text.toString()
            if (messageContent.isNotBlank()) {
                val message = Message(
                    sender = username ?: "unknown", // username 변수를 사용하여 메시지를 전송합니다.
                    content = messageContent
                )
                databaseReference.push().setValue(message)
                messageInput.text.clear()
            }
        }
    }

    // 'fetchNickname' 함수를 정의하여 로그인된 사용자의 닉네임을 가져옵니다.
    private fun fetchNickname(userId: String) {
        val db = FirebaseDatabase.getInstance().getReference("Users")  // Users가 사용자 정보를 저장하는 노드라고 가정
        db.child(userId).child("nickname").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val nickname = dataSnapshot.getValue(String::class.java)
                username = nickname // username 변수에 닉네임을 저장
                textViewNickName.text = nickname // 닉네임 출력
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
}