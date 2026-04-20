MOBİL GITHUB YÜKLEME PAKETİ

Bu paket telefonla GitHub'a yükleme için hazırlandı.
Klasör seçmek zorunda değilsin. Bu klasörde gördüğün dosyaların hepsini doğrudan repo köküne yükle.

NASIL YÜKLENİR
1) GitHub'da boş bir repo aç.
2) "Add file" > "Upload files" ile BU KLASÖRDEKİ tüm dosyaları repo köküne yükle.
   - "WORKFLOW-android-apk.yml.txt" dahil.
3) Upload bitince GitHub'da "Add file" > "Create new file" seç.
4) Dosya adı olarak şunu yaz:
   .github/workflows/android-apk.yml
5) Bu repo içindeki "WORKFLOW-android-apk.yml.txt" dosyasını aç, içeriğini kopyala ve yeni oluşturduğun dosyaya yapıştır.
6) Commit et.
7) Repo'da "Actions" sekmesine gir.
8) "Build Android APK" workflow'unu aç.
9) "Run workflow" ile çalıştır.
10) Bitince artifact içinden APK'yı indir.

NOTLAR
- Bu repo görünümünde dosyalar düz görünür; workflow çalışırken proje klasör yapısını otomatik geri kurar.
- İstersen sonra gereksiz görünen düz dosyaları silmeden bırakabilirsin; workflow onlarla çalışır.
- "keystore.properties.example" release için örnek dosyadır. Debug APK için gerekmez.
