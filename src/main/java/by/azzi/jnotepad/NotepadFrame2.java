package by.azzi.jnotepad;

import by.azzi.jnotepad.actions.NewNotepadFrameAction;
import by.azzi.jnotepad.listeners.DocumentListener;
import by.azzi.jnotepad.listeners.WindowListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

public class NotepadFrame2 extends JFrame implements DocumentListener, WindowListener {

    private static final Preferences P = Preferences.userNodeForPackage(NotepadFrame2.class);
    private static final List<Image> icons = new ArrayList<>();
    static {
        try {
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-48.png")));
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-96.png")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static final String APP_NAME = "Блокнот";
    private static final String DEFAULT_FILE_NAME = "Безымянный";

    private final UndoManager undoManager = new UndoManager();
    {
        undoManager.setLimit(1);
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final JTextArea textArea = new JTextArea();
    private final Font docFont = textArea.getFont();
    private float fontScale = 0;

    {
        support.addPropertyChangeListener("fontScale", evt -> {
            final Font font = textArea.getFont();
            textArea.setFont(font.deriveFont(docFont.getSize() + getFontScale()));
        });

        support.addPropertyChangeListener("wordWrap", evt -> {
            textArea.setWrapStyleWord((Boolean) evt.getNewValue());
            textArea.setLineWrap((Boolean) evt.getNewValue());
        });

        support.addPropertyChangeListener("documentChanged", evt -> updateTitle());
        textArea.addPropertyChangeListener("document", evt -> {
            undoManager.discardAllEdits();
            ((Document) evt.getNewValue()).addUndoableEditListener(undoManager);
        });
        support.addPropertyChangeListener("documentName", evt -> updateTitle());

        textArea.getDocument().addDocumentListener(this);
        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.addPropertyChangeListener("document", evt -> {
            textArea.getDocument().addDocumentListener(NotepadFrame2.this);
            setDocumentChanged(false);
        });
    }

    private File file;
    private String documentName = DEFAULT_FILE_NAME;
    private boolean documentChanged = false;

    public NotepadFrame2() throws HeadlessException {
        super(DEFAULT_FILE_NAME + " - " + APP_NAME);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(600, 400));
        setIconImages(icons);
        setJMenuBar(menuBar());

        addWindowListener(this);

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        pack();
        setLocationByPlatform(true);
    }

    private JMenuBar menuBar() {
        final JMenuBar menuBar = new JMenuBar();
        // == file menu
        final JMenu fileMenu = menuBar.add(new JMenu("Файл"));
        final JMenuItem createMenuItem = fileMenu.add("Создать");
        createMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        createMenuItem.addActionListener(e -> {
            if (documentChanged || file != null) {
                if (documentChanged) {
                    final int confirmAnswer = JOptionPane.showConfirmDialog(NotepadFrame2.this, "Вы хотите сохранить изменения в файле \"" + documentName + "\"?");
                    switch (confirmAnswer) {
                        default: {
                            System.out.println("confirmAnswer: " + confirmAnswer);
                        }
                        case JOptionPane.CANCEL_OPTION: {
                            return;
                        }
                        case JOptionPane.YES_OPTION: {
                            File file = NotepadFrame2.this.file;
                            if (file == null) {
                                final JFileChooser saveFileChooser = getSaveFileChooser();
                                final int chooseAnswer = saveFileChooser.showSaveDialog(NotepadFrame2.this);
                                if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                                    return;
                                }
                                file = saveFileChooser.getSelectedFile();
                            }
                            write(file);
                        }
                        case JOptionPane.NO_OPTION:
                    }
                }
                textArea.setDocument(new PlainDocument());
                setFile(null);
            }
        });

        fileMenu.add(NewNotepadFrameAction.getInstance());

        final JMenuItem openMenuItem = fileMenu.add("Открыть...");
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.addActionListener(e -> {
            if (documentChanged) {
                final int confirmAnswer = JOptionPane.showConfirmDialog(NotepadFrame2.this, "Вы хотите сохранить изменения в файле \"" + documentName + "\"?");
                switch (confirmAnswer) {
                    default: {
                        System.out.println("confirmAnswer: " + confirmAnswer);
                    }
                    case JOptionPane.CANCEL_OPTION: {
                        return;
                    }
                    case JOptionPane.YES_OPTION: {
                        File file = NotepadFrame2.this.file;
                        if (file == null) {
                            final JFileChooser fileChooser = getSaveFileChooser();
                            final int chooseAnswer = fileChooser.showSaveDialog(NotepadFrame2.this);
                            if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                                return;
                            }
                            file = fileChooser.getSelectedFile();
                        }
                        write(file);
                    }
                    case JOptionPane.NO_OPTION:
                }
            }
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Текстовые документы (*.txt)", "txt"));
            final int answer = fileChooser.showOpenDialog(NotepadFrame2.this);
            if (answer != JFileChooser.APPROVE_OPTION) {
                return;
            }
            setFile(fileChooser.getSelectedFile());
        });

        final JMenuItem saveMenuItem = fileMenu.add("Сохранить");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveMenuItem.addActionListener(e -> {
            if (!documentChanged) {
                return;
            }
            File file = NotepadFrame2.this.file;
            if (file == null) {
                final JFileChooser saveFileChooser = getSaveFileChooser();
                final int chooseAnswer = saveFileChooser.showSaveDialog(NotepadFrame2.this);
                if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                file = saveFileChooser.getSelectedFile();
            }
            write(file);
        });

        final JMenuItem saveAsMenuItem = fileMenu.add("Сохранить как");
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAsMenuItem.addActionListener(e -> {
            final JFileChooser saveFileChooser = getSaveFileChooser();
            final int chooseAnswer = saveFileChooser.showSaveDialog(NotepadFrame2.this);
            if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                return;
            }
            write(saveFileChooser.getSelectedFile());
        });
        fileMenu.addSeparator();

