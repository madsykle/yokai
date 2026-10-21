#!/data/data/com.termux/files/usr/bin/bash
set -e
git add -A && git commit -qm "${1:-wip}" || true
git push -q
sleep 8
id=$(gh run list --limit 1 --json databaseId -q '.[0].databaseId')
gh run watch "$id" --exit-status
rm -rf out && gh run download "$id" -D out
apk=$(find out -name '*arm64*.apk' | head -1)
[ -z "$apk" ] && apk=$(find out -name '*.apk' | head -1)
termux-open "$apk"
