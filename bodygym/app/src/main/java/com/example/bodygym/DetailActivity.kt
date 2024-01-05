package com.example.bodygym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

data class User(
    val nickname: String? = null
)


class CommentsAdapter(private val commentsList: ArrayList<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val commentTextView: TextView = view.findViewById(R.id.commentTextView)
        val authorTextView: TextView = view.findViewById(R.id.authorTextView) // 작성자 이름을 보여주는 TextView
        val nicknameTextView: TextView = view.findViewById(R.id.authorTextView) // 사용자 닉네임을 보여주는 TextView 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.commentTextView.text = comment.text
        holder.authorTextView.text = comment.author // 댓글 작성자 이름을 보여줍니다.
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }

    fun addComment(comment: Comment) {
        commentsList.add(comment)
        notifyItemInserted(commentsList.size - 1)
    }

    fun setComments(comments: List<Comment>) {
        commentsList.clear()
        commentsList.addAll(comments)
        notifyDataSetChanged()
    }
}
class DetailActivity : AppCompatActivity() {
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_content)
        auth = FirebaseAuth.getInstance()

// 인텐트에서 게시글 ID를 가져옵니다.
        val postId: String = intent.getStringExtra("postid") ?: return
        Log.d("DetailActivity", "Post ID: $postId")

        val titleTextView: TextView = findViewById(R.id.detailTitleTextView)
        val contentTextView: TextView = findViewById(R.id.detailContentTextView)

        val commentEditText: EditText = findViewById(R.id.commentEditText)
        val submitCommentButton: Button = findViewById(R.id.submitCommentButton)

        submitCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString()
            if (commentText.isNotBlank()) {
                val commentRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)
                val commentId = commentRef.push().key
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    fetchNickname(userId) { nickname ->
                        val comment = Comment(
                            author = nickname,
                            text = commentText,
                            timestamp = System.currentTimeMillis()
                        )

                        commentId?.let {
                            commentRef.child(it).setValue(comment).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("DetailActivity", "Comment saved successfully.")
                                    commentEditText.text.clear()
                                } else {
                                    Log.e("DetailActivity", "Failed to save comment.", task.exception)
                                }
                            }
                        }
                    }
                }
            }
        }

// Firebase에서 게시글 데이터를 가져옵니다.
        val ref = FirebaseDatabase.getInstance().getReference("posts").child(postId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DetailActivity", "Snapshot: $snapshot")
                if (snapshot.exists()) {
                    val post = snapshot.getValue(Post::class.java)
                    titleTextView.text = post?.title  // 제목을 TextView에 설정합니다.
                    contentTextView.text = post?.content  // 내용을 TextView에 설정합니다.
                    val detailAuthorTextView: TextView = findViewById(R.id.detailAuthorTextView)

                    // 작성자를 TextView에 설정합니다.
                    detailAuthorTextView.text = post?.author

                    Log.d("DetailActivity", "Data received: Title - ${post?.title}, Content - ${post?.content}, Author - ${post?.author}")
                } else {
                    Log.w("DetailActivity", "No data found for the post ID: $postId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to read post", error.toException())
            }
        })


        // Firebase에서 댓글 데이터를 가져옵니다.

        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentsAdapter = CommentsAdapter(ArrayList())
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { it.getValue(Comment::class.java) }
                commentsAdapter.setComments(comments) // 어댑터에 댓글 리스트를 설정합니다.
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to load comments.", error.toException())
            }
        })

        // 확인 버튼에 클릭 리스너를 설정합니다.
        findViewById<TextView>(R.id.checkbtn).setOnClickListener {
            // 액티비티를 종료합니다.
            finish()
        }
    }
    fun fetchNickname(userId: String, callback: (String) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nickname = snapshot.child("nickname").getValue(String::class.java) ?: "Unknown User"
                callback(nickname)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error here
            }
        })
    }
}