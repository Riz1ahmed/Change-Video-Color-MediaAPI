package eu.sisik.videotogreyscale

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.absoluteValue

object Utils {
    fun isServiceRunning(context: Context, clazz: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (i in am.getRunningServices(Integer.MAX_VALUE)) {
            if (i.service.className == clazz.name)
                return true
        }

        return false
    }

    fun getSupportedVideoSize(codec: MediaCodec, mime: String, requireRegu: Size): Size {
        fun isSupported(regu: Size) = codec.codecInfo.getCapabilitiesForType(mime)
            .videoCapabilities.isSizeSupported(regu.width, regu.height)
        // First check if exact combination supported
        if (isSupported(requireRegu)) return requireRegu

        // I'm using the resolutions suggested by docs for H.264 and VP8
        // https://developer.android.com/guide/topics/media/media-formats#video-encoding
        val supportedRegus = arrayListOf(
            Size(176, 144),
            Size(320, 240),
            Size(320, 180),
            Size(640, 360),
            Size(720, 480),
            Size(1280, 720),
            Size(1920, 1080)
        )

        // I prefer similar resolution with similar aspect
        val pix = requireRegu.width * requireRegu.height
        val preferredAspect = requireRegu.width.toFloat() / requireRegu.height.toFloat()

        val nearestToFurthest = supportedRegus.sortedWith(
            compareBy(
                { pix - it.width * it.height },
                // First compare by aspect
                {
                    val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                    else it.height.toFloat() / it.width.toFloat()
                    (preferredAspect - aspect).absoluteValue
                })
        )

        for (size in nearestToFurthest) if (isSupported(size)) return size

        throw RuntimeException("Couldn't find supported resolution")
    }

    private fun performFileSearch(
        activity: AppCompatActivity, code: Int, multiple: Boolean, type: String,
        vararg mimetype: String
    ) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            this.type = type
            putExtra(Intent.EXTRA_MIME_TYPES, mimetype)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
        }

        activity.startActivityForResult(intent, code)
    }

    fun performVideoSearch(activity: AppCompatActivity, code: Int) {
        performFileSearch(
            activity, code, false,
            "video/*",
            "video/3gpp",
            "video/dl",
            "video/dv",
            "video/fli",
            "video/m4v",
            "video/mpeg",
            "video/mp4",
            "video/quicktime",
            "video/vnd.mpegurl",
            "video/x-la-asf",
            "video/x-mng",
            "video/x-ms-asf",
            "video/x-ms-wm",
            "video/x-ms-wmx",
            "video/x-ms-wvx",
            "video/x-msvideo",
            "video/x-webex"
        )
    }

    fun getName(context: Context, fromUri: Uri): String? {
        var name: String? = null
        context.contentResolver
            .query(fromUri, null, null, null, null, null)?.use {
                if (it.moveToFirst())
                    name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        return name
    }
}

object StControl{
    fun needsStoragePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(activity: AppCompatActivity, code: Int) {
        if (Build.VERSION.SDK_INT >= 23)
            activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), code)
    }
}

fun Context.showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
fun logD(msg: String) = Log.d("xyz", msg)

