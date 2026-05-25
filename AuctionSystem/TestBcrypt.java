import org.mindrot.jbcrypt.BCrypt;

public class TestBcrypt {
    public static void main(String[] args) {
        String hash = "$2a$10$wNq/r5sH6g7R9x1/4L8t1u.5/K9z2Z5v8Y7w3Q4x1V2u3S6t9R0qK";
        System.out.println("Checking bidder123: " + BCrypt.checkpw("bidder123", hash));
    }
}
