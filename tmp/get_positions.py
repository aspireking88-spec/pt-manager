with open("./tmp/login_flow_detailed.txt") as f:
    text = f.read()

lines = text.splitlines()
for i, l in enumerate(lines):
    if "positions" in l:
        print("Positions section starts at line", i)
        for j in range(i, min(i+50, len(lines))):
            print(lines[j])
        break
