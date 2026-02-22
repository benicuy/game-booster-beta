package com.gamebooster.real;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessManager {
    
    private Context context;
    private ActivityManager activityManager;
    private PackageManager packageManager;
    
    // Daftar paket penting yang TIDAK BOLEH dimatikan
    private static final Set<String> CRITICAL_PACKAGES = new HashSet<String>() {{
        add("com.android.systemui");
        add("com.android.phone");
        add("com.android.settings");
        add("com.android.launcher");
        add("com.google.android.gms");
        add("com.google.android.gsf");
        add("android");
        add("system");
    }};
    
    // Daftar game populer
    private static final Set<String> GAME_PACKAGES = new HashSet<String>() {{
        add("com.mobile.legends");
        add("com.dts.freefireth");
        add("com.dts.freefiremax");
        add("com.tencent.ig");
        add("com.miHoYo.GenshinImpact");
        add("com.activision.callofduty.shooter");
    }};
    
    public ProcessManager(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.packageManager = context.getPackageManager();
    }
    
    /**
     * MEMBERSIHKAN RAM - MATIKAN PROSES BACKGROUND
     * @return jumlah proses yang dimatikan
     */
    public int killBackgroundProcesses() {
        int killed = 0;
        
        List<ActivityManager.RunningAppProcessInfo> processes = 
            activityManager.getRunningAppProcesses();
        
        if (processes == null || processes.isEmpty()) {
            return 0;
        }
        
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            String packageName = getPackageNameFromProcess(process);
            
            // Skip proses sendiri
            if (packageName.equals(context.getPackageName())) {
                continue;
            }
            
            // Skip proses penting
            if (isCriticalProcess(packageName)) {
                continue;
            }
            
            // Skip proses sistem (UID < 10000)
            if (process.uid < 10000) {
                continue;
            }
            
            // Jangan matikan game yang sedang berjalan
            if (isGameRunning(packageName)) {
                continue;
            }
            
            try {
                // Matikan proses
                android.os.Process.killProcess(process.pid);
                
                // Untuk Android 8.0+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activityManager.killBackgroundProcesses(packageName);
                }
                
                killed++;
                Log.d("ProcessManager", "Killed: " + packageName + " (PID: " + process.pid + ")");
                
            } catch (Exception e) {
                Log.e("ProcessManager", "Error killing: " + e.getMessage());
            }
        }
        
        return killed;
    }
    
    /**
     * Force stop aplikasi non-kritis
     * @return jumlah yang dihentikan
     */
    public int forceStopNonCriticalApps() {
        int stopped = 0;
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return 0;
        }
        
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
        
        for (ApplicationInfo app : apps) {
            String packageName = app.packageName;
            
            // Skip aplikasi sendiri
            if (packageName.equals(context.getPackageName())) {
                continue;
            }
            
            // Skip aplikasi sistem
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }
            
            // Skip aplikasi penting
            if (isCriticalProcess(packageName)) {
                continue;
            }
            
            // Skip game
            if (GAME_PACKAGES.contains(packageName)) {
                continue;
            }
            
            // Cek apakah aplikasi sedang berjalan
            if (isAppRunning(packageName)) {
                try {
                    activityManager.killBackgroundProcesses(packageName);
                    stopped++;
                    Log.d("ProcessManager", "Force stopped: " + packageName);
                } catch (Exception e) {
                    Log.e("ProcessManager", "Error force stopping: " + e.getMessage());
                }
            }
        }
        
        return stopped;
    }
    
    /**
     * Bersihkan task stack
     */
    public int clearAppTasks() {
        int cleared = 0;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
            for (ActivityManager.AppTask task : tasks) {
                try {
                    task.finishAndRemoveTask();
                    cleared++;
                } catch (Exception e) {
                    Log.e("ProcessManager", "Error clearing task: " + e.getMessage());
                }
            }
        }
        
        return cleared;
    }
    
    /**
     * Dapatkan jumlah proses yang berjalan
     */
    public int getRunningProcessesCount() {
        List<ActivityManager.RunningAppProcessInfo> processes = 
            activityManager.getRunningAppProcesses();
        return processes != null ? processes.size() : 0;
    }
    
    /**
     * Dapatkan daftar proses yang berjalan (string)
     */
    public String getRunningProcessesString() {
        StringBuilder sb = new StringBuilder();
        List<ActivityManager.RunningAppProcessInfo> processes = 
            activityManager.getRunningAppProcesses();
        
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                String packageName = getPackageNameFromProcess(process);
                sb.append("• ").append(packageName)
                  .append(" (PID: ").append(process.pid)
                  .append(", Import: ").append(process.importance).append(")\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Cek apakah aplikasi sedang berjalan
     */
    private boolean isAppRunning(String packageName) {
        List<ActivityManager.RunningAppProcessInfo> processes = 
            activityManager.getRunningAppProcesses();
        
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                String procPkg = getPackageNameFromProcess(process);
                if (procPkg.equals(packageName)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Cek apakah game sedang berjalan
     */
    private boolean isGameRunning(String packageName) {
        return GAME_PACKAGES.contains(packageName) && isAppRunning(packageName);
    }
    
    /**
     * Ambil package name dari proses
     */
    private String getPackageNameFromProcess(ActivityManager.RunningAppProcessInfo process) {
        if (process.pkgList != null && process.pkgList.length > 0) {
            return process.pkgList[0];
        }
        return process.processName;
    }
    
    /**
     * Cek apakah proses kritis (tidak boleh dimatikan)
     */
    private boolean isCriticalProcess(String packageName) {
        if (packageName == null) return true;
        
        for (String critical : CRITICAL_PACKAGES) {
            if (packageName.contains(critical)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Baca /proc/meminfo untuk info RAM detail
     */
    public String getProcMemInfo() {
        StringBuilder memInfo = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            while ((line = reader.readLine()) != null) {
                memInfo.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            memInfo.append("Error reading /proc/meminfo");
        }
        return memInfo.toString();
    }
}
