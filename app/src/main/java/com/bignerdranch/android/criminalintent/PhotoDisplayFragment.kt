package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment

class PhotoDisplayDialog private constructor(private val path: String, dialogContext: Context) : Dialog(dialogContext) {

    private lateinit var crimeScene: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_display_dialog)
        crimeScene = findViewById(R.id.crime_scene)
        crimeScene.setImageBitmap(getBitmap(path))
        crimeScene.setOnClickListener {
            this.dismiss()
        }
        setCanceledOnTouchOutside(true)
    }

    companion object {
        fun newInstance(path: String, context: Context): PhotoDisplayDialog {
            val photoDisplayFragment = PhotoDisplayDialog(path, context)
            val arg = Bundle().apply {
                putString(PHOTO_PATH, path)
            }
            return photoDisplayFragment
        }

        private const val PHOTO_PATH = "PHOTO_PATH"
    }
}