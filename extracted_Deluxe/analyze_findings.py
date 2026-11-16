import json

with open(r'C:\Users\User\Desktop\Source\extracted_Deluxe\audit_results.json', 'r', encoding='utf-8') as f:
    items = json.load(f)

by_type = {}
for item in items:
    t = item['type']
    by_type.setdefault(t, []).append(item)

for t, entries in sorted(by_type.items(), key=lambda x: len(x[1]), reverse=True):
    print(f"\n=======================================================")
    print(f"CATEGORY: {t} ({len(entries)} occurrences)")
    print(f"=======================================================")
    for e in entries:
        print(f"  {e['file']}:{e['line']}\n    {e['snippet']}")
