import PyPDF2

def main():
    try:
        reader = PyPDF2.PdfReader('IELTS Vocabulary 1000 Words List PDF - IELTS Worldly.pdf')
        with open('pdf_dump.txt', 'w', encoding='utf-8') as f:
            for page in reader.pages:
                text = page.extract_text()
                if text:
                    f.write(text + '\n')
    except Exception as e:
        print(f"Error: {e}")

if __name__ == '__main__':
    main()
