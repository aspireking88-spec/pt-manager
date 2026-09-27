with open("./tmp/classes10_dump.txt") as f:
    text = f.read()

target = "Lcom/example/data/repository/RuijieRepository$loginWithCredentials$2;"
start = text.find(target)
if start != -1:
    end = text.find("Class descriptor", start + len(target))
    chunk = text[start:end if end != -1 else len(text)]
    print("Found chunk of size:", len(chunk))
    for line in chunk.splitlines():
        if "const-string" in line:
            print(line.strip())
else:
    print("Not found")
