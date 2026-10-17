#!/usr/bin/env python3
"""Verify the packaged backend exits promptly when its monitored parent disappears."""
import argparse
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('resources', type=Path, help='Directory containing runtime/ and workos-server.jar')
args = parser.parse_args()
resources = args.resources.resolve()
with tempfile.TemporaryDirectory(prefix='workos-parent-exit-') as directory:
    parent = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(120)'])
    backend = None
    try:
        env = os.environ.copy()
        env.update(WORKOS_DATA_DIR=directory, WORKOS_PARENT_PID=str(parent.pid),
                   WORKOS_DESKTOP_TOKEN='isolated-parent-exit-regression-token')
        with open(Path(directory) / 'desktop.log', 'w') as log:
            backend = subprocess.Popen([str(resources / 'runtime/bin/java'), '-jar',
                str(resources / 'workos-server.jar'), '--spring.profiles.active=desktop',
                '--server.port=0', '--server.address=127.0.0.1'], env=env, stdout=log, stderr=log)
            deadline = time.monotonic() + 45
            while not (Path(directory) / 'ready.json').exists():
                if backend.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError('Backend failed to become ready')
                time.sleep(0.2)
            parent.kill()
            parent.wait()
            started = time.monotonic()
            try:
                code = backend.wait(timeout=12)
            except subprocess.TimeoutExpired:
                raise RuntimeError('Backend still running 12 seconds after parent exit') from None
            assert code == 0, f'Unexpected exit code: {code}'
            with sqlite3.connect(Path(directory) / 'workos.db') as db:
                assert db.execute('PRAGMA quick_check').fetchone()[0] == 'ok'
            print(f'PASS: backend exited cleanly in {time.monotonic() - started:.1f}s; database intact')
    except Exception:
        print((Path(directory) / 'desktop.log').read_text(), file=sys.stderr)
        raise
    finally:
        for process in (backend, parent):
            if process is not None and process.poll() is None:
                process.kill()
                process.wait()
