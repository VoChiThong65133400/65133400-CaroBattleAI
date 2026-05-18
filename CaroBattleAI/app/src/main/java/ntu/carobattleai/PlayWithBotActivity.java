package ntu.carobattleai;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class PlayWithBotActivity extends AppCompatActivity {

    private Button[][] buttons = new Button[3][3];
    private boolean playerTurn = true;
    private String difficulty;

    private LinearLayout layoutPlayerX, layoutPlayerO;
    private TextView tvPlayerXTime, tvPlayerOTime;
    private TextView tvPlayerXName, tvPlayerOName;

    private LinkedList<Button> xQueue = new LinkedList<>();
    private LinkedList<Button> oQueue = new LinkedList<>();

    private CountDownTimer countDownTimer;
    private MediaPlayer moveSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        moveSound = MediaPlayer.create(this, R.raw.move_sound);
        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "Dễ";

        layoutPlayerX = findViewById(R.id.layoutPlayerX);
        layoutPlayerO = findViewById(R.id.layoutPlayerO);
        tvPlayerXTime = findViewById(R.id.tvPlayerXTime);
        tvPlayerOTime = findViewById(R.id.tvPlayerOTime);
        tvPlayerXName = findViewById(R.id.tvPlayerXName);
        tvPlayerOName = findViewById(R.id.tvPlayerOName);

        tvPlayerXName.setText("Bạn (X)");
        tvPlayerOName.setText("Máy (" + difficulty + ")");

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
        updateTurnDisplay();
        startTimer();
    }

    private void onButtonClick(Button b) {
        if (!b.getText().toString().equals("")) return;
        if (moveSound != null) moveSound.start();

        if (playerTurn) {
            placePiece(b, "X", Color.parseColor("#00FFFF"), xQueue);

            if (checkForWin()) {
                showWinDialog("Chúc mừng! Bạn đã thắng Máy.");
            } else {
                playerTurn = false;
                updateTurnDisplay();
                startTimer();

                // Sau khi đánh: Đợi 1.5s xóa quân O cũ nhất
                if (oQueue.size() == 3) {
                    new Handler().postDelayed(() -> removeOldest(oQueue), 1500);
                }
                new Handler().postDelayed(this::botMove, 800);
            }
        } else {
            placePiece(b, "O", Color.parseColor("#FF007F"), oQueue);

            if (checkForWin()) {
                showWinDialog("Rất tiếc! Máy đã thắng rồi.");
            } else {
                playerTurn = true;
                updateTurnDisplay();
                startTimer();

                // SAU KHI MÁY ĐÁNH: Đợi 1.5s xóa quân X cũ nhất (nếu có 3 quân)
                if (xQueue.size() == 3) {
                    new Handler().postDelayed(() -> removeOldest(xQueue), 1500);
                }
            }
        }
    }

    private void placePiece(Button b, String txt, int color, LinkedList<Button> queue) {
        b.setText(txt);
        b.setTextColor(color);
        b.setShadowLayer(15f, 0f, 0f, color);
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
            oldest.setShadowLayer(0, 0, 0, 0);
        }
    }

    private void updateTurnDisplay() {
        if (playerTurn) {
            layoutPlayerX.setAlpha(1.0f);
            layoutPlayerO.setAlpha(0.4f);
        } else {
            layoutPlayerO.setAlpha(1.0f);
            layoutPlayerX.setAlpha(0.4f);
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                if (playerTurn) {
                    tvPlayerXTime.setText(sec + "s");
                    tvPlayerXTime.setTextColor(sec < 6 ? Color.RED : Color.parseColor("#00FFFF"));
                    tvPlayerOTime.setText("00s");
                } else {
                    tvPlayerOTime.setText(sec + "s");
                    tvPlayerOTime.setTextColor(sec < 6 ? Color.RED : Color.parseColor("#FF007F"));
                    tvPlayerXTime.setText("00s");
                }
            }
            public void onFinish() {
                if (playerTurn) showWinDialog("Hết giờ! Máy thắng.");
                else showWinDialog("Máy hết thời gian! Bạn thắng.");
            }
        }.start();
    }

    private void botMove() {
        Button move = null;
        if ("Hard".equals(difficulty)) move = findBestMove();
        else if ("Medium".equals(difficulty)) move = findWinningOrBlockingMove();
        if (move == null) move = getRandomMove();
        if (move != null) onButtonClick(move);
    }

    private Button getRandomMove() {
        ArrayList<Button> empty = new ArrayList<>();
        for (Button[] row : buttons) {
            for (Button b : row) if (b.getText().equals("")) empty.add(b);
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
        return getRandomMove();
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
        if (countDownTimer != null) countDownTimer.cancel();
        saveGameHistory(msg);

        // 1. Khởi tạo Dialog với style trong suốt để không bị viền trắng đè lên viền Neon
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_win);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCancelable(false); // Không cho bấm ra ngoài để tắt

        // 2. Ánh xạ các View trong Dialog
        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvDialogMessage = dialog.findViewById(R.id.tvDialogMessage);
        Button btnExit = dialog.findViewById(R.id.btnDialogExit);
        Button btnRestart = dialog.findViewById(R.id.btnDialogRestart);

        // 3. Hiển thị nội dung
        tvDialogMessage.setText(msg);

        // Tự động đổi màu tiêu đề theo kết quả cho đẹp: Thắng thì Xanh Cyan, Thua thì Hồng Neon
        if (msg.contains("Bạn đã thắng") || msg.contains("Bạn thắng")) {
            tvDialogTitle.setText("CHIẾN THẮNG");
            tvDialogTitle.setTextColor(Color.parseColor("#00FFFF"));
            tvDialogTitle.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"));
        } else {
            tvDialogTitle.setText("THẤT BẠI");
            tvDialogTitle.setTextColor(Color.parseColor("#FF007F"));
            tvDialogTitle.setShadowLayer(15f, 0f, 0f, Color.parseColor("#FF007F"));
        }

        // 4. Xử lý sự kiện click nút
        btnRestart.setOnClickListener(v -> {
            resetGame();
            dialog.dismiss(); // Đóng dialog
        });

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Thoát màn hình chơi
        });

        // 5. Hiển thị lên màn hình
        dialog.show();
    }

    private void saveGameHistory(String result) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("LichSuChoi");
            String gameId = myRef.push().getKey();
            Map<String, Object> history = new HashMap<>();
            history.put("cheDo", "Với Máy");
            history.put("doKho", difficulty);
            history.put("ketQua", result);
            history.put("thoiGian", DateFormat.getDateTimeInstance().format(new Date()));
            if (gameId != null) myRef.child(gameId).setValue(history);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void resetGame() {
        for (Button[] row : buttons) {
            for (Button b : row) {
                b.setText("");
                b.clearAnimation();
                b.setShadowLayer(0, 0, 0, 0);
            }
        }
        xQueue.clear();
        oQueue.clear();
        playerTurn = true;
        updateTurnDisplay();
        startTimer();
    }
}