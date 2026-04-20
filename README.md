# IPTV Player

Android IPTV player starter project with:
- M3U playlist playback
- Xtream Codes login and live channel loading
- ExoPlayer (Media3) playback
- Favorites saved per user profile/session
- Category browsing
- Picture-in-picture
- Android TV launcher support
- Dark Netflix-style Compose UI

## Added in this version
- Login/profile screen
- Source picker: M3U or Xtream Codes
- Netflix-like home screen with hero player and horizontal content rows
- Search + category chips
- Favorites/My List
- Persistent session restore
- GitHub Actions APK build workflow

## Notes
- Xtream implementation is focused on live channels.
- Some providers may require custom headers, HTTPS config, or different output/container formats.
- Local-file M3U playback works, but restoring a previous local file session after app restart may need additional URI persistence logic.

## Build
Open in Android Studio and run, or use the included GitHub Actions workflow to generate a debug APK.


## Yeni eklenenler
- Canlı TV yanında film ve dizi VOD bölümleri
- Xtream live/movie/series yükleme
- Kanal kartlarında kısa EPG: şu an ve sıradaki program
- Android TV için daha belirgin focus/remote davranışı
- Release AAB üretimine uygun signing yapısı
- GitHub Actions ile debug APK + release AAB artifact üretimi

## Release imzalama
1. `keystore.properties.example` dosyasını `keystore.properties` olarak kopyala
2. kendi keystore dosyanı proje köküne koy ya da yol ver
3. alanları doldur
4. Android Studio veya `./gradlew bundleRelease` ile AAB üret

## Notlar
- Series tarafında listeleme eklendi; sezon/bölüm detay ekranı için ayrı akış gerekir.
- EPG, Xtream `get_short_epg` endpoint'i varsa dolacaktır.
- Bazı sağlayıcılarda VOD veya EPG endpoint adları / izinleri farklı olabilir.
