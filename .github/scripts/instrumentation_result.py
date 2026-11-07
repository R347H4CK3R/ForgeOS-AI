"""Require a complete successful AndroidJUnitRunner result, allowing suite growth."""
import argparse
from pathlib import Path
import re


def validate_result(text, minimum):
    if minimum < 1:
        raise ValueError('minimum test count must be positive')
    if re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_STATUS_CODE: -[1-9]', text):
        raise ValueError('instrumentation reported failure or skipped tests')
    results = re.findall(r'^OK \(([1-9][0-9]*) tests\)\s*$', text, re.M)
    if len(results) != 1 or int(results[0]) < minimum:
        raise ValueError('missing or insufficient successful test summary')
    count = int(results[0])
    discovered = {int(n) for n in re.findall(r'^INSTRUMENTATION_STATUS: numtests=([0-9]+)\s*$', text, re.M)}
    if discovered != {count} or not re.search(r'^INSTRUMENTATION_CODE: -1\s*$', text, re.M):
        raise ValueError('incomplete or inconsistent runner result')
    return count


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--minimum', type=int, required=True)
    args = parser.parse_args()
    print('Verified instrumented tests:', validate_result(args.report.read_text(), args.minimum))
