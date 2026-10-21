#!/data/data/com.termux/files/usr/bin/bash
set -e
git add -u && git commit -qm "${1:-wip}" || true
git push -q
sleep 8
id=$(gh run list --limit 1 --json databaseId -q '.[0].databaseId')
gh run watch "$id" --exit-status
d=$HOME/apk-out; rm -rf "$d"; gh run download "$id" -D "$d"
apk=$(find "$d" -name '*arm64*.apk' | head -1)
[ -z "$apk" ] && apk=$(find "$d" -name '*.apk' | head -1)
termux-open "$apk"
