# Vital Pulse 💓

**Aplikasi Pemantauan Tanda Vital Nirkontak Berbasis rPPG (Remote Photoplethysmography) untuk Android**

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)

Vital Pulse mengukur **detak jantung (BPM)**, **variabilitas detak jantung (HRV)**, dan **estimasi tekanan darah** hanya dengan kamera depan smartphone — tanpa sensor fisik, tanpa alat tambahan, dan seluruh pemrosesan berjalan **on-device** (tidak ada data yang dikirim ke server).

---

## 📖 Apa itu rPPG?

Setiap kali jantung berdenyut, volume darah di pembuluh kapiler wajah berubah sesaat. Perubahan ini menyebabkan variasi warna kulit yang sangat halus — tidak terlihat oleh mata telanjang, tetapi terekam secara numerik pada nilai piksel kamera. **Remote Photoplethysmography (rPPG)** adalah teknik mengekstrak sinyal denyut nadi dari variasi warna tersebut, "remote" karena tidak ada kontak fisik antara sensor dan tubuh.

---

## ⚙️ Cara Kerja Sistem

Pipeline pemrosesan sinyal berjalan real-time pada setiap frame kamera (±30 fps):

```
Kamera Depan (CameraX)
        │
        ▼
1. Deteksi Wajah (ML Kit) ──► tentukan ROI dahi (forehead)
        │
        ▼
2. Ekstraksi Sinyal ──► konversi YUV→RGB, rata-rata kanal R/G/B pada ROI
        │
        ▼
3. Algoritma CHROM ──► kombinasi chrominance R/G/B untuk menekan
        │              noise gerakan & pencahayaan
        ▼
4. Hamming Window + FFT (JTransforms, buffer 128 sampel)
        │
        ▼
5. Cari frekuensi dominan pada rentang 0.7–4.0 Hz (42–240 BPM)
        │
        ▼
6. BPM + skor confidence + HRV (RMSSD) ──► tampilkan ke UI
```

Detail tiap tahap:

1. **Face Tracking** — Google ML Kit mendeteksi wajah (mode `FAST`, dijalankan tiap 10 frame agar hemat CPU), lalu *Region of Interest* (ROI) ditetapkan di area **dahi** karena paling minim gangguan (rambut, mata, mulut) dan kaya pembuluh kapiler.
2. **Ekstraksi Sinyal** — Frame YUV dari CameraX dikonversi ke RGB, lalu nilai rata-rata setiap kanal warna di dalam ROI dihitung dan disimpan ke buffer geser (*sliding buffer*) 128 sampel.
3. **Algoritma CHROM** — Ketiga kanal dinormalisasi terhadap rata-ratanya, lalu dikombinasikan menjadi dua sumbu chrominance (`X = 3R − 2G`, `Y = 1.5R + G − 1.5B`). Kombinasi `X − αY` menghasilkan sinyal denyut yang jauh lebih tahan terhadap gerakan kepala dan perubahan pencahayaan dibanding kanal hijau mentah.
4. **FFT** — Sinyal diberi *Hamming window* untuk mengurangi spectral leakage, lalu ditransformasi ke domain frekuensi dengan *Fast Fourier Transform* (JTransforms).
5. **Estimasi BPM** — Frekuensi dengan daya tertinggi pada rentang fisiologis 0.7–4.0 Hz dipilih sebagai detak jantung. **Skor confidence** dihitung dari rasio daya puncak terhadap rata-rata daya; jika confidence rendah beruntun (pengguna banyak bergerak), buffer di-flush dan pengukuran diulang otomatis.
6. **HRV & Estimasi Tekanan Darah** — HRV dihitung dengan metode **RMSSD** dari interval antar puncak sinyal. Tekanan darah diestimasi dari model sederhana berbasis BPM + HRV yang dapat **dikalibrasi** terhadap tensimeter sungguhan melalui menu Settings.

---

## ✨ Fitur

