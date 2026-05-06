package com.example.quartzswing;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class ExampleScheduledJob implements Job {
    private static volatile Consumer<String> logConsumer = System.out::println;

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer == null ? System.out::println : consumer;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String taskName = context.getMergedJobDataMap().getString("taskName");
        String scriptPath = context.getMergedJobDataMap().getString("scriptPath");

        if (scriptPath == null || scriptPath.isBlank()) {
            throw new JobExecutionException("No script selected.");
        }

        File script = new File(scriptPath);
        if (!script.exists() || !script.isFile()) {
            throw new JobExecutionException("Script does not exist: " + scriptPath);
        }

        log("Running scheduled task: " + taskName);
        log("Script: " + script.getAbsolutePath());

        CommandSpec commandSpec = buildCommand(script);
        log("Runner: " + commandSpec.description());
        log("Command: " + String.join(" ", commandSpec.command()));

        ProcessBuilder processBuilder = new ProcessBuilder(commandSpec.command());
        processBuilder.directory(script.getParentFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log(line);
                }
            }

            int exitCode = process.waitFor();
            log("Script exited with code " + exitCode + ".");

            if (exitCode != 0) {
                throw new JobExecutionException("Script exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Script interrupted", e);
        } catch (Exception e) {
            throw new JobExecutionException("Could not run script: " + e.getMessage(), e);
        }
    }

    private CommandSpec buildCommand(File script) throws JobExecutionException {
        String absolutePath = script.getAbsolutePath();
        String lowerName = script.getName().toLowerCase(Locale.ROOT);
        boolean windows = isWindows();

        if (lowerName.endsWith(".sh")) {
            if (windows) {
                File gitBash = findGitBashBashExe()
                        .orElseThrow(() -> new JobExecutionException("""
                                Could not find Git Bash bash.exe.

                                Install Git for Windows, or add Git Bash to PATH.

                                Tried common locations such as:
                                C:\\Program Files\\Git\\bin\\bash.exe
                                C:\\Program Files\\Git\\usr\\bin\\bash.exe
                                C:\\Program Files (x86)\\Git\\bin\\bash.exe
                                """));

                /*
                 * This starts a Git Bash bash process from Java and passes the selected .sh file to it.
                 * It is intentionally not using "cmd /c", because we want Git Bash to run the script.
                 */
                return new CommandSpec(
                        List.of(gitBash.getAbsolutePath(), absolutePath),
                        "Git Bash: " + gitBash.getAbsolutePath()
                );
            }

            return new CommandSpec(
                    List.of("bash", absolutePath),
                    "System bash"
            );
        }

        if (windows && lowerName.endsWith(".bat")) {
            return new CommandSpec(
                    List.of("cmd.exe", "/c", absolutePath),
                    "Windows cmd.exe batch runner"
            );
        }

        if (windows && lowerName.endsWith(".cmd")) {
            return new CommandSpec(
                    List.of("cmd.exe", "/c", absolutePath),
                    "Windows cmd.exe command runner"
            );
        }

        return new CommandSpec(
                List.of(absolutePath),
                "Direct executable"
        );
    }

    private Optional<File> findGitBashBashExe() {
        List<File> candidates = new ArrayList<>();

        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LOCALAPPDATA");

        if (programFiles != null) {
            candidates.add(new File(programFiles, "Git/bin/bash.exe"));
            candidates.add(new File(programFiles, "Git/usr/bin/bash.exe"));
        }

        if (programFilesX86 != null) {
            candidates.add(new File(programFilesX86, "Git/bin/bash.exe"));
            candidates.add(new File(programFilesX86, "Git/usr/bin/bash.exe"));
        }

        if (localAppData != null) {
            candidates.add(new File(localAppData, "Programs/Git/bin/bash.exe"));
            candidates.add(new File(localAppData, "Programs/Git/usr/bin/bash.exe"));
        }

        candidates.add(new File("C:/Program Files/Git/bin/bash.exe"));
        candidates.add(new File("C:/Program Files/Git/usr/bin/bash.exe"));
        candidates.add(new File("C:/Program Files (x86)/Git/bin/bash.exe"));
        candidates.add(new File("C:/Program Files (x86)/Git/usr/bin/bash.exe"));

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return Optional.of(candidate);
            }
        }

        return findOnPath("bash.exe");
    }

    private Optional<File> findOnPath(String executableName) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }

        String[] directories = path.split(File.pathSeparator);
        for (String directory : directories) {
            File candidate = new File(directory, executableName);
            if (candidate.exists() && candidate.isFile()) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SwingUtilities.invokeLater(() -> logConsumer.accept("[" + timestamp + "] " + message));
    }

    private record CommandSpec(List<String> command, String description) {
    }
}
