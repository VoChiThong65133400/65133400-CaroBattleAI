package ntu.carobattleai;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private boolean player1Turn; // True = X đi trước, False = O đi trước

    private LinearLayout layoutPlayerX, layoutPlayerO;
    private TextView tvPlayerXName, tvPlayerOName;
    private TextView tvPlayerXTime, tvPlayerOTime;

    private String player1Name, player2Name;
    private CountDownTimer countDownTimer;

    private LinkedList<Button> xQueue = new LinkedList<>();
    private LinkedList<Button> oQueue = new LinkedList<>();

    private MediaPlayer moveSound;
    private MediaPlayer winSound;

    private int flashCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        moveSound = MediaPlayer.create(this, R.raw.move_sound);
        winSound = MediaPlayer.create(this, R.raw.win_sound);

        // Lấy tên người chơi
        player1Name = getIntent().getStringExtra("p1");
        player2Name = getIntent().getStringExtra("p2");
        if (player1Name == null) player1Name = "Người chơi X";
        if (player2Name == null) player2Name = "Người chơi O";


        layoutPlayerX = findViewById(R.id.layoutPlayerX);
        layoutPlayerO = findViewById(R.id.layoutPlayerO);
        tvPlayerXName = findViewById(R.id.tvPlayerXName);
        tvPlayerOName = findViewById(R.id.tvPlayerOName);
        tvPlayerXTime = findViewById(R.id.tvPlayerXTime);
        tvPlayerOTime = findViewById(R.id.tvPlayerOTime);

        // Gán tên người chơi
        tvPlayerXName.setText(player1Name);
        tvPlayerOName.setText(player2Name);

        // Khởi tạo các nút bấm (Bàn cờ)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "btn_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(v -> onButtonClick((Button) v));
                buttons[i][j].setEnabled(false);
            }
        }

        // Bắt đầu quay số ngẫu nhiên chọn người đi trước
        showRandomStarterDialog();
    }

    private void showRandomStarterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage("Người chơi đi trước là...")
                .setCancelable(false)
                .create();
        dialog.show();

        // Chỉnh nền Dialog tối
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.background_dark);
        }

        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            messageView.setTextSize(22);
            messageView.setTextColor(Color.WHITE);
        }

        flashCount = 0;
        Handler handler = new Handler();

        Runnable flashRunnable = new Runnable() {
            @Override
            public void run() {
                if (flashCount < 16) {
                    if (flashCount % 2 == 0) {
                        dialog.setMessage("Người chơi đi trước là...\n\n X ");
                        messageView.setTextColor(Color.parseColor("#00FFFF")); // Nhấp nháy Xanh
                    } else {
                        dialog.setMessage("Người chơi đi trước là...\n\n O ");
                        messageView.setTextColor(Color.parseColor("#FF007F")); // Nhấp nháy Hồng
                    }
                    flashCount++;
                    handler.postDelayed(this, 150);
                } else {
                    // KẾT THÚC: Chọn người thắng thực sự
                    player1Turn = new Random().nextBoolean();
                    String starterSymbol = player1Turn ? "X" : "O";
                    String starterName = player1Turn ? player1Name : player2Name;
                    int finalColor = player1Turn ? Color.parseColor("#00FFFF") : Color.parseColor("#FF007F");

                    messageView.setTextColor(Color.WHITE);
                    dialog.setMessage("Người chơi đi trước là...\n\n " + starterSymbol + " \n\nChúc mừng " + starterName + "!");

                    // Highlight quân cờ thắng cuộc
                    handler.postDelayed(() -> {
                        if (dialog.isShowing()) dialog.dismiss();
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) buttons[i][j].setEnabled(true);
                        }
                        //Cập nhật lượt đi và đồng hồ
                        updateTurnDisplay();
                        startTimer();
                    }, 2000);
                }
            }
        };
        handler.post(flashRunnable);
    }

    private void onButtonClick(Button b) {
        if (!b.getText().toString().equals("")) return;

        if (moveSound != null) moveSound.start();

        if (player1Turn) {
            // Lượt của X
            if (oQueue.size() == 3) removeOldestPiece(oQueue);
            b.setText("X");
            b.setTextColor(Color.parseColor("#00FFFF")); // Xanh Cyan
            b.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"));

            xQueue.add(b);
            if (checkForWin()) {
                showWinDialog(player1Name + " thắng!");
                return;
            }
            if (xQueue.size() == 3) warnOldestPiece(xQueue);
            player1Turn = false;
        } else {
            // Lượt của O
            if (xQueue.size() == 3) removeOldestPiece(xQueue);
            b.setText("O");
            b.setTextColor(Color.parseColor("#FF007F")); // Hồng Neon
            b.setShadowLayer(15f, 0f, 0f, Color.parseColor("#FF007F"));

            oQueue.add(b);
            if (checkForWin()) {
                showWinDialog(player2Name + " thắng!");
                return;
            }
            if (oQueue.size() == 3) warnOldestPiece(oQueue);
            player1Turn = true;
        }
        // ĐỔI LƯỢT ĐI
        updateTurnDisplay();
        startTimer();
    }

    private void showWinDialog(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        if (winSound != null) winSound.start();

        saveGameHistory(message);
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> resetGame())
                .setNegativeButton("Thoát", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moveSound != null) moveSound.release();
        if (winSound != null) winSound.release();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    //Hàm bắt đầu đồng hồ mới
    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        // Đặt lại text thời gian cho cả 2 khung
        tvPlayerXTime.setText("??s");
        tvPlayerOTime.setText("??s");

        countDownTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;

                if (player1Turn) {
                    // Cập nhật cho X, khung O tối
                    tvPlayerXTime.setText(seconds + "s");
                    tvPlayerXTime.setTextColor(Color.parseColor("#00FFFF")); // Xanh
                    if (seconds < 6) tvPlayerXTime.setTextColor(Color.RED);
                } else {
                    // Cập nhật cho O, khung X tối
                    tvPlayerOTime.setText(seconds + "s");
                    tvPlayerOTime.setTextColor(Color.parseColor("#FF007F")); // Hồng
                    if (seconds < 6) tvPlayerOTime.setTextColor(Color.RED);
                }
            }
            public void onFinish() {
                String winner = player1Turn ? player2Name : player1Name;
                showWinDialog("Hết giờ! " + winner + " thắng.");
            }
        }.start();
    }

    //Hàm cập nhật lượt đi và sáng khung
    private void updateTurnDisplay() {
        if (player1Turn) {

            layoutPlayerX.setAlpha(1.0f);
            layoutPlayerO.setAlpha(0.4f);
        } else {

            layoutPlayerO.setAlpha(1.0f);
            layoutPlayerX.setAlpha(0.4f);
        }
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
            oldest.setShadowLayer(0, 0, 0, 0);
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

    private void resetGame() {
        if (countDownTimer != null) countDownTimer.cancel();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].clearAnimation();
                buttons[i][j].setShadowLayer(0, 0, 0, 0);
                buttons[i][j].setEnabled(false);
            }
        }
        xQueue.clear();
        oQueue.clear();
        showRandomStarterDialog();
    }
}