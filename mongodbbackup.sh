#!/usr/bin/env bash
set -euo pipefail

# Successful daily backups drive the schedule: days 8, 15, 22, ...;
# scheduled weeks 5, 9, 13, ...; scheduled months 13, 25, 37, ...
# Existing directories from before this script's first run are kept as legacy
# archives. The new sequence starts at zero; gaps and failed dumps don't count.
# Keep .mongodb-backup-rotation.state with the backup directories.

root="${MONGO_BACKUP_ROOT:-$HOME}"
# Use the existing Git Bash tools installation if needed; PATH takes precedence.
tools_dir="${MONGODB_TOOLS_BIN:-/c/Users/charl/Downloads/mongodb-database-tools-windows-x86_64-100.18.0/mongodb-database-tools-windows-x86_64-100.18.0/bin}"
if [[ -d "$tools_dir" ]]; then export PATH="$PATH:$tools_dir"; fi

mkdir -p -- "$root"
root="$(cd -- "$root" && pwd -P)"
today="$(date +%Y-%m-%d)"
state="$root/.mongodb-backup-rotation.state"
lock="$root/.mongodb-backup-rotation.lock"
stage="$root/.mongodb-backup-in-progress"
log_file="$root/mongodb-backup-rotation.log"
day_pattern='[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
shopt -s nullglob

log() { printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" | tee -a "$log_file"; }
fail() { log "ERROR: $*"; exit 1; }
exists() { [[ -e "$1" || -L "$1" ]]; }

mkdir -- "$lock" 2>/dev/null || fail "Backup is locked. If no backup is running, remove the empty lock directory: $lock"
trap 'rmdir -- "$lock"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

write_state() {
    # Rename from the same filesystem so readers always see a complete record.
    printf '%s %s %s %s\n' "$1" "$2" "$cutoff" "$3" > "$state.tmp"
    mv -fT -- "$state.tmp" "$state"
}

# list_tier sets a sorted array; strict file names and no symlinks.
export LC_ALL=C
list_tier() {
    local prefix="$1" path stamp
    tier=()
    for path in "$root/$prefix"$day_pattern; do
        [[ -d "$path" && ! -L "$path" ]] || continue
        stamp="${path##*/$prefix}"
        if [[ "$stamp" > "$cutoff" ]]; then tier+=("$path"); fi
    done
}

if [[ ! -f "$state" ]]; then
    exists "$stage" && fail "Found an in-progress dump without its state file: $stage"
    cutoff=0000-00-00
    # A cutoff preserves old backups without assuming their count or cadence.
    for prefix in backup backupweekly backupmonthly backupyearly; do
        for path in "$root/$prefix"$day_pattern; do
            [[ -d "$path" && ! -L "$path" ]] || continue
            stamp="${path##*/$prefix}"
            if [[ "$stamp" > "$cutoff" ]]; then cutoff="$stamp"; fi
        done
    done
    write_state 0 - done
    log "Started a new rotation tally. Existing archives through $cutoff are kept separately."
fi

read -r total last cutoff phase < "$state" || fail "Cannot read rotation state: $state"
[[ "$total" =~ ^(0|[1-9][0-9]{0,8})$ && "$cutoff" == $day_pattern &&
    ( "$last" == - || "$last" == $day_pattern ) &&
    ( "$phase" == done || ( "$phase" == pending && "$last" != - && "$total" != 0 ) ) ]] ||
    fail "Invalid rotation state: $state"

rotate_tier() {
    local from="$1" to="$2" cap="$3" promote="$4" oldest target
    list_tier "$from"
    while (( ${#tier[@]} > cap )); do
        oldest="${tier[0]}"
        if (( promote )); then
            target="$root/$to${oldest##*/$from}"
            exists "$target" && fail "Archive already exists; both backups left untouched: $target"
            mv -T -- "$oldest" "$target"
            log "Promoted: $oldest -> $target"
            promote=0
        else
            rm -rf -- "$oldest"
            log "Expired from rolling retention: $oldest"
        fi
        list_tier "$from"
    done
}

finish_pending() {
    local final="$root/backup$last" weekly=0 monthly=0 yearly=0 week month
    if exists "$stage"; then
        exists "$final" && fail "Both staged and final backups exist; neither was overwritten."
        [[ -d "$stage" && ! -L "$stage" ]] || fail "Invalid staged backup: $stage"
        mv -T -- "$stage" "$final"
    else
        [[ -d "$final" && ! -L "$final" ]] || fail "Pending backup is missing: $final"
    fi
    if (( total >= 8 && (total - 8) % 7 == 0 )); then
        weekly=1
        week=$((1 + (total - 8) / 7))
        if (( week >= 5 && (week - 5) % 4 == 0 )); then
            monthly=1
            month=$((1 + (week - 5) / 4))
            if (( month >= 13 && (month - 13) % 12 == 0 )); then yearly=1; fi
        fi
    fi
    rotate_tier backup backupweekly 7 "$weekly"
    rotate_tier backupweekly backupmonthly 4 "$monthly"
    rotate_tier backupmonthly backupyearly 12 "$yearly"
    # Yearlies are never pruned. Keep the all-time daily tally as well.
    write_state "$total" "$last" done
    log "Backup rotation finished. Successful daily backups in this sequence: $total"
}

if [[ "$phase" == pending ]]; then
    log "Resuming unfinished rotation for $last."
    finish_pending
fi

if [[ "$today" == "$last" || ( "$last" == - && -d "$root/backup$today" ) ]]; then
    log "Today's backup already exists; the tally was not incremented."
    exit 0
fi
[[ "$today" > "$cutoff" && "$today" > "$last" ]] || fail "Backup date is earlier than the rotation sequence."
exists "$root/backup$today" && fail "Today's backup path already exists: $root/backup$today"
command -v mongodump >/dev/null 2>&1 || fail "mongodump was not found on PATH."
: "${MONGO_URI:?MONGO_URI must be set}"

# A leftover stage with no pending record belongs to an incomplete dump.
if exists "$stage"; then
    [[ -d "$stage" && ! -L "$stage" ]] || fail "Unexpected staging path: $stage"
    rm -rf -- "$stage"
fi
mkdir -- "$stage"
log "Starting MongoDB backup for $today."
if ! mongodump --uri "$MONGO_URI" --out "$stage"; then
    rm -rf -- "$stage"
    fail "mongodump failed; no backup counted or rotated."
fi

total=$((total + 1))
last="$today"
write_state "$total" "$last" pending
finish_pending
log "Backup completed: $root/backup$today"
