# RaSaSeR OCR Test — Android 15 compatible independent smoke-test application

**Independent package:** `com.rasaser.factory.universaldocument.test` — existing RaSaSeR apps are untouched.

**Included, actually wired:** Android SAF document picker; PDFBox native text extraction; Android PdfRenderer rendering and automatic on-device Tesseract OCR for image-only PDF pages; local OCR on JPEG/PNG/WebP; offline `eng`, `tur`, `ara` traineddata shipped inside the test APK; ZIP/XML native text extraction for DOCX, XLSX, PPTX, ODT/ODS/ODP, EPUB and plain text; Android built-in Unicode word/phrase text selection and copy; optional ML Kit device translation (explicit first-model Wi-Fi download; after download operates offline). No document upload or remote processing.

**Limitations:** This is the first actual Android adapter test. DOC/XLS/PPT *legacy binary* files are not supported. Complex Office layout and PDF visual glyph positioning are not reconstructed in the selectable TextView, and OCR word bounding boxes / PDF Extra-style overlaid drag handles are NOT yet offered. OCR languages at first install are English, Turkish and Arabic only, despite the extensible core. Cursive handwriting and mixed language detection may be inaccurate. The app renders only one PDF page at a time, caps files at 80 MiB, 20M image pixels, and does not alter source files. OCR output may contain errors; it must not be trusted as Quran source text.

## Build
Run from this folder with Gradle 8.11.1, JDK 17 and Android SDK 35:
`gradle :app:assembleDebug :app:assembleRelease`.

For OFFLINE OCR before building, place the verified Tesseract `eng.traineddata`, `tur.traineddata`, `ara.traineddata` v4+ LSTM files from `tesseract-ocr/tessdata_fast` into `app/src/main/assets/tessdata/`. GitHub Actions downloads them from the official repository into its ephemeral build directory. Models are not stored in the GitHub source tree.

Dependency licensing: Tesseract4Android / Tesseract / PDFBox Android Apache-2.0; tessdata_fast models source-specific licenses must be reviewed; ML Kit SDK terms apply. Android 15 Google Play release testing, security reviews, and model/data QA remain pending. This APK is a *test build*, not a Play Console release.

## Manual acceptance test
1. Open **Dosya seç**, choose a TXT/DOCX/PDF/Image.
2. For text-containing PDF, confirm text appears without OCR.
3. For image-only PDF or image, confirm OCR runs completely with phone offline; select appropriate OCR model.
4. Long press recognized words in the result pane, expand selection, tap Copy or the visible Kopyala button.
5. Translate only after choosing source and target language, allow model download for first use; retry with airplane mode after model exists.
6. PDF previous/next, orientation, large files, and Arabic diacritics must be tested on device.

This demo does NOT auto-install the large OCR/ML models into every other app. Factory uses an independent, pinned versioned snapshot of the portable core; app-specific adapters require explicit opt-in and QA.

This public demonstration contains independent Android test adapters. The versioned full portable core is preserved in the private RaSaSeR Factory repository and is **not published here**. This test app compiles separately without checking out any private repository.
