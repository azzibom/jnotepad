package by.azzi.jnotepad;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.NumberFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotepadFrame2 extends JFrame implements DocumentListener {

    private static final String APP_NAME = "Блокнот";
    private static final String DEFAULT_FILE_NAME = "Безымянный";

    private final UndoManager undoManager = new UndoManager();

    {
        undoManager.setLimit(1);
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final JTextArea textArea = new JTextArea();

    {
        support.addPropertyChangeListener("documentChanged", evt -> updateTitle());
        textArea.addPropertyChangeListener("document", evt -> ((Document) evt.getNewValue()).addUndoableEditListener(undoManager));
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

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(600, 400));
//        setIconImages(icons);
        setJMenuBar(menuBar());

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

        final JMenuItem newNotepadFrameMenuItem = fileMenu.add("Новое окно");
        newNotepadFrameMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        newNotepadFrameMenuItem.addActionListener(e -> new NotepadFrame2().setVisible(true));

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
        // todo add pageParamsMenuItem
        // todo add printMenuItem
        fileMenu.addSeparator();
        final JMenuItem exitMenuItem = fileMenu.add("Выход");
        exitMenuItem.addActionListener(e -> NotepadFrame2.this.dispose());

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

        final JMenuItem findMenuItem = editMenu.add("Найти...");
        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        findMenuItem.setEnabled(textArea.getSelectedText() != null);
        textArea.addCaretListener(e -> findMenuItem.setEnabled(textArea.getSelectedText() != null));
        findMenuItem.addActionListener(e -> {
            final String search = textArea.getSelectedText();
            // todo open find frame
        });
        // todo any find or replace menu

        final JMenuItem moveToMenuItem = editMenu.add("Перейти...");
        moveToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        moveToMenuItem.addActionListener(e -> {
            JOptionPane optionPane = new JOptionPane("");

            final int lineCount = textArea.getLineCount();

            try {

                final int value = Integer.parseInt(JOptionPane.showInputDialog(NotepadFrame2.this, "", textArea.getLineOfOffset(textArea.getCaretPosition())));
                textArea.setCaretPosition(textArea.getLineStartOffset(value - 1));
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });

        editMenu.addSeparator();

        final JMenuItem selectAllMenuItem = editMenu.add("Выделить все");
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllMenuItem.addActionListener(e -> textArea.selectAll());

        final JMenuItem dateTimeMenuItem = editMenu.add("Время и дата");
        dateTimeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        dateTimeMenuItem.addActionListener(e -> textArea.replaceRange(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")), textArea.getSelectionStart(), textArea.getSelectionEnd()));

        return menuBar;
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
        StringBuilder title = new StringBuilder();
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
}
