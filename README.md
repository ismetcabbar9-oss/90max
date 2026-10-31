# 90Max — 90 Günlük Kişisel Gelişim ve Görünüş Uygulaması

90 gün boyunca antrenman, beslenme, cilt/saç/sakal/diş bakımı, duruş, uyku, stil ve gelişim takibini tek yerden yöneten, tamamen **çevrimdışı** çalışan Android uygulaması (telefon + tablet).

## İndirme ve kurulum

1. Depodaki **Releases** bölümünden en son sürümü aç (`Latest` etiketli).
2. `90Max-vX.Y.Z.zip` dosyasını indir, aç; içindeki `90Max.apk`'yı kur. Ayrıntılı adımlar ZIP içindeki `KURULUM.txt` dosyasında ve [`docs/KURULUM.txt`](docs/KURULUM.txt)'de.
3. Gereksinim: Android 8.0 (API 26) ve üzeri.

## Özellikler

| Bölüm | İçerik |
|---|---|
| **Bugün** | Tarih, program günü (Gün X/90, faz), günlük tamamlanma yüzdesi, sabah / gün içi / akşam görevleri, antrenman özeti, yemek planı, su/protein/kalori, bugün kullanılacak ürünler, yaklaşan hatırlatmalar, tamamlanmamış görevler, dünden kalanlar (cezasız: bugüne al / kapat), günlük değerlendirme, görev ekleme, erteleme ve atlama |
| **Antrenman** | 90 günlük aşamalı program (Faz 1 Temel → Faz 2 Hacim → toparlanma haftası → Faz 3 Yoğunluk), 5 antrenman + aktif toparlanma + dinlenme; yan/arka omuz, kanat, üst göğüs, core ve duruş öncelikli. 72 hareket: adım adım anlatım, form ipuçları, sık hatalar, şematik kas haritası, ev alternatifleri. Ekipmana göre otomatik hareket seçimi, hareket/set/tekrar/dinlenme düzenleme, haftalık düzen değiştirme, set-ağırlık-tekrar kaydı, dinlenme sayacı, geçmiş ve tahmini güç grafiği |
| **Beslenme** | Kalori (Mifflin-St Jeor + aktivite + fazla) ve protein (g/kg) hedefi, 6 öğünlük dönen plan, porsiyonlar, alternatif öğünler, kendi öğünlerini ekleme, öğün gizleme, ekonomik/et/balık/laktoz tercihleri, 36 besinli hızlı ekleme, su takibi, haftalık değerlendirme ve öneriler |
| **Ürünler** | 72 gerçek ürünlük hazır katalog (13 kategori: temizleyici, nemlendirici, SPF, aktifler, şampuan, bukle, sakal, diş, el/tırnak, vücut, parfüm/hijyen, spor ekipmanı, takviye). Her üründe amaç, kullanım, sıklık, sabah/akşam, uyarılar, alternatifler, fiyat + kontrol tarihi, fiyat karşılaştırma bağlantısı. Durumlar (satın alınacak / alındı / kullanılıyor / bitti / bıraktım), favoriler, arama ve filtre, ürün ekle-düzenle-sil, ürün fotoğrafı, alışveriş listesi, aylık bütçe ve harcama, 7 günlük kullanım takvimi |
| **Takviyeler** | Kreatin, protein tozu, omega-3, D3, magnezyum; amaç, genel kullanım, zaman, uyarılar, "kullanılıyor" ise günlük takip. Teşhis/doz önerisi yok, hekime yönlendirme var |
| **Bakım** | Cilt (aktifleri aşamalı ve tek tek başlatma rehberi, birlikte kullanım uyarıları), saç (bukle rutini), sakal, ağız-diş (güvenli beyazlatma, diş hekimi tarihi), vücut; her alanda düzenlenebilir sabah/akşam rutini ve 1-5 görünüm puanı takibi |
| **Duruş & Uyku** | Duruş rutini ve mobilite hareketleri, gerçekçi beklentiler (boy uzatma iddiası yok), uyku hedefi ve kaydı, aktivite/yürüyüş kaydı ve haftalık grafik |
| **Stil** | Clean Boy rehberi, kapsül gardırop, V görünümü için seçimler, parfüm; gardırop yönetimi (fotoğraf, renk, fiyat, alınacaklar), kombin oluşturma, kıyafet bütçesi |
| **Gelişim** | Kilo ve 6 vücut ölçüsü (grafik, düzenle/sil), ön/yan/arka + yüz/gülüş/saç fotoğrafları ve tarih karşılaştırma, Gün 1/30/60/90 değerlendirmeleri ve karşılaştırma tablosu, görev tamamlama grafiği |
| **Ayarlar** | Profil, program başlangıcını kaydırma / yeniden başlatma, açık/koyu/sistem tema, hatırlatmalar (saat, gün, aç/kapa, ekle/sil), bütçeler, rutin düzenleyici, ZIP yedek dışa/içe aktarma, tüm verileri silme |

