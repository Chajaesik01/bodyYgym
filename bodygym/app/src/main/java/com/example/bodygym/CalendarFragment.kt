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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

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
        val textViewMemo = view.findViewById<TextView>(R.id.text_view_memo)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = "$year/${month + 1}/$dayOfMonth"

            // 날짜를 클릭하면 먼저 해당 날짜의 메모를 불러옵니다.
            val userId = auth.currentUser?.uid
            userId?.let {
                myRef.child(it).child(date).get()
                    .addOnSuccessListener { snapshot ->
                        val memo = snapshot.getValue(String::class.java)
                        // 메모가 존재하면 그 값을, 존재하지 않으면 공백을 TextView에 설정합니다.
                        textViewMemo.text = memo ?: ""
                    }
                    .addOnFailureListener { e ->
                        // 데이터 불러오기 실패
                        e.printStackTrace()
                    }
            }

            context?.let { context ->
                AlertDialog.Builder(context)
                    .setTitle("메모를 입력하시겠습니까?")
                    .setPositiveButton("예") { dialog, _ ->
                        // '예' 버튼을 클릭하면 EditText를 포함하는 다이얼로그를 띄웁니다.
                        val editText = EditText(context)
                        AlertDialog.Builder(context)
                            .setTitle("$date 메모 추가")
                            .setView(editText)
                            .setPositiveButton("저장") { innerDialog, _ ->
                                // '저장' 버튼을 클릭하면 입력된 메모를 저장하고, TextView에도 설정합니다.
                                val memo = editText.text.toString()
                                saveMemo(date, memo)
                                textViewMemo.text = memo
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
            myRef.child(it).child(date).setValue(memo)
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