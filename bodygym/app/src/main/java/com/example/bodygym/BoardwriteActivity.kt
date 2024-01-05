package com.example.bodygym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BoardwriteActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth  // FirebaseAuth 인스턴스 생성

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_write)

        auth = FirebaseAuth.getInstance()  // FirebaseAuth 인스턴스 초기화

        val titleArea: EditText = findViewById(R.id.titleArea)
        val contentArea: EditText = findViewById(R.id.contentArea)
        val writeBtn: Button = findViewById(R.id.writeBtn)
        val cancelBtn: Button = findViewById(R.id.cancelBtn)  // '취소' 버튼에 대한 참조를 생성


        writeBtn.setOnClickListener {
            val title = titleArea.text.toString()
            val content = contentArea.text.toString()

            val userId = auth.currentUser?.uid  // 현재 로그인한 사용자의 uid를 가져옵니다.
            if (userId != null) {
                fetchNickname(userId) { nickname ->
                    val ref = FirebaseDatabase.getInstance().getReference("posts")
                    val postId = ref.push().key  // Firebase에서 자동으로 유일한 key를 생성합니다.

                    if (postId != null && nickname != null) {
                        val post = ContentModel(postId, title, content, nickname) // postId를 포함하여 객체를 생성합니다.
                        ref.child(postId).setValue(post).addOnCompleteListener {
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
        // '취소' 버튼에 클릭 리스너를 설정
        cancelBtn.setOnClickListener {
            finish()  // 현재 액티비티를 종료
        }
    }

    private fun fetchNickname(userId: String, callback: (nickname: String?) -> Unit) {
        val db = FirebaseDatabase.getInstance().getReference("Users")  // Users가 사용자 정보를 저장하는 노드라고 가정
        db.child(userId).child("nickname").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val nickname = dataSnapshot.getValue(String::class.java)
                callback(nickname)  // 닉네임을 콜백 함수로 반환
            }
            override fun onCancelled(databaseError: DatabaseError) {
                callback(null)
            }
        })
    }
}