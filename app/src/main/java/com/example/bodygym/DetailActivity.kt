package com.example.bodygym

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.Glide;

data class User(
    val nickname: String? = null
)

interface OnCommentClickListener {
    fun onCommentClick(comment: Comment)
}

class CommentsAdapter(
    private val commentsList: ArrayList<Comment>,
    private val commentClickListener: OnCommentClickListener
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val commentTextView: TextView = view.findViewById(R.id.commentTextView)
        val authorTextView: TextView = view.findViewById(R.id.authorTextView)
        val nicknameTextView: TextView = view.findViewById(R.id.authorTextView)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                commentClickListener.onCommentClick(commentsList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.commentTextView.text = comment.text
        holder.authorTextView.text = comment.author
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
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var titleTextView: TextView
    private lateinit var contentTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_content)
        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()


        titleTextView= findViewById(R.id.detailTitleTextView)
        contentTextView = findViewById(R.id.detailContentTextView)



        val commentsList = ArrayList<Comment>()
        val commentsAdapter = CommentsAdapter(commentsList, object : OnCommentClickListener {
            override fun onCommentClick(comment: Comment) {
                // 댓글 클릭 시 동작을 구현합니다.


            }
        })

        val postId: String = intent.getStringExtra("postid") ?: return
        Log.d("DetailActivity", "Post ID: $postId")

        val commentEditText: EditText = findViewById(R.id.commentEditText)
        val submitCommentButton: Button = findViewById(R.id.submitCommentButton)
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid
        val editPostButton: Button = findViewById(R.id.editbtn)
        val deletePostButton: Button = findViewById(R.id.deletebtn)
        val currentUserNickname = currentUser?.displayName
        fetchPostDataAndCheckAuthor(postId, titleTextView, contentTextView, editPostButton, deletePostButton, currentUserId)


        Log.d("DetailActivity", "Current User ID: $currentUserId")

        submitCommentButton.setOnClickListener {
            submitComment(postId, commentEditText)
        }

        fetchPostData(postId, titleTextView, contentTextView, editPostButton, deletePostButton, currentUserId)

        // 수정 버튼에 대한 클릭 리스너 설정
        editPostButton.setOnClickListener {
            showEditDialog(postId, titleTextView.text.toString(), contentTextView.text.toString())
        }

        // 삭제 버튼에 대한 클릭 리스너 설정
        deletePostButton.setOnClickListener {
            showDeleteConfirmationDialog(postId)
        }

        setupCommentsSection(postId)
    }

    private fun setupVideoPlayer(videoUrl: String) {
        Log.d("VideoPlayer", "setupVideoPlayer 함수가 호출되었습니다.") // 로그 출력

        val videoView: VideoView = findViewById(R.id.detailVideoView)
        Log.d("VideoPlayer", "Video URL is set: $videoUrl")
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(Uri.parse(videoUrl))
        videoView.requestFocus()

        videoView.setOnErrorListener { mp, what, extra ->
            Log.e("VideoPlayer", "Error occurred while playing video. Error code: $what, Extra code: $extra")

            // 에러 코드에 따른 분기 처리
            when (what) {
                MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                    Log.e("VideoPlayer", "알 수 없는 미디어 에러가 발생했습니다.")
                }
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                    Log.e("VideoPlayer", "미디어 서버가 죽었습니다.")
                }
                else -> {
                    Log.e("VideoPlayer", "미디어 재생 중 예상치 못한 에러가 발생했습니다.")
                }
            }

            // extra를 활용한 추가적인 에러 분기 처리
            when (extra) {
                MediaPlayer.MEDIA_ERROR_IO -> {
                    Log.e("VideoPlayer", "파일 읽기/쓰기 에러가 발생했습니다.")
                }
                MediaPlayer.MEDIA_ERROR_MALFORMED -> {
                    Log.e("VideoPlayer", "비정상적인 미디어 파일입니다.")
                }
                MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                    Log.e("VideoPlayer", "지원되지 않는 미디어 형식입니다.")
                }
                MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                    Log.e("VideoPlayer", "미디어 재생 중 타임아웃이 발생했습니다.")
                }
                else -> {
                    Log.e("VideoPlayer", "미디어 재생 중 예상치 못한 에러가 발생했습니다.")
                }
            }

            true
        }

        videoView.setOnPreparedListener {
            Log.d("VideoPlayer", "동영상이 준비되었습니다. 재생을 시작합니다.")
            //videoView.start() // 동영상이 준비되면 자동으로 재생
        }
    }

    private fun submitComment(postId: String, commentEditText: EditText) {
        val commentText = commentEditText.text.toString()
        if (commentText.isNotBlank()) {
            val commentRef = firebaseDatabase.getReference("comments").child(postId)
            val commentId = commentRef.push().key
            val userId = auth.currentUser?.uid
            if (userId != null) {
                fetchNickname(userId) { nickname ->
                    val comment = Comment(
                        id = commentId, // Comment 객체 생성 시 id 필드를 댓글의 키로 설정
                        author = nickname,
                        userId = userId, // userId 필드 값을 추가
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

    private fun fetchPostData(postId: String, titleTextView: TextView, contentTextView: TextView,
                              editPostButton: Button, deletePostButton: Button, currentUserId: String?) {
        val ref = firebaseDatabase.getReference("posts").child(postId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val post = snapshot.getValue(Post::class.java)
                    titleTextView.text = post?.title
                    contentTextView.text = post?.content
                    val detailAuthorTextView: TextView = findViewById(R.id.detailAuthorTextView)
                    detailAuthorTextView.text = post?.writer // 'author' 필드를 'writer'로 변경

                    val writerId = post?.writer // 'author' 필드를 'writer'로 변경
                    if (currentUserId == writerId) {
                        Log.d("DetailActivity", "Current user is the writer")
                        editPostButton.visibility = View.VISIBLE
                        deletePostButton.visibility = View.VISIBLE
                    } else {
                        Log.d("DetailActivity", "Current user is not the writer")
                        editPostButton.visibility = View.GONE
                        deletePostButton.visibility = View.GONE
                    }


                    // 동영상 URL 받아오기
                    val videoUrl = post?.videoUrl
                    Log.d("DetailActivity", "Video URL: $videoUrl")
                    if (!videoUrl.isNullOrEmpty()) {
                        // VideoView를 이용하여 동영상 불러오기
                        setupVideoPlayer(videoUrl)
                    } else {
                        Log.w("DetailActivity", "No video URL found for the post ID: $postId")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to read post", error.toException())
            }
        })
    }

    private fun setupCommentsSection(postId: String) {
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentsAdapter = CommentsAdapter(ArrayList(), object : OnCommentClickListener {
            override fun onCommentClick(comment: Comment) {
                AlertDialog.Builder(this@DetailActivity)
                    .setTitle("댓글 삭제")
                    .setMessage("댓글을 삭제하시겠습니까?")
                    .setPositiveButton("예") { _, _ ->
                        deleteComment(postId, comment)
                    }
                    .setNegativeButton("아니오", null)
                    .show()
            }
        })
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter
        val commentsRef = firebaseDatabase.getReference("comments").child(postId)
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { dataSnapshot ->
                    val comment = dataSnapshot.getValue(Comment::class.java)
                    comment?.id = dataSnapshot.key
                    comment
                }
                commentsAdapter.setComments(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to load comments.", error.toException())
            }
        })
    }

    private fun fetchNickname(userId: String, callback: (String) -> Unit) {
        val ref = firebaseDatabase.getReference("Users").child(userId)
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nickname = snapshot.child("nickname").getValue(String::class.java) ?: "Unknown User"
                callback(nickname)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to fetch nickname.", error.toException())
            }
        })
    }

    private fun fetchPostDataAndCheckAuthor(postId: String, titleTextView: TextView, contentTextView: TextView,
                                            editPostButton: Button, deletePostButton: Button, currentUserId: String?) {
        val ref = firebaseDatabase.getReference("posts").child(postId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val post = snapshot.getValue(Post::class.java)
                    titleTextView.text = post?.title
                    contentTextView.text = post?.content
                    val detailAuthorTextView: TextView = findViewById(R.id.detailAuthorTextView)
                    detailAuthorTextView.text = post?.writer

                    val authorId = post?.writer
                    fetchNickname(currentUserId!!) { nickname ->
                        if (nickname == authorId || nickname == "관장님" || nickname == "관리자") {
                            Log.d("DetailActivity", "Current user is the author")
                            editPostButton.visibility = View.VISIBLE
                            deletePostButton.visibility = View.VISIBLE
                        } else {
                            Log.d("DetailActivity", "Current user is not the author")
                            editPostButton.visibility = View.GONE
                            deletePostButton.visibility = View.GONE
                        }
                    }
                    val detailImageView: ImageView = findViewById(R.id.detailImageView)
                    val detailVideoView: VideoView = findViewById(R.id.detailVideoView)

// 이미지 URL 받아오기
                    val imageUrl = post?.imageUrl
                    if (imageUrl != null && imageUrl.isNotBlank()) {
                        // 이미지 URL이 존재하는 경우, Glide를 이용하여 이미지 불러오기
                        Glide.with(this@DetailActivity)
                            .load(imageUrl)
                            .into(detailImageView)

                        Log.d("DetailActivity", "Image URL: $imageUrl") // 이미지 URL 로그 출력

                        // 이미지 클릭 이벤트 설정
                        detailImageView.setOnClickListener {
                            val intent = Intent(this@DetailActivity, FullScreenImageActivity::class.java)
                            intent.putExtra("imageUri", imageUrl) // 이미지 Uri를 인텐트에 추가
                            Log.d(
                                "DetailActivity",
                                "Intent with imageUri: $intent"
                            ) // 인텐트에 추가된 Uri 로그 출력
                            startActivity(intent)
                        }

                        // 이미지뷰 보이게, 비디오뷰 숨기기
                        detailImageView.visibility = View.VISIBLE
                        detailVideoView.visibility = View.GONE
                    } else {
                        // 이미지 URL이 없는 경우, 비디오뷰 보이게, 이미지뷰 숨기기
                        detailImageView.visibility = View.GONE
                        detailVideoView.visibility = View.VISIBLE
                    }

                } else {
                    Log.w("DetailActivity", "No data found for the post ID: $postId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailActivity", "Failed to read post", error.toException())
            }
        })
    }


    private fun showDeleteConfirmationDialog(postId: String) {
        AlertDialog.Builder(this)
            .setTitle("게시글 삭제")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                deletePost(postId)
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun deletePost(postId: String) {
        val postReference = firebaseDatabase.getReference("posts").child(postId)
        postReference.removeValue().addOnSuccessListener {
            Log.d("DetailActivity", "Post deleted successfully")
            Toast.makeText(this, "게시글이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Log.e("DetailActivity", "Failed to delete post", e)
            Toast.makeText(this, "게시글을 삭제하는 데 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(postId: String, oldTitle: String, oldContent: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("게시글 수정")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.edit_dialog, null)

        val titleEditText = dialogLayout.findViewById<EditText>(R.id.editTitle)
        val contentEditText = dialogLayout.findViewById<EditText>(R.id.editContent)

        titleEditText.setText(oldTitle)
        contentEditText.setText(oldContent)

        builder.setView(dialogLayout)
        builder.setPositiveButton("수정") { _, _ ->
            updatePost(postId, titleEditText.text.toString(), contentEditText.text.toString())

        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }

    private fun updatePost(postId: String, newTitle: String, newContent: String) {
        val postReference = firebaseDatabase.getReference("posts").child(postId)

        postReference.child("title").setValue(newTitle)
        postReference.child("content").setValue(newContent).addOnSuccessListener {
            Log.d("DetailActivity", "Post updated successfully")
            Toast.makeText(this, "게시글이 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show()
            refreshPost(postId)
        }.addOnFailureListener { e ->
            Log.e("DetailActivity", "Failed to update post", e)
            Toast.makeText(this, "게시글을 수정하는 데 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshPost(postId: String) {
        val postReference = firebaseDatabase.getReference("posts").child(postId)

        postReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val post = dataSnapshot.getValue(Post::class.java)
                if (post != null) {
                    titleTextView.text  = post.title
                    contentTextView.text = post.content
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("DetailActivity", "Failed to read post", databaseError.toException())
            }
        })
    }

    private fun deleteComment(postId: String, comment: Comment) {
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val currentUserId = firebaseAuth.currentUser?.uid

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentNickname = dataSnapshot.child("nickname").getValue(String::class.java)
                Log.d("YourActivity", "comment author: ${comment.author}, currentNickname: $currentNickname")
                if (comment.author == currentNickname || currentNickname == "관리자" || currentNickname == "관장님") {
                    val commentId = comment.id
                    Log.d("TAG", "commentId: $commentId")
                    if (commentId != null) {
                        val commentRef = FirebaseDatabase.getInstance().getReference("comments").child(postId).child(commentId)

                        commentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(commentSnapshot: DataSnapshot) {
                                val comment = commentSnapshot.getValue(Comment::class.java)

                                AlertDialog.Builder(this@DetailActivity)
                                    .setMessage("정말 댓글을 삭제하시겠습니까?")
                                    .setPositiveButton("예") { dialog, _ ->
                                        commentRef.removeValue()
                                        commentRef.removeValue().addOnSuccessListener {
                                            Toast.makeText(
                                                this@DetailActivity,
                                                "댓글이 삭제되었습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.addOnFailureListener { e ->
                                            Log.e("DetailActivity", "Failed to delete comment.", e)
                                        }
                                    }
                                    .setNegativeButton("아니요", null)
                                    .show()
                            }

                            override fun onCancelled(commentError: DatabaseError) {
                                // 실패한 경우에 대한 처리를 추가합니다.
                            }
                        })
                    } else {
                        Toast.makeText(this@DetailActivity, "댓글 ID가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(this@DetailActivity, "본인이 작성한 댓글만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 실패한 경우에 대한 처리를 추가합니다.
            }
        })
    }

}
