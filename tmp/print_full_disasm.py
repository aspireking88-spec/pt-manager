with open("./tmp/login_flow_detailed.txt") as f:
    text = f.read()

for line in text.splitlines():
    if "|" in line:
        code_part = line.split("|")[-1]
        print(code_part)
