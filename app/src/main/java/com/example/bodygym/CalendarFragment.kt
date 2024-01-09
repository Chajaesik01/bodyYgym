package com.example.bodygym

import SettingFragment
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.GenericTypeIndicator



class CalendarFragment : Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var calendarView: CalendarView
    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize Firebase

        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("memos")
        auth = FirebaseAuth.getInstance()

        val view = inflater.inflate(R.layout.calendar_main, container, false)

        // 뷰를 찾아 변수에 할당
        calendarView = view.findViewById(R.id.calendar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textViewMemo: TextView = view.findViewById(R.id.text_view_memo)
        val textViewComment: TextView = view.findViewById(R.id.text_view_comment)
        val checkBox = view.findViewById<CheckBox>(R.id.checkBox)
        val editTextMemo = EditText(context)
        val editTextComment = EditText(context)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = "$year-${month + 1}-$dayOfMonth"
            val userId = auth.currentUser?.uid
            userId?.let {
                FirebaseDatabase.getInstance().getReference("Memos/$date/$userId").get()
                    .addOnSuccessListener { snapshot ->
                        val typeIndicator : GenericTypeIndicator<HashMap<String, Any>> = object: GenericTypeIndicator<HashMap<String, Any>>() {}
                        val memoData = snapshot.getValue(typeIndicator)

                        // 메모 정보를 불러옵니다.
                        val memo = memoData?.get("memo")?.toString() ?: ""
                        // 코멘트 정보를 불러옵니다.
                        val comment = memoData?.get("comment")?.toString() ?: ""

                        // 메모 정보와 코멘트 정보를 TextView에 설정합니다.
                        textViewMemo.text = "$memo"
                        textViewComment.text = "$comment"
                    }
                    .addOnFailureListener { e ->
                        // 데이터 불러오기 실패
                        e.printStackTrace()
                    }
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val user = auth.currentUser
                user?.let { user ->
                    val userId = user.uid
                    val memoMap = HashMap<String, Any>()
                    memoMap["shared"] = isChecked
                    memoMap["memo"] = textViewMemo.text.toString()

                    // Users 아래에서 자신의 닉네임 값을 얻어옵니다
                    val nicknameRef = FirebaseDatabase.getInstance().getReference("Users/$userId/nickname")
                    nicknameRef.get().addOnSuccessListener { snapshot ->
                        val nickname = snapshot.getValue(String::class.java)
                        memoMap["userName"] = nickname ?: "Unknown User" // 닉네임이 없는 경우 "Unknown User"로 저장
                        FirebaseDatabase.getInstance().getReference("Memos/$date/$userId").updateChildren(memoMap)
                    }.addOnFailureListener {
                        // 닉네임 불러오기 실패
                        it.printStackTrace()
                    }
                }
            }

            context?.let { context ->
                AlertDialog.Builder(context)
                    .setTitle("메모를 입력하시겠습니까?")
                    .setPositiveButton("예") { dialog, _ ->
                        val editText = EditText(context)
                        AlertDialog.Builder(context)
                            .setTitle("$date 메모 추가")
                            .setView(editText)
                            .setPositiveButton("저장") { innerDialog, _ ->
                                val memo = editText.text.toString()
                                val user = auth.currentUser
                                user?.let {
                                    val userId = user.uid
                                    val memoMap = HashMap<String, Any>()
                                    memoMap["shared"] = checkBox.isChecked
                                    memoMap["memo"] = memo
                                    memoMap["comment"] = "" // 'comment' 필드를 빈 문자열로 초기화

                                    val nicknameRef = FirebaseDatabase.getInstance().getReference("Users/$userId/nickname")
                                    nicknameRef.get().addOnSuccessListener { snapshot ->
                                        val nickname = snapshot.getValue(String::class.java)
                                        memoMap["userName"] = nickname ?: "Unknown User"
                                        FirebaseDatabase.getInstance().getReference("Memos/$date/$userId").setValue(memoMap)
                                        // 새로운 메모 저장 후 textViewMemo 업데이트
                                        textViewMemo.text = memo
                                    }.addOnFailureListener {
                                        it.printStackTrace()
                                    }
                                }
                                innerDialog.dismiss()
                            }
                            .setNegativeButton("취소") { innerDialog, _ ->
                                innerDialog.cancel()
                            }
                            .show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("아니요") { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
            }
        }


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
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment).commit()
    }

    private fun saveMemo(date: String, memo: String) {
        val userId = auth.currentUser?.uid
        userId?.let {
            val memoData = mapOf("memo" to memo, "shared" to false)
            myRef.child(it).child(date).setValue(memoData)
                .addOnSuccessListener {
                    // 데이터 저장 성공
                    Toast.makeText(context, "데이터 저장에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // 데이터 저장 실패
                    e.printStackTrace()
                    e.message?.let { it1 -> Log.d("Firebase Error", it1) }
                    Toast.makeText(context, "데이터 저장에 실패하였습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}