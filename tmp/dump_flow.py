with open("./tmp/classes10_dump.txt") as f:
    text = f.read()

target = "Lcom/example/data/repository/RuijieRepository$loginWithCredentials$2;"
start = text.find(target)
if start != -1:
    end = text.find("Class descriptor", start + len(target))
    chunk = text[start:end if end != -1 else len(text)]
    lines = chunk.splitlines()
    for i, l in enumerate(lines):
        if "invoke" in l or "const-string" in l or "sget" in l or "iget" in l or "iput" in l:
            print(f"{i:04d}: {l}")
