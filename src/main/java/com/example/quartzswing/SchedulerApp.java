/*==========================================================================
Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved.

This source code is proprietary and confidential. Unauthorised copying, modification, distribution or use is prohibited.
============================================================================*/
package com.example.quartzswing;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.EventQueue;

public class SchedulerApp {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to the default Swing look and feel.
            }

            SchedulerController controller = new SchedulerController();
            SchedulerFrame frame = new SchedulerFrame(controller);

            Runtime.getRuntime().addShutdownHook(new Thread(controller::shutdown));

            SwingUtilities.invokeLater(() -> {
                frame.setVisible(true);
                frame.appendLog("Application started.");
                frame.appendLog("Define a schedule and press Start Schedule.");
            });
        });
    }
}
