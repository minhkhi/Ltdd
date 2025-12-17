package com.example.ltdd;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Background extends Service {

    private DatabaseReference database;
    private static final String CHANNEL_ID = "smart_garden_background_channel";
    private static final int NOTIFICATION_ID = 2; // ID khác với MainActivity để không đè nhau

    // Ngưỡng mặc định
    private double tempThreshold = 100.0;
    private int humidThreshold = 0;

    // Cờ trạng thái
    private boolean isTempAlertShown = false;
    private boolean isHumidAlertShown = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Khởi tạo Firebase trong Service
        database = FirebaseDatabase.getInstance("https://smart-garden-94677-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("devices/device_01");

        // Lắng nghe dữ liệu kể cả khi App đóng
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double temp = snapshot.child("temp").getValue(Double.class);
                Integer humid = snapshot.child("humid").getValue(Integer.class);

                if (snapshot.hasChild("config")) {
                    Double tThresh = snapshot.child("config/temp_threshold").getValue(Double.class);
                    Integer hThresh = snapshot.child("config/humid_threshold").getValue(Integer.class);
                    if (tThresh != null) tempThreshold = tThresh;
                    if (hThresh != null) humidThreshold = hThresh;
                }

                checkAlert(temp, humid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void checkAlert(Double temp, Integer humid) {
        if (temp == null || humid == null) return;

        boolean isHighTemp = temp > tempThreshold;
        boolean isLowHumid = humid < humidThreshold;

        boolean needAlertTemp = isHighTemp && !isTempAlertShown;
        boolean needAlertHumid = isLowHumid && !isHumidAlertShown;

        if (needAlertTemp || needAlertHumid) {
            String title = "CẢNH BÁO";
            String message = "";

            if (needAlertTemp && needAlertHumid) {
                message = "Nhiệt độ cao (" + temp + "°C) và Độ ẩm thấp (" + humid + "%)!";
                isTempAlertShown = true;
                isHumidAlertShown = true;
            } else if (needAlertTemp) {
                message = "Nhiệt độ quá cao (" + temp + "°C)!";
                isTempAlertShown = true;
            } else if (needAlertHumid) {
                message = "Độ ẩm đất quá thấp (" + humid + "%)!";
                isHumidAlertShown = true;
            }
            showNotification(title, message);
        }

        if (!isHighTemp) isTempAlertShown = false;
        if (!isLowHumid) isHumidAlertShown = false;
    }

    private void showNotification(String title, String content) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Khi bấm vào thông báo sẽ mở lại App
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Quan trọng: Mở app khi click
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Smart Garden Background Service";
            String description = "Kênh thông báo chạy ngầm";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY giúp Service tự khởi động lại nếu bị hệ thống kill
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Không dùng Binding
    }
}