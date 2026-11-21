#!/usr/bin/env python3
"""Lists every place the upstream brand can still reach a user of the Android app.
Exit 1 when anything is found. Run from the repository root: python3 scripts/sapat/brand-sweep.py"""
import glob, re, sys
hits = []
for f in glob.glob('apps/multiplatform/common/src/commonMain/moko-resources/*/strings.xml'):
    t = open(f, encoding='utf-8').read()
    for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', t, flags=re.S):
        v = re.sub(r'https?://\S+|simplex:/\S+', '', m.group(2))
        if re.search(r'(?i)simplex|\bflux\b', v):
            hits.append(f"{f.split('/')[-2]}:{m.group(1)}: {v[:90]}")
for f in glob.glob('apps/multiplatform/common/src/commonMain/kotlin/**/*.kt', recursive=True) + glob.glob('apps/multiplatform/common/src/androidMain/kotlin/**/*.kt', recursive=True) + glob.glob('apps/multiplatform/android/src/main/**/*.kt', recursive=True):
    for i, line in enumerate(open(f, encoding='utf-8'), 1):
        if line.strip().startswith('//') or 'Preview' in f or 'sample' in line.lower():
            continue
        for lit in re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', line):
            if re.search(r'SimpleX|simplex\.chat/(blog|docs)|wefunder', lit):
                hits.append(f"{f}:{i}: {lit[:90]}")
for h in hits:
    print(h)
print(f"{len(hits)} upstream brand mentions reachable by users")
sys.exit(1 if hits else 0)
