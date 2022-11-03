import sun.awt.shell.ShellFolder;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;

public class AddonAppSetPanel implements AddonAppSet.onDataChangeCallback {
    private AddonAppSet appset;
    private JButton btnAppSelect;
    private JCheckBox ckbAppEnable;
    private JLabel tAppName;
    private JPanel panel;
    private JLabel tRunStatus;
    private JLabel btnRemove;

    public AddonAppSetPanel() {
        btnRemove.setVisible(false);
        btnRemove.setForeground(Color.gray);

        btnAppSelect.addActionListener(new FileChooseAction(JFileChooser.FILES_ONLY, "选择程序", ".exe", "可执行文件(*.exe)", new FileChooseAction.FileChooseListener() {
            @Override
            public void onChoose(File file) {
                appset.setPath(file.getPath());
                appset.setEnable(true);
                appset.update();
            }
        }));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (!appset.isEmpty()) {
                    btnRemove.setVisible(true);
                }
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (panel.contains(mouseEvent.getPoint())) {
                    return;
                }
                btnRemove.setVisible(false);
            }
        });
        btnRemove.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                appset.clear();
                repack();
            }
        });
        ckbAppEnable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                appset.setEnable(ckbAppEnable.isSelected());
                appset.update();
            }
        });
    }

    private ImageIcon getExeIcon(File exe) {
        ShellFolder shellFolder = null;
        ImageIcon icon = null;
        try {
            shellFolder = ShellFolder.getShellFolder(exe);
            icon = new ImageIcon(shellFolder.getIcon(true));
            icon.setImage(icon.getImage().getScaledInstance(14, 14, Image.SCALE_DEFAULT));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return icon;
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setData(AddonAppSet appset) {
        this.appset = appset;
        this.appset.setOnDataChangeCallback(this);
        repack();
    }

    @Override
    public void dataChanged(AddonAppSet appset) {
        repack();
    }

    public void repack() {
        // 更新checkbox
        ckbAppEnable.setSelected(appset.isEnable());
        // 更新path信息
        String path = appset.getPath();
        if (!appset.isEmpty()) {
            File exe = new File(appset.getPath());
            if (exe.exists()) {
                tAppName.setText(exe.getName());
                tAppName.setIcon(getExeIcon(exe));
            } else {
                tAppName.setText(String.format("未找到程序 (%s)", exe.getName()));
            }
        } else {
            tAppName.setIcon(null);
            tAppName.setText("");
        }
        // 更新运行状态
        if (appset.getRunStatus() == AddonAppSet.RUNNING) {
            tRunStatus.setText("正在运行");
        } else {
            tRunStatus.setText("");
        }
    }
}
