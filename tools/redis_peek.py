# -*- coding: utf-8 -*-
"""连 Redis 看学习记录缓存 / 延迟队列，确认后端真的在用 Redis。"""
import socket


def resp(*args):
    out = b"*%d\r\n" % len(args)
    for a in args:
        b = str(a).encode("utf-8")
        out += b"$%d\r\n%s\r\n" % (len(b), b)
    return out


def cmd(sock, *args):
    sock.sendall(resp(*args))
    return sock.recv(65536)


s = socket.create_connection(("192.168.150.101", 6379), timeout=5)
print("AUTH:", cmd(s, "AUTH", "123321"))
print("PING:", cmd(s, "PING"))
print("DBSIZE:", cmd(s, "DBSIZE").decode("utf-8", "ignore").strip())
print("CONFIG get databases:", cmd(s, "CONFIG", "GET", "databases").decode("utf-8", "ignore").strip())

print("\n== 全部 key（前 30） ==")
raw = cmd(s, "KEYS", "*").decode("utf-8", "ignore")
keys = [x for x in raw.split("\r\n") if x and not x.startswith(("*", "$", "+", "-", ":"))]
print("\n".join(keys[:30]) or "(空)")
print("key 总数:", len(keys))

print("\n== 学习记录相关 key ==")
for k in cmd(s, "KEYS", "*learning:record*").decode("utf-8", "ignore").split("\r\n"):
    if k and not k.startswith("*") and not k.startswith("$"):
        print(" ", k)

key = "learning:record:1900000000001000303"
print("\n== 某课表的记录缓存 ==")
print("TYPE:", cmd(s, "TYPE", key).decode("utf-8", "ignore").strip())
print("TTL :", cmd(s, "TTL", key).decode("utf-8", "ignore").strip())
print("HLEN:", cmd(s, "HLEN", key).decode("utf-8", "ignore").strip())
print("HGETALL:", cmd(s, "HGETALL", key).decode("utf-8", "ignore")[:300])

print("\n== 延迟队列（Redisson 用 zset 存到期时间） ==")
for k in cmd(s, "KEYS", "*learning:record:delay*").decode("utf-8", "ignore").split("\r\n"):
    if k and not k.startswith("*") and not k.startswith("$"):
        t = cmd(s, "TYPE", k).decode("utf-8", "ignore").strip()
        size = cmd(s, "ZCARD" if t.endswith("zset") else "LLEN", k).decode("utf-8", "ignore").strip()
        print("  %-55s type=%-8s size=%s" % (k, t, size))
s.close()
