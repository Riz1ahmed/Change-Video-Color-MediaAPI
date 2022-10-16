package eu.sisik.videotogreyscale

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private var inputFile: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null)
            inputFile = savedInstanceState.getParcelable("inputFile")

        initView()
    }

    private fun initView() {
        butSelectVid.setOnClickListener {
            if (StControl.needsStoragePermission(this))
                StControl.requestStoragePermission(this, CODE_SELECT_VID)
            else Utils.performVideoSearch(this, CODE_SELECT_VID)
        }

        ivPreview.setOnClickListener { playPreview() }
        butConvertToGreyscale.setOnClickListener { requestConvertVideo() }
    }

    private fun configureUi() {
        if (Utils.isServiceRunning(this, ConversionService::class.java))
            progressEncoding.visibility = View.VISIBLE
        else progressEncoding.visibility = View.INVISIBLE

        tvSelectedVideo.text = ""
        if (inputFile != null) tvSelectedVideo.text = Utils.getName(this, inputFile!!) ?: ""

        val outFile = File(getOutputPath())
        if (outFile.exists()) {
            val thumb = ThumbnailUtils.createVideoThumbnail(outFile.absolutePath,
                MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
            ivPreview.setImageBitmap(thumb)
        }
    }

    private fun playPreview() {
        val outFile = File(getOutputPath())
        if (outFile.exists()) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(this, "$packageName.provider", outFile)
            else Uri.parse(outFile.absolutePath)

            val intent = Intent(Intent.ACTION_VIEW, uri)
                .setDataAndType(uri, "video/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setDataAndType(uri, "video/mp4")

            startActivityForResult(intent, CODE_THUMB)

        } else Toast.makeText(this, getString(R.string.app_name), Toast.LENGTH_LONG).show()
    }

    private fun requestConvertVideo() {
        if (inputFile != null) {
            val intent = Intent(this, ConversionService::class.java).apply {

                putExtra(ConversionService.KEY_OUT_PATH, getOutputPath())
                putExtra(ConversionService.KEY_INPUT_VID_URI, inputFile)

                // We want this Activity to get notified once the encoding has finished
                val pi = createPendingResult(CODE_PROCESSING_FINISHED, intent, 0)
                putExtra(ConversionService.KEY_RESULT_INTENT, pi)
            }

            startService(intent)

            progressEncoding.visibility = View.VISIBLE
        } else Toast.makeText(
            this, getString(R.string.err_no_input_file), Toast.LENGTH_LONG
        ).show()
    }

    private fun getOutputPath() = cacheDir.absolutePath + "/" + OUT_FILE_NAME

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0])
            Toast.makeText(this, getString(R.string.warn_no_storage_permission), Toast.LENGTH_LONG)
                .show()
        } else {
            if (requestCode == CODE_SELECT_VID) Utils.performVideoSearch(this, CODE_SELECT_VID)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_SELECT_VID && resultCode == Activity.RESULT_OK) {
            inputFile = data!!.data!!
        } else if (requestCode == CODE_PROCESSING_FINISHED) {
            progressEncoding.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        configureUi()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable("inputFile", inputFile)
    }

    companion object {
        const val TAG = "MainActivity"
        const val CODE_SELECT_VID = 6660
        const val CODE_THUMB = 6661
        const val CODE_PROCESSING_FINISHED = 6662
        const val OUT_FILE_NAME = "out.mp4"
    }
}
