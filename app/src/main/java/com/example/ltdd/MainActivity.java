package com.example.ltdd;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.ltdd.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference database;
    private static final String CHANNEL_ID = "smart_garden_alert_channel";
    private static final int NOTIFICATION_ID = 1;

    // Biến lưu ngưỡng (mặc định)
    private double tempThreshold = 100.0;
    private int humidThreshold = 0;

    // Cờ theo dõi trạng thái đã cảnh báo CHO TỪNG CHỈ SỐ
    private boolean isTempAlertShown = false;
    private boolean isHumidAlertShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createNotificationChannel();
        checkNotificationPermission();

        Intent serviceIntent = new Intent(this, Background.class);
        startService(serviceIntent);

        database = FirebaseDatabase.getInstance("https://smart-garden-94677-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("devices/device_01");

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

                if (temp != null) {
                    binding.tvTemp.setText(String.format(Locale.US, "%.1f", temp));
                } else {
                    binding.tvTemp.setText("--.-");
                }

                if (humid != null) {
                    binding.tvHumid.setText(String.valueOf(humid));
                } else {
                    binding.tvHumid.setText("--");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi đọc data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnGoControl.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            startActivity(intent);
        });

        binding.btnChangePass.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void checkAlert(Double temp, Integer humid) {
        if (temp == null || humid == null) return;

        // 1. Kiểm tra trạng thái hiện tại
        boolean isHighTemp = temp > tempThreshold;
        boolean isLowHumid = humid < humidThreshold;

        // 2. Xác định xem có cần gửi thông báo MỚI không
        // (Chỉ gửi khi đang nguy hiểm VÀ chưa báo trước đó)
        boolean needAlertTemp = isHighTemp && !isTempAlertShown;
        boolean needAlertHumid = isLowHumid && !isHumidAlertShown;

        if (needAlertTemp || needAlertHumid) {
            String title = "CẢNH BÁO";
            String message = "";

            if (needAlertTemp && needAlertHumid) {
                // Trường hợp cả 2 cùng vi phạm MỚI
                message = "Nhiệt độ cao (" + temp + "°C) và Độ ẩm thấp (" + humid + "%)!";
                // Đánh dấu cả 2 đã báo
                isTempAlertShown = true;
                isHumidAlertShown = true;
            } else if (needAlertTemp) {
                // Chỉ nhiệt độ vi phạm mới
                message = "Nhiệt độ quá cao (" + temp + "°C)!";
                isTempAlertShown = true;
                // Nếu lúc này độ ẩm cũng đang thấp (nhưng đã báo rồi), ta không nhắc lại
            } else if (needAlertHumid) {
                // Chỉ độ ẩm vi phạm mới
                message = "Độ ẩm đất quá thấp (" + humid + "%)!";
                isHumidAlertShown = true;
            }

            showNotification(title, message);
        }

        // 3. Reset trạng thái khi các chỉ số trở về an toàn
        // (Reset riêng lẻ để đảm bảo chính xác)
        if (!isHighTemp) {
            isTempAlertShown = false;
        }
        if (!isLowHumid) {
            isHumidAlertShown = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Smart Garden Alerts";
            String description = "Thông báo cảnh báo nhiệt độ/độ ẩm";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void showNotification(String title, String content) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                // Thêm dòng này để thông báo rung/kêu mỗi khi xuất hiện (nếu kênh hỗ trợ)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // Sử dụng ID cố định để thông báo mới đè lên thông báo cũ (tránh spam danh sách)
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn thoát khỏi tài khoản không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    Toast.makeText(MainActivity.this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}