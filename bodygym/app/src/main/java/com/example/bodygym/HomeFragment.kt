package com.example.bodygym

import SettingFragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var textViewNickName: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageViewQr: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.home, container, false)
        // Firebase 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("Users")



        return inflater.inflate(R.layout.home_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid // Firebase에서 제공하는 uid를 가져옵니다.
        if (userId != null) {
            fetchNickname(userId) // Firebase Realtime Database에서 닉네임 가져오기
        }



        imageViewQr = view.findViewById(R.id.imageView_qr)
        imageViewQr.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setPrompt("QR 코드를 스캔해주세요.")
            integrator.setBeepEnabled(false)
            integrator.setBarcodeImageEnabled(true)
            integrator.captureActivity = CaptureActivity::class.java
            integrator.initiateScan()
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
    }

    private fun fetchNickname(userId: String) {
        val db = FirebaseDatabase.getInstance().getReference("Users")  // Users가 사용자 정보를 저장하는 노드라고 가정
        db.child(userId).child("nickname").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val nickname = dataSnapshot.getValue(String::class.java)
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(context, "스캔 취소", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "스캔 결과: " + result.contents, Toast.LENGTH_LONG).show()

                // QR 코드 인식 성공 시, 해당 URL로 웹 브라우저를 열어줍니다.
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse(result.contents)
                startActivity(openURL)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}