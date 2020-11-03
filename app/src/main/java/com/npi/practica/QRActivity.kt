package com.npi.practica

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import com.budiyev.android.codescanner.*

class QRActivity : AppCompatActivity() {


    /*

    IMPLEMENTADO UTILIZANDO LA LIBRERIA -> https://github.com/yuriy-budiyev/code-scanner

     */


    private val requestCodeCameraPermission = 1001
    private lateinit var codeScanner: CodeScanner

    private lateinit var mainLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        mainLayout = findViewById(R.id.qr_activity_layout)

        askForCameraPermission()

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)

        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE

        codeScanner.scanMode = ScanMode.SINGLE // SINGLE o CONTINUOUS o PREVIEW

        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {

                if (Patterns.WEB_URL.matcher(it.text).matches()) {

                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.text))
                    startActivity(browserIntent)

                    /*

                    //Esto es por si se quisiera implementar un snackbar, el cual al clickarlo se abriria el enlace

                    val urlSnackbar = Snackbar.make(mainLayout, "Quieres abrir la URL?", Snackbar.LENGTH_INDEFINITE)

                    urlSnackbar.setAction("Abrir", object : View.OnClickListener {
                        override fun onClick(v: View?) {

                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.text))
                            startActivity(browserIntent)
                        }

                    })

                    urlSnackbar.show()

                     */

                }

            }
        }
        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                /*
                Toast.makeText(
                        this,
                        "ERROR inicializando la camara: ${it.message}",
                        Toast.LENGTH_LONG
                ).show()

                 */
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }

    }

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
                this@QRActivity,
                arrayOf(Manifest.permission.CAMERA),
                requestCodeCameraPermission
        )
    }


    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }


    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

}