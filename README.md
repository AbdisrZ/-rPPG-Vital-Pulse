# Vital Pulse: Remote Photoplethysmography (rPPG) Android App

**Vital Pulse** adalah aplikasi Android berbasis *non-contact heart rate monitoring* yang menggunakan teknologi **rPPG** (Remote Photoplethysmography). Aplikasi ini mampu mendeteksi detak jantung (BPM), variabilitas detak jantung (HRV), dan saturasi oksigen (SpO2) hanya dengan menggunakan kamera depan ponsel tanpa sensor fisik tambahan.

---

## 🚀 Cara Aplikasi Berjalan

Aplikasi ini bekerja dengan mendeteksi perubahan mikroskopis pada warna kulit wajah yang disebabkan oleh aliran darah di bawah kulit (denyut nadi).

1.  **Face Tracking:** Menggunakan ML Kit untuk mendeteksi wajah dan menentukan *Region of Interest* (ROI), khususnya area dahi.
2.  **Signal Extraction:** Mengekstrak nilai intensitas warna rata-rata dari kanal **Green** (hijau), karena kanal ini memiliki rasio sinyal-ke-derau (SNR) terbaik untuk hemoglobin.
3.  **Preprocessing:** Sinyal mentah dibersihkan menggunakan *Hamming Window* dan teknik normalisasi untuk menghilangkan *noise* akibat gerakan atau pencahayaan.
4.  **Signal Processing (FFT):** Mengubah sinyal dari domain waktu ke domain frekuensi menggunakan *Fast Fourier Transform* untuk menentukan frekuensi dominan yang merupakan detak jantung.
5.  **Visualization:** Menampilkan grafik rPPG secara *real-time* dan hasil akhir BPM kepada pengguna.

---

## 🛠️ Teknologi & Stack

### **Arsitektur & Infrastruktur**
*   **Language:** Kotlin (100%)
*   **UI Framework:** Jetpack Compose (Modern Declarative UI)
*   **Camera API:** CameraX (Analysis mode untuk pemrosesan frame per frame)
*   **AI/ML:** Google ML Kit Face Detection (Local, on-device processing)
*   **Signal Processing:** JTransforms (Library FFT berbasis Java tercepat untuk JVM)
*   **Concurrency:** Kotlin Coroutines (Untuk pemrosesan background agar UI tetap lancar)

---

## 📉 Mengapa Tidak Menggunakan FFT.js & Expo Go?

Pada rencana awal, aplikasi ini dipertimbangkan menggunakan React Native dengan Expo Go dan FFT.js. Namun, demi mencapai akurasi standar medis/tesis, diputuskan untuk menggunakan **Native Android (Kotlin)** karena:

1.  **Kompatibilitas & Performa Pixel:** rPPG membutuhkan akses ke *raw buffer* dari setiap frame kamera (30-60 fps). Expo Go memiliki *overhead* pada *bridge* JavaScript yang menyebabkan *dropped frames*, sehingga sinyal menjadi tidak konsisten.
2.  **Akses Native Terbatas:** Expo Go membatasi akses ke API kamera tingkat rendah yang diperlukan untuk mengunci fokus, eksposur, dan *white balance* secara manual—elemen krusial agar deteksi warna kulit stabil.
3.  **Optimasi Matematika:** Library FFT di JavaScript tidak secepat JTransforms (JVM) dalam melakukan operasi *Double Floating Point* secara paralel pada perangkat *mobile*.
4.  **Manajemen Memori:** Pemrosesan gambar di tingkat native jauh lebih efisien dalam penggunaan RAM dibandingkan melakukan transfer data gambar yang besar dari Native ke JavaScript.

---

## 📋 Software Requirements Specification (SRS)

### **Fitur Utama**
*   **Real-time Scanning:** Deteksi BPM dalam 15 detik.
*   **Advanced Vitals:** Estimasi HRV (RMSSD) dan Saturasi Oksigen.
*   **Live Waveform:** Visualisasi gelombang denyut nadi secara langsung.
*   **Data Export:** Kemampuan mengekspor data sinyal mentah ke format CSV untuk kebutuhan riset/tesis.

### **Persyaratan Sistem**
*   **OS:** Android 8.0 (Oreo) - API Level 26 atau lebih tinggi.
*   **Hardware:** Kamera depan minimal 720p; Prosesor Quad-core (disarankan Octa-core untuk ML Kit).
*   **Permissions:** Izin Kamera (Wajib).

---

## 📂 Struktur Proyek
*   `MainActivity.kt`: Logika UI dan manajemen siklus hidup kamera.
*   `rPPGAnalyzer.kt`: Mesin inti pemrosesan sinyal, deteksi wajah, dan kalkulasi FFT.
*   `Theme.kt`: Definisi desain sistem (Vital Pulse Theme).

---
*Developed for research and health monitoring innovation.*
