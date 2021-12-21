package by.azzi.jnotepad.actions;

import by.azzi.jnotepad.NotepadFrame2;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class NewNotepadFrameAction extends AbstractAction {

    private static final NewNotepadFrameAction instance = new NewNotepadFrameAction();

    public static NewNotepadFrameAction getInstance() {
        return instance;
    }

    public NewNotepadFrameAction() {
        super("Новое окно");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new NotepadFrame2().setVisible(true);
    }
}
