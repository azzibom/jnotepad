package by.azzi.jnotepad;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotepadFrame extends JFrame implements by.azzi.jnotepad.DocumentListener {

    private static final Logger LOG = Logger.getLogger(NotepadFrame.class.getName());

    private static final ActionListener newNotepadFrameActionListener = e -> new NotepadFrame().setVisible(true);
    private static final ActionListener setupLightThemeActionListener = e -> Theme.setupTheme(Theme.LIGHT);
    private static final ActionListener setupDarkThemeActionListener = e -> Theme.setupTheme(Theme.DARK);

    private static final List<Image> icons = new ArrayList<>();

    static {
        try {
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-48.png")));
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-96.png")));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "load icons error", ex);
        }
    }

    private static final JFileChooser fileChooser = new JFileChooser();

    static {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Текстовые документы (*.txt)", "txt"));
    }

    private static final String APP_NAME = "Блокнот";
    private static final String DEFAULT_DOCUMENT_NAME = "Безымянный";
    private String documentName = DEFAULT_DOCUMENT_NAME;


    private final UndoManager undoManager = new UndoManager();
    {
        undoManager.setLimit(1);
    }

    private final JTextArea textArea = new JTextArea();

    {
        textArea.getDocument().addDocumentListener(this);
        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.addPropertyChangeListener("document", evt -> {
            File file = (File) ((Document) evt.getNewValue()).getProperty(Document.StreamDescriptionProperty);
            if (file == null) {
                NotepadFrame.this.setTitle(" - Блокнот");
            } else {
                NotepadFrame.this.setTitle(file.getName() + " - Блокнот");
            }
        });
        textArea.addPropertyChangeListener("document", evt -> ((Document) evt.getNewValue()).addDocumentListener(this));
        textArea.addPropertyChangeListener("document", evt -> ((Document) evt.getNewValue()).addUndoableEditListener(undoManager));
    }

    public NotepadFrame() throws HeadlessException {
        super(DEFAULT_DOCUMENT_NAME + " - " + APP_NAME);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(600, 400));
        setIconImages(icons);
        setJMenuBar(menuBar());

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        pack();
        setLocationByPlatform(true);
        System.out.println(Arrays.toString(textArea.getRegisteredKeyStrokes()));

    }

    private JMenuBar menuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = menuBar.add(new JMenu("Файл"));
        final JMenuItem createMI = fileMenu.add("Создать");
        createMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        createMI.addActionListener(e -> {
            if (undoManager.canUndoOrRedo()) {
                File file = (File) textArea.getDocument().getProperty(Document.StreamDescriptionProperty);
                final int answer = JOptionPane.showConfirmDialog(NotepadFrame.this, "Вы хотите сохранить изменения в файле \"" + (file == null ? "Безымянный" : file.getAbsolutePath()) + "\"?");
                if (answer == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (answer == JOptionPane.YES_OPTION) {
                    if (file == null) {
                        final int ans = fileChooser.showSaveDialog(NotepadFrame.this);
                        if (ans == JFileChooser.APPROVE_OPTION) {
                            file = fileChooser.getSelectedFile();
                        } else {
                            return;
                        }
                    }
                    write(file);
                }
                textArea.setDocument(textArea.getUI().getEditorKit(textArea).createDefaultDocument());
            }
        });
        final JMenuItem newNotepadFrameMI = fileMenu.add("Новое окно");
        newNotepadFrameMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        newNotepadFrameMI.addActionListener(newNotepadFrameActionListener);
        final JMenuItem openMI = fileMenu.add("Открыть...");
        openMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMI.addActionListener(e -> {
            int ans = fileChooser.showOpenDialog(NotepadFrame.this);
            if (ans == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                read(file);
            }
        });
        final JMenuItem saveMI = fileMenu.add("Сохранить");
        saveMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveMI.addActionListener(e -> {
            final File file = (File) textArea.getDocument().getProperty(Document.StreamDescriptionProperty);
            if (file == null) {
                // todo call saveAs
                return;
            }
            write(file);
        });
        final JMenuItem saveAsMI = fileMenu.add("Сохранить как...");
        saveAsMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        fileMenu.addSeparator();
        fileMenu.add("Параметры страницы");
        final JMenuItem printMI = fileMenu.add("Печать...");
        printMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        fileMenu.addSeparator();
        final JMenuItem closeMI = fileMenu.add("Выход");
        closeMI.addActionListener(e -> NotepadFrame.this.dispose());

        final JMenu pravkaMenu = menuBar.add(new JMenu("Правка"));
        final JMenuItem cancelMI = pravkaMenu.add("Отменить");
        cancelMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        pravkaMenu.addSeparator();
        final JMenuItem cutMI = pravkaMenu.add("Вырезать");
        cutMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        final JMenuItem copyMI = pravkaMenu.add("Копировать");
        copyMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
        final JMenuItem pasteMI = pravkaMenu.add("Вставить");
        pasteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        final JMenuItem removeMI = pravkaMenu.add("Удалить");
        removeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        pravkaMenu.addSeparator();
        final JMenuItem findWithMI = pravkaMenu.add("Поиск с помощью...");
        final JMenuItem findMI = pravkaMenu.add("Найти...");
        findMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        final JMenuItem findNextMI = pravkaMenu.add("Найти далее");
        findNextMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        final JMenuItem findPrevMI = pravkaMenu.add("Найти ранее");
        findPrevMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK));
        final JMenuItem replaceMI = pravkaMenu.add("Заменить...");
        replaceMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
        final JMenuItem moveToMI = pravkaMenu.add("Перейти...");
        moveToMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
        pravkaMenu.addSeparator();
        final JMenuItem selectAllMI = pravkaMenu.add("Выделить все");
        selectAllMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        selectAllMI.addActionListener(e -> textArea.selectAll());
        final JMenuItem dateTimeMI = pravkaMenu.add("Время и дата");
        dateTimeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        dateTimeMI.addActionListener(e -> textArea.insert(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")), textArea.getCaretPosition()));
        final JMenu formatMenu = menuBar.add(new JMenu("Формат"));
        formatMenu.add(new JCheckBoxMenuItem("Перенос по словам"));
        formatMenu.add("Шрифт...");
        final JMenu vidMenu = menuBar.add(new JMenu("Вид"));
        final JMenu themeMI = (JMenu) vidMenu.add(new JMenu("Тема"));
        final JRadioButtonMenuItem lightThemeMI = new JRadioButtonMenuItem("Светлая");
        lightThemeMI.addActionListener(setupLightThemeActionListener);
        final JRadioButtonMenuItem darkThemeMI = new JRadioButtonMenuItem("Темная");
        darkThemeMI.addActionListener(setupDarkThemeActionListener);
        final ButtonGroup themeButtonGroup = new ButtonGroup();
        themeButtonGroup.add(lightThemeMI);
        themeButtonGroup.add(darkThemeMI);
        themeButtonGroup.setSelected(Theme.isCurrentDark() ? darkThemeMI.getModel() : lightThemeMI.getModel(), true);
        themeMI.add(lightThemeMI);
        themeMI.add(darkThemeMI);

        final JMenu masshtabMI = (JMenu) vidMenu.add(new JMenu("Масштаб"));
        masshtabMI.add("Увеличить");
        masshtabMI.add("Уменьшить");
        masshtabMI.add("Восстановить массштаб по умолчанию");
        vidMenu.add(new JCheckBoxMenuItem("Строка состояния"));
        final JMenu helpMenu = menuBar.add(new JMenu("Справка"));
        final JMenuItem aboutMI = helpMenu.add("О программе");
        aboutMI.addActionListener(e -> {
            final JOptionPane optionPane = new JOptionPane(new String[]{"JNotePad 0.1", "by azzi",}, JOptionPane.INFORMATION_MESSAGE);
            optionPane.setIcon(new ImageIcon(icons.get(1)));
            final JDialog dialog = optionPane.createDialog(NotepadFrame.this, "Блокнот");
            dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.setVisible(true);
            NotepadFrame.this.toFront();
        });

        return menuBar;
    }

    private void write(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            textArea.write(writer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void read(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            textArea.read(reader, file);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        NotepadFrame.this.setTitle("*" + documentName);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (e.getDocument().getProperty(Document.StreamDescriptionProperty) == null
                && textArea.getText().isEmpty()) {
            NotepadFrame.this.setTitle(documentName);
        }
    }

    boolean documentUpdated = true;

    private void update(DocumentEvent e) {
        if (e.getType() == DocumentEvent.EventType.REMOVE
                && e.getDocument().getProperty(Document.StreamDescriptionProperty) == null) {
            if (textArea.getText().isEmpty()) {
                NotepadFrame.this.setTitle(documentName);
                documentUpdated = false;
                return;
            }
        }
        if (!documentUpdated) {
            NotepadFrame.this.setTitle("*" + documentName);
            documentUpdated = true;
        }
    }
}