        final JMenuItem printMenuItem = fileMenu.add("Печать");
        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        printMenuItem.addActionListener(e -> {
            try {
                final JTextArea printTextArea = new JTextArea();
                printTextArea.setDocument(textArea.getDocument());
                printTextArea.setFont(docFont);
                printTextArea.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(NotepadFrame2.this, "Ошибка печати", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        fileMenu.addSeparator();
        final JMenuItem exitMenuItem = fileMenu.add("Выход");
        exitMenuItem.addActionListener(e -> NotepadFrame2.this.processWindowEvent(new WindowEvent(NotepadFrame2.this, WindowEvent.WINDOW_CLOSING)));

        // == edit menu
        final JMenu editMenu = menuBar.add(new JMenu("Правка"));
        final JMenuItem undoMenuItem = editMenu.add("Отменить");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.setEnabled(false);
        support.addPropertyChangeListener("documentChanged", evt -> undoMenuItem.setEnabled(true));
        textArea.addPropertyChangeListener("document", evt -> undoMenuItem.setEnabled(false));
        undoMenuItem.addActionListener(e -> {
            if (undoManager.canUndoOrRedo()) {
                undoManager.undoOrRedo();
                textArea.selectAll();
            }
        });
        editMenu.addSeparator();

        final JMenuItem cutMenuItem = editMenu.add("Вырезать");
        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cutMenuItem.setEnabled(false);
        textArea.addCaretListener(e -> cutMenuItem.setEnabled(textArea.getSelectedText() != null));
        cutMenuItem.addActionListener(e -> textArea.cut());

        final JMenuItem copyMenuItem = editMenu.add("Копировать");
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copyMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> copyMenuItem.setEnabled(textArea.getSelectedText() != null));
        copyMenuItem.addActionListener(e -> textArea.copy());

        final JMenuItem pasteMenuItem = editMenu.add("Вставить");
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        pasteMenuItem.setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));
        clipboard.addFlavorListener(e -> pasteMenuItem.setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)));
        pasteMenuItem.addActionListener(e -> textArea.paste());

        final JMenuItem deleteMenuItem = editMenu.add("Удалить");
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> deleteMenuItem.setEnabled(textArea.getSelectedText() != null));
        deleteMenuItem.addActionListener(e -> textArea.copy());

        editMenu.addSeparator();

        final JMenuItem findWithMenuItem = editMenu.add("Найти с помощью Google...");
        findWithMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        findWithMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> findWithMenuItem.setEnabled(textArea.getSelectedText() != null));
        findWithMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://google.com/search?q=" + textArea.getSelectedText()));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });

        final JMenuItem findMenuItem = editMenu.add("Найти... (todo)");
        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        findMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""))); // заменить на DocListener
        findMenuItem.addActionListener(e -> {
            final String search = textArea.getSelectedText();
            // todo open find frame
        });

        final JMenuItem findNextMenuItem = editMenu.add("Найти далее (todo)");
        findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        findNextMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findNextMenuItem.setEnabled(!Objects.equals(textArea.getText(), "")));
        findMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem findPrevMenuItem = editMenu.add("Найти далее (todo)");
        findPrevMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
        findPrevMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findPrevMenuItem.setEnabled(!Objects.equals(textArea.getText(), "")));
        findPrevMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem replaceMenuItem = editMenu.add("Заменить... (todo)");
        replaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        replaceMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem moveToMenuItem = editMenu.add("Перейти...");
        moveToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        moveToMenuItem.addActionListener(e -> {
            final JDialog dialog = new JDialog(NotepadFrame2.this, "Переход на строку", Dialog.ModalityType.DOCUMENT_MODAL);
            final JPanel root = new JPanel();
            root.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            dialog.setContentPane(root);
            dialog.setLayout(new GridBagLayout());
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;

            final JLabel label = new JLabel("Номер строки:");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 2, 5);
            dialog.add(label, gbc);

            final String curLineStr = String.valueOf(getCurrentLine() + 1);

            final JTextField textField = new JTextField(curLineStr, 20);
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(2, 5, 2, 5);
            dialog.add(textField, gbc);

            final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            final JButton okBtn = new JButton("Переход");
            dialog.getRootPane().setDefaultButton(okBtn);
            okBtn.addActionListener(e12 -> {
                try {
                    textArea.setCaretPosition(textArea.getLineStartOffset(Integer.parseInt(textField.getText()) - 1));
                    dialog.dispose();
                } catch (BadLocationException | NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Номер строки превышает общее число строк", APP_NAME + " - Переход на строку", JOptionPane.ERROR_MESSAGE);
                    textField.setText(curLineStr);
                }
            });
            final JButton cancelBtn = new JButton("Отмена");
            cancelBtn.addActionListener(e1 -> dialog.dispose());

            btnPanel.add(okBtn);
            btnPanel.add(cancelBtn);
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.insets = new Insets(2, 0, 5, 0);
            dialog.add(btnPanel, gbc);

            dialog.pack();
            dialog.setLocationRelativeTo(NotepadFrame2.this);
            dialog.setResizable(false);
            dialog.setVisible(true);
        });

        editMenu.addSeparator();

        final JMenuItem selectAllMenuItem = editMenu.add("Выделить все");
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllMenuItem.addActionListener(e -> textArea.selectAll());

        final JMenuItem dateTimeMenuItem = editMenu.add("Время и дата");
        dateTimeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        dateTimeMenuItem.addActionListener(e -> textArea.replaceRange(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")), textArea.getSelectionStart(), textArea.getSelectionEnd()));

        // == format menu
        final JMenu formatMenu = menuBar.add(new JMenu("Формат"));

        final JMenuItem wordWrapMenuItem = formatMenu.add(new JCheckBoxMenuItem("Перенос по словам"));
        wordWrapMenuItem.setSelected(getWordWrap());
        wordWrapMenuItem.addActionListener(e -> {
            setWordWrap(wordWrapMenuItem.isSelected());
            moveToMenuItem.setEnabled(!wordWrapMenuItem.isSelected());
        });

        // todo add choose font menu

        // == view menu
        final JMenu viewMenu = menuBar.add(new JMenu("Вид"));
        final JMenu scaleMenuItem = (JMenu) viewMenu.add(new JMenu("Масштаб"));

        final JMenuItem zoomInMenuItem = scaleMenuItem.add("Увеличить");
        final ActionListener zoomInAction = e -> setFontScale(getFontScale() + 1);
        zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        textArea.registerKeyboardAction(zoomInAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        textArea.registerKeyboardAction(zoomInAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        zoomInMenuItem.addActionListener(zoomInAction);

        final JMenuItem zoomOutMenuItem = scaleMenuItem.add("Уменьшить");
        zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        final ActionListener zoomOutAction = e -> setFontScale(getFontScale() - 1);
        textArea.registerKeyboardAction(zoomOutAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        zoomOutMenuItem.addActionListener(zoomOutAction);

        final JMenuItem restoreScaleMenuItem = scaleMenuItem.add("Восстановить масштаб по умолчанию");
        restoreScaleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        final ActionListener restoreScaleAction = e -> setFontScale(0);
        textArea.registerKeyboardAction(restoreScaleAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        restoreScaleMenuItem.addActionListener(restoreScaleAction);

        final JMenuItem statusBar = viewMenu.add(new JCheckBoxMenuItem("Строка состояния", true));
        statusBar.addActionListener(e -> {/*todo*/});

        // == help menu
        final JMenu helpMenu = menuBar.add(new JMenu("Справка"));
        JMenuItem aboutMenuItem = helpMenu.add("О программе");
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(NotepadFrame2.this, APP_NAME, APP_NAME + ": Сведенья", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(icons.get(1))));

        return menuBar;
    }

    private int getCurrentLine() {
        try {
            return textArea.getLineOfOffset(textArea.getCaretPosition());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return 0;
        }
    }


    private void setFile(File file) {
        this.file = file;
        if (file == null) {
            setDocumentName(DEFAULT_FILE_NAME);
        } else {
            setDocumentName(file.getName());
            try (Reader reader = new FileReader(file)) {
                textArea.read(reader, file);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        setDocumentChanged(true);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        setDocumentChanged(file != null || !textArea.getText().isEmpty());
    }

    private void setDocumentChanged(boolean changed) {
        boolean old = documentChanged;
        this.documentChanged = changed;
        support.firePropertyChange("documentChanged", old, documentChanged);
    }

    private void updateTitle() {
        final StringBuilder title = new StringBuilder();
        if (documentChanged) {
            title.append("*");
        }
        title.append(documentName).append(" - ").append(APP_NAME);
        setTitle(title.toString());
    }

    private void setDocumentName(String documentName) {
        String old = this.documentName;
        this.documentName = documentName;
        support.firePropertyChange("documentName", old, documentName);
    }

    private void write(File file) {
        try (Writer writer = new FileWriter(file)) {
            textArea.write(writer);
            setFile(file);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private JFileChooser saveFileChooser;

    private JFileChooser getSaveFileChooser() {
        if (saveFileChooser == null) {
            saveFileChooser = new JFileChooser() {
                @Override
                public void approveSelection() {
                    final File file = getSelectedFile();
                    if (file.exists()) {
                        final int confirmAnswer = JOptionPane.showConfirmDialog(NotepadFrame2.this, file.getName() + " уже существует.\nВы хотите заменить его?", "Подтвердить сохранение в виде", JOptionPane.YES_NO_OPTION);
                        if (confirmAnswer == JOptionPane.YES_OPTION) {
                            super.approveSelection();
                        }
                    } else {
                        super.approveSelection();
                    }
                }
            };
            saveFileChooser.setFileFilter(new FileNameExtensionFilter("Текстовые документы (*.txt)", "txt"));
        }
        return saveFileChooser;
    }

    boolean wordWrap = P.getBoolean("wordWrap", false);

    private void setWordWrap(boolean wrap) {
        boolean old = wordWrap;
        wordWrap = wrap;
        support.firePropertyChange("wordWrap", old, wrap);
    }

    private boolean getWordWrap() {
        return wordWrap;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("windowClosing");
        if (documentChanged) {
            final int confirmAnswer = JOptionPane.showConfirmDialog(NotepadFrame2.this, "Вы хотите сохранить изменения в файле \"" + documentName + "\"?");
            switch (confirmAnswer) {
                default: {
                    System.out.println("confirmAnswer: " + confirmAnswer);
                }
                case JOptionPane.CANCEL_OPTION: {
                    return;
                }
                case JOptionPane.YES_OPTION: {
                    File file = NotepadFrame2.this.file;
                    if (file == null) {
                        final JFileChooser saveFileChooser = getSaveFileChooser();
                        final int chooseAnswer = saveFileChooser.showSaveDialog(NotepadFrame2.this);
                        if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                            return;
                        }
                        file = saveFileChooser.getSelectedFile();
                    }
                    write(file);
                }
                case JOptionPane.NO_OPTION:
            }
        }
        dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        System.out.println("windowClosed");
        P.putBoolean("wordWrap", getWordWrap());
    }

    private void setFontScale(float scale) {
        float old = this.fontScale;
        this.fontScale = scale;
        support.firePropertyChange("fontScale", old, scale);
    }

    private float getFontScale() {
        return fontScale;
    }
}
