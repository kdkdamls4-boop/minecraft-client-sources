import os
import re

base = r'C:\Users\User\Desktop\Source\extracted_Deluxe\deluxe-source\src'
urls = set()
for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith('.java'):
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                for line in fp:
                    m = re.findall(r'https?://[^\s"\'<>)]+', line)
                    for u in m:
                        urls.add((u, f))

for u, f in sorted(urls):
    print(f'{f}: {u}')
