package by.azzi.jnotepad;

import javax.swing.*;

/**
 * Hello world!
 */
public class JNotepad {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.setupTheme();
            new NotepadFrame2().setVisible(true);
        });
    }

}
