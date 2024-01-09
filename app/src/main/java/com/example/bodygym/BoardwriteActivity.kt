package com.example.bodygym

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.Serializable
import java.util.*
import android.Manifest


class BoardwriteActivity : AppCompatActivity() {

    data class ContentModel(
        val postId: String,
        val title: String,
        val content: String,
        val writer: String,
        val imageUrl : String? = null,
        val videoUrl: String? = null  // videoUrl 프로퍼티 추가
    ) : Serializable


    private lateinit var auth: FirebaseAuth  // FirebaseAuth 인스턴스 생성
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var filePath: Uri? = null
    private val PICK_IMAGE_REQUEST = 71
    private val PICK_VIDEO_REQUEST = 72
    private var fileType: String? = null
    companion object {
        private const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_write)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }

        auth = FirebaseAuth.getInstance()  // FirebaseAuth 인스턴스 초기화
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        val titleArea: EditText = findViewById(R.id.titleArea)
        val contentArea: EditText = findViewById(R.id.contentArea)
        val writeBtn: Button = findViewById(R.id.writeBtn)
        val cancelBtn: Button = findViewById(R.id.cancelBtn)  // '취소' 버튼에 대한 참조를 생성
        val chooseBtn: Button = findViewById(R.id.chooseVideoBtn)  // '동영상 선택' 버튼에 대한 참조를 생성
        val chooseImageBtn: Button = findViewById(R.id.chooseImageBtn)  // '사진 선택' 버튼에 대한 참조를 생성
        val chooseVideoBtn: Button = findViewById(R.id.chooseVideoBtn)  // '동영상 선택' 버튼에 대한 참조를 생성

        chooseBtn.setOnClickListener {
            chooseVideo()
        }

        writeBtn.setOnClickListener {
            val title = titleArea.text.toString()
            val content = contentArea.text.toString()

            val userId = auth.currentUser?.uid  // 현재 로그인한 사용자의 uid를 가져옵니다.
            if (userId != null) {
                fetchNickname(userId) { nickname ->
                    uploadFile { fileUrl ->  // 파일 업로드
                        val ref = FirebaseDatabase.getInstance().getReference("posts")
                        val postId = ref.push().key  // Firebase에서 자동으로 유일한 key를 생성합니다.

                        if (postId != null && nickname != null) {
                            val post = if(fileType == "image") {  // fileType에 따라 imageUrl 또는 videoUrl에 파일 URL 저장
                                ContentModel(postId, title, content, nickname, imageUrl=fileUrl)
                            } else {
                                ContentModel(postId, title, content, nickname, videoUrl=fileUrl)
                            }
                            ref.child(postId).setValue(post).addOnCompleteListener {
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }


        chooseImageBtn.setOnClickListener {
            chooseImage()
        }

        chooseVideoBtn.setOnClickListener {
            chooseVideo()
        }

        // '취소' 버튼에 클릭 리스너를 설정
        cancelBtn.setOnClickListener {
            finish()  // 현재 액티비티를 종료
        }
    }


    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
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


    private fun chooseVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.data != null )
        {
            filePath = data.data
            fileType = "image"
        }
        else if(requestCode == PICK_VIDEO_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.data != null )
        {
            filePath = data.data
            fileType = "video"
        }
    }

    private fun uploadFile(callback: (fileUrl: String?) -> Unit) {
        if(filePath != null && fileType != null) {
            val ref = storageReference.child("$fileType/" + UUID.randomUUID().toString())
            ref.putFile(filePath!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        callback(it.toString())
                    }
                }
                .addOnFailureListener {
                    callback(null)
                }
        } else {
            callback(null)
        }
    }
}