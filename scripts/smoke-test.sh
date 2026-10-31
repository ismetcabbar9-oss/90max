#!/usr/bin/env bash
# Gerçek bir Android emülatöründe: APK'yı kurar, açar, karşılama ekranını geçer,
# ana ekranları gezer, ekranı döndürür ve çökme olmadığını doğrular.
set -euo pipefail
APK_DIR="${1:-dist}"
PKG=com.ninetymax.app
OUT=smoke-out
mkdir -p "$OUT"
APK="$(ls "$APK_DIR"/*.apk | head -1)"

dump() { # ekrandaki görünür metinleri xml olarak al
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1 || true
}
has_text() { dump; grep -q "text=\"[^\"]*$1" "$OUT/ui.xml"; }
tap_text() { # metni içeren ilk öğenin ortasına dokun
  dump
  python3 - "$OUT/ui.xml" "$1" <<'PY' | { read -r x y || exit 1; adb shell input tap "$x" "$y"; }
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
want = sys.argv[2]
nodes = []
for m in re.finditer(r'<node [^>]*>', xml):
    node = m.group(0)
    t = re.search(r'text="([^"]*)"', node)
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    if t and b:
        nodes.append((t.group(1).strip(), tuple(map(int, b.groups()))))
# Önce birebir eşleşme (ör. menü etiketi), yoksa içeren ilk öğe.
match = next((n for n in nodes if n[0] == want), None) or next((n for n in nodes if want in n[0]), None)
if match:
    x1, y1, x2, y2 = match[1]
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  sleep 2
}
screenshot() { adb exec-out screencap -p > "$OUT/$1.png" || true; }
alive() { adb shell pidof "$PKG" >/dev/null; }
fail() { echo "HATA: $1"; adb logcat -d > "$OUT/logcat.txt" || true; exit 1; }

SIZE="$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)"
W="${SIZE%x*}"; H="${SIZE#*x}"
swipe_up() { adb shell input swipe $((W / 2)) $((H * 4 / 5)) $((W / 2)) $((H / 4)) 300; sleep 1; }

echo "Kuruluyor: $APK"
adb install -r "$APK"
adb logcat -c
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell am start -W -n "$PKG/.MainActivity"
sleep 6
alive || fail "Uygulama açılışta kapandı"
screenshot 01-karsilama
has_text "90Max" || fail "Karşılama ekranı görünmedi"

# Karşılama ekranını aşağı kaydırıp programı başlat
for i in 1 2 3 4 5 6 7 8; do
  if has_text "Programı başlat"; then break; fi
  swipe_up
done
tap_text "Programı başlat" || fail "'Programı başlat' düğmesi bulunamadı"
sleep 3
has_text "Sabah rutini" || has_text "Gün 1" || fail "Bugün ekranı açılmadı"
screenshot 02-bugun

for screen in Antrenman Beslenme "Ürünler"; do
  tap_text "$screen" || fail "$screen sekmesi bulunamadı"
  alive || fail "$screen ekranında çökme"
  screenshot "03-$screen"
done

# Ekranı yatay çevir ve tekrar dene
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 3
alive || fail "Yatay moda geçişte çökme"
tap_text "Bugün" || true
screenshot 04-yatay
adb shell settings put system user_rotation 0
sleep 2

adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/.MainActivity"
sleep 4
alive || fail "Yeniden açılışta çökme"
if has_text "Programı başlat"; then fail "Veriler kalıcı değil (karşılama tekrar açıldı)"; fi
screenshot 05-yeniden-acilis

adb logcat -d > "$OUT/logcat.txt" || true
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  grep -A 20 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -40
  fail "Logcat'te çökme bulundu"
fi
echo "Cihaz testi başarılı."
