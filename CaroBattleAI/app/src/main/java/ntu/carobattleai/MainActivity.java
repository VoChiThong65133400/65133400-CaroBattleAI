package ntu.carobattleai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnVsBot, btnVsPlayer, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ các nút từ giao diện XML
        btnVsBot = findViewById(R.id.btnVsBot);
        btnVsPlayer = findViewById(R.id.btnVsPlayer);
        btnHistory = findViewById(R.id.btnHistory);

        // 2. Xử lý khi nhấn nút "VS Máy (AI)"
        btnVsBot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BotModeActivity.class);
            startActivity(intent);
        });

        // 3. Xử lý khi nhấn nút "VS Người"
        btnVsPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TwoPlayerActivity.class);
            startActivity(intent);
        });

        // 4. Xử lý khi nhấn nút "Lịch sử"
        btnHistory.setOnClickListener(v -> {
            // T
        });
    }
}