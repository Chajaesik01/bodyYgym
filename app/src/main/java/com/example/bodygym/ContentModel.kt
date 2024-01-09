package com.example.bodygym
import java.io.Serializable

data class ContentModel(val postid : String, val title: String, val content: String, val author: String) : Serializable