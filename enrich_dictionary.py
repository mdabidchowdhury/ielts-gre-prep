import os
import json
import urllib.request
import urllib.parse
import urllib.error
from concurrent.futures import ThreadPoolExecutor

def fetch_details(word):
    req = urllib.request.Request(
        f'https://api.dictionaryapi.dev/api/v2/entries/en/{urllib.parse.quote(word)}',
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    try:
        response = urllib.request.urlopen(req)
        data = json.loads(response.read().decode())
        if data and isinstance(data, list):
            entry = data[0]
            phonetic = entry.get('phonetic', '')
            if not phonetic and 'phonetics' in entry:
                for p in entry['phonetics']:
                    if p.get('text'):
                        phonetic = p['text']
                        break
            
            example = ''
            synonyms_list = []
            for meaning in entry.get('meanings', []):
                syns = meaning.get('synonyms', [])
                for s in syns:
                    if s not in synonyms_list:
                        synonyms_list.append(s)
                for d in meaning.get('definitions', []):
                    if d.get('example') and not example:
                        example = d['example']
            
            synonyms_str = ", ".join(synonyms_list[:2])
            return word, phonetic, example, synonyms_str
    except Exception:
        pass
    return word, None, None, None

def process_file(filepath):
    lines = []
    if not os.path.exists(filepath):
        print(f"{filepath} not found")
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = [line.strip() for line in f if line.strip()]
        
    results = []
    
    def work(line):
        parts = line.split('|')
        word = parts[0].strip()
        meaning = parts[1].strip() if len(parts) > 1 else ''
        existing_phonetic = parts[2].strip() if len(parts) > 2 else ''
        existing_example = parts[3].strip() if len(parts) > 3 else ''
        existing_synonyms = parts[4].strip() if len(parts) > 4 else ''
        
        # We want to re-fetch if synonyms are missing, or we can just always fetch.
        # But we only want to fetch if something is missing.
        if not existing_synonyms or not existing_example or "The student learned" in existing_example or existing_example == "No sample sentence available.":
            w, p, e, s = fetch_details(word)
            phonetic = p if p else f"/{word.lower()}/"
            example = e if e else "No sample sentence available."
            synonyms = s if s else ""
            return f"{word}|{meaning}|{phonetic}|{example}|{synonyms}"
        else:
            return line
            
    with ThreadPoolExecutor(max_workers=20) as executor:
        results = list(executor.map(work, lines))
        
    with open(filepath, 'w', encoding='utf-8') as f:
        for r in results:
            if r:
                f.write(r + '\n')
    print(f"Finished processing {filepath}")

def main():
    process_file(r'app\src\main\res\raw\ielts_words.txt')
    process_file(r'app\src\main\res\raw\gre_words.txt')

if __name__ == '__main__':
    main()
