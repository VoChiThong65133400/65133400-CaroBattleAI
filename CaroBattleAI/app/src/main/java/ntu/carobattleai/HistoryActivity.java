package ntu.carobattleai;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private ListView lvHistory;
    private ArrayList<String> historyList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        lvHistory = findViewById(R.id.lvHistory);
        Button btnBack = findViewById(R.id.btnBackHistory);

        historyList = new ArrayList<>();
        // ArrayAdapter đơn giản để hiển thị mỗi trận đấu thành 1 dòng chữ
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        lvHistory.setAdapter(adapter);

        // Kết nối Firebase lấy dữ liệu
        loadHistoryFromFirebase();

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadHistoryFromFirebase() {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("LichSuChoi");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                historyList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // Lấy các thông tin từ Map trên Firebase
                    String ketQua = data.child("ketQua").getValue(String.class);
                    String doKho = data.child("doKho").getValue(String.class);
                    String thoiGian = data.child("thoiGian").getValue(String.class);

                    // Gộp thành 1 chuỗi để hiển thị
                    String entry = "Kết quả: " + ketQua + "\nĐộ khó: " + doKho + "\nNgày: " + thoiGian;
                    historyList.add(0, entry); // add(0,...) để trận mới nhất hiện lên đầu
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Xử lý lỗi nếu có
            }
        });
    }
}