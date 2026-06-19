import re
import os

def process():
    existing_words = set()
    txt_path = r'app\src\main\res\raw\ielts_words.txt'
    if os.path.exists(txt_path):
        with open(txt_path, 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split('|')
                if parts:
                    existing_words.add(parts[0].lower().strip())
    
    words_added = 0
    with open('pdf_dump.txt', 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_entries = []
    
    in_space_separated_section = False

    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        if "Most Used English Word List" in line or "Most Used English Words List" in line:
            in_space_separated_section = True
            continue

        if 'http' in line or len(line) < 2 or "IELTS Vocabulary" in line or "Must Learn" in line:
            continue

        if in_space_separated_section:
            parts = line.split()
            for w in parts:
                w = w.strip()
                if len(w) > 1 and w.isalpha() and w.lower() not in existing_words:
                    new_entries.append(f"{w}|High-frequency English word")
                    existing_words.add(w.lower())
                    words_added += 1
        else:
            # Hyphen separated section
            idx = -1
            for i, char in enumerate(line):
                if char in ['-', '–']:
                    idx = i
                    break
            if idx != -1:
                word = line[:idx].strip()
                meaning = line[idx+1:].strip()
                if 1 < len(word) < 50 and len(meaning) > 0:
                    if word.lower() not in ['non', 'self', 'well', 'off']:
                        word_clean = word.lower().replace('to ', '', 1).strip() if word.lower().startswith('to ') else word
                        if word_clean.lower() not in existing_words:
                            new_entries.append(f"{word}|{meaning}")
                            existing_words.add(word_clean.lower())
                            words_added += 1

    if new_entries:
        with open(txt_path, 'a', encoding='utf-8') as f:
            for entry in new_entries:
                f.write(entry + '\n')
                
    print(f"Added {words_added} words to ielts_words.txt")

if __name__ == '__main__':
    process()
