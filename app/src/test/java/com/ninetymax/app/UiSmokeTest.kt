package com.ninetymax.app

import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.ninetymax.app.data.AppData
import com.ninetymax.app.data.ProductStatus
import com.ninetymax.app.ui.MaxRoot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Uygulamayı gerçek Compose arayüzüyle açıp her ana ekranın çökmeden çizildiğini doğrular. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
abstract class BaseUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    protected val app: MaxApp get() = ApplicationProvider.getApplicationContext()

    @Before fun setUp() {
        app.repository.replaceAll(AppData())
        app.repository.update { d ->
            d.copy(onboarded = true, products = d.products.map { if (it.id == "creatine") it.copy(status = ProductStatus.IN_USE) else it })
        }
        compose.setContent { MaxRoot(app.repository) }
    }

    protected fun open(label: String) {
        compose.onAllNodesWithText(label).onFirst().performClick()
        compose.waitForIdle()
    }

    protected fun tab(label: String) {
        compose.onAllNodesWithText(label).onFirst().performClick()
        compose.waitForIdle()
    }

    protected fun assertShown(text: String) {
        compose.onAllNodesWithText(text, substring = true).onFirst().assertExists()
    }
}

@Config(sdk = [34], qualifiers = "w411dp-h891dp-port")
class PhoneUiTest : BaseUiTest() {
    @Test fun todayShowsCoreSections() {
        assertShown("Gün 1 / 90")
        assertShown("Sabah rutini")
        compose.onNodeWithText("Yüzü nazik temizleyiciyle yıka").performClick()
        compose.waitForIdle()
        assertShown("Akşam rutini")
    }

    @Test fun navigatesAllPrimaryScreens() {
        open("Antrenman"); assertShown("Başlat ve kaydet")
        tab("Haftalık plan"); assertShown("Ekipmanım")
        tab("Geçmiş"); assertShown("Performans gelişimi")
        tab("Hareketler"); assertShown("Hareket ara")
        open("Beslenme"); assertShown("Günlük hedefler")
        tab("Plan & hedefler"); assertShown("Hedef hesabı")
        tab("Haftalık"); assertShown("Son 7 gün")
        open("Ürünler"); assertShown("Ürün, marka veya amaç ara")
        tab("Benim ürünlerim"); assertShown("Kreatin")
        tab("Alışveriş & bütçe"); assertShown("Aylık bakım bütçesi")
        tab("Kullanım takvimi"); assertShown("Bugün ·")
    }

    @Test fun navigatesSecondaryScreens() {
        open("Daha")
        open("Bakım"); assertShown("Cilt rutini")
        listOf("Saç", "Sakal", "Ağız & diş", "Vücut").forEach { tab(it) }
        assertShown("Vücut, el & hijyen rutini")
        open("Daha"); open("Duruş & Uyku"); assertShown("Duruş & Kondisyon rutini")
        tab("Uyku"); assertShown("Uyku hedefi")
        tab("Aktivite"); assertShown("Aktivite ekle")
        open("Daha"); open("Takviyeler"); assertShown("Bugünkü takip")
        open("Daha"); open("Stil"); assertShown("Clean Boy stilinin temeli")
        tab("Gardırop"); assertShown("Kıyafet bütçesi")
        tab("Kombinler"); assertShown("Henüz kombin yok")
        open("Daha"); open("Gelişim"); assertShown("Program")
        tab("Kilo & ölçü"); assertShown("Kilo ekle")
        tab("Fotoğraflar"); assertShown("Fotoğraf ekle")
        tab("Değerlendirme"); assertShown("Başlangıç (Gün 1)")
        open("Daha"); open("Ayarlar"); assertShown("Profil")
        open("Daha"); open("Rutinleri düzenle"); assertShown("Sabah (")
    }

    @Test fun workoutSessionSavesLog() {
        open("Antrenman")
        compose.onNodeWithText(" Başlat ve kaydet", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Antrenmanı bitir ve kaydet", substring = true).performScrollTo().performClick()
        compose.waitForIdle()
        assert(app.repository.state.value.workoutLogs.size == 1)
    }

    @Test fun productDetailOpens() {
        open("Ürünler")
        compose.onNodeWithText("Nemlendiren Temizleyici (Hydrating Cleanser)").performClick()
        compose.waitForIdle()
        assertShown("Ne işe yarar")
        assertShown("Kontrol edilmedi")
    }
}

@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land")
class TabletUiTest : BaseUiTest() {
    @Test fun railAndTwoColumns() {
        // Geniş ekranda tüm bölümler yan menüde, Bugün ekranı iki sütun.
        assertShown("Gelişim")
        assertShown("Günün antrenmanı")
        assertShown("Sabah rutini")
        open("Bakım"); assertShown("Cilt rutini")
        open("Gelişim"); assertShown("Kilo trendi")
        open("Ürünler"); assertShown("Ürün, marka veya amaç ara")
    }
}

@Config(sdk = [34], qualifiers = "w891dp-h411dp-land")
class PhoneLandscapeUiTest : BaseUiTest() {
    @Test fun landscapeWorks() {
        assertShown("Sabah rutini")
        open("Antrenman"); assertShown("Başlat ve kaydet")
    }
}
