package com.example.touchlesstester


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.touchless_fingerprint.TouchlessMainActivity
import com.example.touchless_fingerprint.screens.DeviceHashActivity

import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable

class MainActivity : AppCompatActivity() {

    private lateinit var enrollButton: MaterialButton
    private lateinit var bunchButton: MaterialButton
    private lateinit var matchButton: MaterialButton
    private lateinit var startButton: MaterialButton
    private lateinit var hashButton: MaterialButton
    private lateinit var idEditText: TextInputEditText
    private lateinit var resultsContainer: LinearLayout
    private lateinit var handRadioGroup: RadioGroup

    private var isEnrollMode = false
    private var isBunchMode = false
    private var isMatchMode = false
    private var isLeftHand = true

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
    fun wsqFileToBase64(filePath: String): String? {
        val wsqFile = File(filePath)
        if (!wsqFile.exists()) return null

        return try {
            val wsqBytes = wsqFile.readBytes()
            Base64.encodeToString(wsqBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        enrollButton = findViewById(R.id.enrollButton)
        bunchButton = findViewById(R.id.bunchButton)
        matchButton = findViewById(R.id.matchButton)
        startButton = findViewById(R.id.startButton)
        hashButton = findViewById(R.id.hashButton)
        idEditText = findViewById(R.id.idEditText)
        resultsContainer = findViewById(R.id.resultsContainer)
        handRadioGroup = findViewById(R.id.handRadioGroup)

        setupUI()
    }

    private fun setupUI() {
        isBunchMode = true
        bunchButton.isEnabled = false

        hashButton.setOnClickListener{
            // Open DeviceHashActivity
            startActivity(Intent(this, DeviceHashActivity::class.java))
        }
        // Mode buttons
        enrollButton.setOnClickListener {
            // Enable enroll mode
            isEnrollMode = true
            isMatchMode = false
            isBunchMode = false

            // Disable this button and enable the other
            enrollButton.isEnabled = false
            matchButton.isEnabled = true
            bunchButton.isEnabled = true

        }
        bunchButton.setOnClickListener {
            // Enable enroll mode
            isEnrollMode = false
            isMatchMode = false
            isBunchMode = true

            // Disable this button and enable the other
            enrollButton.isEnabled = true
            matchButton.isEnabled = true
            bunchButton.isEnabled = false
            idEditText.text?.clear()   // clears the current input
        }

        matchButton.setOnClickListener {
            // Enable match mode
            isMatchMode = true
            isEnrollMode = false
            isBunchMode = false

            // Disable this button and enable the other
            matchButton.isEnabled = false
            enrollButton.isEnabled = true
            bunchButton.isEnabled = true

//            updateStartButtonState()
        }


        // Hand selection
        handRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            isLeftHand = checkedId == R.id.leftHandRadio
        }

        // Enable start button when ID is entered
//        idEditText.addTextChangedListener {
//            updateStartButtonState()
//        }

        startButton.setOnClickListener {
            val id = idEditText.text.toString()
            if (id.isEmpty() && !isBunchMode) return@setOnClickListener

            // Open plugin
            TouchlessMainActivity().openCameraPreview(
                this,
                enrollID = if(id.isEmpty()) null else id,
                isLeftHand = isLeftHand,
                isEnrollment = isEnrollMode
            )
        }
        updateStartButtonState()

    }

    private fun updateStartButtonState() {
        startButton.isEnabled = true
        hashButton.isEnabled = true
//        startButton.isEnabled = idEditText.text?.isNotEmpty() == true && (isEnrollMode || isMatchMode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            displayResultsRealtime(data)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayResultsRealtime(data: Intent?) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(Color.WHITE)
            setContentPadding(24, 24, 24, 24)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        data?.extras?.keySet()?.forEach { key ->
            val value = data.extras?.get(key)
            Log.i("TESTER_APP", "This data  result $key, value $value")

            when (key.lowercase()) {
                "imagepath", "imagename" -> {
                    value?.toString()?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val imageView = ImageView(this).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 500
                                ).apply { bottomMargin = 12 }
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(file.toUri())
                            }
                            container.addView(imageView)
                        }
                    }
                }

                "wsqbase64" -> {

                    val tv = TextView(this).apply {
                        text = "WSQ Data: [Base64 length=${(value as? String)?.length ?: 0}]"
                        setTextColor(Color.DKGRAY)
                    }
                    container.addView(tv)
                }

                "nfiqscore" -> {
                    val score = value as? Int ?: -1
                    val (label, color) = when (score) {
                        1 -> "Excellent Quality" to Color.parseColor("#2E7D32") // Green
                        2 -> "Good Quality" to Color.parseColor("#388E3C")
                        3 -> "Fair Quality" to Color.parseColor("#F9A825") // Yellow
                        4 -> "Poor Quality" to Color.parseColor("#F57C00") // Orange
                        5 -> "Very Poor" to Color.parseColor("#C62828") // Red
                        else -> "Unknown" to Color.GRAY
                    }

                    val tv = TextView(this).apply {
                        text = "NFIQ Score: $score ($label)"
                        setTextColor(color)
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }
                    container.addView(tv)
                }

