with open("./tmp/login_flow_detailed.txt") as f:
    text = f.read()

start = text.find("00eacc:") # where line 01d6 is
if start != -1:
    chunk = text[start:]
    with open("./tmp/flow_clean.txt", "w") as out:
        out.write(chunk)
    print("Clean flow written to ./tmp/flow_clean.txt")
