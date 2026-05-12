package ntu.carobattleai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BotModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_mode);

        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnMedium = findViewById(R.id.btnMedium);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnBack = findViewById(R.id.btnBackBot);

        // Xử lý các mức độ khó
        btnEasy.setOnClickListener(v -> startBotGame("Easy"));
        btnMedium.setOnClickListener(v -> startBotGame("Medium"));
        btnHard.setOnClickListener(v -> startBotGame("Hard"));

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());
    }

    private void startBotGame(String level) {
        Toast.makeText(this, "Đã chọn mức: " + level, Toast.LENGTH_SHORT).show();
        // Sau này sẽ truyền 'level' sang màn hình GameActivity
    }
}