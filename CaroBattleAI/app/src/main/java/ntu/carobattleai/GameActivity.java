package ntu.carobattleai;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private Button[][] buttons = new Button[3][3];
    private boolean player1Turn = true;
    private int roundCount = 0;
    private TextView tvTurn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvTurn = findViewById(R.id.tvTurn);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "btn_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(v -> onButtonClick((Button) v));
            }
        }

        findViewById(R.id.btnReset).setOnClickListener(v -> resetGame());
    }

    private void onButtonClick(Button b) {
        if (!b.getText().toString().equals("")) return;

        if (player1Turn) {
            b.setText("X");
            b.setTextColor(Color.BLUE);
            tvTurn.setText("Lượt của: O");
        } else {
            b.setText("O");
            b.setTextColor(Color.RED);
            tvTurn.setText("Lượt của: X");
        }

        roundCount++;

        if (checkForWin()) {
            if (player1Turn) {
                showWinDialog("Người chơi X thắng!");
            } else {
                showWinDialog("Người chơi O thắng!");
            }
        } else if (roundCount == 9) {
            showWinDialog("Hòa rồi!");
        } else {
            player1Turn = !player1Turn;
        }
    }

    private boolean checkForWin() {
        String[][] field = new String[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                field[i][j] = buttons[i][j].getText().toString();
            }
        }

        // Kiểm tra hàng ngang và hàng dọc
        for (int i = 0; i < 3; i++) {
            if (field[i][0].equals(field[i][1]) && field[i][0].equals(field[i][2]) && !field[i][0].equals("")) return true;
            if (field[0][i].equals(field[1][i]) && field[0][i].equals(field[2][i]) && !field[0][i].equals("")) return true;
        }

        // Kiểm tra 2 đường chéo
        if (field[0][0].equals(field[1][1]) && field[0][0].equals(field[2][2]) && !field[0][0].equals("")) return true;
        if (field[0][2].equals(field[1][1]) && field[0][2].equals(field[2][0]) && !field[0][2].equals("")) return true;

        return false;
    }

    private void showWinDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc trận đấu")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> resetGame())
                .setNegativeButton("Thoát", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
            }
        }
        roundCount = 0;
        player1Turn = true;
        tvTurn.setText("Lượt của: X");
    }
}