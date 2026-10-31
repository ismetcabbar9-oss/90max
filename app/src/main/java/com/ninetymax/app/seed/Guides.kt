package com.ninetymax.app.seed

data class Guide(val title: String, val points: List<String>)

/** Uygulama içi rehber metinleri. Genel bilgi amaçlıdır; tıbbi tavsiye değildir. */
object Guides {
    const val DISCLAIMER = "Bu bilgiler genel bilgilendirme amaçlıdır; tıbbi tavsiye, teşhis veya tedavi yerine geçmez. Sağlık sorunun, ilaç kullanımın veya şüphen varsa hekime ya da eczacıya danış."

    val skin = listOf(
        Guide("Temel rutin (ilk 14 gün)", listOf(
            "Sabah: nazik temizleme (veya sadece su) → nemlendirici → SPF 50.",
            "Akşam: temizleme → nemlendirici.",
            "İlk iki hafta hiçbir aktif içerik ekleme; cildin temel rutine alışsın.",
        )),
        Guide("Aktif içerikleri güvenle başlatma", listOf(
            "Tek seferde yalnızca BİR yeni aktif başlat; en az 3-4 hafta gözlemle.",
            "Önerilen sıra: 15. gün azelaik asit (haftada 3 akşam) → sorun yoksa 43. gün retinoid (haftada 2 akşam).",
            "Bu adımlar Ayarlar > Rutinler'de varsayılan olarak kapalıdır; hazır olduğunda kendin aç.",
            "Retinoid için 'bezelye kuralı': tüm yüze bezelye büyüklüğü yeterlidir. Göz çevresi ve dudak kenarlarından uzak dur.",
            "Kuruluk/soyulma olursa sıklığı azalt veya 'sandviç yöntemi' uygula: nemlendirici → aktif → nemlendirici.",
            "Yanma, belirgin kızarıklık, şişlik veya kabarcık olursa kullanımı bırak ve gerekirse dermatoloğa başvur.",
        )),
        Guide("Birlikte kullanım uyarıları", listOf(
            "Retinoid + AHA/BHA (glikolik, salisilik) + benzoil peroksit aynı akşam kullanılmamalı.",
            "Retinoid ve azelaik asidi farklı akşamlarda kullanmak tahriş riskini azaltır.",
            "Niasinamid çoğu içerikle uyumludur; ancak yeni başladığın aktifle aynı dönemde ekleme.",
            "Aktif kullanırken güneş koruyucu atlanmaz; cilt güneşe daha duyarlı olabilir.",
            "Tıraş sonrası tahriş olmuş cilde aktif sürme.",
        )),
        Guide("Cilt görünümü takibi", listOf(
            "Haftada bir aynı ışıkta, aynı açıdan yüz fotoğrafı çek (Gelişim > Fotoğraflar > Yüz).",
            "Bakım > Görünüm notu ile 1-5 arası puan ver; değişimleri 4 haftalık periyotlarla değerlendir.",
        )),
    )

    val hair = listOf(
        Guide("Bukleleri belirginleştirme", listOf(
            "Yıkama 2-3 günde bir; sülfatsız şampuanı yalnızca saç diplerine uygula.",
            "Saç kremini bol kullan ve saç ıslakken parmaklarla aç; kuru saçı tarama.",
            "Saç sırılsıklamken krem → jel uygula, sıkarak (scrunch) şekillendir.",
            "Havluyla ovma; mikrofiber havlu veya pamuklu tişört kullan.",
            "Havada veya difüzörle düşük ısıda kurut; kuruduktan sonra jelin oluşturduğu sertliği avuçlayarak kır.",
            "Yıkamasız günlerde su spreyi + az krem ile tazele.",
        )),
        Guide("Uzunluğu koruyarak sağlıklı görünüm", listOf(
            "8-12 haftada bir yalnızca uçları aldır (kuru kesim/bukle kesimi yapan kuaför tercih edilebilir).",
            "Haftada bir maske veya yoğun bakım.",
            "Isıyla şekillendirmeyi azalt; kullanırsan ısı koruyucu sür.",
            "Gece saten yastık kılıfı veya gevşek topuz sürtünmeyi azaltır.",
            "Yeterli protein, uyku ve stres yönetimi saç görünümünü de etkiler.",
        )),
    )

    val beard = listOf(
        Guide("Temiz ve bakımlı sakal", listOf(
            "Sakalı haftada 2-3 kez sakal şampuanıyla yıka; diğer günler su yeterli.",
            "Her sabah 2-4 damla sakal yağı; köke kadar yedir ve tara.",
            "Haftada 1 boy eşitleme: makinede uzun taraktan başla, kısaltarak ilerle.",
            "Yanak hattı doğal çizgiyi takip etsin; boyun hattı Adem elmasının ~2 parmak üstünde bitsin.",
            "Bıyık dudak çizgisini geçmesin; makasla düzelt.",
            "Tıraş sonrası tahriş için onarıcı krem; batık kıla karşı haftalık nazik peeling.",
        )),
    )

    val oral = listOf(
        Guide("Günlük ağız hijyeni", listOf(
            "Günde 2 kez, 2 dakika florürlü macunla fırçala.",
            "Fırçaladıktan sonra tükür; hemen suyla çalkalama ki florür etkisini sürdürsün.",
            "Günde bir kez diş ipi veya arayüz fırçası.",
            "Dil temizliği nefes tazeliğine yardımcı olur.",
            "Fırça başlığını 3 ayda bir değiştir.",
        )),
        Guide("Güvenli beyazlatma bilgileri", listOf(
            "En güvenli başlangıç: profesyonel diş temizliği (tartar/leke temizliği).",
            "Kahve, çay, sigara ve kola leke yapar; sonrasında su içmek veya ağzı çalkalamak yardımcı olur.",
            "Kömürlü ve aşındırıcı tozlar mineyi aşındırabilir; önerilmez.",
            "Limon, karbonat gibi ev yöntemleri mineye zarar verebilir.",
            "Peroksitli beyazlatma (ev tipi plak veya klinikte) diş hekimi kontrolünde yapılmalı; hassasiyet yapabilir.",
        )),
        Guide("Diş hekimi kontrolü", listOf(
            "Genel öneri: 6 ayda bir kontrol ve gerekirse temizlik.",
            "Ayarlar'dan bir sonraki kontrol tarihini girersen o gün görev olarak görünür.",
            "Kanayan diş eti, ağrı veya hassasiyet devam ederse beklemeden diş hekimine git.",
        )),
    )

