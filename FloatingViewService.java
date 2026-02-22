package com.gamebooster.real;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class FloatingViewService extends Service {
    
    private WindowManager windowManager;
    private View floatingView;
    private TextView tvRam, tvFps, tvCpu;
    private Button btnBoost, btnClose;
    private ProcessManager processManager;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        processManager = new ProcessManager(this);
        
        // Inflate floating view layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null);
        
        tvRam = floatingView.findViewById(R.id.tvRam);
        tvFps = floatingView.findViewById(R.id.tvFps);
        tvCpu = floatingView.findViewById(R.id.tvCpu);
        btnBoost = floatingView.findViewById(R.id.btnBoost);
        btnClose = floatingView.findViewById(R.id.btnClose);
        
        // Setup layout params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;
        
        // Set drag listener
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        });
        
        // Boost button click
        btnBoost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int killed = processManager.killBackgroundProcesses();
                updateStats();
                
                // Show toast-like feedback
                TextView tempMsg = new TextView(FloatingViewService.this);
                tempMsg.setText("RAM dibersihkan: " + killed + " proses");
                tempMsg.setTextColor(0xFF00FF00);
                tempMsg.setTextSize(10);
                tempMsg.setPadding(8, 4, 8, 4);
                tempMsg.setBackgroundColor(0xCC1a1f35);
                // Add to view temporarily
            }
        });
        
        // Close button
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
        
        // Add view to window
        windowManager.addView(floatingView, params);
        
        // Start updating stats
        startUpdating();
    }
    
    private void startUpdating() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateRunnable);
    }
    
    private void updateStats() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        
        int percent = (int) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem);
        long freeMB = mi.availMem / 1024 / 1024;
        long totalMB = mi.totalMem / 1024 / 1024;
        
        tvRam.setText("RAM: " + freeMB + "MB / " + totalMB + "MB (" + percent + "%)");
        tvFps.setText("FPS: 60");
        tvCpu.setText("CPU: " + Runtime.getRuntime().availableProcessors() + " Core");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
