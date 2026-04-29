package com.team4.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class User extends Entity {

    // Dùng enum
    public enum Role {
        ADMIN,
        SELLER,
        BIDDER
    }
    // Dùng Regex để kiểm tra định dạng của tên và email
    // Tên User phải có 4-30 kí tự
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    // Email hợp lệ phải có dạng local@domain.tld, không có khoảng trắng, và có ít nhất 1 ký tự ở mỗi phần
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String username;
    // Mật khẩu sẽ được lưu dưới dạng hash theo nguyên tắc hash + salt. Viêc xử lý sẽ được thực hiện ở service
    private String passwordHash;
    private String fullName;
    private String email;
    private final Role role;
    private BigDecimal balance; // dùng BigDecimal để chính xác hơn với tiền tệ

    /**
     * Dùng khi tạo user mới trong hệ thống.
     * Password ở đây phải là passwordHash, không phải raw password.
     */
    protected User(String username, String passwordHash, String fullName, String email, Role role) {
        super();
        this.username = normalizeUsername(username);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.fullName = normalizeOptional(fullName);
        this.email = normalizeEmail(email);
        this.role = Objects.requireNonNull(role, "role không được null");
        this.balance = money(BigDecimal.ZERO);

        validateBaseInfo();
    }

    /**
     * Dùng khi nạp user từ database.
     */
    protected User(String id, LocalDateTime createdAt, String username, String passwordHash, String fullName, String email, Role role, BigDecimal balance) {
        super(id, createdAt);
        this.username = normalizeUsername(username);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.fullName = normalizeOptional(fullName);
        this.email = normalizeEmail(email);
        this.role = Objects.requireNonNull(role, "role không được null" );
        this.balance = money(balance);

        validateBaseInfo();
    }

    // Kiểm tra tính hợp lệ các thông tin của người dùng
    protected final void validateBaseInfo() {
        // Nếu tên đăng nhập không đúng với regex thì ném ra lỗi
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập phải dài từ 4–30 ký tự và chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) và gạch ngang (-)."
            );
        }
        // Email cũng vậy
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        // Họ tên không được rỗng và dài không quá 50 kí tự
        if (fullName != null && fullName.length() > 50) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 100 ký tự.");
        }
        // Số dư không được âm
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư không thể âm.");
        }
    }

    public final void updateProfile(String fullName, String email) {
        this.fullName = normalizeOptional(fullName);
        this.email = normalizeEmail(email);
        validateBaseInfo();
    }

    // Tạo method để thay đổi mật khẩu, method này sẽ nhận mật khẩu đó dưới dạng hash và kiểm tra xem có hợp lệ không
    public final void changePasswordHash(String newPasswordHash) {
        this.passwordHash = requirePasswordHash(newPasswordHash);
    }

    // Nạp tiền
    public final void deposit(BigDecimal amount) {
        BigDecimal normalizedAmount = money(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tền nạp va phải dương.");
        }

        this.balance = this.balance.add(normalizedAmount);
    }

    // Rút tiền
    public final boolean withdraw(BigDecimal amount) {
        BigDecimal normalizedAmount = money(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải dương.");
        }

        if (this.balance.compareTo(normalizedAmount) < 0) {
            return false;
        }

        this.balance = this.balance.subtract(normalizedAmount);
        return true;
    }
    // Kiểm tra số tiền có đủ để thanh toán không
    public final boolean hasEnoughBalance(BigDecimal amount) {
        BigDecimal normalizedAmount = money(amount);
        return this.balance.compareTo(normalizedAmount) >= 0;
    }

    /**
     * Cho DAO dùng để lưu vào DB.
     * Không trả ra client.
     */
    public final String readPasswordHashForPersistence() {
        return passwordHash;
    }

    public final String getUsername() {
        return username;
    }

    public final void setUsername(String username) {
        this.username = normalizeUsername(username);
        validateBaseInfo();
    }

    public final String getFullName() {
        return fullName;
    }

    public final String getEmail() {
        return email;
    }

    public final Role getRole() {
        return role;
    }

    public final BigDecimal getBalance() {
        return balance;
    }

    // Không dùng UI cứng
    @Override
    public String toString() {
        return "User: " +
                "id: '" + getId() +
                " | username: " + username +
                " | fullName: " + fullName +
                " | email: " + email +
                " | role: " + role +
                " | balance: " + balance +
                " | createdAt: " + getCreatedAt();
    }

    /** CÁC HÀM CHUẨN HÓA (thuôc về lớp,không thuộc về đối tượng). Dùng để chuẩn hóa dữ liệu đầu vào trước khi gán vào thuộc tính của đối tượng
     * username: loại bỏ khoảng trắng ở đầu và cuối, và kiểm tra null
     * email: loại bỏ khoảng trắng, chuyển về chữ thường, và kiểm tra null
     */
    protected static String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username không được bỏ trống.");
        }
        return username.trim();
    }

    protected static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email không được bỏ trống.");
        }
        return email.trim().toLowerCase();
    }
    // Method để định dạng cho nhiều field.
    protected static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Mật khẩu không được null
    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ.");
        }
        return passwordHash.trim();
    }

    // Làm tròn tiền đến phần trăm
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount không được bỏ trống.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
