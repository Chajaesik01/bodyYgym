package com.example.bodygym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextId: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        editTextId = findViewById(R.id.editTextId)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = editTextId.text.toString()
            val password = editTextPassword.text.toString()
            loginUserOnFirebase(email, password)
        }

        btnRegister.setOnClickListener {
            // RegisterActivity를 실행하기 위한 Intent 생성
            val intent = Intent(this, RegisterActivity::class.java)

            // Intent를 실행하여 RegisterActivity를 시작
            startActivity(intent)

            Toast.makeText(this, "회원가입 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUserOnFirebase(email: String, password: String) {
        // 이메일과 비밀번호가 비어 있지 않은지 확인
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "$email 님, 로그인에 성공하셨습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java) // HomeActivity로 이동하는 Intent 생성
                    startActivity(intent) // Intent 시작
                    finish() // 현재 Activity 종료
                } else {
                    // 로그인 실패 시 예외 메시지를 사용자에게 보여줍니다.
                    Toast.makeText(this, "로그인 실패: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }
}