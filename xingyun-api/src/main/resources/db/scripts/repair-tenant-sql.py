#!/usr/bin/env python3
"""Repair corrupted tenant SQL files (missing quote closures + optional mojibake fix)."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


def count_broken_patterns(text: str) -> dict[str, int]:
    return {
        "comment_field": len(re.findall(r"COMMENT '[^'\n]*\?,", text)),
        "comment_table": len(re.findall(r"COMMENT = '[^']*\? ROW_FORMAT", text)),
        "insert_string": len(re.findall(r"'[^']*\?,", text)),
        "insert_trailing": len(re.findall(r"'[^']*\?\);", text)),
    }


def fix_syntax(text: str) -> tuple[str, dict[str, int]]:
    before = count_broken_patterns(text)

    # Table-level COMMENT missing closing quote before ROW_FORMAT
    text = re.sub(
        r"COMMENT = '([^']*)\? ROW_FORMAT",
        r"COMMENT = '\1' ROW_FORMAT",
        text,
    )

    # Trailing INSERT string missing closing quote before );
    text = re.sub(
        r"'([^']*)\?\);",
        r"'\1');",
        text,
    )

    # Field COMMENT / INSERT strings: loop until stable (multiple per line)
    pattern = re.compile(r"'([^']*)\?,")
    while True:
        new_text, count = pattern.subn(r"'\1',", text)
        text = new_text
        if count == 0:
            break

    # Explicit field COMMENT at end of line
    text = re.sub(
        r"COMMENT '([^'\n]*)\?,(\s*)$",
        r"COMMENT '\1',\2",
        text,
        flags=re.MULTILINE,
    )

    after = count_broken_patterns(text)
    return text, {"before": before, "after": after}


def fix_sys_parameter_description_width(text: str) -> str:
    """Expand sys_parameter.description to fit long OSS config descriptions."""
    return re.sub(
        r"(CREATE TABLE `sys_parameter`\s*\([^;]*?`description`) varchar\(200\)",
        r"\1 varchar(500)",
        text,
        count=1,
        flags=re.DOTALL,
    )


# Typical UTF-8-as-GBK mojibake markers (Latin-1 supplement, CJK compatibility noise).
MOJIBAKE_MARKERS = re.compile(
    r"[\u0080-\u024f]|锟|拷|斤|涓|璇|缁|鍙|鏂|鐨|灏|姹|"
)
# Artifact from mis-applying mojibake recovery to valid UTF-8 Chinese (e.g. 结算 -> 算算算).
REPEATED_CJK = re.compile(r"([\u4e00-\u9fff])\1{2,}")


def looks_like_mojibake(line: str) -> bool:
    """Return True only when a line likely contains mis-decoded UTF-8 text."""
    if not line.strip():
        return False
    # Valid UTF-8 Chinese SQL without mojibake markers must not be transformed.
    if re.search(r"[\u4e00-\u9fff]", line) and not MOJIBAKE_MARKERS.search(line):
        return False
    if MOJIBAKE_MARKERS.search(line):
        return True
    # Broken string placeholders from syntax corruption often accompany encoding issues.
    if "?" in line and re.search(r"'[^']*\?", line):
        return True
    return False


def fix_mojibake_line(line: str) -> str:
    """Recover UTF-8 text that was misinterpreted as GBK (classic mojibake)."""
    if not looks_like_mojibake(line):
        return line
    try:
        recovered = line.encode("gbk").decode("utf-8")
    except UnicodeError:
        return line
    if REPEATED_CJK.search(recovered):
        return line
    return recovered


def fix_mojibake(text: str) -> str:
    """Apply mojibake recovery line-by-line; skip lines that already look like valid UTF-8."""
    return "".join(fix_mojibake_line(line) for line in text.splitlines(keepends=True))


def repair_file(path: Path, fix_encoding: bool) -> None:
    original = path.read_text(encoding="utf-8")
    repaired, stats = fix_syntax(original)
    repaired = fix_sys_parameter_description_width(repaired)

    if fix_encoding:
        repaired = fix_mojibake(repaired)

    path.write_text(repaired, encoding="utf-8", newline="\n")

    print(f"Repaired: {path}")
    print(f"  Syntax before: {stats['before']}")
    print(f"  Syntax after:  {stats['after']}")
    print(f"  Encoding fix:  {fix_encoding}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Repair corrupted tenant SQL files.")
    parser.add_argument(
        "files",
        nargs="+",
        type=Path,
        help="SQL files to repair in place",
    )
    parser.add_argument(
        "--fix-encoding",
        action="store_true",
        help="Apply GBK->UTF-8 mojibake recovery only to lines that look mis-encoded",
    )
    args = parser.parse_args()

    for file_path in args.files:
        if not file_path.is_file():
            print(f"ERROR: file not found: {file_path}", file=sys.stderr)
            return 1
        repair_file(file_path, args.fix_encoding)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
