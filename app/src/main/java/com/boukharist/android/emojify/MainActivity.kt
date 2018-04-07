/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.boukharist.android.emojify


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.example.android.emojify.R
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var mTempPhotoPath: String? = null
    private var mResultsBitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupClickListeners()
        // Set up Timber
        Timber.plant(Timber.DebugTree())
    }

    private fun setupClickListeners() {
        emojifyButton.setOnClickListener({ emojifyMe() })
        saveButton.setOnClickListener({ saveMe() })
        shareButton.setOnClickListener({ shareMe() })
        clearButton.setOnClickListener { clearImage() }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        // Called when you request permission to read and write to external storage
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera()
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // If the image capture activity was called and was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage()
        } else {
            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath)
        }
    }

    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private fun processAndSetImage() {
        // Toggle Visibility of the views
        emojifyButton.visibility = View.GONE
        titleTextView.visibility = View.GONE
        saveButton.visibility = View.VISIBLE
        shareButton.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath)

        // Detect the faces and overlay the appropriate emoji
        mResultsBitmap = Emojifier.detectFacesAndOverlayEmoji(this, mResultsBitmap)

        // Set the new bitmap to the ImageView
        imageView.setImageBitmap(mResultsBitmap)
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private fun launchCamera() {

        // Create the capture image intent
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the temporary File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = BitmapUtils.createTempImageFile(this)
            } catch (ex: IOException) {
                // Error occurred while creating the File
                ex.printStackTrace()
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.absolutePath

                // Get the content URI for the image file
                val photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile)

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     */
    private fun emojifyMe() {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION)
        } else {
            // Launch the camera if the permission exists
            launchCamera()
        }
    }

    /**
     * OnClick method for the save button.
     */
    private fun saveMe() {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath)

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap)
    }

    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     */
    private fun shareMe() {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath)

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap)

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath)
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     */
    private fun clearImage() {
        // Clear the image and toggle the view visibility
        imageView.setImageResource(0)
        emojifyButton.visibility = View.VISIBLE
        titleTextView.visibility = View.VISIBLE
        shareButton.visibility = View.GONE
        saveButton.visibility = View.GONE
        clearButton.visibility = View.GONE

        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath)
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_STORAGE_PERMISSION = 1
        private const val FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider"
    }
}
