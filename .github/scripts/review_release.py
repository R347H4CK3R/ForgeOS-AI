"""Reject reuse of any review container with a different source or publication state."""
import argparse
import json
from pathlib import Path


def validate_release(data, tag, source):
    if (data.get('tagName') != tag or data.get('targetCommitish') != source
            or data.get('isDraft') is not True or data.get('isPrerelease') is not True):
        raise ValueError('review container identity or publication state mismatch')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--tag', required=True)
    parser.add_argument('--source', required=True)
    args = parser.parse_args()
    validate_release(json.loads(args.metadata.read_text()), args.tag, args.source)
