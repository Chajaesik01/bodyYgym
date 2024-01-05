package com.example.bodygym

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("Users")

        val editTextEmail: EditText = findViewById(R.id.editTextId_Reg)
        val editTextRePass: EditText = findViewById(R.id.editTextRePass_Reg)
        val editTextNickName: EditText = findViewById(R.id.editTextNickName_Reg) // 추가한 닉네임 입력란
        val btnRegister: Button = findViewById(R.id.btnRegister_Reg)

        btnRegister.setOnClickListener {
            val userEmail = editTextEmail.text.toString()
            val userPassword = editTextRePass.text.toString()
            val userNickName = editTextNickName.text.toString() // 닉네임 가져오기
            if (userEmail.isNotEmpty() && userPassword.isNotEmpty() && userNickName.isNotEmpty()) {
                registerUserOnFirebase(userEmail, userPassword, userNickName) // 닉네임 추가
            } else {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUserOnFirebase(email: String, password: String, nickname: String) { // 닉네임 파라미터 추가
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid // Firebase에서 제공하는 uid를 가져옵니다.
                    if (userId != null) {
                        db.child(userId).child("nickname").setValue(nickname) // Realtime Database에 닉네임 저장
                    }
                    Toast.makeText(this, "$email 님, 회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java) // MainActivity로 이동하는 Intent 생성
                    startActivity(intent) // Intent 시작
                    finish() // 현재 Activity 종료
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}