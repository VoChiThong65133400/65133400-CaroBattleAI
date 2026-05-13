package ntu.carobattleai;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private Button[][] buttons = new Button[3][3];
    private boolean player1Turn;
    private TextView tvTurn, tvTimer;
    private String player1Name, player2Name;
    private CountDownTimer countDownTimer;

    private LinkedList<Button> xQueue = new LinkedList<>();
    private LinkedList<Button> oQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        player1Name = getIntent().getStringExtra("p1");
        player2Name = getIntent().getStringExtra("p2");
        if (player1Name == null) player1Name = "Người chơi X";
        if (player2Name == null) player2Name = "Người chơi O";

        tvTurn = findViewById(R.id.tvTurn);
        tvTimer = findViewById(R.id.tvTimer);

        // Quyết định ngẫu nhiên ai đi trước
        player1Turn = new Random().nextBoolean();
        String starter = player1Turn ? player1Name : player2Name;
        Toast.makeText(this, "Trọng tài: " + starter + " đi trước!", Toast.LENGTH_LONG).show();

        updateTurnText();
        startTimer();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "btn_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(v -> onButtonClick((Button) v));
            }
        }

        findViewById(R.id.btnReset).setOnClickListener(v -> resetGame());
        //Xác nhận khi nhấn nút xin thua
        findViewById(R.id.btnSurrender).setOnClickListener(v -> {
            String currentPlayer = player1Turn ? player1Name : player2Name;

            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận đầu hàng")
                    .setMessage(currentPlayer + " ơi, bạn thực sự muốn xin thua sao?")
                    .setPositiveButton("Đồng ý", (dialog, which) -> {
                        String winner = player1Turn ? player2Name : player1Name;
                        showWinDialog(currentPlayer + " đã xin thua! " + winner + " thắng.");
                    })
                    .setNegativeButton("Tiếp tục đấu", null)
                    .show();
        });
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Thời gian: " + millisUntilFinished / 1000 + "s");
                if (millisUntilFinished < 6000) tvTimer.setTextColor(Color.RED);
                else tvTimer.setTextColor(Color.parseColor("#D32F2F"));
            }
            public void onFinish() {
                String winner = player1Turn ? player2Name : player1Name;
                showWinDialog("Hết giờ! " + winner + " thắng.");
            }
        }.start();
    }

    private void onButtonClick(Button b) {
        if (!b.getText().toString().equals("")) return;

        if (player1Turn) {
            if (oQueue.size() == 3) removeOldestPiece(oQueue);
            b.setText("X");
            b.setTextColor(Color.BLUE);
            xQueue.add(b);
            if (checkForWin()) {
                showWinDialog(player1Name + " thắng!");
                return;
            }
            if (xQueue.size() == 3) warnOldestPiece(xQueue);
            player1Turn = false;
        } else {
            if (xQueue.size() == 3) removeOldestPiece(xQueue);
            b.setText("O");
            b.setTextColor(Color.RED);
            oQueue.add(b);
            if (checkForWin()) {
                showWinDialog(player2Name + " thắng!");
                return;
            }
            if (oQueue.size() == 3) warnOldestPiece(oQueue);
            player1Turn = true;
        }
        updateTurnText();
        startTimer();
    }

    private void updateTurnText() {
        tvTurn.setText("Lượt của: " + (player1Turn ? player1Name : player2Name));
    }

    private void warnOldestPiece(LinkedList<Button> queue) {
        Button oldest = queue.peek();
        if (oldest != null) {
            Animation blink = AnimationUtils.loadAnimation(this, R.anim.blink_fade);
            oldest.startAnimation(blink);
        }
    }

    private void removeOldestPiece(LinkedList<Button> queue) {
        Button oldest = queue.poll();
        if (oldest != null) {
            oldest.clearAnimation();
            oldest.setText("");
        }
    }

    private void saveGameHistory(String result) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("LichSuChoi");
            String gameId = myRef.push().getKey();
            Map<String, Object> history = new HashMap<>();
            history.put("cheDo", "2 Người");
            history.put("doKho", player1Name + " vs " + player2Name);
            history.put("ketQua", result);
            history.put("thoiGian", DateFormat.getDateTimeInstance().format(new Date()));
            if (gameId != null) myRef.child(gameId).setValue(history);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean checkForWin() {
        String[][] field = new String[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) field[i][j] = buttons[i][j].getText().toString();
        for (int i = 0; i < 3; i++) {
            if (field[i][0].equals(field[i][1]) && field[i][0].equals(field[i][2]) && !field[i][0].equals("")) return true;
            if (field[0][i].equals(field[1][i]) && field[0][i].equals(field[2][i]) && !field[0][i].equals("")) return true;
        }
        if (field[0][0].equals(field[1][1]) && field[0][0].equals(field[2][2]) && !field[0][0].equals("")) return true;
        if (field[0][2].equals(field[1][1]) && field[0][2].equals(field[2][0]) && !field[0][2].equals("")) return true;
        return false;
    }

    private void showWinDialog(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        saveGameHistory(message);
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> resetGame())
                .setNegativeButton("Thoát", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) { buttons[i][j].setText(""); buttons[i][j].clearAnimation(); }
        xQueue.clear(); oQueue.clear();
        player1Turn = new Random().nextBoolean();
        updateTurnText();
        startTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}