package org.outdoorbits.lbbscanner

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.text.Html
import android.util.Log
import java.io.IOException

class AssetImageGetter(private val context: Context) : Html.ImageGetter {
	override fun getDrawable(source: String): Drawable? {
		Log.d("AssetImageGetter", "Requested image: $source")
		return if (source.startsWith("file:///android_asset/")) {
			val assetPath = source.removePrefix("file:///android_asset/")
			try {
				val inputStream = context.assets.open(assetPath)
				val drawable = BitmapDrawable(context.resources, inputStream)
				drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
				drawable
			} catch (e: IOException) {
				Log.w("AssetImageGetter", "Image not found in assets: $assetPath", e)
				null
			}
		} else {
			null
		}
	}
}
