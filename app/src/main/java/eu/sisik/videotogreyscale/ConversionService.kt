package eu.sisik.videotogreyscale

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri

/**
 * Copyright (c) 2019 by Roman Sisik. All rights reserved.
 */
class ConversionService: IntentService("ConversionService") {
    override fun onHandleIntent(intent: Intent?) {
        val outPath = intent?.getStringExtra(KEY_OUT_PATH)
        val inputVidUri = intent?.getParcelableExtra<Uri>(KEY_INPUT_VID_URI)!!

        VideoToGrayscaleConverter().convert(this, inputVidUri,outPath!!)

        val pi = intent.getParcelableExtra<PendingIntent>(KEY_RESULT_INTENT)
        pi.send()
    }

    companion object {
        const val KEY_OUT_PATH = "eu.sisik.videotogreyscale.key.OUT_PATH"
        const val KEY_INPUT_VID_URI = "eu.sisik.videotogreyscale.key.INPUT_VID_URI"
        const val KEY_RESULT_INTENT = "eu.sisik.videotogreyscale.key.RESULT_INTENT"
    }
}