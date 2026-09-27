with open("./tmp/flow_clean.txt") as f:
    lines = f.readlines()

for line in lines:
    line = line.strip()
    if any(k in line for k in ["const-string", "invoke-", "new-instance", "if-", "return-object"]):
        print(line)
