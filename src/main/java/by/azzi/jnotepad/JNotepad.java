package by.azzi.jnotepad;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 */
public class JNotepad {
    private static final ActionListener newNotepadFrameActionListener = e -> notepadFrame().setVisible(true);
    private static final ActionListener setupLightThemeActionListener = e -> Theme.setupTheme(Theme.LIGHT);
    private static final ActionListener setupDarkThemeActionListener = e -> Theme.setupTheme(Theme.DARK);
    private static final List<Image> icons = new ArrayList<>();

    static {
        try {
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-48.png")));
            icons.add(ImageIO.read(ClassLoader.getSystemResource("./img/icons8-spiral-notepad-96.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.setupTheme();
            notepadFrame().setVisible(true);
        });
    }

    private static JFrame notepadFrame() {
        final JFrame frame = new JFrame("Блокнот");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setIconImages(icons);


        frame.setJMenuBar(menuBar(frame));

        final JTextArea textArea = new JTextArea();
        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        frame.add(scrollPane);

        frame.pack();
        frame.setLocationByPlatform(true);

        return frame;
    }

    private static JMenuBar menuBar(JFrame frame) {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = menuBar.add(new JMenu("Файл"));
        fileMenu.add("Создать");
        final JMenuItem newNotepadFrameMI = fileMenu.add("Новое окно");
        newNotepadFrameMI.addActionListener(newNotepadFrameActionListener);
        fileMenu.add("Открыть...");
        fileMenu.add("Сохранить");
        fileMenu.add("Сохранить как...");
        fileMenu.addSeparator();
        fileMenu.add("Параметры страницы");
        fileMenu.add("Печать...");
        fileMenu.addSeparator();
        final JMenuItem closeMI = fileMenu.add("Выход");
        closeMI.addActionListener(e -> frame.dispose());
        final JMenu pravkaMenu = menuBar.add(new JMenu("Правка"));
        pravkaMenu.add("Отменить");
        pravkaMenu.addSeparator();
        pravkaMenu.add("Вырезать");
        pravkaMenu.add("Копировать");
        pravkaMenu.add("Вставить");
        pravkaMenu.add("Удалить");
        pravkaMenu.addSeparator();
        pravkaMenu.add("Поиск с помощью...");
        pravkaMenu.add("Найти...");
        pravkaMenu.add("Найти далее");
        pravkaMenu.add("Найти ранее");
        pravkaMenu.add("Заменить...");
        pravkaMenu.add("Перейти...");
        pravkaMenu.addSeparator();
        pravkaMenu.add("Выделить все");
        pravkaMenu.add("Время и дата");
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
            final JOptionPane optionPane = new JOptionPane(new String[]{"JNotepad 1.0", "by azzi"}, JOptionPane.INFORMATION_MESSAGE);
            optionPane.setIcon(new ImageIcon(icons.get(1)));
            final JDialog dialog = optionPane.createDialog(frame, "Блокнот");
            dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.setVisible(true);
            frame.toFront();
        });

        return menuBar;
    }

}
