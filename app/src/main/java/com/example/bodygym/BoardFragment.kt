package com.example.bodygym

import SettingFragment
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

data class Comment(
    var id : String? = "",
    var userId: String? = null,
    var text: String? = null,
    var timestamp: Long? = null,
    var author: String? = null
)

data class Post(
    var content: String? = null,
    var imageUrl: String? = null,
    var videoUrl : String ? = null,
    var postId: String? = null,
    var title: String? = null,
    var writer: String? = null,
    var timeStamp: Long? = null  // 게시글 생성 시간
)



class Board {
    private val database = FirebaseDatabase.getInstance()
    private val myRef = database.getReference("posts")

    fun getAllPosts(): DatabaseReference {
        return myRef
    }
    // 기타 Post 관련 메서드들...
}
class BoardAdapter(private var posts: MutableList<Post>) : RecyclerView.Adapter<BoardAdapter.ViewHolder>(), ChildEventListener {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    init {
        FirebaseDatabase.getInstance().getReference("posts").addChildEventListener(this)
    }
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        val newPost = snapshot.getValue(Post::class.java)?.apply {
            postId = snapshot.key // Firebase 스냅샷의 키를 게시글 ID로 설정
        }
        newPost?.let {
            posts.add(it)
            notifyItemInserted(posts.size - 1)
        }
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        // 데이터 삭제가 감지될 때의 동작을 여기에 작성합니다.
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        // 데이터 이동이 감지될 때의 동작을 여기에 작성합니다.
    }

    override fun onCancelled(error: DatabaseError) {
        // 데이터 읽기가 취소될 때의 동작을 여기에 작성합니다.
        Log.w(TAG, "loadPost:onCancelled", error.toException())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        // 제목 설정
        holder.itemView.findViewById<TextView>(R.id.detailTitleTextView).text = post.title
        // 작성자 설정
        holder.itemView.findViewById<TextView>(R.id.authorTextView).text = post.writer
        // 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            val context = holder.view.context
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("postid", post.postId) // 'postId'는 Post 모델의 고유 ID 속성을 나타냅니다.
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = posts.size

    fun updatePosts(newPosts: MutableList<Post>) {
        this.posts.clear() // 기존의 데이터를 삭제합니다.
        this.posts.addAll(newPosts) // 새로운 데이터를 추가합니다.
        notifyDataSetChanged() // 데이터가 변경되었음을 어댑터에 알립니다.
    }

    fun filterPosts(query: String) {
        val filteredPosts = posts.filter { it.title?.contains(query) ?: false } // 'title'을 기준으로 필터링하였습니다. 필요에 따라 변경 가능합니다.
        this.posts.clear()
        this.posts.addAll(filteredPosts)
        notifyDataSetChanged()
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        val changedPost = snapshot.getValue(Post::class.java)
        changedPost?.let { post ->
            val index = posts.indexOfFirst { it.postId == post.postId }
            if (index != -1) {
                posts[index] = post
                notifyItemChanged(index) // 변경된 아이템에 대한 업데이트만 알립니다.
            }
        }
    }

    // onChildChanged, onChildRemoved, onChildMoved, onCancelled 메서드들도 구현해야 합니다.
}
class BoardFragment : Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageViewQr: ImageView
    private val board = Board()
    private lateinit var recyclerView: RecyclerView
    private lateinit var writeBtn: ImageView
    private lateinit var adapter: BoardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_content_list, container, false)

        val searchButton = view.findViewById<ImageView>(R.id.searchButton)
        searchButton.setOnClickListener {
            val context = context
            if (context != null) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("제목으로 게시물 검색")

                val input = EditText(context)
                builder.setView(input)

                builder.setPositiveButton("검색") { dialog, _ ->
                    val searchQuery = input.text.toString()
                    adapter.filterPosts(searchQuery)
                    dialog.dismiss()
                }
                builder.setNegativeButton("취소") { dialog, _ -> dialog.cancel() }

                builder.show()
            } else {
                Log.e(TAG, "Context is null")
            }
        }

        recyclerView = view.findViewById(R.id.recyclerview)
        writeBtn = view.findViewById(R.id.contentWriteBtn)

        // 빈 리스트를 가진 아답터를 설정합니다.
        adapter = BoardAdapter(mutableListOf())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 데이터베이스에서 모든 게시물을 읽어와서 리사이클러뷰에 표시합니다.
        val postsRef = board.getAllPosts()
        if (postsRef != null) {
            postsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d(TAG, "onDataChange 호출됨, 데이터 개수: ${dataSnapshot.childrenCount}")

                    // dataSnapshot에서 받아온 데이터를 MutableList로 변환합니다.
                    val posts = dataSnapshot.children.mapNotNull { it.getValue(Post::class.java) }

                    // writer가 "관리자" 또는 "관장님"인 게시글과 그렇지 않은 게시글을 분리합니다.
                    val adminPosts = posts.filter { it.writer == "관리자" || it.writer == "관장님" }
                    val otherPosts = posts.filter { it.writer != "관리자" && it.writer != "관장님" }

                    // 그 외 게시글은 역순으로 정렬합니다.
                    val reversedOtherPosts = otherPosts.reversed()

                    // "관리자" 또는 "관장님"의 게시글을 앞에 두고, 그 외 게시글을 뒤에 붙여서 최종 게시글 목록을 만듭니다.
                    val sortedPosts = adminPosts + reversedOtherPosts

                    // 아답터의 데이터를 업데이트합니다.
                    adapter.updatePosts(sortedPosts.toMutableList())
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 에러 로그를 출력합니다.
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                }
            })
        } else {
            Log.e(TAG, "postsRef is null")
        }

        // 글쓰기 버튼 클릭 리스너 설정
        writeBtn.setOnClickListener {
            val context = context
            if (context != null) {
                val intent = Intent(context, BoardwriteActivity::class.java)
                startActivity(intent)
            } else {
                Log.e(TAG, "Context is null")
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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