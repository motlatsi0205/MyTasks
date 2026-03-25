package com.example.mytasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AboutFragment : Fragment() {

    private lateinit var ivProfile: ImageView
    private lateinit var etAppName: TextInputEditText
    private lateinit var etDevName: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private var profileUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profileUri = uri
                ivProfile.setImageURI(uri)
                // Request persistable permission
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        ivProfile = view.findViewById(R.id.ivProfile)
        etAppName = view.findViewById(R.id.etAppName)
        etDevName = view.findViewById(R.id.etDevName)
        etBio = view.findViewById(R.id.etBio)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveAbout)

        loadAboutData()

        ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        btnSave.setOnClickListener {
            saveAboutData()
            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun saveAboutData() {
        val prefs = requireActivity().getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("about_app_name", etAppName.text.toString())
            putString("about_dev_name", etDevName.text.toString())
            putString("about_bio", etBio.text.toString())
            putString("about_profile_uri", profileUri?.toString())
            apply()
        }
    }

    private fun loadAboutData() {
        val prefs = requireActivity().getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)
        etAppName.setText(prefs.getString("about_app_name", "My Tasks"))
        etDevName.setText(prefs.getString("about_dev_name", "Developer Name"))
        etBio.setText(prefs.getString("about_bio", "App Description..."))
        
        val uriString = prefs.getString("about_profile_uri", null)
        if (uriString != null) {
            profileUri = Uri.parse(uriString)
            ivProfile.setImageURI(profileUri)
        }
    }
}
