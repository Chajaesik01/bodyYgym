import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.bodygym.BoardFragment
import com.example.bodygym.CalendarFragment
import com.example.bodygym.ChatListFragment
import com.example.bodygym.HomeFragment
import com.example.bodygym.LoginActivity
import com.example.bodygym.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingFragment :  Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.setting_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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