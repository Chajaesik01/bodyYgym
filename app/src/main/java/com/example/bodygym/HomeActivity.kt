package com.example.bodygym

import SettingFragment
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        val intent = Intent(this, HomeActivity::class.java)
        val post = intent.getSerializableExtra("post") as? ContentModel
        val fragment = BoardFragment()
        val bundle = Bundle()
        bundle.putSerializable("post", post)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction().replace(R.id.relativeLayout, fragment).commit()

        // BottomNavigationView를 찾습니다.
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        // BottomNavigationView의 아이템 선택 이벤트를 처리합니다.
        navView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_post-> {
                    replaceFragment(BoardFragment())
                    true
                }
                R.id.navigation_chat-> {
                    replaceFragment(ChatListFragment())
                    true
                }
                R.id.navigation_calendar -> {
                    replaceFragment(CalendarFragment())
                    true
                }
                R.id.navigation_settings -> {
                    replaceFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }

        // 앱이 시작될 때 'HomeFragment'를 보여줍니다.
        replaceFragment(HomeFragment())
    }

    // 프래그먼트를 교체하는 메소드입니다.
    fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.relativeLayout, fragment)
        transaction.commit()
    }
}