"""Read-only DSU allocation planner. Never installs, enables, unlocks or boots."""
import argparse
import json

GIB = 1 << 30
MIN_USERDATA = 2 * GIB
RESERVE = GIB
ALLOCATION_ALLOWANCE = 128 << 20


def allocation_plan(available, system, transport, userdata=MIN_USERDATA):
    for name, value in [('available', available), ('system', system), ('transport', transport), ('userdata', userdata)]:
        if type(value) is not int or value < 0 or value > (1 << 63) - 1:
            raise ValueError(name + ' must be a nonnegative signed-64-bit byte count')
    if system == 0 or transport == 0:
        raise ValueError('system and transport must be positive')
    if userdata < MIN_USERDATA:
        raise ValueError('userdata must be at least 2 GiB; do not shrink it to force a fit')
    allocated = system + userdata + ALLOCATION_ALLOWANCE
    streaming_required = allocated + RESERVE
    return {
        'schema': 1,
        'available_bytes': available,
        'system_bytes': system,
        'transport_bytes': transport,
        'userdata_bytes': userdata,
        'reserve_bytes': RESERVE,
        'allocation_allowance_bytes': ALLOCATION_ALLOWANCE,
        'streaming': {
            'required_bytes': streaming_required,
            'free_after_allocation_bytes': available - allocated,
            'fits': available >= streaming_required,
        },
        'staged': {
            'required_bytes': streaming_required + transport,
            'free_after_allocation_bytes': available - allocated - transport,
            'fits': available >= streaming_required + transport,
        },
        'boot_ready': False,
        'limitations': [
            'Capacity estimate only; vendor allocation and guest first boot are untested.',
            'Streaming requires an actually reachable source; no stream server is provisioned by this tool.',
            'OEM key trust, unlock policy, VINTF compatibility and recovery remain separate gates.',
            'A 2 GiB guest is for a small evaluation, not a complete development workspace.',
            'Adopted SD storage is neither counted nor reformatted.',
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['available', 'system', 'transport']:
        parser.add_argument('--' + name + '-bytes', type=int, required=True)
    parser.add_argument('--userdata-bytes', type=int, default=MIN_USERDATA)
    args = parser.parse_args()
    try:
        result = allocation_plan(args.available_bytes, args.system_bytes, args.transport_bytes, args.userdata_bytes)
    except ValueError as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
