with open("./tmp/disasm_output.txt") as f:
    lines = f.readlines()

print(f"Total lines: {len(lines)}")
for idx, l in enumerate(lines):
    if any(k in l for k in ["const-string", "invoke", "iput", "iget", "if-", "goto", "return"]):
        print(f"{idx:04d}: {l.strip()}")
