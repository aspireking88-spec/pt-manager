with open("./tmp/classes10_dump.txt") as f:
    text = f.read()

target = "Class descriptor  : 'Lcom/example/data/api/RuijieApiService;'"
start = text.find(target)
if start != -1:
    end = text.find("Class descriptor", start + len(target))
    chunk = text[start:end if end != -1 else len(text)]
    print(chunk)
else:
    print("RuijieApiService interface not found directly")
