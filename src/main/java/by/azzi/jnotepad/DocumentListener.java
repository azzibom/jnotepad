package by.azzi.jnotepad;

import javax.swing.event.DocumentEvent;

public interface DocumentListener extends javax.swing.event.DocumentListener {

    @Override
    default void insertUpdate(DocumentEvent e) {
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
    }
}
