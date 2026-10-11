#!/usr/bin/env python3
"""Fails when a Compose `Res.string.<name>` use lacks its accessor import or its strings.xml entry.

Compose modules cannot compile on machines without Google Maven (ADR 0007); this catches the most common
mistake there before CI does.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
problems = []
for resources in [p for p in ROOT.glob("**/src/commonMain/composeResources") if "build" not in p.parts]:
    source_root = resources.parent.parent
    strings_files = list(resources.glob("values*/strings.xml"))
    declared = {m for f in strings_files for m in re.findall(r'<string name="([^"]+)"', f.read_text())}
    for strings in strings_files:
        if "\\'" in strings.read_text():
            problems.append(f"{strings.relative_to(ROOT)}: Compose resources show \\' literally; use ’ instead")
    for source in source_root.glob("**/*.kt"):
        text = source.read_text()
        used = set(re.findall(r"\bRes\.string\.([A-Za-z0-9_]+)", text))
        imported = set(re.findall(r"^import [\w.]+\.resources\.([A-Za-z0-9_]+)$", text, re.MULTILINE))
        for name in sorted(used - imported):
            problems.append(f"{source.relative_to(ROOT)}: Res.string.{name} is not imported")
        for name in sorted(used - declared):
            problems.append(f"{source.relative_to(ROOT)}: string '{name}' is not in strings.xml")

if problems:
    print("\n".join(problems))
    sys.exit(1)
print("Compose resource references are consistent.")
