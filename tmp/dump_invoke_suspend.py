with open("./tmp/login_flow_detailed.txt") as f:
    text = f.read()

start = text.find("invokeSuspend")
if start != -1:
    chunk = text[start:]
    lines = chunk.splitlines()
    for i in range(0, min(len(lines), 400)):
        print(lines[i])
