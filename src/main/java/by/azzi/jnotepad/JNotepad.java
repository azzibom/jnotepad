package by.azzi.jnotepad;

import by.azzibom.utils.gui.swing.SwingLocalizer;

import javax.swing.*;

public class JNotepad implements Runnable {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new JNotepad(args));
    }

    private final String[] args;

    public JNotepad(String[] args) {
        this.args = args;
    }

    @Override
    public void run() {
        SwingLocalizer.localize();
        Theme.setupTheme();
        new NotepadFrame2().setVisible(true);
    }
}
