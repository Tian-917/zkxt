# -*- coding: utf-8 -*-
"""在远端 docker 上跑一次性命令（带挂载）。用法: python tools/vm_run.py <image> <src:dest[,src2:dest2]> <shell命令>"""
import sys

sys.path.insert(0, "tools")
from vm_docker import run2  # noqa: E402


def main():
    image = sys.argv[1]
    mounts = {}
    spec = sys.argv[2]
    if spec and spec != "-":
        for pair in spec.split(","):
            src, dst = pair.split(":")
            mounts[src] = dst
    cmd = sys.argv[3]
    out = run2(image, ["sh", "-c", cmd], mounts)
    sys.stdout.write(out)


if __name__ == "__main__":
    main()
