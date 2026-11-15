# Rencana & Laporan Testing — Pertemuan 13

**Kelompok:** Mind-Ur-Set (4 anggota)
**Nama aplikasi:** Mind-Ur-Set, aplikasi Android check-in, reframe, dan companion (`com.mindurset.app`)

| Anggota (Nama / GitHub) | Tugas testing | Berkas test yang ditulis |
|---|---|---|
| Christopher (@tophrr) | dependensi test, `MainDispatcherRule`, laporan ini | `testutil/MainDispatcherRuleTest.kt`, `gradle/libs.versions.toml`, `TESTING.md` |
| Christian (@repyh) | pisahkan consent gate, UI test consent | `ui/consent/ConsentScreenTest.kt`, `ui/consent/ConsentScreen.kt` |
| Tora (@toraaxlv) | pisahkan kartu krisis, UI test kartu krisis | `ui/safety/CrisisScreenTest.kt`, `ui/safety/CrisisScreen.kt` |
| Evan (@VANTER25) | DAO test Room in-memory | `data/local/CheckInDaoTest.kt`, `data/local/JournalDaoTest.kt` |

NIM tidak dicantumkan karena repository ini publik; daftar nama dan NIM tinggal di dokumen internal
kelompok.

**Link repository:** https://github.com/tophrr/mindurset
**Jenis proyek** (boleh lebih dari satu):
- [x] Room (data lokal, 11 tabel)
- [x] Retrofit / REST API (Companion memanggil Cloudflare Worker di `worker/`, kontrak v1)
- [ ] Firebase (Auth / Firestore / Storage)

---

## A. Audit Testability (hasil `cek_proyek.sh`)

Audit dijalankan pada `main` tanggal 13 November 2026, sebelum PR testing; hasil akhir setelah semua
PR merge: **✔ 14 · ⚠ 3 · ✘ 1**, dan satu-satunya ✘ adalah berkas ini sebelum ditambahkan.

| No | Pemeriksaan | Sudah | Belum | Commit perbaikan |
|----|-------------|:-----:|:-----:|------------------|
| 1 | Setiap ViewModel menerima repository lewat constructor | ✔ | | fondasi `f4e9a8e`, dijaga oleh 51 test ViewModel |
| 2 | Repository punya interface, ViewModel bergantung pada interface | ✔ | | `domain/repository/Repositories.kt`, 8 interface |
| 3 | ViewModel tidak memakai `Context`, `Uri`, `Log`, `Toast`, `FirebaseFirestore` | ✔ | | tidak ada pelanggaran di 15 ViewModel |
| 4 | Logika ada di fungsi murni, bukan di Composable | ✔ | | `domain/usecase/` + `safety/` (6 kelas, semua ber-test JVM) |
| 5 | Screen yang diuji stateless (menerima state + lambda) | ✔ | | #39 `306399a` (consent), #40 `fde7df5` (krisis) |
| 6 | Ikon yang dapat diketuk punya `contentDescription` | ✔ | | 12 `contentDescription = null` diperiksa manual: semuanya ikon dekoratif (chevron, centang mood, ikon di dalam tombol berlabel), jadi `null` sudah benar |

Catatan butir 5: dua screen yang diuji sekarang stateless (`ConsentContent`, `CrisisContent`). Screen
lain (`CheckIn`, `Dashboard`, `Therapy`, `Companion`, `Reflection*`, `Profile*`) masih membaca
`LocalAppContainer` di dalam composable, jadi belum dapat dipasang di `createComposeRule()` tanpa
refactor serupa.

## B. Daftar Unit yang Akan Diuji

| Jenis | Nama kelas / fungsi | Berkas | Prioritas (1–3) |
|-------|---------------------|--------|:---------------:|
| ViewModel | `CheckInViewModel` | `ui/home/checkin/CheckInViewModelTest.kt` | 1 |
| ViewModel | `CompanionViewModel` | `ui/companion/CompanionViewModelTest.kt` | 1 |
| ViewModel | `ConsentViewModel` | `ui/consent/ConsentViewModelTest.kt` | 1 |
| ViewModel | `ActionViewModel`, `ReframeViewModel`, `CrisisContactViewModel`, `ReflectionFormViewModel` | `ui/*/…Test.kt` | 2 |
| Fungsi murni | `ClassifyRiskLocally`, `EvaluateTriage`, `ComputeIntensityScore`, `IsQuietHour`, `ReframeStateMachine`, `LocalRiskFloor` | `domain/usecase/`, `safety/` | 1 |
| Mapper | `GenerationResponseParser` (kontrak Worker → domain) | `data/remote/GenerationResponseParserTest.kt` | 2 |
| Screen (UI) | `ConsentContent` | `androidTest/…/ui/consent/ConsentScreenTest.kt` | 1 |
| Screen (UI) | `CrisisContent` (L2 dan L3) | `androidTest/…/ui/safety/CrisisScreenTest.kt` | 1 |
| DAO (tantangan) | `CheckInDao`, `JournalDao` | `androidTest/…/data/local/` | 2 |