Tasarım: Material 3, Türkçe arayüz, açık/koyu tema. Dar ekranda alt menü; 600 dp üzeri genişlikte yan menü (navigation rail), 840 dp üzerinde iki sütunlu düzen. Dikey ve yatay yönelim desteklenir.

## Mimari

- **Kotlin + Jetpack Compose (Material 3)**, tek Activity, Navigation Compose.
- **Veri:** Tüm durum tek bir `AppData` nesnesinde; `kotlinx.serialization` ile `files/data.json`'a yazılır. Yazma işlemi geçici dosya + atomik yer değiştirme ile yapılır; her gün otomatik yerel yedek tutulur (son 7 gün) ve ana dosya bozulursa en yeni sağlam yedekten kurtarılır. Android Yedekleme de etkindir. Kişisel ve küçük ölçekli bir veri seti için bu yaklaşım bir SQL veritabanından daha basit ve taşınabilir.
- **Bildirimler:** `AlarmManager.setAndAllowWhileIdle` (kesin alarm izni gerektirmez), açılışta ve cihaz yeniden başlatıldığında yeniden planlanır.
- **Çevrimdışı:** İnternet yalnızca ürünlerdeki "Fiyat karşılaştır" bağlantısı için gerekir (tarayıcıda açılır).

```
app/src/main/java/com/ninetymax/app/
  data/     Modeller, Repository (kalıcılık + yedek), ZIP yedekleme, fotoğraf içe aktarma
  domain/   Program/fazlar, hareket kütüphanesi, antrenman planı, beslenme, günlük görev motoru
  seed/     Varsayılan rutinler, hatırlatmalar, ürün kataloğu, rehber metinleri
  notify/   Hatırlatma planlama ve bildirim alıcıları
  ui/       Tema, ortak bileşenler, ekranlar
```

## Varsayımlar ve sınırlar

- Başlangıç: 73 kg, hedef 78 kg; boy 178 cm ve yaş 24 **varsayımdır** (yalnızca kalori tahmini için) — karşılama ekranında ve Ayarlar'da değiştirilebilir.
- Varsayılan ekipman: dambıl, direnç bandı, barfiks barı (değiştirilebilir; hiç ekipman yoksa program vücut ağırlığına geçer).
- **Fiyatlar uydurulmadı.** Katalogda tahmini fiyat alanı boştur; kullanıcı kontrol ettiği fiyatı ve tarihini girer. "Fiyat karşılaştır" bağlantısı bir fiyat karşılaştırma sitesinde arama açar.
- **Ürün ve hareket görselleri:** Ürün fotoğrafları telif nedeniyle eklenmedi; kategori ikonu gösterilir ve kullanıcı kendi fotoğrafını ekleyebilir. Hareketler için fotoğraf/video yerine adım adım anlatım + şematik kas haritası vardır.
- Kalori/protein değerleri yaklaşık değerlerdir. Uygulama tıbbi tavsiye vermez.

## Geliştirme

```bash
./gradlew :app:testDebugUnitTest   # birim + Robolectric arayüz testleri (telefon, tablet, yatay)
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # release APK (imzalama bilgisi varsa imzalı)
```

Testler: alan mantığı (program, görev planlama, erteleme, ekipmana göre hareket seçimi, beslenme hedefleri, katalog bütünlüğü, hatırlatma zamanları), kalıcılık (yeniden yükleme, bozuk dosyadan kurtarma, ZIP yedek gidiş-dönüş, geçersiz içe aktarmada veri koruma) ve tüm ekranların telefon/tablet/yatay boyutta gezildiği Compose arayüz testleri.

## CI/CD (GitHub Actions)

`.github/workflows/android.yml` her push'ta:
1. Testleri çalıştırır,
2. R8 ile küçültülmüş release APK derler ve imzayı `apksigner` ile doğrular,
3. APK'yı gerçek emülatörlerde (telefon API 29, tablet API 34) kurar, açar, karşılamayı geçer, ekranları gezer, yatay moda çevirir, yeniden açar ve çökme olmadığını kontrol eder (`scripts/smoke-test.sh`),
4. Hepsi geçerse `vX.Y.Z` etiketiyle bir **GitHub Release** oluşturur: ZIP (APK + KURULUM.txt), APK ve SHA256 özetleri.

## İmzalama

İmzalama anahtarı depoya **eklenmez**. Kalıcı anahtar için (güncellemelerin veri kaybı olmadan üzerine kurulabilmesi için) bir kez:

```bash
bash scripts/generate-keystore.sh      # kendi bilgisayarında
```

Çıkan değerleri GitHub → Settings → Secrets and variables → Actions altına ekle:
`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

Secret'lar tanımlı değilse CI, o derlemeye özel **geçici** bir anahtarla imzalar: APK kurulur ve çalışır, ancak farklı anahtarlı bir önceki sürümün üzerine güncelleme olarak kurulamaz (sürüm notlarında uyarı yer alır; önce yedek alıp eski sürümü kaldırmak gerekir).
