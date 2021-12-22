package by.azzi.jnotepad;

import by.azzi.jnotepad.listeners.DocumentListener;
import by.azzi.jnotepad.listeners.WindowListener;
import by.azzibom.utils.gui.swing.SwingLocalizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class JNotepad extends JFrame implements DocumentListener, WindowListener, Runnable {

    private static final Preferences PREF = Preferences.userNodeForPackage(JNotepad.class);

    public static void main(String[] args) {
        SwingLocalizer.localize();
        Theme.setupTheme();
        SwingUtilities.invokeLater(new JNotepad());
    }

    @Override
    public void run() {
        this.setVisible(true);
    }

    private static final String FONT_SCALE_PROPERTY = "fontScale";
    private static final String WORD_WRAP_PROPERTY = "wordWrap";
    private static final String DOCUMENT_CHANGED_PROPERTY = "documentChanged";
    private static final String DOCUMENT_PROPERTY = "document";
    private static final String DOCUMENT_NAME_PROPERTY = "documentName";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("notepad");
    private static final List<Image> ICONS = loadIcons();
    private static final FileFilter TXT_FILE_FILTER = new FileNameExtensionFilter(BUNDLE.getString("txtFileFilterDesc"), "txt");

    private static final String APP_NAME = BUNDLE.getString("app.name");
    private static final String DEFAULT_FILE_NAME = BUNDLE.getString("document.defaultName");

    private static final ActionListener NEW_NOTEPAD_FRAME_ACTION_LISTENER = e -> SwingUtilities.invokeLater(new JNotepad());

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final JTextArea textArea = new JTextArea();
    private final UndoManager undoManager = new UndoManager();

    private Font docFont = textArea.getFont();
    private float fontScale = 0;
    private File file;
    private String documentName = DEFAULT_FILE_NAME;
    private boolean documentChanged = false;
    private boolean wordWrap = PREF.getBoolean(WORD_WRAP_PROPERTY, false);

    public JNotepad() throws HeadlessException {
        super(DEFAULT_FILE_NAME + " - " + APP_NAME);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(600, 400));
        setIconImages(ICONS);
        setJMenuBar(createMenuBar());

        addWindowListener(this);
        support.addPropertyChangeListener(FONT_SCALE_PROPERTY, evt -> textArea.setFont(docFont.deriveFont(docFont.getSize() + fontScale)));
        support.addPropertyChangeListener(WORD_WRAP_PROPERTY, evt -> {
            Boolean wrap = (Boolean) evt.getNewValue();
            textArea.setWrapStyleWord(wrap);
            textArea.setLineWrap(wrap);
        });
        support.addPropertyChangeListener(DOCUMENT_CHANGED_PROPERTY, evt -> updateTitle());
        support.addPropertyChangeListener(DOCUMENT_NAME_PROPERTY, evt -> updateTitle());

        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.addPropertyChangeListener(DOCUMENT_PROPERTY, evt -> {
            undoManager.discardAllEdits();
            ((Document) evt.getNewValue()).addUndoableEditListener(undoManager);
        });

        textArea.getDocument().addDocumentListener(this);
        textArea.addPropertyChangeListener(DOCUMENT_PROPERTY, evt -> {
            textArea.getDocument().addDocumentListener(JNotepad.this);
            setDocumentChanged(false);
        });

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        pack();
        setLocationByPlatform(true);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        // == file menu
        final JMenu fileMenu = menuBar.add(new JMenu(BUNDLE.getString("menuBar.file")));
        final JMenuItem createMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.create"));
        createMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        createMenuItem.addActionListener(e -> {
            if (documentChanged || file != null) {
                if (saveToFileWithConfirmDialog()) {
                    textArea.setDocument(new PlainDocument());
                    setFile(null);
                }
            }
        });

        final JMenuItem newNotepadMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.newFrame"));
        newNotepadMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        newNotepadMenuItem.addActionListener(NEW_NOTEPAD_FRAME_ACTION_LISTENER);

        final JMenuItem openMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.open"));
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.addActionListener(e -> {
            if (!saveToFileWithConfirmDialog()) {
                return;
            }

            final JFileChooser openFileChooser = getFileChooser();
            final int answer = openFileChooser.showOpenDialog(JNotepad.this);
            if (answer != JFileChooser.APPROVE_OPTION) {
                return;
            }
            setFile(openFileChooser.getSelectedFile());
        });

        final JMenuItem saveMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.save"));
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveMenuItem.addActionListener(e -> saveToFile(false));

        final JMenuItem saveAsMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.saveAs"));
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAsMenuItem.addActionListener(e -> saveToFile(true));
        fileMenu.addSeparator();

        final JMenuItem printMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.print"));
        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        printMenuItem.addActionListener(e -> {
            try {
                final JTextArea printTextArea = new JTextArea();
                printTextArea.setDocument(textArea.getDocument());
                printTextArea.setFont(docFont);
                printTextArea.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(JNotepad.this, "Ошибка отпрвки на печать", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        fileMenu.addSeparator();
        final JMenuItem exitMenuItem = fileMenu.add(BUNDLE.getString("menuBar.file.close"));
        exitMenuItem.addActionListener(e -> JNotepad.this.processWindowEvent(new WindowEvent(JNotepad.this, WindowEvent.WINDOW_CLOSING)));

        // == edit menu
        final JMenu editMenu = menuBar.add(new JMenu(BUNDLE.getString("menuBar.edit")));
        final JMenuItem undoMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.undo"));
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.setEnabled(false);
        support.addPropertyChangeListener(DOCUMENT_CHANGED_PROPERTY, evt -> undoMenuItem.setEnabled(true));
        textArea.addPropertyChangeListener(DOCUMENT_PROPERTY, evt -> undoMenuItem.setEnabled(false));
        undoMenuItem.addActionListener(e -> {
            if (undoManager.canUndoOrRedo()) {
                undoManager.undoOrRedo();
                textArea.selectAll();
            }
        });
        editMenu.addSeparator();

        final JMenuItem cutMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.cut"));
        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cutMenuItem.setEnabled(false);
        textArea.addCaretListener(e -> cutMenuItem.setEnabled(textArea.getSelectedText() != null));
        cutMenuItem.addActionListener(e -> textArea.cut());

        final JMenuItem copyMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.copy"));
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copyMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> copyMenuItem.setEnabled(textArea.getSelectedText() != null));
        copyMenuItem.addActionListener(e -> textArea.copy());

        final JMenuItem pasteMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.paste"));
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        pasteMenuItem.setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));
        clipboard.addFlavorListener(e -> pasteMenuItem.setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)));
        pasteMenuItem.addActionListener(e -> textArea.paste());

        final JMenuItem deleteMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.delete"));
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> deleteMenuItem.setEnabled(textArea.getSelectedText() != null));
        deleteMenuItem.addActionListener(e -> textArea.copy());

        editMenu.addSeparator();

        final JMenuItem findWithMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.searchByGoogle"));
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

        final JMenuItem findMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.find"));
        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        findMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""))); // заменить на DocListener
        findMenuItem.addActionListener(e -> {
//            final String search = textArea.getSelectedText();
            // todo open find frame
        });

        final JMenuItem findNextMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.findNext"));
        findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        findNextMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findNextMenuItem.setEnabled(!Objects.equals(textArea.getText(), "")));
        findMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem findPrevMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.findPrev"));
        findPrevMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
        findPrevMenuItem.setEnabled(!Objects.equals(textArea.getText(), ""));
        textArea.addCaretListener(e -> findPrevMenuItem.setEnabled(!Objects.equals(textArea.getText(), "")));
        findPrevMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem replaceMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.replace"));
        replaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        replaceMenuItem.addActionListener(e -> {/*todo*/});

        final JMenuItem moveToMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.moveTo"));
        moveToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        moveToMenuItem.addActionListener(e -> {
            final JDialog dialog = new JDialog(JNotepad.this, "Переход на строку", Dialog.ModalityType.DOCUMENT_MODAL);
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
            dialog.setLocationRelativeTo(JNotepad.this);
            dialog.setResizable(false);
            dialog.setVisible(true);
        });

        editMenu.addSeparator();

        final JMenuItem selectAllMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.selectAll"));
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllMenuItem.addActionListener(e -> textArea.selectAll());

        final JMenuItem dateTimeMenuItem = editMenu.add(BUNDLE.getString("menuBar.edit.dateTime"));
        dateTimeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        dateTimeMenuItem.addActionListener(e -> textArea.replaceRange(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")), textArea.getSelectionStart(), textArea.getSelectionEnd()));

        // == format menu
        final JMenu formatMenu = menuBar.add(new JMenu(BUNDLE.getString("menuBar.format")));

        final JMenuItem wordWrapMenuItem = formatMenu.add(new JCheckBoxMenuItem(BUNDLE.getString("menuBar.format.wordWrap")));
        wordWrapMenuItem.setSelected(wordWrap);
        wordWrapMenuItem.addActionListener(e -> {
            setWordWrap(wordWrapMenuItem.isSelected());
            moveToMenuItem.setEnabled(!wordWrapMenuItem.isSelected());
        });

        // todo add choose font menu

        // == view menu
        final JMenu viewMenu = menuBar.add(new JMenu(BUNDLE.getString("menuBar.view")));
        final JMenu scaleMenuItem = (JMenu) viewMenu.add(new JMenu(BUNDLE.getString("menuBar.view.scale")));

        final JMenuItem zoomInMenuItem = scaleMenuItem.add(BUNDLE.getString("menuBar.view.scale.zoomIn"));
        final ActionListener zoomInAction = e -> setFontScale(fontScale + 1);
        zoomInMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        textArea.registerKeyboardAction(zoomInAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        textArea.registerKeyboardAction(zoomInAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        zoomInMenuItem.addActionListener(zoomInAction);

        final JMenuItem zoomOutMenuItem = scaleMenuItem.add(BUNDLE.getString("menuBar.view.scale.zoomOut"));
        zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        final ActionListener zoomOutAction = e -> setFontScale(fontScale - 1);
        textArea.registerKeyboardAction(zoomOutAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        zoomOutMenuItem.addActionListener(zoomOutAction);

        final JMenuItem restoreScaleMenuItem = scaleMenuItem.add(BUNDLE.getString("menuBar.view.scale.restoreDefault"));
        restoreScaleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        final ActionListener restoreScaleAction = e -> setFontScale(0);
        textArea.registerKeyboardAction(restoreScaleAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        restoreScaleMenuItem.addActionListener(restoreScaleAction);

        final JMenuItem statusBar = viewMenu.add(new JCheckBoxMenuItem(BUNDLE.getString("menuBar.view.statusBar"), true));
        statusBar.addActionListener(e -> {/*todo*/});

        // == help menu
        final JMenu helpMenu = menuBar.add(new JMenu(BUNDLE.getString("menuBar.help")));
        final JMenuItem aboutMenuItem = helpMenu.add(BUNDLE.getString("menuBar.help.about"));
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(JNotepad.this, APP_NAME, MessageFormat.format(BUNDLE.getString("aboutDialog.title"), APP_NAME), JOptionPane.INFORMATION_MESSAGE, new ImageIcon(ICONS.get(1))));

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


    private void setDocumentChanged(boolean changed) {
        boolean old = documentChanged;
        this.documentChanged = changed;
        support.firePropertyChange(DOCUMENT_CHANGED_PROPERTY, old, documentChanged);
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
        support.firePropertyChange(DOCUMENT_NAME_PROPERTY, old, documentName);
    }

    private void setWordWrap(boolean wrap) {
        boolean old = wordWrap;
        wordWrap = wrap;
        support.firePropertyChange(WORD_WRAP_PROPERTY, old, wrap);
    }

    private void setFontScale(float scale) {
        float old = this.fontScale;
        this.fontScale = scale;
        support.firePropertyChange(FONT_SCALE_PROPERTY, old, scale);
    }


    // == ==

    /**
     * сохранение в файл с предварительным подтверждением
     * вернет false в случает отмены сохранения
     */
    private boolean saveToFileWithConfirmDialog() {
        if (documentChanged) {
            final int confirmAnswer = JOptionPane.showConfirmDialog(JNotepad.this, "Вы хотите сохранить изменения в файле \"" + documentName + "\"?");
            switch (confirmAnswer) {
                default:
                case JOptionPane.CANCEL_OPTION: {
                    return false;
                }
                case JOptionPane.YES_OPTION: {
                    return saveToFile(false);
                }
                case JOptionPane.NO_OPTION:
            }
        }
        return true;
    }


    /**
     * сохраняем файл
     * если передан choose true, то идет обязательный выбор файла из JFileChooser
     * если choose false и был открыт файл выбираем его
     * иначе из JFileChooser
     */
    private boolean saveToFile(boolean choose) {
        File localFile = this.file;
        if (choose || Objects.isNull(this.file)) {
            final JFileChooser saveFileChooser = getApproveFileChooser(this);
            final int chooseAnswer = saveFileChooser.showSaveDialog(this);
            if (chooseAnswer != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            localFile = saveFileChooser.getSelectedFile();
        }

        try (Writer writer = new FileWriter(localFile)) {
            textArea.write(writer);
            setFile(localFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return true;
    }

    // == listeners methods ==

    @Override
    public void insertUpdate(DocumentEvent e) {
        setDocumentChanged(true);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        setDocumentChanged(file != null || !textArea.getText().isEmpty());
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (saveToFileWithConfirmDialog()) {
            dispose();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        PREF.putBoolean(WORD_WRAP_PROPERTY, wordWrap);
    }

    // == static utils

    /**
     * вернем JFileChooser с подтверждением перезаписи файла
     */
    private static JFileChooser getApproveFileChooser(Component parentComponent) {
        final JFileChooser fileChooser = new JFileChooser() {
            @Override
            public void approveSelection() {
                final File file = getSelectedFile();
                if (file.exists()) {
                    final int confirmAnswer = JOptionPane.showConfirmDialog(parentComponent, file.getName() + " уже существует.\nВы хотите заменить его?", "Подтвердить сохранение в виде", JOptionPane.YES_NO_OPTION);
                    if (confirmAnswer == JOptionPane.YES_OPTION) {
                        super.approveSelection();
                    }
                } else {
                    super.approveSelection();
                }
            }
        };
        fileChooser.setFileFilter(TXT_FILE_FILTER);

        return fileChooser;
    }

    private static JFileChooser getFileChooser() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(TXT_FILE_FILTER);
        return fileChooser;
    }

    private static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        try {
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-48.png")));
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-96.png")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return icons;
    }
}
