package com.example.quicknote

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.btnCreateNote)
        button.setOnClickListener {
            Toast.makeText(this, "Pretend we just created a note ✨", Toast.LENGTH_SHORT).show()
        }
    }
}
