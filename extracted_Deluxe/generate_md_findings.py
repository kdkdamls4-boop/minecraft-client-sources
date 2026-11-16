import json
import os

with open(r'C:\Users\User\Desktop\Source\extracted_Deluxe\audit_results.json', 'r', encoding='utf-8') as f:
    items = json.load(f)

out_path = r'C:\Users\User\Desktop\Source\extracted_Deluxe\detailed_findings.md'
with open(out_path, 'w', encoding='utf-8') as f:
    f.write("# Detailed Audit Findings for Deluxe Client\n\n")
    
    by_type = {}
    for item in items:
        by_type.setdefault(item['type'], []).append(item)
    
    for t in sorted(by_type.keys()):
        entries = by_type[t]
        f.write(f"## Category: {t} ({len(entries)} items)\n\n")
        for e in entries:
            f.write(f"- **File**: `{e['file']}:{e['line']}`\n")
            f.write(f"  ```java\n  {e['snippet']}\n  ```\n\n")

print(f"Written detailed findings to {out_path}")