## C. Matriks Skenario ViewModel

| No | ViewModel | Skenario (Given / When / Then) | Jenis | Nama test |
|----|-----------|--------------------------------|-------|-----------|
| 1 | `ActionViewModel` | Given tabel kosong / When state dibaca / Then state = empty, bukan daftar kosong yang diklaim terisi | Kosong | `an empty table is the empty state, not a blank populated list` |
| 2 | `ActionViewModel` | Given repository melempar error / When state dibaca / Then state = error, dan retry membaca ulang | Gagal | `a failing read surfaces the error state and retry reads again` |
| 3 | `ConsentViewModel` | Given consent sudah diberikan / When grant dipanggil lagi / Then tetap satu baris di tabel | Aksi pengguna | `granting twice keeps one row` |
| 4 | `CheckInViewModel` | Given consent belum ada / When check-in disimpan / Then ditolak dan tidak ada baris tertulis | Gagal | `without consent a check-in is refused and nothing is stored` |
| 5 | `CheckInViewModel` | Given catatan memuat frasa L3 / When disimpan / Then kartu krisis muncul dan catatan tidak pernah ditulis | Loading / validasi | `an L3 note goes straight to the card and is never stored` |

**Test double yang dipakai:**

| Dependency asli | Pengganti di test | Jenis | Alasan |
|-----------------|-------------------|-------|--------|
| `CheckInRepository` | `Fake…Repository` di dalam `CheckInViewModelTest` | fake | memeriksa state hasil, bukan interaksi |
| `MicroActionRepository` | `FakeMicroActionRepository` | fake | daftar di memori + saklar error |
| `SessionRepository` | `FakeSessionRepository` | fake | sesi dapat diganti tanpa DataStore |
| `ConsentRepository` | `FakeConsentRepository` | fake | dua kondisi: granted dan belum |
| `SessionStore` | `FakeSessionStore` | fake | sesi null/terisi tanpa disk |
| Room (`MindUrSetDatabase`) | database in-memory | nyata, tapi sementara | membuktikan SQL, bukan tiruan SQL |

## D. Hasil

| Jenis test | Jumlah | Lulus | Perintah |
|------------|:------:|:-----:|----------|
| Unit test (JVM) | 107 | 107 | `./gradlew :app:testDebugUnitTest` |
| UI test (Compose) | 8 | 8 | `./gradlew :app:connectedDebugAndroidTest` |
| Integration test (DAO) | 7 | 7 | `./gradlew :app:connectedDebugAndroidTest` |

Angka di atas berasal dari run di mesin pengembang pada `main` `fde7df5`:

- Unit: 107 test di 21 kelas, 0 gagal (`app/build/test-results/testDebugUnitTest/*.xml`), di antaranya
  **51 test ViewModel** di 7 kelas.
- Instrumented, dijalankan pada handset `M2101K6G` (Android 16, SDK 36):
  `adb shell am instrument -w com.mindurset.app.test/androidx.test.runner.AndroidJUnitRunner` →
  `OK (15 tests)` dalam 11,9 detik. Rinciannya 5 `CheckInDaoTest`, 2 `JournalDaoTest`,
  4 `ConsentScreenTest`, 4 `CrisisScreenTest`.
- Gate lokal yang dipakai CI juga hijau:
  `flock /tmp/mindurset-gradle.lock ./gradlew spotlessCheck :app:lintDebug :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest`
  → `BUILD SUCCESSFUL in 2m 31s`.
- Link run GitHub Actions yang hijau:
  https://github.com/tophrr/mindurset/actions/runs/43753299109
- Screenshot hasil test: tidak ada. Kedua run dijalankan tanpa sesi Android Studio (host tanpa display),
  jadi buktinya berupa XML hasil test dan keluaran `am instrument` di atas, bukan tangkapan layar.

**Catatan instrumented test.** Dua hal harus ada di build debug agar test di atas dapat berjalan di
handset Android 16, keduanya hanya untuk debug/test:

1. `debugImplementation(libs.kotlinx.coroutines.test)`. Tanpa ini, `runTest` (yang dipanggil
   `createComposeRule` di dalamnya) gagal dengan `Exception handler was not found via a ServiceLoader`:
   pada Android 16 classloader hanya melihat service file APK target, bukan APK instrumentasi
   ([kotlinx.coroutines#4782](https://github.com/Kotlin/kotlinx.coroutines/issues/4782), bug upstream).
2. `androidTestImplementation` `androidx.test.espresso:espresso-core` 3.7.0. Versi 3.5.0 yang ikut
   `ui-test-junit4` memanggil `InputManager.getInstance()` yang sudah dihapus, sehingga setiap test
   gagal di `Espresso.onIdle`.

Task Gradle `connectedDebugAndroidTest` pada AGP 9.4.1 masih keluar dengan kode 1 di mesin ini padahal
laporannya mencatat `tests="15" failures="0"`, sementara APK yang sama menghasilkan `OK (15 tests)` lewat
`am instrument`. Itu masalah pencatatan UTP, bukan test yang gagal; instrumented test bukan gerbang CI
di repository ini.

**(Bonus) Coverage baris ViewModel: 60,4 %** (`346/584` baris pada kelas ViewModel tingkat atas;
`./gradlew :app:createDebugUnitTestCoverageReport`). Ambang bonus 70 % belum tercapai: 7 dari 15
ViewModel punya test, enam di antaranya di atas 90 % (`ActionViewModel` 95,0 %, `ReframeViewModel`
94,6 %, `CrisisContactViewModel` 93,5 %, `ConsentViewModel` 92,9 %, `CompanionViewModel` 91,8 %,
`CheckInViewModel` 91,4 %; `ReflectionFormViewModel` 73,5 %), sedangkan `Dashboard`, `Login`,
`Register`, `Profile`, `ProfileEdit`, `Settings`, `ReflectionList`, dan `ReflectionDetail` belum punya
test sama sekali. Bonus DAO test (+5) terpenuhi.

## E. Refleksi

1. **Bug apa yang ditemukan test?** Bukan bug baru, tetapi urutan yang salah: check-in dulu menyimpan
   ke Room lalu memeriksa frasa risiko, sehingga catatan L3 tetap tersimpan meski kartu krisis muncul.
   Skenario `an L3 note goes straight to the card and is never stored` sekarang gagal bila urutan itu
   kembali, dan perbaikannya ada di `c17b4d2`. Saat menulis test pada pertemuan ini, yang ditemukan
   justru dua bug lingkungan: `runTest` tidak dapat menemukan exception collector di Android 16 dan
   espresso 3.5.0 memanggil API yang sudah dihapus. Keduanya membuat *semua* test gagal sebelum satu
   assertion pun berjalan, dan keduanya diperbaiki dengan pin dependensi di `4e66242` — bukan dengan
   melonggarkan test.

2. **Refactor apa yang paling sulit?** Memisahkan `CrisisScreen` menjadi `CrisisRoute` + `CrisisContent`.
   Kartu krisis memegang `BackHandler`, intent telepon, dan sheet trusted contact sekaligus, jadi
   pemisahannya harus memastikan tidak ada satu pun dari ketiganya yang ikut ke composable stateless,
   tanpa mengubah satu kata pun teks yang sudah ditinjau secara klinis. Yang termudah: `ConsentScreen`,
   karena isinya hanya teks dan dua tombol.

3. **Satu test yang paling bernilai.** `an L3 note goes straight to the card and is never stored`.
   Test ini melindungi janji produk yang paling tidak boleh salah: ketika sinyal risiko muncul, tidak
   ada teks pengguna yang tersimpan. Kegagalan di sini tidak terlihat di UI — kartunya tetap muncul —
   sehingga hanya test yang memeriksa sisi penyimpanan yang dapat menangkapnya.

4. **Apa yang belum diuji, dan mengapa.** Delapan layar masih route-coupled sehingga belum punya UI
   test (`CheckIn`, `Dashboard`, `Therapy`, `Companion`, `Reflection*`, `Profile*`), dan delapan
   ViewModel tanpa test adalah alasan coverage berhenti di 60,4 %. Jalur jaringan Companion ke Worker
   hanya diuji di sisi kontrak (`worker/`, Vitest) dan parser DTO di JVM; panggilan sungguhan ke Worker
   belum masuk suite otomatis karena butuh `wrangler dev` di mesin yang sama. Pengujian pada bahasa
   Indonesia juga belum otomatis: assertion UI test mengambil teks dari resource sehingga lolos di
   `values-in/`, tetapi tidak ada test yang benar-benar menjalankan aplikasi dalam locale Indonesia.
