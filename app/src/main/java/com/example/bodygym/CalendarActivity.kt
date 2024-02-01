package com.example.bodygym

import android.graphics.Color
import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CalendarActivity : AppCompatActivity() {

    private lateinit var memoAdapter: MemoAdapter
    private val memoList = ArrayList<Memo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_detail)

        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val closeButton: Button = findViewById(R.id.close_button)

        memoAdapter = MemoAdapter(memoList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = memoAdapter

        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val date = "$year-${month + 1}-$dayOfMonth"

            FirebaseDatabase.getInstance().getReference("Memos/$date")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            memoList.clear()
                            val memos = snapshot.children.iterator()
                            while (memos.hasNext()) {
                                val memoSnapshot = memos.next()
                                val memo = memoSnapshot.child("memo").getValue(String::class.java)
                                val userName = memoSnapshot.child("userName").getValue(String::class.java)
                                val shared = memoSnapshot.child("shared").getValue(Boolean::class.java)
                                val comment = memoSnapshot.child("comment").getValue(String::class.java) ?: ""

                                if (shared == true) {
                                    memoList.add(Memo(memo!!, userName!!, comment, memoSnapshot))
                                }
                            }

                            val dialogView = LayoutInflater.from(this@CalendarActivity).inflate(R.layout.dialog_recycler_view, null)
                            val builder = AlertDialog.Builder(this@CalendarActivity).setView(dialogView)
                            val alertDialog = builder.show()

                            val dateText: TextView = dialogView.findViewById(R.id.dateText)
                            dateText.text = date

                            val recyclerView: RecyclerView = dialogView.findViewById(R.id.recyclerView)
                            recyclerView.layoutManager = LinearLayoutManager(this@CalendarActivity)
                            recyclerView.adapter = memoAdapter

                            val closeButton: Button = dialogView.findViewById(R.id.closeButton)
                            closeButton.setOnClickListener { alertDialog.dismiss() }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // 로그를 출력하거나 사용자에게 오류 상황을 알리는 코드를 추가합니다.
                    }
                })
        }

        closeButton.setOnClickListener { finish() }
    }

    inner class MemoAdapter(private val memoList: ArrayList<Memo>) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

        inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val memoText: TextView = itemView.findViewById(R.id.memoText)
            val userName: TextView = itemView.findViewById(R.id.userName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.memo_item, parent, false)
            return MemoViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
            val memo = memoList[position]
            holder.memoText.text = memo.memo
            holder.userName.text = memo.userName

            if (memo.comment == "") {
                // 코멘트가 빈 문자열인 경우
                holder.memoText.setTextColor(Color.BLACK) // 기본 글자색을 검은색으로 설정
            } else {
                // 코멘트가 빈 문자열이 아닌 경우
                holder.memoText.setTextColor(Color.RED) // 글자색을 빨간색으로 변경
            }


            holder.itemView.setOnClickListener {
                val builder = AlertDialog.Builder(this@CalendarActivity)
                builder.setTitle("공유된 메모")
                builder.setMessage("메모: ${memo.memo}\n작성자: ${memo.userName}")

                val inputComment = EditText(this@CalendarActivity)
                builder.setView(inputComment)
                builder.setPositiveButton("확인") { dialog, which ->
                    val comment = inputComment.text.toString()
                    if (comment.isNotEmpty()) {
                        // 메모의 comment 필드를 업데이트합니다.
                        memo.memoSnapshot.ref.child("comment").setValue(comment).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this@CalendarActivity, "코멘트가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@CalendarActivity, "코멘트 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                builder.show()
            }
        }

        override fun getItemCount() = memoList.size
    }
}

data class Memo(val memo: String, val userName: String, val comment: String, val memoSnapshot: DataSnapshot)
