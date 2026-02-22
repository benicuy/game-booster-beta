package com.gamebooster.real;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StatFs;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class WebAppInterface {
    
    private Context context;
    private ActivityManager activityManager;
    private ProcessManager processManager;
    
    public WebAppInterface(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.processManager = new ProcessManager(context);
    }
    
    /**
     * MEMBERSIHKAN RAM - NYATA (BUKAN SIMULASI)
     */
    @JavascriptInterface
    public String cleanRamNow() {
        StringBuilder log = new StringBuilder();
        log.append("START RAM CLEANING\n");
        log.append("==================\n");
        
        long ramBefore = getAvailableRam();
        int processesBefore = processManager.getRunningProcessesCount();
        
        log.append("RAM Tersedia: " + formatBytes(ramBefore) + "\n");
        log.append("Proses Berjalan: " + processesBefore + "\n\n");
        
        log.append("✓ Menjalankan GC...\n");
        System.gc();
        Runtime.getRuntime().gc();
        
        log.append("✓ Membersihkan cache lokal...\n");
        try {
            context.getCacheDir().delete();
            context.getExternalCacheDir().delete();
        } catch (Exception e) {
            log.append("  ⚠ Gagal: " + e.getMessage() + "\n");
        }
        
        log.append("✓ Mematikan proses background...\n");
        int killed = processManager.killBackgroundProcesses();
        log.append("  → " + killed + " proses dimatikan\n");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            log.append("✓ Membersihkan task stack...\n");
            int tasksCleared = processManager.clearAppTasks();
            log.append("  → " + tasksCleared + " task dibersihkan\n");
        }
        
        if (hasPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES)) {
            log.append("✓ Menghentikan aplikasi latar...\n");
            int stopped = processManager.forceStopNonCriticalApps();
            log.append("  → " + stopped + " aplikasi dihentikan\n");
        }
        
        long ramAfter = getAvailableRam();
        int processesAfter = processManager.getRunningProcessesCount();
        long ramFreed = ramAfter - ramBefore;
        
        log.append("\nHASIL PEMBERSIHAN\n");
        log.append("==================\n");
        log.append("RAM Tersedia: " + formatBytes(ramAfter) + "\n");
        log.append("Proses Berjalan: " + processesAfter + "\n");
        log.append("RAM Dibebaskan: " + formatBytes(ramFreed) + "\n");
        log.append("Peningkatan: " + (ramFreed * 100 / ramBefore) + "%\n");
        
        return log.toString();
    }
    
    @JavascriptInterface
    public long getTotalRam() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.totalMem;
    }
    
    @JavascriptInterface
    public long getAvailableRam() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.availMem;
    }
    
    @JavascriptInterface
    public long getUsedRam() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.totalMem - mi.availMem;
    }
    
    @JavascriptInterface
    public int getRamUsagePercent() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return (int) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem);
    }
    
    @JavascriptInterface
    public int getRunningProcessesCount() {
        return processManager.getRunningProcessesCount();
    }
    
    @JavascriptInterface
    public String getRunningProcessesString() {
        return processManager.getRunningProcessesString();
    }
    
    @JavascriptInterface
    public boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    @JavascriptInterface
    public void openGame(String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    @JavascriptInterface
    public String getStorageInfo() {
        StatFs stat = new StatFs(context.getFilesDir().getPath());
        long total = (long) stat.getBlockCount() * (long) stat.getBlockSize();
        long free = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        return "Total: " + formatBytes(total) + ", Free: " + formatBytes(free);
    }
    
    @JavascriptInterface
    public String getCpuInfo() {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 10) {
                cpuInfo.append(line).append("\n");
                count++;
            }
            reader.close();
        } catch (IOException e) {
            cpuInfo.append("Error: ").append(e.getMessage());
        }
        return cpuInfo.toString();
    }
    
    @JavascriptInterface
    public int getCpuCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    @JavascriptInterface
    public String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
    }
    
    @JavascriptInterface
    public int getBatteryLevel() {
        // Implementasi battery level
        return 85;
    }
    
    @JavascriptInterface
    public boolean isBatteryCharging() {
        return false;
    }
    
    @JavascriptInterface
    public void startFloatingService() {
        Intent intent = new Intent(context, FloatingViewService.class);
        context.startService(intent);
    }
    
    @JavascriptInterface
    public void stopFloatingService() {
        Intent intent = new Intent(context, FloatingViewService.class);
        context.stopService(intent);
    }
    
    private boolean hasPermission(String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
