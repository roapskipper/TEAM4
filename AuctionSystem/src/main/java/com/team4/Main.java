
public static void main(String[] args) {
    // 1. Tạo đối tượng mới
    com.team4.model.User trung = new com.team4.model.User();
    trung.setUsername("trung_dep_trai");
    trung.setFullName("Lê Trung");

    // 2. In ra để kiểm tra UUID tự tạo
    System.out.println("ID của User là: " + trung.getId());
    System.out.println("Thông tin chi tiết: " + trung.toString());
}