                "matchingscore" -> {
                    val score = value as? Int ?: -1
                    val tv = TextView(this).apply {
                        text = "Matching Score: $score"
                        setTextColor(if (score >= 70) Color.parseColor("#2E7D32") else Color.RED)
                    }
                    container.addView(tv)
                }

                "fingersdata" -> {
                    val converter = FingersDataConverter()
                    val restoredData = runCatching { converter.fromJson(value as String) }.getOrNull()
                    restoredData?.forEachIndexed { index, finger ->
                        val fingerContainer = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 16, 16, 16)
                            background = ContextCompat.getDrawable(
                                this@MainActivity, // replace with your Activity class
                                R.drawable.bg_card_rounded // make a drawable with soft corners
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 12 }
                        }

                        val title = TextView(this).apply {
                            text = "Finger #${index + 1}, ${finger.imageName}"
                            textSize = 18f
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(Color.BLACK)
                        }
                        fingerContainer.addView(title)

                        // Horizontal gallery of images
                        val imageRow = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 8 }
                        }

                        listOf(
                            finger.grayscalePath to "Grayscale",
                            finger.wsqPath to "WSQ",
                            finger.binaryPath to "Binary"
                        ).forEach { (path, label) ->
                            val file = File(path)
                            if (file.exists()) {
                                val imgLayout = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                                        marginEnd = 8
                                    }
                                }

                                val imgView = ImageView(this).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, 500
                                    )
                                    scaleType = ImageView.ScaleType.CENTER_CROP

                                    if (path.endsWith(".wsq", true)) {
                                        // ❌ Android cannot display WSQ directly
                                        // ✅ Show a placeholder instead
                                        setImageResource(R.drawable.bg_card_rounded)
                                    } else {
                                        setImageURI(file.toUri())
                                    }
                                }

                                val caption = TextView(this).apply {
                                    text = label
                                    gravity = Gravity.CENTER
                                    textSize = 12f
                                    setTextColor(Color.DKGRAY)
                                }

                                imgLayout.addView(imgView)
                                imgLayout.addView(caption)
                                imageRow.addView(imgLayout)
                            }
                        }
                        fingerContainer.addView(imageRow)


                        // Metadata
                        fingerContainer.addView(TextView(this).apply {
                            text = "NFIQ Score: ${finger.nfiqScore}"
                            setTextColor(
                                when (finger.nfiqScore) {
                                    1 -> Color.parseColor("#2E7D32")
                                    2 -> Color.parseColor("#388E3C")
                                    3 -> Color.parseColor("#F9A825")
                                    4 -> Color.parseColor("#F57C00")
                                    5 -> Color.parseColor("#C62828")
                                    else -> Color.GRAY
                                }
                            )
                        })

                        fingerContainer.addView(TextView(this).apply {
                            text = "Size: ${finger.imageWidth}x${finger.imageHeight} | DPI: ${finger.imageDp}"
                            setTextColor(Color.DKGRAY)
                        })

                        // Action buttons
                        val actions = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.END
                        }
                        val wsq_ctions = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.END
                        }


                        val openBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                            text = "Open"
                            setOnClickListener {
                                val file = File(finger.grayscalePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${packageName}.provider", // must match the authority in Manifest
                                        file
                                    )

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "image/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    startActivity(intent)
                                }

                            }
                        }

                        val shareBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                            text = "Share"
                            setOnClickListener {
                                val file = File(finger.grayscalePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${packageName}.provider", // same authority as in Manifest
                                        file
                                    )

                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "image/*"
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }

                                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                                }
                            }
                        }

                        actions.addView(openBtn)
                        actions.addView(shareBtn)
                        fingerContainer.addView(actions)


                        val file = File(finger.wsqPath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                this,
                                "${packageName}.provider",
                                file
                            )
                            // Share raw WSQ file
                            val shareFileBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                                text = "Share WSQ"
                                setOnClickListener {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/octet-stream"
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    startActivity(Intent.createChooser(shareIntent, "Share WSQ via"))
                                }
                            }

                            // Decode WSQ → Base64 → share as text
                            val shareBase64Btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                                text = "Share WSQ Base64"
                                setOnClickListener {
                                    try {
                                        val wsqBase64 = wsqFileToBase64(finger.wsqPath)
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, wsqBase64)
                                            type = "text/plain"
                                        }
                                        startActivity(Intent.createChooser(shareIntent, "Share WSQ Base64 via"))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Failed to decode WSQ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            wsq_ctions.addView(shareFileBtn)
                            wsq_ctions.addView(shareBase64Btn)
                        }

                        fingerContainer.addView(wsq_ctions)

                        container.addView(fingerContainer)
                    }
                }

                else -> {
                    val tv = TextView(this).apply {
                        text = "$key: $value"
                        setTextColor(Color.DKGRAY)
                    }
                    container.addView(tv)
                }
            }
        }

        card.addView(container)
        resultsContainer.addView(card, 0)
    }

}
