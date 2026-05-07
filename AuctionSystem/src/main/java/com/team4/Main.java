import com.team4.util.DatabaseSetup;

public static void main(String[] args) {
    DatabaseSetup.initDatabase(); // Khởi tạo pool kết nối
    System.out.println("Setup thành công!");
}
