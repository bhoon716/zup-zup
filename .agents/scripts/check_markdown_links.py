#!/usr/bin/env python3
"""Check local Markdown links across the repository."""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


SKIP_DIR_NAMES = {
    ".git",
    "node_modules",
    ".next",
    ".turbo",
    "dist",
    "build",
    "coverage",
    "out",
    ".vercel",
}

EXTERNAL_SCHEMES = ("http://", "https://", "mailto:", "tel:", "javascript:")

INLINE_LINK_RE = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
REFERENCE_LINK_RE = re.compile(r"!?\[(?P<text>[^\]]*)\]\[(?P<ref>[^\]]*)\]")
REFERENCE_DEF_RE = re.compile(r"^\s*\[([^\]]+)\]:\s*(\S.*)$")
FENCED_BLOCK_RE = re.compile(r"^\s*```")
HEADING_RE = re.compile(r"^\s{0,3}(#{1,6})\s+(.*?)\s*$")


@dataclass(frozen=True)
class BrokenLink:
    source: Path
    line: int
    target: str
    reason: str


def scan_markdown_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*.md"):
        if any(part in SKIP_DIR_NAMES for part in path.parts):
            continue
        if path.is_file():
            files.append(path)
    return sorted(files)


def _iter_non_fenced_lines(lines: list[str]):
    in_fence = False
    for line_number, line in enumerate(lines, start=1):
        if FENCED_BLOCK_RE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        yield line_number, line


def _parse_reference_definitions(lines: list[str]) -> dict[str, str]:
    definitions: dict[str, str] = {}
    for _, line in _iter_non_fenced_lines(lines):
        match = REFERENCE_DEF_RE.match(line)
        if match:
            definitions[match.group(1).strip().lower()] = match.group(2).strip()
    return definitions


def _clean_destination(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        target = target[1 : target.index(">")].strip()
    if " " in target:
        target = target.split(" ", 1)[0].strip()
    return target


def _normalize_target(raw_target: str, definitions: dict[str, str]) -> str | None:
    target = _clean_destination(raw_target)
    if not target:
        return None

    if target.startswith("#"):
        return target

    reference = definitions.get(target.lower())
    if reference is not None:
        target = _clean_destination(reference)

    if target.startswith(EXTERNAL_SCHEMES):
        return None

    return target


def _slugify_heading(text: str) -> str:
    cleaned = re.sub(r"[`*_~<>\[\]{}()#]", "", text)
    cleaned = re.sub(r"[^\w\s-]", "", cleaned, flags=re.UNICODE)
    cleaned = cleaned.strip().lower()
    cleaned = re.sub(r"[\s_-]+", "-", cleaned)
    return cleaned.strip("-")


def _extract_headings(markdown_path: Path) -> set[str]:
    headings: set[str] = set()
    lines = markdown_path.read_text(encoding="utf-8").splitlines()
    for _, line in _iter_non_fenced_lines(lines):
        match = HEADING_RE.match(line)
        if match:
            headings.add(_slugify_heading(match.group(2)))
    return headings


def _split_fragment(target: str) -> tuple[str, str | None]:
    if "#" not in target:
        return target, None
    path_part, fragment = target.split("#", 1)
    return path_part, fragment or None


def _resolve_target_path(source: Path, root: Path, raw_target: str) -> tuple[Path | None, str | None]:
    if raw_target.startswith("#"):
        return source, raw_target[1:] or None

    path_part, fragment = _split_fragment(raw_target)
    if not path_part:
        return None, None

    candidate = Path(path_part)
    if candidate.is_absolute():
        if candidate.exists():
            return candidate, fragment
        repo_relative = (root / path_part.lstrip("/")).resolve()
        return repo_relative, fragment

    resolved = (source.parent / candidate).resolve()
    if resolved.exists():
        return resolved, fragment

    repo_relative = (root / candidate).resolve()
    return repo_relative, fragment


def _extract_targets(lines: list[str], definitions: dict[str, str]) -> list[tuple[int, str]]:
    targets: list[tuple[int, str]] = []
    for line_number, line in _iter_non_fenced_lines(lines):
        for match in INLINE_LINK_RE.finditer(line):
            target = _normalize_target(match.group(1), definitions)
            if target is not None:
                targets.append((line_number, target))

        for match in REFERENCE_LINK_RE.finditer(line):
            ref = match.group("ref").strip()
            if not ref:
                ref = match.group("text").strip()
            target = definitions.get(ref.lower())
            if target is None:
                continue
            normalized = _normalize_target(target, definitions)
            if normalized is not None:
                targets.append((line_number, normalized))

    return targets


def find_broken_links_in_markdown(markdown_path: Path, root: Path) -> list[BrokenLink]:
    lines = markdown_path.read_text(encoding="utf-8").splitlines()
    definitions = _parse_reference_definitions(lines)
    broken: list[BrokenLink] = []
    headings_cache: dict[Path, set[str]] = {}

    for line_number, target in _extract_targets(lines, definitions):
        resolved_path, fragment = _resolve_target_path(markdown_path, root, target)
        if resolved_path is None:
            continue

        if not resolved_path.exists():
            broken.append(
                BrokenLink(
                    source=markdown_path,
                    line=line_number,
                    target=target,
                    reason="missing file",
                )
            )
            continue

        if fragment:
            headings = headings_cache.get(resolved_path)
            if headings is None:
                headings = _extract_headings(resolved_path)
                headings_cache[resolved_path] = headings
            if _slugify_heading(fragment) not in headings:
                broken.append(
                    BrokenLink(
                        source=markdown_path,
                        line=line_number,
                        target=target,
                        reason="missing heading",
                    )
                )

    return broken


def check_repository(root: Path) -> list[BrokenLink]:
    broken: list[BrokenLink] = []
    for markdown_path in scan_markdown_files(root):
        broken.extend(find_broken_links_in_markdown(markdown_path, root))
    return broken


def _format_broken_link(item: BrokenLink, root: Path) -> str:
    return f"{item.source.relative_to(root)}:{item.line}: {item.target} ({item.reason})"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Check local Markdown links across the repository."
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="Repository root to scan.",
    )
    args = parser.parse_args(argv)

    root = args.root.resolve()
    broken = check_repository(root)
    if broken:
        for item in broken:
            print(_format_broken_link(item, root))
        print(f"Broken links: {len(broken)}", file=sys.stderr)
        return 1

    print(f"OK: checked {len(scan_markdown_files(root))} markdown files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
