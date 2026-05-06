package com.example.quartzswing;

import org.quartz.CronExpression;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Path;

public class SchedulerFrame extends JFrame {
    private static final Color BOOTSTRAP_PRIMARY = new Color(13, 110, 253);
    private static final Color BOOTSTRAP_SUCCESS = new Color(25, 135, 84);
    private static final Color BOOTSTRAP_DANGER = new Color(220, 53, 69);
    private static final Color BOOTSTRAP_WARNING = new Color(255, 193, 7);
    private static final Color BOOTSTRAP_LIGHT = new Color(248, 249, 250);
    private static final Color BOOTSTRAP_DARK = new Color(33, 37, 41);
    private static final Color BOOTSTRAP_BORDER = new Color(222, 226, 230);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color INPUT_BACKGROUND = Color.WHITE;

    private final SchedulerController controller;

    private final JTextField taskNameField = new JTextField("Example scheduled process");
    private final JTextField scriptPathField = new JTextField(defaultDummyScriptPath());

    private final JRadioButton intervalRadio = new JRadioButton("Simple interval", true);
    private final JRadioButton cronRadio = new JRadioButton("Use cron schedule");

    private final JSpinner secondsSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 86400, 1));

    private final JComboBox<String> cronPresetCombo = new JComboBox<>(new String[]{
            "Every N seconds",
            "Every N minutes",
            "Every N hours",
            "Daily at time",
            "Weekly at time",
            "Monthly on day at time",
            "Advanced manual cron"
    });

    private final JPanel cronBuilderCards = new JPanel(new CardLayout());

    private final JSpinner everySecondsSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 59, 1));
    private final JSpinner everyMinutesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 59, 1));
    private final JSpinner everyHoursSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 23, 1));

    private final JSpinner dailyHourSpinner = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
    private final JSpinner dailyMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JComboBox<String> weeklyDayCombo = new JComboBox<>(new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    });
    private final JSpinner weeklyHourSpinner = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
    private final JSpinner weeklyMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JSpinner monthlyDaySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
    private final JSpinner monthlyHourSpinner = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
    private final JSpinner monthlyMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JTextField cronField = new JTextField("0/30 * * * * ?");
    private final JLabel cronExplanationLabel = new JLabel("Runs every 30 seconds.");

    private final JTextArea logArea = new JTextArea();
    private final JLabel statusPill = new JLabel("Idle", SwingConstants.CENTER);

    public SchedulerFrame(SchedulerController controller) {
        super("Quartz Swing Scheduler - JDK 17");
        this.controller = controller;
        this.controller.setLogConsumer(this::appendLog);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        setContentPane(buildContent());
        wireCronBuilderEvents();
        applyControlState();
        updateCronFromBuilder();
        startStatusTimer();
    }

    public void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BOOTSTRAP_LIGHT);

        root.add(buildHeader(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(buildMainPanel());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        root.add(scrollPane, BorderLayout.CENTER);

        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BOOTSTRAP_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Quartz Scheduled Process");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        JLabel subtitle = new JLabel("JDK 17 SE + Swing UI + Quartz scheduler + friendly cron builder");
        subtitle.setForeground(new Color(206, 212, 218));
        subtitle.setFont(subtitle.getFont().deriveFont(14f));

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);

        statusPill.setOpaque(true);
        statusPill.setBackground(new Color(108, 117, 125));
        statusPill.setForeground(Color.WHITE);
        statusPill.setBorder(new RoundedBorder(new Color(108, 117, 125), 16, 1));
        statusPill.setPreferredSize(new Dimension(130, 34));

        header.add(text, BorderLayout.WEST);
        header.add(statusPill, BorderLayout.EAST);

        return header;
    }

    private JPanel buildMainPanel() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(BOOTSTRAP_LIGHT);
        container.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        container.add(buildScheduleCard(), gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        container.add(buildLogCard(), gbc);

        return container;
    }

    private JPanel buildScheduleCard() {
        JPanel card = cardPanel();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = standardGbc();

        addTitle(card, gbc, "Schedule setup", "Choose a script and build a schedule without needing to know Quartz cron syntax.");

        gbc.gridy++;
        card.add(label("Task name"), gbc);

        gbc.gridy++;
        taskNameField.setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));
        taskNameField.setMargin(new Insets(8, 10, 8, 10));
        card.add(taskNameField, gbc);

        gbc.gridy++;
        card.add(buildScriptPicker(), gbc);

        gbc.gridy++;
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        radios.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        group.add(intervalRadio);
        group.add(cronRadio);

        intervalRadio.setOpaque(false);
        cronRadio.setOpaque(false);
        intervalRadio.addActionListener(e -> applyControlState());
        cronRadio.addActionListener(e -> applyControlState());

        radios.add(intervalRadio);
        radios.add(cronRadio);
        card.add(radios, gbc);

        gbc.gridy++;
        card.add(buildIntervalPanel(), gbc);

        gbc.gridy++;
        card.add(buildCronBuilderPanel(), gbc);

        gbc.gridy++;
        card.add(buildButtons(), gbc);

        return card;
    }

    private JPanel buildScriptPicker() {
        JPanel outer = new JPanel(new BorderLayout(8, 4));
        outer.setOpaque(false);

        JLabel title = label("Process script");

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        scriptPathField.setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));
        scriptPathField.setMargin(new Insets(8, 10, 8, 10));

        JButton browse = button("Browse / Search", BOOTSTRAP_PRIMARY);
        browse.addActionListener(e -> browseForScript());

        row.add(scriptPathField, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);

        JLabel hint = new JLabel("Included example: scripts/dummy.sh. It counts down from 10, updating once per second.");
        hint.setForeground(MUTED);

        outer.add(title, BorderLayout.NORTH);
        outer.add(row, BorderLayout.CENTER);
        outer.add(hint, BorderLayout.SOUTH);

        return outer;
    }

    private JPanel buildIntervalPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JLabel every = label("Run every");
        JLabel seconds = label("second(s)");

        styleSpinner(secondsSpinner, 6);

        panel.add(every);
        panel.add(secondsSpinner);
        panel.add(seconds);

        return panel;
    }

    private JPanel buildCronBuilderPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = standardGbc();

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);

        JLabel title = label("Cron builder");
        JLabel subtitle = new JLabel("Choose plain English options and the Quartz cron expression is generated automatically.");
        subtitle.setForeground(MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        gbc.gridy = 0;
        panel.add(header, gbc);

        gbc.gridy++;
        cronPresetCombo.setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));
        cronPresetCombo.setBackground(INPUT_BACKGROUND);
        panel.add(cronPresetCombo, gbc);

        buildCronCards();

        gbc.gridy++;
        panel.add(cronBuilderCards, gbc);

        gbc.gridy++;
        JPanel generated = new JPanel(new BorderLayout(8, 4));
        generated.setOpaque(false);

        JLabel generatedTitle = label("Generated Quartz cron");
        generated.add(generatedTitle, BorderLayout.NORTH);

        cronField.setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));
        cronField.setMargin(new Insets(8, 10, 8, 10));
        generated.add(cronField, BorderLayout.CENTER);

        JPanel generatedButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        generatedButtons.setOpaque(false);

        JButton refresh = button("Update Cron", BOOTSTRAP_PRIMARY);
        refresh.addActionListener(e -> updateCronFromBuilder());

        JButton validate = button("Validate", BOOTSTRAP_SUCCESS);
        validate.addActionListener(e -> validateCron());

        generatedButtons.add(refresh);
        generatedButtons.add(validate);

        generated.add(generatedButtons, BorderLayout.EAST);

        gbc.gridy++;
        panel.add(generated, gbc);

        gbc.gridy++;
        cronExplanationLabel.setForeground(MUTED);
        panel.add(cronExplanationLabel, gbc);

        return panel;
    }

    private void buildCronCards() {
        cronBuilderCards.setOpaque(false);

        cronBuilderCards.add(rowPanel(
                label("Every"),
                spinner(everySecondsSpinner, 6),
                label("second(s). Use 1 to 59.")
        ), "Every N seconds");

        cronBuilderCards.add(rowPanel(
                label("Every"),
                spinner(everyMinutesSpinner, 6),
                label("minute(s).")
        ), "Every N minutes");

        cronBuilderCards.add(rowPanel(
                label("Every"),
                spinner(everyHoursSpinner, 6),
                label("hour(s).")
        ), "Every N hours");

        cronBuilderCards.add(rowPanel(
                label("Run daily at"),
                spinner(dailyHourSpinner, 4),
                label(":"),
                spinner(dailyMinuteSpinner, 4)
        ), "Daily at time");

        cronBuilderCards.add(rowPanel(
                label("Run every"),
                weeklyDayCombo,
                label("at"),
                spinner(weeklyHourSpinner, 4),
                label(":"),
                spinner(weeklyMinuteSpinner, 4)
        ), "Weekly at time");

        cronBuilderCards.add(rowPanel(
                label("Run every month on day"),
                spinner(monthlyDaySpinner, 4),
                label("at"),
                spinner(monthlyHourSpinner, 4),
                label(":"),
                spinner(monthlyMinuteSpinner, 4),
                label("Use 1-28 for all months.")
        ), "Monthly on day at time");

        JPanel advanced = new JPanel(new BorderLayout(8, 4));
        advanced.setOpaque(false);
        JLabel advancedText = new JLabel("Edit the generated cron box directly. Quartz order is: second minute hour day-of-month month day-of-week.");
        advancedText.setForeground(MUTED);
        advanced.add(advancedText, BorderLayout.CENTER);
        cronBuilderCards.add(advanced, "Advanced manual cron");
    }

    private JPanel rowPanel(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setOpaque(false);
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    private JSpinner spinner(JSpinner spinner, int columns) {
        styleSpinner(spinner, columns);
        return spinner;
    }

    private void styleSpinner(JSpinner spinner, int columns) {
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.setOpaque(false);

        JButton start = button("Start Schedule", BOOTSTRAP_SUCCESS);
        JButton runNow = button("Run Once Now", BOOTSTRAP_PRIMARY);
        JButton stop = button("Stop", BOOTSTRAP_DANGER);
        JButton clear = button("Clear Log", new Color(108, 117, 125));

        start.addActionListener(e -> startSchedule());
        runNow.addActionListener(e -> controller.runOnceNow(taskNameField.getText(), scriptPathField.getText()));
        stop.addActionListener(e -> controller.stopSchedule());
        clear.addActionListener(e -> logArea.setText(""));

        panel.add(start);
        panel.add(runNow);
        panel.add(stop);
        panel.add(clear);

        return panel;
    }

    private JPanel buildLogCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Execution log");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitle = new JLabel("Quartz activity and script output appear here.");
        subtitle.setForeground(MUTED);

        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);

        logArea.setEditable(false);
        logArea.setRows(12);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(new RoundedBorder(BOOTSTRAP_BORDER, 8, 1));

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private void wireCronBuilderEvents() {
        cronPresetCombo.addActionListener(e -> {
            String selected = selectedCronPreset();
            CardLayout layout = (CardLayout) cronBuilderCards.getLayout();
            layout.show(cronBuilderCards, selected);
            updateCronFromBuilder();
            applyControlState();
        });

        everySecondsSpinner.addChangeListener(e -> updateCronFromBuilder());
        everyMinutesSpinner.addChangeListener(e -> updateCronFromBuilder());
        everyHoursSpinner.addChangeListener(e -> updateCronFromBuilder());
        dailyHourSpinner.addChangeListener(e -> updateCronFromBuilder());
        dailyMinuteSpinner.addChangeListener(e -> updateCronFromBuilder());
        weeklyDayCombo.addActionListener(e -> updateCronFromBuilder());
        weeklyHourSpinner.addChangeListener(e -> updateCronFromBuilder());
        weeklyMinuteSpinner.addChangeListener(e -> updateCronFromBuilder());
        monthlyDaySpinner.addChangeListener(e -> updateCronFromBuilder());
        monthlyHourSpinner.addChangeListener(e -> updateCronFromBuilder());
        monthlyMinuteSpinner.addChangeListener(e -> updateCronFromBuilder());
    }

    private void updateCronFromBuilder() {
        String selected = selectedCronPreset();

        if ("Advanced manual cron".equals(selected)) {
            cronField.setEditable(true);
            cronExplanationLabel.setText("Manual cron mode. Quartz format is: second minute hour day-of-month month day-of-week.");
            return;
        }

        cronField.setEditable(false);

        String cron;
        String explanation;

        switch (selected) {
            case "Every N seconds" -> {
                int seconds = (Integer) everySecondsSpinner.getValue();
                cron = "0/" + seconds + " * * * * ?";
                explanation = "Runs every " + seconds + " second(s).";
            }
            case "Every N minutes" -> {
                int minutes = (Integer) everyMinutesSpinner.getValue();
                cron = "0 0/" + minutes + " * * * ?";
                explanation = "Runs every " + minutes + " minute(s).";
            }
            case "Every N hours" -> {
                int hours = (Integer) everyHoursSpinner.getValue();
                cron = "0 0 0/" + hours + " * * ?";
                explanation = "Runs every " + hours + " hour(s).";
            }
            case "Daily at time" -> {
                int hour = (Integer) dailyHourSpinner.getValue();
                int minute = (Integer) dailyMinuteSpinner.getValue();
                cron = "0 " + minute + " " + hour + " * * ?";
                explanation = "Runs every day at " + two(hour) + ":" + two(minute) + ".";
            }
            case "Weekly at time" -> {
                int hour = (Integer) weeklyHourSpinner.getValue();
                int minute = (Integer) weeklyMinuteSpinner.getValue();
                String quartzDay = quartzDay((String) weeklyDayCombo.getSelectedItem());
                cron = "0 " + minute + " " + hour + " ? * " + quartzDay;
                explanation = "Runs every " + weeklyDayCombo.getSelectedItem() + " at " + two(hour) + ":" + two(minute) + ".";
            }
            case "Monthly on day at time" -> {
                int day = (Integer) monthlyDaySpinner.getValue();
                int hour = (Integer) monthlyHourSpinner.getValue();
                int minute = (Integer) monthlyMinuteSpinner.getValue();
                cron = "0 " + minute + " " + hour + " " + day + " * ?";
                explanation = "Runs every month on day " + day + " at " + two(hour) + ":" + two(minute) + ".";
            }
            default -> {
                cron = "0/30 * * * * ?";
                explanation = "Runs every 30 seconds.";
            }
        }

        cronField.setText(cron);
        cronExplanationLabel.setText(explanation);
    }

    private void validateCron() {
        String cron = cronField.getText().trim();
        if (CronExpression.isValidExpression(cron)) {
            appendLog("Valid Quartz cron: " + cron);
        } else {
            appendLog("Invalid Quartz cron: " + cron);
        }
    }

    private String selectedCronPreset() {
        Object selected = cronPresetCombo.getSelectedItem();
        return selected == null ? "Every N seconds" : selected.toString();
    }

    private String quartzDay(String day) {
        if (day == null) {
            return "MON";
        }
        return switch (day) {
            case "Monday" -> "MON";
            case "Tuesday" -> "TUE";
            case "Wednesday" -> "WED";
            case "Thursday" -> "THU";
            case "Friday" -> "FRI";
            case "Saturday" -> "SAT";
            case "Sunday" -> "SUN";
            default -> "MON";
        };
    }

    private String two(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private void browseForScript() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Find process script");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Scripts (*.sh, *.bat, *.cmd)", "sh", "bat", "cmd"));

        String current = scriptPathField.getText();
        if (current != null && !current.isBlank()) {
            File currentFile = new File(current);
            if (currentFile.exists()) {
                chooser.setSelectedFile(currentFile);
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            scriptPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            appendLog("Selected script: " + chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startSchedule() {
        try {
            if (intervalRadio.isSelected()) {
                controller.scheduleSimpleInterval((Integer) secondsSpinner.getValue(), taskNameField.getText(), scriptPathField.getText());
            } else {
                updateCronFromBuilder();
                String cron = cronField.getText().trim();
                if (!CronExpression.isValidExpression(cron)) {
                    appendLog("Invalid Quartz cron expression: " + cron);
                    return;
                }
                controller.scheduleCron(cron, taskNameField.getText(), scriptPathField.getText());
            }
        } catch (Exception ex) {
            appendLog("Could not start schedule: " + ex.getMessage());
        }
    }

    private void applyControlState() {
        boolean interval = intervalRadio.isSelected();
        boolean cron = cronRadio.isSelected();

        secondsSpinner.setEnabled(interval);
        setEnabledRecursively(cronBuilderCards, cron);
        cronPresetCombo.setEnabled(cron);
        cronField.setEnabled(cron);
    }

    private void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    private void startStatusTimer() {
        Timer timer = new Timer(750, e -> {
            boolean scheduled = controller.isScheduled();
            statusPill.setText(scheduled ? "Scheduled" : "Idle");
            statusPill.setBackground(scheduled ? BOOTSTRAP_SUCCESS : new Color(108, 117, 125));
            statusPill.repaint();
        });
        timer.start();
    }

    private static String defaultDummyScriptPath() {
        /*
         * Try a few likely working directories.
         *
         * This helps when running from:
         * - project root: mvn clean package && java -jar target/...
         * - target directory
         * - Eclipse with a different working directory
         */
        Path[] candidates = new Path[]{
                Path.of("scripts", "dummy.sh"),
                Path.of("..", "scripts", "dummy.sh"),
                Path.of("..", "..", "scripts", "dummy.sh"),
                Path.of("target", "scripts", "dummy.sh")
        };

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (absolute.toFile().exists()) {
                return absolute.toString();
            }
        }

        return Path.of("scripts", "dummy.sh").toAbsolutePath().normalize().toString();
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BOOTSTRAP_BORDER, 16, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        return panel;
    }

    private JPanel sectionPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(252, 252, 253));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BOOTSTRAP_BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    private void addTitle(JPanel panel, GridBagConstraints gbc, String titleText, String subtitleText) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setForeground(MUTED);

        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);

        panel.add(titlePanel, gbc);
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(BOOTSTRAP_DARK);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 14f));
        return label;
    }

    private JButton button(String text, Color background) {
        JButton button = new BootstrapButton(text, background);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(9, 14, 9, 14));
        return button;
    }

    private GridBagConstraints standardGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);
        return gbc;
    }

    private static final class BootstrapButton extends JButton {
        private final Color background;
        private final Color hoverBackground;
        private final Color pressedBackground;
        private final Color disabledBackground;
        private final int radius = 8;

        private BootstrapButton(String text, Color background) {
            super(text);
            this.background = background;
            this.hoverBackground = background.brighter();
            this.pressedBackground = background.darker();
            this.disabledBackground = new Color(173, 181, 189);

            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(Math.max(120, getPreferredSize().width + 24), 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill;
                if (!isEnabled()) {
                    fill = disabledBackground;
                    setForeground(Color.WHITE);
                } else if (getModel().isPressed()) {
                    fill = pressedBackground;
                    setForeground(Color.WHITE);
                } else if (getModel().isRollover()) {
                    fill = hoverBackground;
                    setForeground(Color.WHITE);
                } else {
                    fill = background;
                    setForeground(Color.WHITE);
                }

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

                g2.setColor(fill.darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            } finally {
                g2.dispose();
            }

            super.paintComponent(g);
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        private RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(thickness));
                g2.draw(new RoundRectangle2D.Double(
                        x + thickness / 2.0,
                        y + thickness / 2.0,
                        width - thickness,
                        height - thickness,
                        radius,
                        radius
                ));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 10, 8, 10);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 8;
            insets.left = 10;
            insets.bottom = 8;
            insets.right = 10;
            return insets;
        }
    }
}
