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
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bodygym.databinding.ActivityMainBinding
import com.example.bodygym.databinding.HomeMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private var _binding: HomeMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = HomeMainBinding.inflate(inflater, container, false)

        // Firebase 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("Users")

        // Mobile Ads SDK 초기화
        context?.let { MobileAds.initialize(it) }

        // AdView 로드
        loadBannerAd()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid
        userId?.let {
            fetchNickname(it)
            fetchImage(it)
        }

        binding.textViewGuide.setOnClickListener {
            openWebPage("https://m.blog.naver.com/body_y_gym_gogang?tab=1")
        }

        binding.imageViewQr.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setPrompt("QR 코드를 스캔해주세요.")
            integrator.setBeepEnabled(false)
            integrator.setBarcodeImageEnabled(true)
            integrator.captureActivity = CaptureActivity::class.java
            integrator.initiateScan()
        }

        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_post -> {
                    replaceFragment(BoardFragment())
                    true
                }
                R.id.navigation_chat -> {
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if(result != null) {
            if(result.contents == null) {
                Toast.makeText(context, "스캔이 취소되었습니다.", Toast.LENGTH_LONG).show()
            } else {
                openWebPage(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun fetchNickname(userId: String) {
        db.child(userId).child("nickname").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val nickname = dataSnapshot.getValue(String::class.java)
                //binding.textViewNickname.text = nickname
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리
            }
        })
    }

    private fun fetchImage(userId: String) {
        val storageReference = FirebaseStorage.getInstance().getReference("images").child("mainlogo.png")
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            if (isAdded) {
                Glide.with(this).load(uri).into(binding.imageViewCenter)
            }
        }.addOnFailureListener {
            // 오류 처리
        }
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "웹페이지를 열 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}