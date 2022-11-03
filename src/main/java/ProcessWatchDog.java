import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

class ProcessWatchDog implements Runnable {
    private String status = "";
    private ArrayList<AddonAppSet> addonApps;

    @Override
    public void run() {
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                try {
                    if (addonApps != null && addonApps.size() > 0) {
                        Process p = Runtime.getRuntime().exec("cmd /c tasklist");
                        BufferedReader bw = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("GBK")));
                        String str = null;
                        StringBuffer sb = new StringBuffer();
                        while (true) {
                            str = bw.readLine();
                            if (str != null) {
                                sb.append(str.toLowerCase());
                            } else {
                                break;
                            }
                        }
                        status = sb.toString();
                    }
                } catch (IOException e) {
                }

                for (AddonAppSet app : addonApps) {
                    if (app.getPath() != null) {
                        File f = new File(app.getPath());
                        app.updateRunningStatus(isRunning(f.getName()));
                    }
                }
                Thread.currentThread().sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.print("ProcessWatchDog Exception");
        }
    }

    public void setAddonApps(ArrayList<AddonAppSet> apps) {
        this.addonApps = apps;
    }

    public boolean isRunning(String processName) {
        return status.indexOf(processName.toLowerCase()) > 0;
    }
}