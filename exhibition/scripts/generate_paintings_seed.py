#!/usr/bin/env python3
"""Генерирует Flyway-миграцию V2__seed_paintings.sql из CSV (uuid, author, description, url, title, style)."""
from __future__ import annotations

import argparse
import csv
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CSV = ROOT / "src/main/resources/db/seed/my_paintings_with_uuid.csv"
OUT_SQL = ROOT / "src/main/resources/db/migration/V2__seed_paintings.sql"


def esc(value: str | None) -> str:
    if value is None:
        return "NULL"
    text = str(value).strip()
    if not text:
        return "NULL"
    text = text.replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def trunc(value: str | None, max_len: int) -> str | None:
    if value is None:
        return None
    text = str(value)
    return text if len(text) <= max_len else text[:max_len]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "csv",
        nargs="?",
        default=str(DEFAULT_CSV),
        help="Путь к CSV (колонки: uuid, author, description, url, title, style)",
    )
    args = parser.parse_args()
    csv_path = Path(args.csv).resolve()
    if not csv_path.is_file():
        raise SystemExit(f"CSV не найден: {csv_path}")

    fieldnames = ["uuid", "author", "description", "url", "title", "style"]
    rows: list[dict[str, str]] = []
    with csv_path.open(newline="", encoding="utf-8") as handle:
        peek = handle.read(4096)
        handle.seek(0)
        first_line = peek.splitlines()[0] if peek else ""
        has_header = first_line.strip().lower().startswith("uuid,")
        if has_header:
            reader = csv.DictReader(handle)
            if not set(fieldnames).issubset(reader.fieldnames or []):
                raise SystemExit(f"Ожидаются колонки {fieldnames}, получено: {reader.fieldnames}")
            for row in reader:
                if row.get("uuid", "").strip():
                    rows.append(row)
        else:
            reader = csv.DictReader(handle, fieldnames=fieldnames)
            for row in reader:
                if row.get("uuid", "").strip():
                    rows.append(row)

    DEFAULT_CSV.parent.mkdir(parents=True, exist_ok=True)
    if csv_path != DEFAULT_CSV.resolve():
        shutil.copy(csv_path, DEFAULT_CSV)

    lines = [
        "-- Каталог картин из my_paintings_with_uuid.csv",
        "SET NAMES utf8mb4;",
        "",
    ]
    for row in rows:
        painting_id = row["uuid"].strip().lower()
        title = trunc(row.get("title"), 255)
        author = trunc(row.get("author"), 255)
        style = trunc(row.get("style"), 255)
        description = trunc(row.get("description"), 1000)
        image_url = trunc(row.get("url"), 255)
        lines.append(
            "INSERT INTO paintings (id, title, author, style, description, image_url) VALUES ("
            f"{esc(painting_id)}, {esc(title)}, {esc(author)}, {esc(style)}, "
            f"{esc(description)}, {esc(image_url)});"
        )
    lines.append("")
    lines.append(f"-- Imported {len(rows)} paintings")

    OUT_SQL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"OK: {len(rows)} rows -> {OUT_SQL}")
    print(f"CSV copy -> {DEFAULT_CSV}")


if __name__ == "__main__":
    main()
