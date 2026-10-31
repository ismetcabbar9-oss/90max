package com.ninetymax.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.ninetymax.app.data.ThemeMode
import org.junit.Test
import org.robolectric.annotation.Config
import java.io.File

/** Görsel kontrol için ekran görüntüleri üretir (app/build/screenshots). */
abstract class ScreenshotBase : BaseUiTest() {
    protected fun shot(name: String) {
        compose.waitForIdle()
        val dir = File("build/screenshots").apply { mkdirs() }
        compose.runOnUiThread {
            val v = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp))
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

@Config(sdk = [34], qualifiers = "w411dp-h891dp-port-xhdpi")
class PhoneScreenshots : ScreenshotBase() {
    @Test fun capture() {
        shot("phone_today")
        open("Antrenman"); shot("phone_workout")
        open("Beslenme"); shot("phone_nutrition")
        open("Ürünler"); shot("phone_products")
        open("Daha"); shot("phone_more")
        open("Bakım"); shot("phone_care")
        app.repository.update { it.copy(settings = it.settings.copy(theme = ThemeMode.DARK)) }
        open("Bugün"); shot("phone_today_dark")
    }
}

@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-mdpi")
class TabletScreenshots : ScreenshotBase() {
    @Test fun capture() {
        shot("tablet_today")
        open("Antrenman"); shot("tablet_workout")
        open("Ürünler"); shot("tablet_products")
    }
}
