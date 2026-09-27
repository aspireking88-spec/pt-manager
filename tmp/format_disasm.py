with open("./tmp/disasm_output.txt") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    line = line.strip()
    if any(k in line for k in ["const-string", "invoke-", "new-instance", "iget-object", "iput-object", "if-", "goto", "return"]):
        print(f"/* {idx:04d} */ {line}")
