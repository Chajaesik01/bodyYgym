package com.example.bodygym

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CalendarActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_detail)

        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val textViewMemo: TextView = findViewById(R.id.text_view_memo)
        val closeButton: Button = findViewById(R.id.close_button)

        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val date = "$year-${month + 1}-$dayOfMonth"

            FirebaseDatabase.getInstance().getReference("Memos/$date")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val memos = snapshot.children.iterator()
                            while (memos.hasNext()) {
                                val memoSnapshot = memos.next()
                                val memo = memoSnapshot.child("memo").getValue(String::class.java)
                                val userName = memoSnapshot.child("userName").getValue(String::class.java)
                                val shared = memoSnapshot.child("shared").getValue(Boolean::class.java)

                                // shared 값이 true인 경우에만 다이얼로그를 통해 메모 내용과 작성자의 닉네임을 출력합니다.
                                if (shared == true) {
                                    val builder = AlertDialog.Builder(this@CalendarActivity)
                                    builder.setTitle("공유된 메모")
                                    builder.setMessage("메모: $memo\n작성자: $userName")

                                    // 새로운 EditText 생성
                                    val inputComment = EditText(this@CalendarActivity)
                                    builder.setView(inputComment)
                                    builder.setPositiveButton("확인") { dialog, which ->
                                        val comment = inputComment.text.toString()
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                                        if (comment.isNotEmpty()) {
                                            // 메모의 comment 필드를 업데이트합니다.
                                            memoSnapshot.ref.child("comment").setValue(comment).addOnCompleteListener {
                                                if (it.isSuccessful) {
                                                    Toast.makeText(this@CalendarActivity, "코멘트가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(this@CalendarActivity, "코멘트 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {

                                        }
                                    }
                                    builder.show()
                                }
                            }
                        }
                    }


                    override fun onCancelled(error: DatabaseError) {
                        // 로그를 출력하거나 사용자에게 오류 상황을 알리는 코드를 추가합니다.
                    }
                })

        }


        closeButton.setOnClickListener { finish() }
    }
}