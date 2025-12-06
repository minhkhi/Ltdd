package com.example.ltdd;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kết nối đến node "devices/device_01" trên Firebase
        database = FirebaseDatabase.getInstance("https://smart-garden-94677-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("devices/device_01");

        // Lắng nghe dữ liệu realtime
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Giả sử cấu trúc JSON: { "temp": 30.5, "humid": 60 }
                // Cần xử lý null safety
                Double temp = snapshot.child("temp").getValue(Double.class);
                Integer humid = snapshot.child("humid").getValue(Integer.class);

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

        // Chuyển màn hình điều khiển
        binding.btnGoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                startActivity(intent);
            }
        });

        // Đăng xuất
        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}