| Fitur | Deskripsi |
|---|---|
| 💓 **Real-time BPM** | Deteksi detak jantung langsung dari kamera dengan indikator status (Searching → Stabilizing → Measuring) |
| 📈 **Live Waveform** | Visualisasi gelombang sinyal rPPG secara real-time |
| 🫀 **HRV (RMSSD)** | Variabilitas detak jantung sebagai indikator kondisi tubuh |
| 🩺 **Estimasi Tekanan Darah** | Model estimasi sistolik/diastolik dengan kalibrasi manual |
| 📊 **Klasifikasi & Rekomendasi** | Status LOW / NORMAL / ELEVATED / HIGH beserta saran kesehatan |
| 🗂️ **Riwayat Pengukuran** | Semua hasil scan tersimpan lokal di database Room |
| 📅 **Insights Mingguan** | Statistik rata-rata, maksimum, dan minimum BPM 7 hari terakhir |
| 🔒 **Privasi Penuh** | Seluruh pemrosesan on-device, tidak ada data yang keluar dari perangkat |

---

## 🛠️ Teknologi

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Kamera | CameraX (`ImageAnalysis`, akses raw YUV buffer per frame) |
| Deteksi Wajah | Google ML Kit Face Detection (on-device) |
| Signal Processing | JTransforms (FFT), algoritma CHROM |
| Database | Room (SQLite) |
| Arsitektur | MVVM (ViewModel + StateFlow + Coroutines) |
| Build | Gradle Kotlin DSL, AGP 8.8, Kotlin 2.1 |

---

## 📂 Struktur Proyek

```
src/main/kotlin/com/invisiblepulse/rppg/
├── MainActivity.kt          # Entry point, navigasi, siklus hidup kamera
├── rPPGAnalyzer.kt          # Mesin inti: deteksi wajah, ekstraksi sinyal, CHROM, FFT
├── Theme.kt                 # Design system (Vital Pulse Theme)
├── data/
│   ├── AppDatabase.kt       # Konfigurasi Room database
│   ├── ScanDao.kt           # Query riwayat pengukuran
│   └── ScanRecord.kt        # Entity hasil scan
├── ui/
│   ├── VitalsScreen.kt      # Layar pengukuran utama (kamera + waveform)
│   ├── ResultsScreen.kt     # Hasil pengukuran & rekomendasi
│   ├── HistoryScreen.kt     # Riwayat pengukuran
│   ├── InsightsScreen.kt    # Statistik mingguan
│   └── SettingsScreen.kt    # Kalibrasi tekanan darah & pengaturan
├── utils/
│   ├── BPEstimator.kt       # Model estimasi tekanan darah & klasifikasi
│   └── CalibrationPrefs.kt  # Penyimpanan offset kalibrasi
└── viewmodel/
    └── ScanViewModel.kt     # State management & akses database
```

---

## 🚀 Cara Menjalankan

### Prasyarat

- **Android Studio** Ladybug atau lebih baru
- **JDK 11+**
- **Android SDK 35** (compile target), perangkat fisik dengan **Android 8.0 (API 26)** ke atas
- Perangkat fisik sangat disarankan (emulator tidak memiliki kamera depan sungguhan)

### Langkah

```bash
# 1. Clone repositori
git clone https://github.com/AbdisrZ/-rPPG-Vital-Pulse.git
cd -rPPG-Vital-Pulse

# 2. Buka di Android Studio → biarkan Gradle sync selesai

# 3. Hubungkan perangkat Android (aktifkan USB Debugging), lalu Run ▶
```

Atau lewat command line:

```bash
./gradlew assembleDebug        # build APK debug
./gradlew installDebug         # install ke perangkat yang terhubung
```

### Tips Pengukuran Akurat

1. Gunakan di ruangan dengan **pencahayaan cukup dan stabil** (hindari cahaya berkedip/backlight).
2. Posisikan wajah memenuhi frame dan **tahan posisi tetap** selama ±15 detik.
3. Pastikan dahi tidak tertutup rambut atau topi.

---

## 📱 Persyaratan Sistem

- **OS:** Android 8.0 (Oreo) / API 26 ke atas
- **Hardware:** Kamera depan minimal 720p
- **Izin:** Kamera (wajib) — satu-satunya izin yang diminta aplikasi

---

## ⚠️ Disclaimer

Aplikasi ini dikembangkan untuk **keperluan riset dan edukasi**. Hasil pengukuran — khususnya estimasi tekanan darah — **bukan alat diagnosis medis** dan belum tervalidasi secara klinis. Selalu gunakan alat medis tersertifikasi dan konsultasikan dengan tenaga medis profesional untuk keputusan kesehatan.

---

*Developed for research and health monitoring innovation.*
