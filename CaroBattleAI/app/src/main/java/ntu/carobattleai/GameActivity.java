package ntu.carobattleai;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
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
                        if (messageView != null) messageView.setTextColor(Color.parseColor("#00FFFF")); // Nhấp nháy Xanh
                    } else {
                        dialog.setMessage("Người chơi đi trước là...\n\n O ");
                        if (messageView != null) messageView.setTextColor(Color.parseColor("#FF007F")); // Nhấp nháy Hồng
                    }
                    flashCount++;
                    handler.postDelayed(this, 150);
                } else {
                    // KẾT THÚC: Chọn người thắng thực sự
                    player1Turn = new Random().nextBoolean();
                    String starterSymbol = player1Turn ? "X" : "O";
                    String starterName = player1Turn ? player1Name : player2Name;

                    if (messageView != null) messageView.setTextColor(Color.WHITE);
                    dialog.setMessage("Người chơi đi trước là...\n\n " + starterSymbol + " \n\nChúc mừng " + starterName + "!");

                    handler.postDelayed(() -> {
                        if (dialog.isShowing()) dialog.dismiss();
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) buttons[i][j].setEnabled(true);
                        }
                        // Cập nhật lượt đi và đồng hồ
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
        // đổi lượt đi
        updateTurnDisplay();
        startTimer();
    }

    // Đoạn hiển thị dialog kết thúc
    private void showWinDialog(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        if (winSound != null) winSound.start();

        saveGameHistory(message);

        final Dialog dialog = new Dialog(this);

        // 1. Tạo Layout nền cho Dialog trực tiếp từ code
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(60, 60, 60, 60);

        // Custom shape: Nền tím đen huyền bí bo góc tròn
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#0A0813"));
        shape.setCornerRadius(48f);

        // 2. Tạo TextView Tiêu đề
        TextView tvDialogTitle = new TextView(this);
        tvDialogTitle.setText("CHIẾN THẮNG");
        tvDialogTitle.setTextSize(26);
        tvDialogTitle.setTypeface(null, Typeface.BOLD);
        tvDialogTitle.setGravity(Gravity.CENTER);
        tvDialogTitle.setPadding(0, 20, 0, 15);

        // 3. Tạo TextView Nội dung thông báo (Ví dụ: "Người chơi X thắng!")
        TextView tvDialogMessage = new TextView(this);
        tvDialogMessage.setTextSize(16);
        tvDialogMessage.setTextColor(Color.parseColor("#94A3B8"));
        tvDialogMessage.setGravity(Gravity.CENTER);
        tvDialogMessage.setPadding(0, 0, 0, 50);
        tvDialogMessage.setText(message);

        if (message.contains(player2Name) || message.contains("O thắng")) {
            // Trường hợp 1: Người chơi O thắng -> Chữ và Viền màu Hồng Neon
            tvDialogTitle.setTextColor(Color.parseColor("#FF007F"));
            tvDialogTitle.setShadowLayer(15f, 0f, 0f, Color.parseColor("#FF007F"));

            shape.setStroke(4, Color.parseColor("#FF007F"));
        } else {
            // Trường hợp 2: Người chơi X thắng (Mặc định) -> Chữ và Viền màu Xanh Cyan
            tvDialogTitle.setTextColor(Color.parseColor("#00FFFF"));
            tvDialogTitle.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"));

            shape.setStroke(4, Color.parseColor("#00FFFF"));
        }

        // Gán shape nền đã đổi màu viền vào layout
        layout.setBackground(shape);

        // 4. Nút "CHƠI LẠI"
        Button btnRestart = new Button(this);
        btnRestart.setText("CHƠI LẠI");
        btnRestart.setTextSize(16);
        btnRestart.setTextColor(Color.parseColor("#00FFFF"));
        btnRestart.setTypeface(null, Typeface.BOLD);

        GradientDrawable btnRestartShape = new GradientDrawable();
        btnRestartShape.setColor(Color.parseColor("#150A0813"));
        btnRestartShape.setStroke(3, Color.parseColor("#00FFFF"));
        btnRestartShape.setCornerRadius(100f);
        btnRestart.setBackground(btnRestartShape);

        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            resetGame();
        });

        // 5. Nút "TRỞ VỀ MENU"
        Button btnMenu = new Button(this);
        btnMenu.setText("TRỞ VỀ MENU");
        btnMenu.setTextSize(16);
        btnMenu.setTextColor(Color.WHITE);
        btnMenu.setTypeface(null, Typeface.BOLD);

        GradientDrawable btnMenuShape = new GradientDrawable();
        btnMenuShape.setColor(Color.parseColor("#150A0813"));
        btnMenuShape.setStroke(3, Color.WHITE);
        btnMenuShape.setCornerRadius(100f);
        btnMenu.setBackground(btnMenuShape);

        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        menuParams.topMargin = 30;
        btnMenu.setLayoutParams(menuParams);

        btnMenu.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        // Add các view vào Layout
        layout.addView(tvDialogTitle);
        layout.addView(tvDialogMessage);
        layout.addView(btnRestart);
        layout.addView(btnMenu);

        // Hiển thị Dialog
        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(850, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moveSound != null) moveSound.release();
        if (winSound != null) winSound.release();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        tvPlayerXTime.setText("??s");
        tvPlayerOTime.setText("??s");

        countDownTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;

                if (player1Turn) {
                    tvPlayerXTime.setText(seconds + "s");
                    tvPlayerXTime.setTextColor(Color.parseColor("#00FFFF"));
                    if (seconds < 6) tvPlayerXTime.setTextColor(Color.RED);
                } else {
                    tvPlayerOTime.setText(seconds + "s");
                    tvPlayerOTime.setTextColor(Color.parseColor("#FF007F"));
                    if (seconds < 6) tvPlayerOTime.setTextColor(Color.RED);
                }
            }
            public void onFinish() {
                String winner = player1Turn ? player2Name : player1Name;
                showWinDialog("Hết giờ! " + winner + " thắng.");
            }
        }.start();
    }

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