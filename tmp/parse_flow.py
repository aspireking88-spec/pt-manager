with open("./tmp/login_flow_detailed.txt") as f:
    lines = f.readlines()

for l in lines:
    if any(k in l for k in ["const-string", "invoke-virtual", "invoke-direct", "invoke-static", "invoke-interface", "iget", "sget"]):
        print(l.strip())
