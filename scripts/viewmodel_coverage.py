#!/usr/bin/env python3
"""Report (and gate) ViewModel line coverage from the JaCoCo XML that Gradle writes.

The course asks for ViewModel coverage, not whole-application coverage, so this counts only
classes whose simple name ends with "ViewModel" (nested lambdas excluded) and ignores the
Composable/…Impl code that unit tests never reach.

Usage, from the repository root:

    ./gradlew :app:createDebugUnitTestCoverageReport
    python3 scripts/viewmodel_coverage.py                    # print the percentage
    python3 scripts/viewmodel_coverage.py --write            # refresh docs/testing/coverage.json
    python3 scripts/viewmodel_coverage.py --check --min 70   # what CI runs

--check fails when the measured number differs from the badge JSON (a stale badge is a lie) or when
it is below --min.
"""

from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_REPORT = Path("app/build/reports/coverage/test/debug/report.xml")
DEFAULT_BADGE = Path("docs/testing/coverage.json")
TOLERANCE = 0.1


def view_model_lines(report: Path) -> tuple[int, int]:
    if not report.exists():
        raise SystemExit(f"{report} not found. Run :app:createDebugUnitTestCoverageReport first.")
    root = ET.parse(report).getroot()
    covered = missed = 0
    for cls in root.iter("class"):
        name = cls.get("name", "").split("/")[-1]
        if not name.endswith("ViewModel"):
            continue
        for counter in cls.findall("counter"):
            if counter.get("type") == "LINE":
                covered += int(counter.get("covered"))
                missed += int(counter.get("missed"))
    return covered, missed


def colour(percentage: float) -> str:
    if percentage >= 70:
        return "brightgreen"
    if percentage >= 50:
        return "yellowgreen"
    if percentage >= 40:
        return "yellow"
    return "red"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--badge", type=Path, default=DEFAULT_BADGE)
    parser.add_argument("--write", action="store_true", help="rewrite the badge JSON from the report")
    parser.add_argument("--check", action="store_true", help="fail when the badge is stale or below --min")
    parser.add_argument("--min", type=float, default=70.0)
    args = parser.parse_args()

    covered, missed = view_model_lines(args.report)
    total = covered + missed
    if total == 0:
        raise SystemExit("No ViewModel classes found in the report; is the class filter right?")
    percentage = round(100 * covered / total, 1)
    message = f"{percentage:.1f}%"

    if args.write:
        args.badge.parent.mkdir(parents=True, exist_ok=True)
        args.badge.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "label": "ViewModel coverage",
                    "message": message,
                    "color": colour(percentage),
                },
                indent=2,
            )
            + "\n",
        )
        print(f"wrote {args.badge} with {message}")

    print(f"ViewModel line coverage: {message} ({covered}/{total})")

    if not args.check:
        return 0

    badge_message = None
    if args.badge.exists():
        badge_message = json.loads(args.badge.read_text()).get("message")
    if badge_message is None:
        print(f"{args.badge} is missing; run with --write and commit the result.", file=sys.stderr)
        return 1
    try:
        badge_value = float(badge_message.rstrip("%"))
    except ValueError:
        print(f"{args.badge} has an unreadable message {badge_message!r}.", file=sys.stderr)
        return 1
    if abs(badge_value - percentage) > TOLERANCE:
        print(
            f"badge says {badge_message} but the report says {message}; "
            "run the script with --write and commit the refreshed file.",
            file=sys.stderr,
        )
        return 1
    if percentage < args.min:
        print(f"coverage {message} is below the required {args.min:.0f}%.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
