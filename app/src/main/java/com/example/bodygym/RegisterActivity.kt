package com.example.bodygym

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private var finalcheck = false

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("Users")

        val editTextEmail: EditText = findViewById(R.id.editTextId_Reg)
        val editTextRePass: EditText = findViewById(R.id.editTextRePass_Reg)
        val editTextNickName: EditText = findViewById(R.id.editTextNickName_Reg)
        val btnRegister: Button = findViewById(R.id.btnLogin)
        val btnCheckNick: Button = findViewById(R.id.btnCheckNick_Reg)
        val checkBoxAgree: CheckBox = findViewById(R.id.checkBoxAgree)
        val btnCancel : Button = findViewById((R.id.btnCancel))

        checkBoxAgree.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("약관 및 개인정보보호 동의")
                .setMessage("바디와이짐은 이용자의 개인정보를 중요하게 생각하며, 이용자의 개인정보 보호를 위해 최선을 다하고 있습니다. 서비스 제공을 위해 필요한 최소한의 개인정보만을 수집하고 있습니다.\n" +
                        "\n" +
                        "바디와이짐은 이용자의 사전 동의 없이는 이용자의 개인 정보를 공개하지 않으며, 수집된 정보는 다음과 같은 목적으로만 사용됩니다.\n" +
                        "\n" +
                        "1. 회원관리\n" +
                        "2. 기타 새로운 서비스 정보제공\n" +
                        "\n" +
                        "이용자는 개인정보 수집 및 이용에 대한 동의를 거부할 권리가 있으며, 동의 거부 시 바디와이짐의 서비스 제공에 제한을 받을 수 있습니다.\n" +
                        "\n" +
                        "아래 '동의' 버튼을 누르시면 위와 같이 개인정보 수집 및 이용에 동의하는 것으로 간주됩니다.")
                .setPositiveButton("동의") { _, _ ->
                    checkBoxAgree.isChecked = true
                }
                .setNegativeButton("아니요") { _, _ ->
                    checkBoxAgree.isChecked = false
                }
                .show()
        }

        btnCheckNick.setOnClickListener {
            val inputNick = editTextNickName.text.toString()
            checkNickname(inputNick)
            editTextNickName.isEnabled = false
        }
        btnCancel.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val userEmail = editTextEmail.text.toString()
            val userPassword = editTextRePass.text.toString()
            val userNickName = editTextNickName.text.toString()

            if (!checkBoxAgree.isChecked) {
                Toast.makeText(this, "개인정보보호동의를 체크하세요.", Toast.LENGTH_SHORT).show()
            } else if (userEmail.isNotEmpty() && userPassword.isNotEmpty() && userNickName.isNotEmpty()) {
                if (!finalcheck) {
                    Toast.makeText(this, "닉네임 중복확인을 해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    registerUserOnFirebase(userEmail, userPassword, userNickName)
                }
            } else {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkNickname(nickname: String) {
        db.orderByChild("nickname").equalTo(nickname).addListenerForSingleValueEvent(object:
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // 닉네임이 이미 존재합니다.
                    Toast.makeText(this@RegisterActivity, "이미 존재하는 닉네임입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 사용 가능한 닉네임입니다.
                    finalcheck = true
                    Toast.makeText(this@RegisterActivity, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 쿼리가 취소되었거나 실패했습니다.
                Toast.makeText(this@RegisterActivity, "닉네임 검색 실패: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUserOnFirebase(email: String, password: String, nickname: String) { // 닉네임 파라미터 추가
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid // Firebase에서 제공하는 uid를 가져옵니다.
                    if (userId != null) {
                        db.child(userId).child("nickname").setValue(nickname) // Realtime Database에 닉네임 저장
                    }
                    Toast.makeText(this, "$nickname 님, 회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java) // MainActivity로 이동하는 Intent 생성
                    startActivity(intent) // Intent 시작
                    finish() // 현재 Activity 종료
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}