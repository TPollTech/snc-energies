"""Verifies the Mercadão structure NBT parses cleanly (root compound with
DataVersion/size/palette/blocks/entities, like vanilla structure files)."""
import gzip
import os
import struct

PATH = os.environ.get("NBT_PATH", "src/main/resources/data/snc_energies/structure/mercadao.nbt")

data = gzip.decompress(open(PATH, "rb").read())
pos = 0
DEPTH = [0]


def fail(msg):
    raise SystemExit(f"FAIL at pos={pos}: {msg}")


def rtag():
    global pos
    if pos + 1 > len(data):
        fail("tag truncated")
    t = data[pos]
    pos += 1
    if t == 0:
        return 0, ""  # TAG_End has no name
    if pos + 2 > len(data):
        fail("name length truncated")
    n = struct.unpack(">H", data[pos:pos + 2])[0]
    pos += 2
    if pos + n > len(data):
        fail("name truncated")
    nm = data[pos:pos + n].decode()
    pos += n
    return t, nm


def rstring():
    global pos
    if pos + 2 > len(data):
        fail("string len truncated")
    n = struct.unpack(">H", data[pos:pos + 2])[0]
    pos += 2
    if pos + n > len(data):
        fail("string truncated")
    v = data[pos:pos + n].decode()
    pos += n
    return v


def rpayload(tid):
    global pos
    start = pos
    if tid == 1:
        v = data[pos]
        pos += 1
        return v
    if tid == 3:
        v = struct.unpack(">i", data[pos:pos + 4])[0]
        pos += 4
        return v
    if tid == 7:
        n = struct.unpack(">i", data[pos:pos + 4])[0]
        pos += 4 + n
        return f"<{n} bytes>"
    if tid == 8:
        return rstring()
    if tid == 9:
        et = data[pos]
        n = struct.unpack(">i", data[pos + 1:pos + 5])[0]
        pos += 5
        return [rpayload(et) for _ in range(n)]
    if tid == 10:
        d = {}
        DEPTH[0] += 1
        while True:
            t, name = rtag()
            if t == 0:
                DEPTH[0] -= 1
                return d
            d[name] = rpayload(t)
    fail(f"unsupported tag {tid}")


t, nm = rtag()
if t != 10 or nm != "":
    fail("root is not an unnamed compound")
c = rpayload(10)
if pos != len(data):
    fail(f"length mismatch: parsed {pos}, file {len(data)}")
if "DataVersion" not in c:
    fail("missing DataVersion")
print("VALID NBT | DataVersion:", c["DataVersion"], "| size:", c["size"],
      "| palette:", len(c["palette"]), "| blocks:", len(c["blocks"]), "bytes")
names = [p["Name"] for p in c["palette"]]
print("mercadao blocks:", [n for n in names if "mercadao" in n])
shelf = [p for p in c["palette"] if p["Name"] == "snc_energies:mercadao_shelf"][0]
print("shelf props:", shelf.get("Properties"))
print("STRUCTURE NBT OK")
