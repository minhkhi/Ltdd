package com.example.ltdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ltdd.databinding.ActivityControlBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ControlActivity extends AppCompatActivity {

    private ActivityControlBinding binding;
    private DatabaseReference deviceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kết nối Firebase (Server Singapore)
        deviceRef = FirebaseDatabase.getInstance("https://smart-garden-94677-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("devices/device_01");

        // 1. Lấy dữ liệu hiện tại để hiển thị lên màn hình
        loadCurrentConfig();

        // 2. Xử lý bật tắt đèn
        binding.swLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    deviceRef.child("led_status").setValue(isChecked);
                }
            }
        });

        // 3. Gửi thông số cài đặt (Nhiệt độ & Độ ẩm)
        binding.btnSendConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfig();
            }
        });

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadCurrentConfig() {
        // Lấy trạng thái đèn
        deviceRef.child("led_status").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot snapshot) {
                Boolean status = snapshot.getValue(Boolean.class);
                if (status != null) binding.swLight.setChecked(status);
            }
        });

        // Lấy cấu hình (Nhiệt độ & Độ ẩm)
        deviceRef.child("config").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot snapshot) {
                // Lấy ngưỡng nhiệt độ
                Double tempThresh = snapshot.child("temp_threshold").getValue(Double.class);
                if (tempThresh != null) {
                    binding.etThreshold.setText(String.valueOf(tempThresh));
                }

                // Lấy ngưỡng độ ẩm (MỚI)
                Integer humidThresh = snapshot.child("humid_threshold").getValue(Integer.class);
                if (humidThresh != null) {
                    binding.etHumidThreshold.setText(String.valueOf(humidThresh));
                }
            }
        });
    }

    private void saveConfig() {
        String strTemp = binding.etThreshold.getText().toString();
        String strHumid = binding.etHumidThreshold.getText().toString();

        if (TextUtils.isEmpty(strTemp) || TextUtils.isEmpty(strHumid)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông số", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double tempThreshold = Double.parseDouble(strTemp);
            int humidThreshold = Integer.parseInt(strHumid);

            // Gom dữ liệu vào Map để cập nhật cùng lúc
            Map<String, Object> configUpdates = new HashMap<>();
            configUpdates.put("config/temp_threshold", tempThreshold);
            configUpdates.put("config/humid_threshold", humidThreshold);

            deviceRef.updateChildren(configUpdates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(ControlActivity.this, "Đã cập nhật cấu hình Smart Garden!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ControlActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Định dạng số không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}