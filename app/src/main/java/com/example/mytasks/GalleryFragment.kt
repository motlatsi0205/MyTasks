package com.example.mytasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

class GalleryFragment : Fragment() {

    private val photoList = mutableListOf<Uri>()
    private lateinit var gridView: GridView
    private lateinit var emptyText: TextView
    private lateinit var adapter: GalleryAdapter

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Add new image and persist
                photoList.add(uri)
                saveGallery()
                updateUI()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        
        gridView = view.findViewById(R.id.galleryGridView)
        emptyText = view.findViewById(R.id.emptyGalleryText)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddImage)

        // Load saved images
        loadGallery()

        adapter = GalleryAdapter(requireContext(), photoList)
        gridView.adapter = adapter

        btnAdd.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        updateUI()
        return view
    }

    private fun updateUI() {
        if (photoList.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            gridView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            gridView.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveGallery() {
        val prefs = requireActivity().getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        photoList.forEach { jsonArray.put(it.toString()) }
        prefs.edit().putString("gallery_photos", jsonArray.toString()).apply()
    }

    private fun loadGallery() {
        val prefs = requireActivity().getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("gallery_photos", null) ?: return
        val jsonArray = JSONArray(saved)
        photoList.clear()
        for (i in 0 until jsonArray.length()) {
            photoList.add(Uri.parse(jsonArray.getString(i)))
        }
    }

    private class GalleryAdapter(val context: Context, val uris: List<Uri>) : BaseAdapter() {
        override fun getCount(): Int = uris.size
        override fun getItem(position: Int): Any = uris[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val imageView = if (convertView == null) {
                ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        350
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(4, 4, 4, 4)
                }
            } else {
                convertView as ImageView
            }
            
            try {
                // Request persistable permission to keep access after reboot
                context.contentResolver.takePersistableUriPermission(
                    uris[position],
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}

            imageView.setImageURI(uris[position])
            return imageView
        }
    }
}
