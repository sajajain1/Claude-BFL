#!/usr/bin/env bash
# Builds the demo upload archives from the sample project folders.
set -euo pipefail
cd "$(dirname "$0")"
for project in legacy-shop modern-service; do
  rm -f "$project.zip"
  if command -v zip >/dev/null 2>&1; then
    zip -qr "$project.zip" "$project" -x '*/target/*'
  else
    python3 -c "import shutil,sys; shutil.make_archive(sys.argv[1], 'zip', '.', sys.argv[1])" "$project"
  fi
  echo "created samples/$project.zip"
done
