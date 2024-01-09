package com.example.bodygym

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUri: String? = intent.getStringExtra("imageUri") // 문자열로 받아오기
        Log.d("FullScreenImageActivity", "Image Uri: $imageUri") // 로그 출력

        val imageView: ImageView = findViewById(R.id.fullScreenImageView)
        Glide.with(this)
            .load(imageUri)
            .into(imageView)
    }
}