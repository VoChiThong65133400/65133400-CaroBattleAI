package ntu.carobattleai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TwoPlayerActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện
    EditText edtPlayer1, edtPlayer2;
    Button btnStartGame, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_player);

        // 1. Ánh xạ các nút và ô nhập từ XML
        edtPlayer1 = findViewById(R.id.edtPlayer1);
        edtPlayer2 = findViewById(R.id.edtPlayer2);
        btnStartGame = findViewById(R.id.btnStartGame);
        btnBack = findViewById(R.id.btnBack); // Nút quay lại mình vừa thêm

        // 2. Xử lý nút Bắt đầu game
        btnStartGame.setOnClickListener(v -> {
            String name1 = edtPlayer1.getText().toString().trim();
            String name2 = edtPlayer2.getText().toString().trim();

            if (name1.isEmpty() || name2.isEmpty()) {
                Toast.makeText(this, "Bạn ơi, nhập đủ tên 2 người đã nhé!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Chào mừng " + name1 + " và " + name2, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(TwoPlayerActivity.this, GameActivity.class);
                intent.putExtra("p1", name1);
                intent.putExtra("p2", name2);
                startActivity(intent);

            }
        });

        // 3. Xử lý nút Quay lại Menu
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng màn hình này để về lại MainActivity
        });
    }
}