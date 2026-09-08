package vn.edu.aros.aroscore.utils;

import java.util.regex.Pattern;

/**
 * Quy tắc mã sinh viên cho OMR (cố định 8 chữ số).
 */
public final class StudentCodeRules {

    public static final int OMR_STUDENT_CODE_LENGTH = 8;
    public static final String OMR_STUDENT_CODE_PATTERN = "^\\d{8}$";

    private static final Pattern PATTERN = Pattern.compile(OMR_STUDENT_CODE_PATTERN);

    private StudentCodeRules() {
    }

    public static boolean isValidOmrFormat(String studentCode) {
        return studentCode != null && PATTERN.matcher(studentCode.trim()).matches();
    }

    /**
     * Chuẩn hóa và validate format OMR 8 số. Trả về mã đã trim.
     * @throws IllegalArgumentException nếu null/blank hoặc sai format
     */
    public static String requireValidOmrFormat(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống!");
        }
        String normalized = studentCode.trim();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Mã sinh viên phải gồm đúng " + OMR_STUDENT_CODE_LENGTH + " chữ số!");
        }
        return normalized;
    }
}
