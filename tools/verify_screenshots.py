#!/usr/bin/env python3
"""Assert the screenshots referenced by the README exist and are not placeholders."""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
README = REPO / "README.md"
MIN_BYTES = 20 * 1024
EXPECTED = [
    "docs/img/dashboard-overview.png",
    "docs/img/route-hazard-detail.png",
    "docs/img/test-and-benchmark-output.png",
]


def main() -> int:
    text = README.read_text()
    linked = re.findall(r"!\[[^\]]*\]\(([^)]+)\)", text)
    failures = []

    for relative in EXPECTED:
        path = REPO / relative
        if not path.is_file():
            failures.append(f"missing file: {relative}")
            continue
        size = path.stat().st_size
        if size <= MIN_BYTES:
            failures.append(f"{relative} is {size} bytes, expected more than {MIN_BYTES}")
        if relative not in linked:
            failures.append(f"{relative} is not linked from README.md")

    extra = [item for item in linked if item not in EXPECTED]
    if extra:
        failures.append(f"README links images outside docs/img: {extra}")

    if failures:
        for failure in failures:
            print(f"FAIL {failure}")
        return 1
    for relative in EXPECTED:
        print(f"OK   {relative} ({(REPO / relative).stat().st_size // 1024} KB, linked from README)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
