package com.example.mytasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import androidx.fragment.app.Fragment

class GalleryFragment : Fragment() {

    // A simple list of built-in Android icons to simulate a gallery
    private val icons = listOf(
        android.R.drawable.ic_menu_agenda,
        android.R.drawable.ic_menu_camera,
        android.R.drawable.ic_menu_call,
        android.R.drawable.ic_menu_my_calendar,
        android.R.drawable.ic_menu_recent_history,
        android.R.drawable.ic_menu_edit,
        android.R.drawable.ic_menu_delete,
        android.R.drawable.ic_menu_save,
        android.R.drawable.ic_menu_search
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        val gridView = view.findViewById<GridView>(R.id.galleryGridView)

        gridView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = icons.size
            override fun getItem(position: Int): Any = icons[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val imageView = if (convertView == null) {
                    ImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(300, 300)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setPadding(16, 16, 16, 16)
                    }
                } else {
                    convertView as ImageView
                }
                imageView.setImageResource(icons[position])
                return imageView
            }
        }
        return view
    }
}
