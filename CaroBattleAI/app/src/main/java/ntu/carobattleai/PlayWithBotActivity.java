package ntu.carobattleai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// --- IMPORT THÊM CHO FIREBASE ---
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
// --------------------------------

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class PlayWithBotActivity extends AppCompatActivity {

    private Button[][] buttons = new Button[3][3];
    private boolean playerTurn = true; // Người chơi (X) đi trước
    private TextView tvTurn;
    private String difficulty;

    // Hàng đợi quản lý 3 quân cờ của mỗi bên để thực hiện luật "biến mất"
    private LinkedList<Button> xQueue = new LinkedList<>();
    private LinkedList<Button> oQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "Easy";

        tvTurn = findViewById(R.id.tvTurn);
        tvTurn.setText("Chế độ: " + difficulty);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "btn_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(v -> {
                    if (playerTurn) onButtonClick((Button) v);
                });
            }
        }

        findViewById(R.id.btnReset).setOnClickListener(v -> resetGame());
    }

    private void onButtonClick(Button b) {
        if (!b.getText().toString().equals("")) return;

        if (playerTurn) {
            if (oQueue.size() == 3) removeOldest(oQueue);
            placePiece(b, "X", Color.BLUE, xQueue);

            if (checkForWin()) {
                showWinDialog("Chúc mừng! Bạn đã thắng Máy.");
            } else {
                playerTurn = false;
                tvTurn.setText("Máy đang suy nghĩ...");
                new Handler().postDelayed(this::botMove, 800);
            }
        } else {
            if (xQueue.size() == 3) removeOldest(xQueue);
            placePiece(b, "O", Color.RED, oQueue);

            if (checkForWin()) {
                showWinDialog("Rất tiếc! Máy đã thắng rồi.");
            } else {
                playerTurn = true;
                tvTurn.setText("Lượt của bạn (X)");
            }
        }
    }

    private void placePiece(Button b, String txt, int color, LinkedList<Button> queue) {
        b.setText(txt);
        b.setTextColor(color);
        queue.add(b);

        if (queue.size() == 3) {
            Animation blink = AnimationUtils.loadAnimation(this, R.anim.blink_fade);
            queue.peek().startAnimation(blink);
        }
    }

    private void removeOldest(LinkedList<Button> queue) {
        Button oldest = queue.poll();
        if (oldest != null) {
            oldest.clearAnimation();
            oldest.setText("");
        }
    }

    //HÀM LƯU LỊCH SỬ VÀO FIREBASE REALTIME
    private void saveGameHistory(String result) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("LichSuChoi");

            String gameId = myRef.push().getKey();

            Map<String, Object> history = new HashMap<>();
            history.put("cheDo", "Voi May");
            history.put("doKho", difficulty);
            history.put("ketQua", result);
            history.put("thoiGian", DateFormat.getDateTimeInstance().format(new Date()));

            if (gameId != null) {
                myRef.child(gameId).setValue(history);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void botMove() {
        Button move = null;
        if ("Hard".equals(difficulty)) {
            move = findBestMove();
        } else if ("Medium".equals(difficulty)) {
            move = findWinningOrBlockingMove();
        }

        if (move == null) {
            move = getRandomMove();
        }
        if (move != null) onButtonClick(move);
    }

    private Button getRandomMove() {
        ArrayList<Button> empty = new ArrayList<>();
        for (Button[] row : buttons) {
            for (Button b : row) {
                if (b.getText().equals("")) empty.add(b);
            }
        }
        if (empty.isEmpty()) return null;
        return empty.get(new Random().nextInt(empty.size()));
    }

    private Button findWinningOrBlockingMove() {
        Button move = searchForBestSpot("O");
        if (move != null) return move;
        move = searchForBestSpot("X");
        if (move != null) return move;
        return null;
    }

    private Button findBestMove() {
        Button move = findWinningOrBlockingMove();
        if (move != null) return move;
        if (buttons[1][1].getText().equals("")) return buttons[1][1];
        int[][] corners = {{0,0}, {0,2}, {2,0}, {2,2}};
        for (int[] c : corners) {
            if (buttons[c[0]][c[1]].getText().equals("")) return buttons[c[0]][c[1]];
        }
        return null;
    }

    private Button searchForBestSpot(String s) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().equals("")) {
                    buttons[i][j].setText(s);
                    boolean isWin = checkForWin();
                    buttons[i][j].setText("");
                    if (isWin) return buttons[i][j];
                }
            }
        }
        return null;
    }

    private boolean checkForWin() {
        String[][] f = new String[3][3];
        for (int i=0; i<3; i++) for (int j=0; j<3; j++) f[i][j] = buttons[i][j].getText().toString();

        for (int i=0; i<3; i++) {
            if (f[i][0].equals(f[i][1]) && f[i][0].equals(f[i][2]) && !f[i][0].equals("")) return true;
            if (f[0][i].equals(f[1][i]) && f[0][i].equals(f[2][i]) && !f[0][i].equals("")) return true;
        }
        if (f[0][0].equals(f[1][1]) && f[0][0].equals(f[2][2]) && !f[0][0].equals("")) return true;
        return f[0][2].equals(f[1][1]) && f[0][2].equals(f[2][0]) && !f[0][2].equals("");
    }

    private void showWinDialog(String msg) {
        //GỌI LƯU LỊCH SỬ KHI HIỆN DIALOG
        saveGameHistory(msg);

        new AlertDialog.Builder(this)
                .setTitle("Kết thúc")
                .setMessage(msg)
                .setPositiveButton("Chơi lại", (d, w) -> resetGame())
                .setNegativeButton("Thoát", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        for (Button[] row : buttons) {
            for (Button b : row) {
                b.setText("");
                b.clearAnimation();
            }
        }
        xQueue.clear();
        oQueue.clear();
        playerTurn = true;
        tvTurn.setText("Lượt của bạn (X)");
    }
}