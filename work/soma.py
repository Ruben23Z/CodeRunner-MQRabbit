import sys

total = 0
for linha in sys.stdin:
    total = total + float(linha)
print(total)