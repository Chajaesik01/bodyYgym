package com.example.bodygym

import SettingFragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

data class ChatRoom(val id: String, val name: String, val author : String)

class ChatRoomAdapter(private val chatRooms: MutableList<ChatRoom>) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    class ChatRoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chatRoomName: TextView = view.findViewById(R.id.chat_room_name)
        val chatRoomAuthor: TextView = view.findViewById(R.id.chat_room_author) // 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_list_item, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.chatRoomName.text = chatRooms[position].name
        holder.chatRoomAuthor.text = chatRooms[position].author
        holder.itemView.setOnClickListener {
            // 채팅방을 클릭했을 때의 이벤트 처리를 여기에 작성합니다.
            val context = holder.itemView.context
            val intent = Intent(context, ChatDetailActivity::class.java) // ChatDetailActivity는 채팅방의 내용을 보여주는 액티비티입니다.
            intent.putExtra("chatRoomId", chatRooms[position].id) // ChatRoom의 id를 넘겨줍니다.
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return chatRooms.size
    }
}


class ChatListFragment : Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var chatListRecyclerView: RecyclerView
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // "채팅방 만들기" 버튼
        val createRoomButton: Button = view.findViewById(R.id.btn_create_room)

        // chatListRecyclerView 초기화
        chatListRecyclerView = view.findViewById(R.id.chat_list_recycler_view)
        chatListRecyclerView.layoutManager = LinearLayoutManager(context)

        val chatRooms = mutableListOf<ChatRoom>()
        // 버튼 클릭 리스너 설정
        createRoomButton.setOnClickListener {
            // 다이얼로그 생성
            val builder = AlertDialog.Builder(requireContext())
            val inflater = requireActivity().layoutInflater
            builder.setTitle("채팅방 만들기")

            // 다이얼로그에 채팅방 이름을 입력할 EditText 추가
            val dialogLayout = inflater.inflate(R.layout.dialog_create_room, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)

            builder.setView(dialogLayout)

            // Firebase의 현재 로그인한 사용자의 정보를 가져옵니다
            val user = FirebaseAuth.getInstance().currentUser
            val userId = user?.uid

            // Firebase Realtime Database에서 사용자 닉네임을 가져옵니다
            val db = FirebaseDatabase.getInstance().getReference("Users")  // Users가 사용자 정보를 저장하는 노드라고 가정
            db.child(userId!!).child("nickname").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userName = dataSnapshot.getValue(String::class.java)

                    builder.setPositiveButton("만들기") { dialogInterface, i ->
                        val roomName = editText.text.toString()
                        val roomAuthor = userName ?: "Anonymous" // 현재 로그인한 사용자 이름으로 변경했습니다.

                        // 채팅방 이름이 비어있지 않은 경우에만 채팅방 생성
                        if (roomName.isNotBlank()) {
                            val chatRoom = ChatRoom(UUID.randomUUID().toString(), roomName, roomAuthor)

                            // Firebase에 채팅방 정보 저장
                            databaseReference.child(chatRoom.id).setValue(chatRoom)
                        }
                    }
                    builder.setNegativeButton("취소", null)
                    builder.show()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 에러 발생 시 처리
                }
            })
        }

        val adapter = ChatRoomAdapter(chatRooms)
        chatListRecyclerView = view.findViewById(R.id.chat_list_recycler_view)
        chatListRecyclerView.adapter = adapter

        // Firebase Database 연결
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats")

        // 채팅방 목록을 가져옵니다.
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chatRooms.clear()
                for (snapshot in dataSnapshot.children) {
                    val chatRoom = ChatRoom(
                        id = snapshot.key!!,
                        name = snapshot.child("name").value.toString(),
                        author = snapshot.child("author").value.toString() // 추가
                    )
                    chatRooms.add(chatRoom)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // DB에서 데이터 가져오기 실패
                Toast.makeText(context, "Failed to load chat rooms.", Toast.LENGTH_SHORT).show()
            }
        })


        bottomNavigationView = view.findViewById(R.id.nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 홈 화면 프래그먼트로 전환
                    val fragment = HomeFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_post -> {
                    // 게시글 프래그먼트로 전환
                    val fragment = BoardFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_chat -> {
                    // 채팅 프래그먼트로 전환
                    val fragment = ChatListFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_calendar -> {
                    // 캘린더 프래그먼트로 전환
                    val fragment = CalendarFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_settings -> {
                    val fragment = SettingFragment()
                    replaceFragment(fragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }
}