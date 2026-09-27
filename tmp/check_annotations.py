with open("./tmp/classes8_dump.txt") as f:
    text = f.read()

for m in ["authPolicy", "authGlobal", "getAccountInfo"]:
    start = text.find(f"name          : '{m}'")
    if start != -1:
        print(text[start-100:start+300])