    val posture = listOf(
        Guide("Dik ve atletik duruş", listOf(
            "Duvar testi: topuk, kalça, kürek kemikleri ve baş duvara değsin; bel boşluğuna bir el sığsın.",
            "Kürek kemiklerini 'arka cebe koyuyormuş' gibi geri-aşağı al; göğsü zorla şişirme.",
            "Ekranı göz hizasına yaklaştır; telefona bakarken başını değil telefonu kaldır.",
            "Arka omuz, üst sırt ve core çalışmaları duruşu destekler (programda her hafta var).",
        )),
        Guide("Gerçekçi beklentiler", listOf(
            "Duruş düzeldikçe daha uzun ve geniş görünmek mümkündür; ancak yetişkinlerde boy uzamasını veya kemik yapısını değiştiren egzersiz, takviye ya da yöntem yoktur.",
            "Omuz genişliği görünümü yan omuz ve kanat kası gelişimiyle, bel inceliği ise kontrollü yağ oranıyla iyileşir.",
        )),
        Guide("Kondisyon ve nefes", listOf(
            "Günde 30 dk tempolu yürüyüş veya 7.000-10.000 adım iyi bir hedef.",
            "Haftada 1-2 kez 10-15 dk ip atlama veya tempo artırılmış yürüyüş eklenebilir.",
            "Kutu nefes (4-4-4-4) antrenman sonrası ve uyku öncesi sakinleşmeye yardımcı olur.",
        )),
    )

    val sleep = listOf(
        Guide("Kaliteli uyku", listOf(
            "Her gün aynı saatte yat ve kalk (hafta sonu ±1 saat).",
            "Yetişkinler için 7-9 saat; kas gelişimi ve toparlanma için 7,5+ saat hedefle.",
            "Yatmadan 6 saat önce kafeini bırak; geç saatte ağır yemek yeme.",
            "Oda karanlık, serin (18-20 °C) ve sessiz olsun.",
            "Sabah uyanınca 5-10 dk gün ışığı almak ritmi düzenler.",
        )),
    )

    val supplements = listOf(
        Guide("Takviye yaklaşımı", listOf(
            "Önce beslenme, uyku ve antrenman. Takviyeler bu temeller üzerine küçük katkılardır.",
            "Az ama anlamlı: kreatin monohidrat en güçlü kanıta sahip seçeneklerden biridir.",
            "Protein tozu yalnızca yemekle protein hedefine ulaşamadığın günler için pratik bir alternatiftir.",
            "D vitamini, omega-3 ve magnezyum herkes için zorunlu değildir; ihtiyaç kişiye özeldir. Eksiklik şüphesinde hekim değerlendirmesi ve gerekirse kan testi en doğru yoldur.",
            "Bir ürünü 'Kullanılıyor' olarak işaretlersen günlük görevlerde takip edilir.",
        )),
    )

    val style = listOf(
        Guide("Clean Boy stilinin temeli", listOf(
            "Az ama kaliteli parça; sade renkler: beyaz, siyah, lacivert, gri, bej, haki.",
            "İyi oturan kıyafet her şeydir: omuz dikişi omuz ucunda, tişört kolları kolu hafif sarar.",
            "Logo yerine kumaş ve kesim; düz, temiz yüzeyler.",
            "Temiz beyaz sneaker gardırobun yıldızıdır; düzenli temizle.",
            "Bakımlı saç, düzgün sakal ve temiz tırnaklar stilin yarısıdır.",
        )),
        Guide("Kapsül gardırop önerisi", listOf(
            "Üst: 3 beyaz/siyah/gri düz tişört, 1-2 polo, 1 oxford gömlek, 1 ince triko (yuvarlak yaka), 1 kapüşonlu/yarım fermuarlı sweatshirt.",
            "Alt: 1 koyu indigo slim/straight jean, 1 açık renk chino (bej/taş), 1 siyah veya gri jogger/rahat pantolon.",
            "Dış giyim: 1 denim veya overshirt ceket, 1 bomber veya kısa ceket, kış için sade bir mont/kaban.",
            "Ayakkabı: beyaz deri sneaker, koyu renk günlük sneaker, isteğe bağlı loafer/desert boot.",
            "Aksesuar: sade saat, ince kemer (ayakkabıyla uyumlu), minimal güneş gözlüğü.",
        )),
        Guide("V görünümünü destekleyen seçimler", listOf(
            "Omuzda iyi oturan, gövdede hafif daralan üstler V-taper görünümünü vurgular.",
            "Yatay omuz detayları (raglan olmayan dikiş, apolet) omzu geniş gösterir.",
            "Çok bol (oversize) parçaları dar alt ile dengele.",
        )),
        Guide("Parfüm", listOf(
            "Gündüz: ferah, narenciye, akuatik, temiz sabun notaları.",
            "Akşam: odunsu, hafif baharatlı notalar.",
            "2-3 sıkım yeterli; boyun ve bileklere sık, ovma.",
            "Almadan önce ten üzerinde 4-6 saat dene.",
        )),
    )
}
