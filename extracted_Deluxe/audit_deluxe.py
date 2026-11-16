import os
import re
import json

SOURCE_ROOT = r"C:\Users\User\Desktop\Source\extracted_Deluxe\deluxe-source"

PATTERNS = {
    "DISCORD_WEBHOOK": re.compile(r"discord(?:app)?\.com/api/webhooks", re.IGNORECASE),
    "WEBHOOK_GENERIC": re.compile(r"webhook", re.IGNORECASE),
    "TELEGRAM_BOT": re.compile(r"api\.telegram\.org/bot", re.IGNORECASE),
    "URL_HTTP": re.compile(r"https?://[^\s\"'>]+", re.IGNORECASE),
    "PROCESS_EXEC": re.compile(r"(Runtime\.getRuntime\(\)\.exec|ProcessBuilder)", re.IGNORECASE),
    "SYSTEM_EXIT_HALT": re.compile(r"(System\.exit|Runtime\.getRuntime\(\)\.halt)", re.IGNORECASE),
    "SOCKET_CALL": re.compile(r"\b(Socket|ServerSocket|DatagramSocket|WebSocketClient|WebSocketListener)\b"),
    "HTTP_CLIENT": re.compile(r"\b(HttpURLConnection|HttpClient|HttpRequest|HttpResponse|OkHttpClient)\b"),
    "URL_OPEN": re.compile(r"\b(new\s+URL\(|\.openConnection\(|\.openStream\()"),
    "BASE64_DECODE": re.compile(r"(Base64\.(?:getDecoder\(\)|decode))", re.IGNORECASE),
    "DYNAMIC_CLASSLOAD": re.compile(r"(ClassLoader|URLClassLoader|defineClass|sun\.misc\.Unsafe)", re.IGNORECASE),
    "NATIVE_METHOD": re.compile(r"\bnative\s+[a-zA-Z0-9_<>[\]]+\s+[a-zA-Z0-9_]+\s*\("),
    "SYSTEM_LOAD": re.compile(r"(System\.load(?:Library)?|Native\.load)", re.IGNORECASE),
    "HWID_SYSTEM_ID": re.compile(r"(HWID|hwid|ProcessorId|BIOS|UUID\.nameUUIDFromBytes|System\.getenv\(\"COMPUTERNAME\"\))", re.IGNORECASE),
    "CREDENTIALS_MINING": re.compile(r"(launcher_profiles|launcher_accounts|session\.getSelectedProfile|getSessionToken|getAccessToken)", re.IGNORECASE),
    "DREAMIX_PROFILE": re.compile(r"(ru\.dreamix|ProfileAPI|Dreamix)", re.IGNORECASE),
}

IGNORED_URLS = {
    "http://fabricmc.net", "https://fabricmc.net",
    "http://schemas.microsoft.com", "http://www.w3.org",
    "https://maven.fabricmc.net", "https://libraries.minecraft.net",
    "https://maven.maxhenkel.de", "https://api.modrinth.com"
}

def scan_files():
    findings = []
    file_count = 0
    java_count = 0
    
    for root, dirs, files in os.walk(SOURCE_ROOT):
        # Skip .git, gradle cache or recovery reports if any
        if "recovery" in root.split(os.sep):
            continue
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in [".png", ".otf", ".obj", ".gltf", ".wav", ".dll", ".jar", ".class"]:
                continue
            
            file_count += 1
            if ext == ".java":
                java_count += 1
            
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, SOURCE_ROOT)
            
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    lines = f.readlines()
            except Exception as e:
                findings.append({
                    "file": rel_path,
                    "line": 0,
                    "type": "READ_ERROR",
                    "snippet": str(e)
                })
                continue
            
            for line_idx, line in enumerate(lines, start=1):
                clean_line = line.strip()
                if not clean_line or clean_line.startswith("//") or clean_line.startswith("*"):
                    continue
                
                for cat, regex in PATTERNS.items():
                    matches = regex.findall(clean_line)
                    if matches:
                        # Filter out innocuous URLs
                        if cat == "URL_HTTP":
                            filtered_matches = []
                            for m in matches:
                                if any(m.startswith(ign) for ign in IGNORED_URLS):
                                    continue
                                filtered_matches.append(m)
                            if not filtered_matches:
                                continue
                        
                        findings.append({
                            "file": rel_path,
                            "line": line_idx,
                            "type": cat,
                            "snippet": clean_line[:200]
                        })

    print(f"Scanned {file_count} text/code files ({java_count} Java files).")
    print(f"Found {len(findings)} potential security items.")
    
    with open(r"C:\Users\User\Desktop\Source\extracted_Deluxe\audit_results.json", "w", encoding="utf-8") as out:
        json.dump(findings, out, indent=2, ensure_ascii=False)

if __name__ == "__main__":
    scan_files()
