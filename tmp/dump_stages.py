with open("./tmp/login_flow_detailed.txt") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    if any(k in line for k in ["const-string", "invoke-", "new-instance", "iget-object", "iput-object", "if-", "goto", "return"]):
        if any(s in line for s in ["policy", "global", "account/info", "token", "password", "username", "tenant", "Bearer"]):
            print(f"L{idx:04d}: {line.strip()}")
