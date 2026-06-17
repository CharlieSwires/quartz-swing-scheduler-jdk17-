#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# MongoDB Atlas/local backup rotation script
#
# Creates:
#   ~/backupYYYY-MM-DD
#
# Then rotates:
#   daily   -> weekly  after 7 days
#   weekly  -> monthly after 4 weeklies
#   monthly -> yearly  after 12 monthlies
#
# Required environment variable:
#   MONGO_URI
#
# Example:
#   export MONGO_URI='mongodb+srv://user:password@cluster.xxxxx.mongodb.net/example_security'
# ============================================================

# ---- Configuration ----
MONGO_URI="mongodb+srv://username:password@cluster0.icebq.mongodb.net/example_security"
PATH="C:\Users\owner\Downloads\mongodb-database-tools-windows-x86_64-100.17.0\mongodb-database-tools-windows-x86_64-100.17.0\bin":$PATH

: "${MONGO_URI:?ERROR: MONGO_URI environment variable is not set.}"

DATE="$(date +%Y-%m-%d)"

DAILY_PREFIX="$HOME/backup"
WEEKLY_PREFIX="$HOME/backupweekly"
MONTHLY_PREFIX="$HOME/backupmonthly"
YEARLY_PREFIX="$HOME/backupyearly"

TODAY_BACKUP="${DAILY_PREFIX}${DATE}"

LOG_FILE="$HOME/mongodb-backup-rotation.log"

# ---- Logging ----

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

# ---- Safety checks ----

if ! command -v mongodump >/dev/null 2>&1; then
    log "ERROR: mongodump was not found on PATH."
    log "Install MongoDB Database Tools and make sure mongodump is available."
    exit 1
fi

# Avoid overwriting an existing backup from the same day.
if [ -e "$TODAY_BACKUP" ]; then
    log "ERROR: Today's backup already exists: $TODAY_BACKUP"
    log "Not overwriting it."
    exit 1
fi

# ---- Create today's daily backup ----

log "Starting MongoDB backup."
log "Output directory: $TODAY_BACKUP"

mongodump --uri "$MONGO_URI" --out "$TODAY_BACKUP"

log "Backup completed: $TODAY_BACKUP"

# ---- Helper: list matching backup directories oldest first ----

list_dirs_oldest_first() {
    local prefix="$1"

    # Finds directories like:
    #   /home/user/backup2026-06-17
    #   /home/user/backupweekly2026-06-17
    #
    # Then sorts by name, which works because YYYY-MM-DD sorts chronologically.
    find "$HOME" -maxdepth 1 -type d -name "$(basename "$prefix")????-??-??" | sort
}

count_dirs() {
    local prefix="$1"
    list_dirs_oldest_first "$prefix" | wc -l | tr -d ' '
}

oldest_dir() {
    local prefix="$1"
    list_dirs_oldest_first "$prefix" | head -n 1
}

# ---- Rotate daily -> weekly ----
#
# After creating today's backup, if there are more than 7 daily backups,
# move the oldest daily backup into weekly storage.

daily_count="$(count_dirs "$DAILY_PREFIX")"

if [ "$daily_count" -gt 7 ]; then
    oldest_daily="$(oldest_dir "$DAILY_PREFIX")"
    oldest_date="${oldest_daily#$DAILY_PREFIX}"
    weekly_target="${WEEKLY_PREFIX}${oldest_date}"

    log "Daily backups count is $daily_count, moving oldest daily to weekly:"
    log "  $oldest_daily"
    log "  -> $weekly_target"

    if [ -e "$weekly_target" ]; then
        log "WARNING: Weekly target already exists, deleting old daily instead:"
        log "  $oldest_daily"
        rm -rf "$oldest_daily"
    else
        mv "$oldest_daily" "$weekly_target"
    fi
else
    log "Daily backups count is $daily_count, no daily rotation needed."
fi

# ---- Rotate weekly -> monthly ----
#
# If there are more than 4 weekly backups,
# move the oldest weekly backup into monthly storage.

weekly_count="$(count_dirs "$WEEKLY_PREFIX")"

if [ "$weekly_count" -gt 4 ]; then
    oldest_weekly="$(oldest_dir "$WEEKLY_PREFIX")"
    oldest_date="${oldest_weekly#$WEEKLY_PREFIX}"
    monthly_target="${MONTHLY_PREFIX}${oldest_date}"

    log "Weekly backups count is $weekly_count, moving oldest weekly to monthly:"
    log "  $oldest_weekly"
    log "  -> $monthly_target"

    if [ -e "$monthly_target" ]; then
        log "WARNING: Monthly target already exists, deleting old weekly instead:"
        log "  $oldest_weekly"
        rm -rf "$oldest_weekly"
    else
        mv "$oldest_weekly" "$monthly_target"
    fi
else
    log "Weekly backups count is $weekly_count, no weekly rotation needed."
fi

# ---- Rotate monthly -> yearly ----
#
# If there are more than 12 monthly backups,
# move the oldest monthly backup into yearly storage.

monthly_count="$(count_dirs "$MONTHLY_PREFIX")"

if [ "$monthly_count" -gt 12 ]; then
    oldest_monthly="$(oldest_dir "$MONTHLY_PREFIX")"
    oldest_date="${oldest_monthly#$MONTHLY_PREFIX}"
    yearly_target="${YEARLY_PREFIX}${oldest_date}"

    log "Monthly backups count is $monthly_count, moving oldest monthly to yearly:"
    log "  $oldest_monthly"
    log "  -> $yearly_target"

    if [ -e "$yearly_target" ]; then
        log "WARNING: Yearly target already exists, deleting old monthly instead:"
        log "  $oldest_monthly"
        rm -rf "$oldest_monthly"
    else
        mv "$oldest_monthly" "$yearly_target"
    fi
else
    log "Monthly backups count is $monthly_count, no monthly rotation needed."
fi

log "Backup rotation finished."

log "Current backup counts:"
log "  Daily:   $(count_dirs "$DAILY_PREFIX")"
log "  Weekly:  $(count_dirs "$WEEKLY_PREFIX")"
log "  Monthly: $(count_dirs "$MONTHLY_PREFIX")"
log "  Yearly:  $(count_dirs "$YEARLY_PREFIX")"