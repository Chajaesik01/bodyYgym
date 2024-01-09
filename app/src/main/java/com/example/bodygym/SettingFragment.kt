import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bodygym.BoardFragment
import com.example.bodygym.CalendarActivity
import com.example.bodygym.CalendarFragment
import com.example.bodygym.ChatListFragment
import com.example.bodygym.HomeFragment
import com.example.bodygym.LoginActivity
import com.example.bodygym.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingFragment :  Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var memoCheckButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setting_main, container, false)
    }


    /*
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationView = view.findViewById(R.id.nav_view)
        memoCheckButton = view.findViewById(R.id.checkMemoButton)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        userId?.let {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userNickname = dataSnapshot.child("nickname").value.toString()

                    // 특정 닉네임을 가진 사용자만 '메모 확인' 버튼을 볼 수 있습니다.
                    if ((userNickname == "관장님") || (userNickname == "관리자")) {
                        memoCheckButton.visibility = View.VISIBLE
                    } else {
                        memoCheckButton.visibility = View.GONE
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 데이터베이스 에러 처리
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException())
                }
            })
        }

        memoCheckButton.setOnClickListener {
            // AlertDialog로 "admin1234" 입력 받기
            val editText = EditText(requireActivity())
            AlertDialog.Builder(requireActivity())
                .setTitle("관리자 확인")
                .setMessage("관리자 코드를 입력하세요.")
                .setView(editText)
                .setPositiveButton("확인") { _, _ ->
                    if (editText.text.toString() == "admin1234") {
                        val intent = Intent(requireActivity(), CalendarActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireActivity(), "관리자 코드가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
         */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationView = view.findViewById(R.id.nav_view)
        memoCheckButton = view.findViewById(R.id.checkMemoButton)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        userId?.let {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userNickname = dataSnapshot.child("nickname").value.toString()

                    // 특정 닉네임을 가진 사용자만 '메모 확인' 버튼을 볼 수 있습니다.
                    if ((userNickname == "관장님") || (userNickname == "관리자")) {
                        memoCheckButton.visibility = View.VISIBLE
                    } else {
                        memoCheckButton.visibility = View.GONE
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 데이터베이스 에러 처리
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException())
                }
            })
        }

        memoCheckButton.setOnClickListener {
            val intent = Intent(requireActivity(), CalendarActivity::class.java)
            startActivity(intent)
        }







        bottomNavigationView = view.findViewById(R.id.nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val fragment = HomeFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_post -> {
                    val fragment = BoardFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_chat -> {
                    val fragment = ChatListFragment()
                    replaceFragment(fragment)
                    true
                }
                R.id.navigation_calendar -> {
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

        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()
    }
}