import com.formdev.flatlaf.FlatDarkLaf;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.VerRsrc.VS_FIXEDFILEINFO;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.win32.Version;
import org.ini4j.Wini;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainForm {
    private static int ADDON_APP_COUNT = 5;
    private static String PATH_INI = "DCSLauncher.ini";
    private static String DCS_UPDATE_UPDATE = "update";
    private static String DCS_UPDATE_REPAIR = "repair";

    private static String PATH_DCS_UPDATER = "/bin/DCS_updater.exe";
    private ArrayList<AddonAppSet> addonApps = new ArrayList<AddonAppSet>();
    private ProcessWatchDog watchDog;
    private Thread watchDogThread;
    private JFrame frame;
    private JPanel panel;
    private JButton btnStartDCS;
    private JButton btnUpdate;
    private JTabbedPane tabbedPane1;
    private JComboBox selectMonitorSetup;
    private JComboBox selectResolution;
    private JCheckBox ckb_vr;
    private JPanel tabViewDisplay;
    private JPanel tabViewAddon;
    private JPanel tabTools;
    private JButton btnDCSSite;
    private JButton btnDCSPathSelect;
    private JTextField tfDCSPath;
    private JTextField tfDCSSavedPath;
    private JButton btnDCSSavedPathSelect;
    private JButton btnDCSRepair;
    private JButton btnDCSUpdateSite;
    private JTextField tfUpdateArgs;
    private JButton btnUpdateWithArgs;
    private JButton btnDCSHistory;
    private JTextArea taDesc;
    private HashMap<String, String> options;
    private LuaValue luaOptions;
    private Wini cfg;

    private String dcsVersionCode;

    public void loadDcsMultiMonitorSetup() {
        for (ActionListener listener : selectMonitorSetup.getActionListeners()) {
            selectMonitorSetup.removeActionListener(listener);
        }
        selectMonitorSetup.removeAllItems();

        if (options != null) {
            // 获取可选的显示配置文件
            List<SelectItem> ms = getMonitorSetup();
            if (ms != null) {
                for (SelectItem item : ms) {
                    selectMonitorSetup.addItem(item);
                }
                String currentMonitorSetup = options.get("multiMonitorSetup");
                for (SelectItem item : ms) {
                    if (item.getExtra().equals(currentMonitorSetup)) {
                        selectMonitorSetup.setSelectedItem(item);
                        break;
                    }
                }
            }
        }
    }

    public void loadSystemResolution() {
        for (ActionListener listener : selectResolution.getActionListeners()) {
            selectResolution.removeActionListener(listener);
        }
        selectResolution.removeAllItems();

        if (options != null) {
            // 获取系统显示器分辨率
            List<SelectItem> resolution = new ArrayList<SelectItem>();
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int screen_no = 0;
            int width_max = 0;
            int height_max = 0;
            int width_total = 0;
            int height_total = 0;
            for (GraphicsDevice screen : devices) {
                screen_no++;  //显示器序号
                DisplayMode dm = screen.getDisplayMode();
                // 计算最大拼合分辨率
                width_max = Math.max(width_max, dm.getWidth());
                height_max = Math.max(height_max, dm.getHeight());
                width_total += dm.getWidth();
                height_total += dm.getHeight();
                SelectItem item = new SelectItem(String.format("显示器%s %sx%s", screen_no, dm.getWidth(), dm.getHeight()), new Screen(dm.getWidth(), dm.getHeight()));
                resolution.add(item);
            }
            // 如果显示器多余2个，则显示显示器组合
            if (devices.length >= 2) {
                Screen h = new Screen(width_max, height_total);
                resolution.add(new SelectItem(String.format("显示器纵置 %sx%s", h.getWidth(), h.getHeight()), h));
                Screen l = new Screen(width_total, height_max);
                resolution.add(new SelectItem(String.format("显示器横置 %sx%s", l.getWidth(), l.getHeight()), l));
            }
            for (SelectItem item : resolution) {
                selectResolution.addItem(item);
            }
            // 显示当前所选的分辨率选项
            String currentWidth = options.get("width");
            String currentHeight = options.get("height");
            Screen currentScreen = new Screen(Integer.parseInt(currentWidth), Integer.parseInt(currentHeight));
            for (SelectItem item : resolution) {
                if (item.getExtra().equals(currentScreen)) {
                    selectResolution.setSelectedItem(item);
                    break;
                }
            }
        }
    }

    public void loadDcsSavedLuaOptions() throws Exception {
        // 读取DCS的lua配置文件
        String path_dcs_options = getPathDcsSavedGame() + "/config/options.lua";
        if (new File(path_dcs_options).exists()) {
            Globals globals = JsePlatform.standardGlobals();
            globals.loadfile(path_dcs_options).call();
            luaOptions = globals.get("options");
            if (luaOptions == null) {
                throw new Exception("读取DCS配置文件失败!\n");
            }
            // 从配置文件中读参数
            options = new HashMap<String, String>();
            options.put("multiMonitorSetup", luaOptions.get("graphics").get("multiMonitorSetup").toString());
            options.put("width", luaOptions.get("graphics").get("width").toString());
            options.put("height", luaOptions.get("graphics").get("height").toString());
            options.put("vr", luaOptions.get("VR").get("enable").toString());
        } else {
            options = null;
            luaOptions = null;
        }
    }

    private void loadDcsVersionCode() throws Exception{
        String dcsPath = getPathDcs();
        if (dcsPath != null) {
            String dcsExeName = "dcs.exe";
            String dcsExe = dcsPath + "/bin/" + dcsExeName;
            File f = new File(dcsExe);
            if (f.exists()) {

                IntByReference dwDummy = new IntByReference();
                dwDummy.setValue(0);

                int versionlength = Version.INSTANCE.GetFileVersionInfoSize(dcsExe, dwDummy);
                byte[] bufferarray = new byte[versionlength];
                Pointer lpData = new Memory(bufferarray.length);
                PointerByReference lplpBuffer = new PointerByReference();
                IntByReference puLen = new IntByReference();

                boolean fileInfoResult =
                        com.sun.jna.platform.win32.Version.INSTANCE.GetFileVersionInfo(
                                dcsExe, 0, versionlength, lpData);

                boolean verQueryVal =
                        com.sun.jna.platform.win32.Version.INSTANCE.VerQueryValue(
                                lpData, "\\", lplpBuffer, puLen);

                VS_FIXEDFILEINFO lplpBufStructure = new VS_FIXEDFILEINFO(lplpBuffer.getValue());
                lplpBufStructure.read();

                int v1 = (lplpBufStructure.dwFileVersionMS).intValue() >> 16;
                int v2 = (lplpBufStructure.dwFileVersionMS).intValue() & 0xffff;
                int v3 = (lplpBufStructure.dwFileVersionLS).intValue() >> 16;
                int v4 = (lplpBufStructure.dwFileVersionLS).intValue() & 0xffff;

                this.dcsVersionCode =  String.valueOf(v1) + "." + String.valueOf(v2) + "." + String.valueOf(v3) + "." + String.valueOf(v4);
            }
        }
    }

    public void reloadDcsOptions() throws Exception {
        // 加载保存的游戏lua配置
        loadDcsSavedLuaOptions();
        // 加载多显示器配置
        loadDcsMultiMonitorSetup();
        // 加载系统分辨率
        loadSystemResolution();
        // 显示配置文件下拉框响应
        selectMonitorSetup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectItem item = (SelectItem) selectMonitorSetup.getSelectedItem();
                luaOptions.get("graphics").set("multiMonitorSetup", (String) item.getExtra());
            }
        });
        // 分辨率下拉框响应
        selectResolution.addActionListener(actionEvent -> {
            SelectItem item = (SelectItem) selectResolution.getSelectedItem();
            Screen screen = (Screen) item.getExtra();
            luaOptions.get("graphics").set("width", screen.getWidth());
            luaOptions.get("graphics").set("height", screen.getHeight());
            luaOptions.get("graphics").set("aspect", screen.getAspect());
        });
        // VR选项
        if (options != null) {
            String vr_enable = options.get("vr");
            if (vr_enable.equals("true")) {
                ckb_vr.setSelected(true);
            } else {
                ckb_vr.setSelected(false);
            }
            ckb_vr.addActionListener(actionEvent -> {
                luaOptions.get("VR").set("enable", ckb_vr.isSelected() ? LuaValue.TRUE : LuaValue.FALSE);
            });
        }
    }

    public MainForm(Wini cfg) throws Exception {
        this.cfg = cfg;
        loadDcsVersionCode();
        // 显示页
        if (getPathDcsSavedGame() != null) {
            reloadDcsOptions();
        }
        // 自动启动页
        BoxLayout boxLayout = new BoxLayout(tabViewAddon, BoxLayout.Y_AXIS);
        tabViewAddon.setLayout(boxLayout);
        for (int i = 0; i < this.ADDON_APP_COUNT; i++) {
            AddonAppSet appset = new AddonAppSet();
            appset.setCfg(this.cfg);
            String addonAppID = "AddonApp" + i;
            appset.setId(addonAppID);
            appset.setPath(cfg.get(addonAppID, "path"));
            appset.setEnable(Boolean.parseBoolean(cfg.get(addonAppID, "enable")));
            addonApps.add(appset);

            AddonAppSetPanel addonPanel = new AddonAppSetPanel();
            addonPanel.setData(appset);
            tabViewAddon.add(addonPanel.getPanel());
        }
        // DCS 目录设置
        tfDCSPath.setText(getPathDcs());
        btnDCSPathSelect.addActionListener(new FileChooseAction(JFileChooser.DIRECTORIES_ONLY, "选择目录", null, null, file -> {
            String dcsPath = file.getPath();
            tfDCSPath.setText(dcsPath);
            cfg.put("DCS", "path", dcsPath);
            try {
                cfg.store();
            } catch (IOException e) {
            }
        }));
        // Save Game目录设置
        tfDCSSavedPath.setText(getPathDcsSavedGame());
        btnDCSSavedPathSelect.addActionListener(new FileChooseAction(JFileChooser.DIRECTORIES_ONLY, "选择目录", null, null, file -> {
            String dcsSavedGamePath = file.getPath();
            tfDCSSavedPath.setText(dcsSavedGamePath);
            cfg.put("DCS", "pathSavedGame", dcsSavedGamePath);
            try {
                cfg.store();
            } catch (IOException e) {
            }
            try {
                reloadDcsOptions();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }));
        // ED官网
        btnDCSSite.addActionListener(actionEvent -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://www.digitalcombatsimulator.com/en/"));
            } catch (IOException e) {
            }
        });
        // DCS版本发布页
        btnDCSUpdateSite.addActionListener(actionEvent -> {
            try {
                Desktop.getDesktop().browse(URI.create("http://updates.digitalcombatsimulator.com/"));
            } catch (IOException e) {
            }
        });
        // dcs历史版本
        btnDCSHistory.addActionListener(actionEvent-> {
            try {
                Desktop.getDesktop().browse(URI.create("https://www.digitalcombatsimulator.com/en/news/changelog/openbeta/"));
            } catch (IOException e) {
            }
        });
        // dcs 启动
        String text = "<html><body>启动DCS <span style=\"color:#999999\">("+this.dcsVersionCode+")</span></body></html>";
        btnStartDCS.setText(text);
        btnStartDCS.addActionListener(actionEvent -> {
            boolean saveSuccess = saveDCSOptionsLua();
            if (saveSuccess) {
                run_addon();
                run_dcs();
                System.exit(0);
            }
        });
        // dcs 更新
        btnUpdate.addActionListener(actionEvent -> {
            run_dcs_update(DCS_UPDATE_UPDATE);
        });
        // dcs 修复
        btnDCSRepair.addActionListener(actionEvent -> {
            run_dcs_update(DCS_UPDATE_REPAIR);
        });
        // dcs 更新带指定参数
        btnUpdateWithArgs.addActionListener(actionEvent -> {
            String arg = tfUpdateArgs.getText();
            if(arg != null || !"".equals(arg)){
                run_dcs_update(arg);
            }
        });
    }

    /**
     * 获取文件扩展名
     *
     * @param filename
     * @return
     */
    private String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    /**
     * 去掉文件扩展名
     *
     * @param filename
     * @return
     */
    private String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    /**
     * 从dcs获取显示器配置名称
     */
    private List<SelectItem> getMonitorSetup() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        File path = new File(getPathDcsSavedGame() + "/Config/MonitorSetup");
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if ("lua".equals(getExtensionName(file.getName()))) {
                    String profile = getFileNameNoEx(file.getName());
                    items.add(new SelectItem(profile, profile.toLowerCase()));
                }
            }
        }
        path = new File(getPathDcs() + "/Config/MonitorSetup");
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if ("lua".equals(getExtensionName(file.getName()))) {
                    String profile = getFileNameNoEx(file.getName());
                    items.add(new SelectItem(profile, profile.toLowerCase()));
                }
            }
        }
        return items;
    }

    /**
     * 启动dcs updater
     *
     * @return
     * @throws IOException
     */
    private void run_dcs_update(String arg) {
        String dcsPath = getPathDcs();
        if (dcsPath != null) {
            String dcsUpdater = dcsPath + PATH_DCS_UPDATER;
            File f = new File(dcsUpdater);
            if (f.exists()) {
                try {
                    Runtime.getRuntime().exec("cmd /c \"" + dcsUpdater + "\" " + arg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(panel, "未找到DCS更新程序");
            }
        } else {
            JOptionPane.showMessageDialog(panel, "未设置DCS主目录");
        }
    }

    /**
     * 自动启动
     *
     * @return
     * @throws IOException
     */
    private void run_addon() {
        for (int i = 0; i < ADDON_APP_COUNT; i++) {
            String addonID = "AddonApp" + i;
            String addonPath = cfg.get(addonID, "path");
            String addonEnable = cfg.get(addonID, "enable");
            if (addonPath != null && addonEnable != null && Boolean.parseBoolean(addonEnable)) {
                File exe = new File(addonPath);
                if (exe.exists()) {
                    String exeName = exe.getName();
                    if (watchDog.isRunning(exeName) == false) {
                        try {
                            Runtime runtime = Runtime.getRuntime();
                            runtime.exec("cmd /c \"" + addonPath + "\"", null, exe.getParentFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("skip running " + exe.getName());
                    }
                }
            }
        }
    }

    /**
     * 启动dcs
     *
     * @return
     * @throws IOException
     */
    private void run_dcs() {
        String dcsPath = getPathDcs();
        if (dcsPath != null) {
            String dcsExeName = "dcs.exe";
            String dcsExe = dcsPath + "/bin/" + dcsExeName;
            File f = new File(dcsExe);
            if (f.exists()) {
                if (watchDog.isRunning(dcsExeName) == false) {
                    try {
                        Runtime.getRuntime().exec("cmd /c \"" + dcsExe + "\"");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "未找到DCS主程序");
            }
        } else {
            JOptionPane.showMessageDialog(null, "未设置DCS主目录");
        }
    }

    /**
     * 保存为lua table
     *
     * @param str    StringBuffer
     * @param indent 缩进字符串
     * @param key    键
     * @param value  值
     * @param parent 父
     */
    private void toLuaCode(StringBuffer str, String indent, String key, LuaValue value, LuaValue parent) {
        if (value.istable()) {
            str.append(indent).append(luaKey(key, parent)).append("{\n");
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = value.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                LuaValue v = n.arg(2);
                toLuaCode(str, indent + "\t", k.toString(), v, value);
            }
            str.append(indent).append("}");
            if (parent != null && parent.istable()) {
                str.append(",\n");
            }
        } else if (value.isnumber()) {
            str.append(indent).append(luaKey(key, parent));
            str.append(value.toString()).append(",\n");
        } else if (value.isstring()) {
            str.append(indent).append(luaKey(key, parent));
            str.append("\"" + value.toString() + "\",\n");
        } else {
            str.append(indent).append(luaKey(key, parent));
            str.append(value.toString()).append(",\n");
        }
    }

    private String luaKey(String key, LuaValue parent) {
        if (parent != null && parent.istable()) {
            return "[\"" + key + "\"] = ";
        } else {
            return key + " = ";
        }
    }

    /**
     * 获取DCS主目录
     *
     * @return
     */
    public String getPathDcs() {
        return this.cfg.get("DCS", "path");
    }

    /**
     * 获取DCS保存的游戏目录
     *
     * @return
     */
    public String getPathDcsSavedGame() {
        return this.cfg.get("DCS", "pathSavedGame");
    }

    /**
     * 写入dcs options.lua
     *
     * @return
     */
    private boolean saveDCSOptionsLua() {
        File configFile = new File(getPathDcsSavedGame() + "/Config/options.lua");
        FileWriter writer = null;
        boolean success = false;
        try {
            writer = new FileWriter(configFile, false);
            StringBuffer code = new StringBuffer();
            toLuaCode(code, "", "options", luaOptions, null);
            writer.write(code.toString());
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
        return success;
    }

    /**
     * 启动进程监测
     */
    public void startWatchDog() {
        watchDog = new ProcessWatchDog();
        watchDog.setAddonApps(addonApps);
        watchDogThread = new Thread(watchDog);
        watchDogThread.start();
    }

    /**
     * 停止进程监测
     */
    public void stopWatchDog() {
        if (watchDogThread != null) {
            watchDogThread.interrupt();
        }
    }

    public static void main(String[] args) throws IOException {
        // 初始化配置文件
        File ini = new File(PATH_INI);
        Wini cfg = new Wini();
        if (!ini.exists()) {
            ini.createNewFile();
        } else {
            cfg.load(ini);
        }
        cfg.setFile(ini);

        // UI initial
        FlatDarkLaf.install();
        MainForm main = null;
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            main = new MainForm(cfg);
            JFrame frame = new JFrame("DCS World Launcher");
            main.frame = frame;
            frame.getRootPane().setWindowDecorationStyle(2);
            frame.setContentPane(main.panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            Point point = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            frame.setBounds(point.x - frame.getWidth() / 2, point.y - frame.getHeight() / 2, frame.getWidth(), frame.getHeight());
            URL resource = main.getClass().getResource("icon.png");
            frame.setIconImage(ImageIO.read(resource));
            frame.setVisible(true);
            main.startWatchDog();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        } finally {
        }
    }

}
