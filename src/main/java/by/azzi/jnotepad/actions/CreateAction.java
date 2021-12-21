package by.azzi.jnotepad.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class CreateAction extends AbstractAction {

    public CreateAction() {
        super("Создать");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        putValue(Action.ACTION_COMMAND_KEY, "create");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
