#!/usr/bin/env python3
"""生成桌面资源：Vue 静态资源、Java 应用、自带精简运行时。"""
import os
import json
import re
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
def run(args, cwd=ROOT, env=None):
    subprocess.run([str(x) for x in args], cwd=cwd, env=env, check=True)

java = Path(os.environ.get('WORKOS_JAVA_HOME') or subprocess.check_output(['/usr/libexec/java_home', '-v', '17'], text=True).strip()).resolve()
if not subprocess.check_output([java / 'bin/javac', '-version'], text=True).startswith('javac 17.'):
    raise SystemExit('桌面构建需要 JDK 17，可用 WORKOS_JAVA_HOME 指定。')
env = {**os.environ, 'JAVA_HOME': str(java), 'MAVEN_SKIP_RC': 'true', 'VITE_WORKOS_DESKTOP': 'true', 'VITE_WORKOS_DEMO': 'false'}
manifest = ROOT / 'workos-desktop/src-tauri/Cargo.toml'
host = next(line.split(': ', 1)[1] for line in subprocess.check_output(['rustc', '-vV'], text=True).splitlines() if line.startswith('host: '))
run(['cargo', 'fetch', '--manifest-path', manifest, '--target', host], env=env)
web = ROOT / 'workos-web'
if not (web / 'node_modules').exists(): run(['npm', 'ci'], web)
run(['npm', 'run', 'build', '--', '--outDir', 'dist-desktop'], web, env)
run(['mvn', '-B', '-Pdesktop', 'package', '-DskipTests'], ROOT / 'workos-server', env)
resources = ROOT / 'workos-desktop/src-tauri/resources'
resources.mkdir(parents=True, exist_ok=True)
shutil.copy2(ROOT / 'workos-server/target/workos-server-0.1.0-SNAPSHOT.jar', resources / 'workos-server.jar')
runtime = resources / 'runtime'
if runtime.exists(): shutil.rmtree(runtime)
run([java / 'bin/jlink', '--add-modules', 'java.se,jdk.unsupported,jdk.crypto.ec,jdk.management,jdk.zipfs', '--strip-debug', '--no-header-files', '--no-man-pages', '--output', runtime])
# jlink 产生的只读许可文件会让 Tauri 下一次增量复制失败；只调整构建副本的权限。
for file in runtime.rglob('*'):
    if file.is_file(): file.chmod(file.stat().st_mode | 0o200)
run([runtime / 'bin/java', '-version'])

# 保留编译进原生壳和前端的依赖许可；Java 依赖许可在 jar 内，JRE 许可在 runtime/legal。
notices = ['WorkOS third-party notices\nJava library notices are retained inside workos-server.jar.\nJava runtime licenses are in runtime/legal.\n']
def collect(directory, label):
    license_files = sorted(p for p in directory.iterdir() if p.is_file() and p.name.lower().startswith(('license', 'licence', 'notice', 'copyright')))
    for file in license_files:
        notices.append(f'\n--- {label} / {file.name} ---\n' + file.read_text(errors='replace'))

cargo_home = Path(os.environ.get('CARGO_HOME', str(Path.home() / '.cargo')))
for package in (manifest.parent / 'Cargo.lock').read_text().split('[[package]]')[1:]:
    name = re.search(r'^name = "([^"]+)"', package, re.M)
    version = re.search(r'^version = "([^"]+)"', package, re.M)
    if name and version:
        label = f'{name[1]}-{version[1]}'
        for directory in (cargo_home / 'registry/src').glob('*/' + label): collect(directory, label)
for package_json in (web / 'node_modules').glob('*/package.json'):
    package = json.loads(package_json.read_text()); collect(package_json.parent, f'{package.get("name")} {package.get("version")}')
for package_json in (web / 'node_modules').glob('@*/*/package.json'):
    package = json.loads(package_json.read_text()); collect(package_json.parent, f'{package.get("name")} {package.get("version")}')
(resources / 'THIRD-PARTY-NOTICES.txt').write_text('\n'.join(notices))
print('桌面运行资源已准备：', resources)
