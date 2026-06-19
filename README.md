# IELTS & GRE Prep App

An Android application designed to help users prepare for the IELTS and GRE exams by building their vocabulary. Built with Kotlin, Jetpack Compose, and the Gemini AI API.

## Overview

This project includes two main components:
1. **Android App**: A Jetpack Compose application that helps users learn and practice high-frequency English words. It uses the Gemini API for intelligent features and supports notifications to keep users engaged in their learning journey.
2. **Data Extraction Scripts**: Python scripts used to extract and process vocabulary from a PDF source into a clean, app-ready format.

## Project Structure

- `app/`: The Android application source code (Kotlin, Jetpack Compose).
- `extract.py`: A Python script that uses `PyPDF2` to extract raw text from the vocabulary PDF (`IELTS Vocabulary 1000 Words List PDF - IELTS Worldly.pdf`) into `pdf_dump.txt`.
- `process.py`: A Python script that parses the raw text in `pdf_dump.txt`, formats the words and their meanings, removes duplicates, and appends them to the app's raw resources (`app/src/main/res/raw/ielts_words.txt`).
- `IELTS Vocabulary 1000 Words List PDF - IELTS Worldly.pdf`: The source material for the vocabulary list.

## Prerequisites

### For the Android App
- [Android Studio](https://developer.android.com/studio) (latest version recommended)
- Java Development Kit (JDK) 11 or higher
- A Gemini API Key from [Google AI Studio](https://aistudio.google.com/)

### For Data Processing (Python Scripts)
- Python 3.x
- `PyPDF2` library (`pip install PyPDF2`)

## Setup and Installation

### 1. Configure the API Key
Create a `.env` file in the root directory (you can copy `.env.example`) and add your Gemini API Key:
```env
GEMINI_API_KEY=your_api_key_here
```

### 2. Run the Android App
1. Open the project folder in Android Studio.
2. Let Gradle sync and resolve all dependencies.
3. Select an emulator or connected physical device.
4. Click the **Run** button to build and install the app.

### 3. (Optional) Process Vocabulary Data
If you want to re-process the vocabulary list from the PDF:
1. Install Python dependencies: `pip install PyPDF2`
2. Run the extraction script: `python extract.py` (This generates `pdf_dump.txt`)
3. Run the processing script: `python process.py` (This updates the vocabulary file in the Android app's resources)

## Technologies Used

- **Android**: Kotlin, Jetpack Compose, Room Database, Coroutines, Retrofit, OkHttp.
- **AI**: Google Gemini API (via Firebase AI SDK).
- **Python**: PyPDF2 for text extraction and data wrangling.

## License

This project is licensed under the standard terms. See the repository for details.
