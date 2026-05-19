package com.team4.service;

import com.team4.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import com.team4.util.BusinessException;

/**
 * Lớp dịch vụ này chịu trách nhiệm cho việc tạo và xác thực các JSON Web Tokens (JWT).
 * Nó là trung tâm của hệ thống xác thực, giúp chuyển đổi thông tin người dùng
 * thành một chuỗi token an toàn và ngược lại.
 */
public class JwtService {

    // Khởi tạo logger cho lớp này để ghi lại các sự kiện quan trọng.
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    // CHÌA KHÓA BÍ MẬT - YẾU TỐ QUAN TRỌNG NHẤT CỦA BẢO MẬT JWT.
    // Đây là một chuỗi bí mật chỉ server biết, dùng để ký và xác thực token.
    // LƯU Ý: Trong một dự án thực tế, chuỗi này KHÔNG BAO GIỜ được viết cứng trong code.
    // Nó nên được lưu trong file cấu hình (.properties), biến môi trường, hoặc một dịch vụ quản lý bí mật.
    private static final String SECRET_STRING = "DayLaChuoiBiMatSieuDaiVaKhongTheDoanNoiCuaToi_HayThayTheBangChuoiCuaBan_1234567890";

    // Tạo đối tượng Key từ chuỗi bí mật để sử dụng cho việc ký
    private static final Key SECRET_KEY = new SecretKeySpec(Base64.getDecoder().decode(SECRET_STRING),
            SignatureAlgorithm.HS256.getJcaName());

    // Thời gian hết hạn của token, ví dụ: 24 giờ.
    // Hết thời gian này, người dùng sẽ phải đăng nhập lại.
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * Tạo ra một chuỗi JWT từ thông tin của người dùng.
     * Hàm này được gọi sau khi người dùng đăng nhập thành công.
     *
     * @param user Đối tượng User chứa thông tin cần đưa vào token.
     * @return Một chuỗi JWT.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                // 1. Subject (chủ thể): Thường là một định danh duy nhất của người dùng, ở đây ta dùng username.
                .setSubject(user.getUsername())

                // 2. Custom Claims (thông tin thêm): Đặt các thông tin cần thiết khác vào đây.
                // Các thông tin này có thể được truy cập lại sau khi giải mã token.
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())

                // 3. Issued At (thời điểm phát hành): Thời gian token được tạo.
                .setIssuedAt(now)

                // 4. Expiration (thời điểm hết hạn): Thời điểm token không còn hợp lệ.
                .setExpiration(expiryDate)

                // 5. Signature (chữ ký): Ký lên token bằng thuật toán HS256 và chìa khóa bí mật.
                // Đây là phần đảm bảo token không bị giả mạo.
                .signWith(SECRET_KEY)

                // 6. Compact: Hoàn tất và tạo ra chuỗi JWT.
                .compact();
    }

    /**
     * Xác thực một chuỗi JWT và trích xuất thông tin (Claims) từ nó.
     * Thư viện jjwt sẽ tự động kiểm tra chữ ký và thời gian hết hạn.
     *
     * @param token Chuỗi JWT do client gửi lên.
     * @return Đối tượng Claims chứa thông tin từ token nếu hợp lệ, ngược lại trả về null.
     */
    public Claims getClaimsFromToken(String token) {
        try {
            // Dùng parser để giải mã token.
            // Nó sẽ dùng SECRET_KEY để xác thực chữ ký.
            // Nếu chữ ký không khớp, token đã hết hạn, hoặc token bị lỗi, nó sẽ ném ra một Exception.
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (BusinessException ex) {
            // Token hết hạn là một lỗi thường gặp, không quá nghiêm trọng. Dùng mức WARN.
            logger.warn("JWT đã hết hạn: {}", ex.getMessage());
            throw new BusinessException("JWT đã hết hạn");
        } catch (SignatureException ex) {
            // Lỗi chữ ký là một vấn đề BẢO MẬT. Có thể ai đó đang cố giả mạo token. Dùng mức ERROR.
            logger.error("Lỗi chữ ký JWT không hợp lệ. Có thể là dấu hiệu của một cuộc tấn công.", ex);
            throw new BusinessException("Lỗi chữ ký JWT không hợp lệ. Có thể là dấu hiệu của một cuộc tấn công.");
        } catch (MalformedJwtException ex) {
            // Token bị sai định dạng, có thể do client gửi lỗi hoặc bị cắt xén. Dùng mức WARN.
            logger.warn("Token JWT không đúng định dạng: {}", ex.getMessage());
            throw new BusinessException("Token JWT không đúng định dạng");
        } catch (UnsupportedJwtException ex) {
            logger.warn("Token JWT không được hỗ trợ: {}", ex.getMessage());
            throw new BusinessException("Token JWT không được hỗ trợ");
        } catch (IllegalArgumentException ex) {
            logger.warn("Chuỗi JWT rỗng hoặc null.", ex);
            throw new BusinessException("Chuỗi JWT rỗng hoặc null.");
        }
    }
}
