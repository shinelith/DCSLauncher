import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileChooseAction implements ActionListener {
    public interface FileChooseListener {
        void onChoose(File file);
    }

    private FileChooseListener fileChooseListener;
    private int fileSelectionMode;
    private String fileFilterDescription;
    private String dialogTitle;
    private String fileFilterAcceptExt;

    public FileChooseAction(int mode, String dialogTitle, String ext, String fileFilterDescription, FileChooseListener fileChooseListener) {
        this.fileSelectionMode = mode;
        this.fileFilterAcceptExt = ext;
        this.dialogTitle = dialogTitle;
        this.fileFilterDescription = fileFilterDescription;
        this.fileChooseListener = fileChooseListener;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(fileSelectionMode);
        if (this.fileFilterAcceptExt != null) {
            jfc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.getName().contains(fileFilterAcceptExt) || f.isDirectory()) {
                        return true;
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return fileFilterDescription;
                }
            });
        }
        jfc.showDialog(new JLabel(), dialogTitle);
        File file = jfc.getSelectedFile();
        if (file != null) {
            if (fileChooseListener != null) {
                fileChooseListener.onChoose(file);
            }
        }
    }
}
