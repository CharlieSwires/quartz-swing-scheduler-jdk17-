# Quartz Swing Scheduler - JDK 17

A compact Java SE 17 desktop application using:

- Swing for the desktop UI
- Quartz for scheduling
- Maven for build/package
- A Bootstrap 5-inspired Swing look using cards, spacing, rounded borders and Bootstrap-like colours
- A script/process picker using `JFileChooser`
- A dummy shell process at `scripts/dummy.sh`

## Run from source

```bash
mvn clean package
java -jar target/quartz-swing-scheduler-jdk17-1.0.0.jar
```

## What it does

The UI lets you choose a process script and schedule it in two ways:

1. Simple interval, for example every 15 seconds.
2. A friendly cron builder that generates Quartz cron expressions for you.

The cron builder supports:

- Every N seconds
- Every N minutes
- Every N hours
- Daily at a chosen time
- Weekly on a chosen day and time
- Monthly on a chosen day and time
- Advanced manual Quartz cron

The generated cron expression is displayed in a rounded text box and can be validated before starting the schedule.

The selected script output is streamed back into the Swing execution log.

## Included dummy process

The project includes:

```text
scripts/dummy.sh
```

It displays a 10 second countdown:

```bash
#!/usr/bin/env bash

echo "Dummy process started."

for i in {10..1}
do
  echo "Exiting in ${i} second(s)..."
  sleep 1
done

echo "Dummy process finished."
exit 0
```

## Windows note

On Windows, running `.sh` files requires `bash.exe` on your PATH.

That usually means one of these:

- Git Bash
- WSL
- Cygwin
- MSYS2

The Java code runs `.sh` files using:

```text
bash path/to/script.sh
```

For a pure Windows script, you can select a `.bat` or `.cmd` file instead.

## Where to put your real scheduled process

You can either:

1. Replace `scripts/dummy.sh`, or
2. Use the Browse / Search button to select another `.sh`, `.bat`, or `.cmd` file.

The Java execution logic is in:

```text
src/main/java/com/example/quartzswing/ExampleScheduledJob.java
```

## Common Quartz cron examples

| Meaning | Expression |
|---|---|
| Every 30 seconds | `0/30 * * * * ?` |
| Every minute | `0 0/1 * * * ?` |
| Every day at 9 AM | `0 0 9 * * ?` |
| Every weekday at 9 AM | `0 0 9 ? * MON-FRI` |
| Every Monday at 9 AM | `0 0 9 ? * MON` |

Quartz cron has seconds as the first field, so it is not exactly the same as standard Unix cron.


## Friendly cron builder

You do not need to remember Quartz cron syntax.

Select **Use cron schedule**, then choose a preset such as:

- `Every N seconds`
- `Daily at time`
- `Weekly at time`

The application fills the generated Quartz cron box automatically.

For example:

| Plain English choice | Generated Quartz cron |
|---|---|
| Every 30 seconds | `0/30 * * * * ?` |
| Every 5 minutes | `0 0/5 * * * ?` |
| Every day at 09:00 | `0 0 9 * * ?` |
| Every Monday at 09:00 | `0 0 9 ? * MON` |



## Small screen support

The main UI is wrapped in a `JScrollPane`, so if the scheduler controls are taller than your display, use the vertical scrollbar or mouse wheel to reach the lower buttons and log area.


## Button visibility fix

This version uses a small custom `BootstrapButton` class instead of relying on the Windows Swing look-and-feel to paint button backgrounds. This prevents white button text disappearing on pale Windows-rendered buttons.


## Running `.sh` files through Git Bash on Windows

This version deliberately starts Git Bash from the Java Quartz job when the selected process script ends in `.sh`.

On Windows, the Java code searches for Git Bash in common locations, including:

```text
C:\Program Files\Git\bin\bash.exe
C:\Program Files\Git\usr\bin\bash.exe
C:\Program Files (x86)\Git\bin\bash.exe
C:\Program Files (x86)\Git\usr\bin\bash.exe
%LOCALAPPDATA%\Programs\Git\bin\bash.exe
%LOCALAPPDATA%\Programs\Git\usr\bin\bash.exe
```

Then it runs:

```text
"C:\Program Files\Git\bin\bash.exe" "C:\...\scripts\dummy.sh"
```

The output is streamed back into the Swing execution log.

The log now prints the runner and command, for example:

```text
Runner: Git Bash: C:\Program Files\Git\bin\bash.exe
Command: C:\Program Files\Git\bin\bash.exe C:\...\scripts\dummy.sh
```

There is also a Windows fallback script:

```text
scripts/dummy.bat
```

You can select it with Browse / Search if you want to test without Git Bash.
