import glob
import re

patterns = [
    re.compile(r"https?://[^\s\"\'<>]+"),
    re.compile(r"/service/[^\s\"\'<>]+"),
    re.compile(r"/api/[^\s\"\'<>]+")
]

found = set()
for dex in glob.glob("/tmp/apk_inspect/*.dex"):
    with open(dex, "rb") as f:
        data = f.read().decode("latin1", errors="ignore")
    for p in patterns:
        for m in p.findall(data):
            if any(k in m.lower() for k in ["ruijie", "reyee", "voucher", "token", "login", "auth"]):
                found.add(m)

for item in sorted(found):
    print(item)
