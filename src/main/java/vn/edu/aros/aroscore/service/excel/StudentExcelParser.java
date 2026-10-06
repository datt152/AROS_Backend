package vn.edu.aros.aroscore.service.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Đọc .xlsx theo template FE cố định (bỏ qua dòng 1 = tiêu đề):
 * A=STT (bỏ qua, không lưu DB) | B=Họ đệm | C=Tên | D=MSSV | E=Email
 * fullName lưu DB = "Họ đệm" + " " + "Tên".
 */
@Component
public class StudentExcelParser {

    public static final int MAX_ROWS = 500;
    /** Cột A — chỉ có trên file, không map vào entity. */
    @SuppressWarnings("unused")
    private static final int COL_STT = 0;
    private static final int COL_LAST_MIDDLE = 1;
    private static final int COL_FIRST_NAME = 2;
    private static final int COL_STUDENT_CODE = 3;
    private static final int COL_EMAIL = 4;

    private final DataFormatter formatter = new DataFormatter();

    public List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File Excel không được để trống!");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!name.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file .xlsx!");
        }

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("File Excel không có sheet dữ liệu!");
            }

            List<ParsedRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            // Dòng 0 = tiêu đề (bỏ qua), đọc từ dòng 1
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                // Cột A (STT) giữ trên file cho người dùng — không đọc vào DB
                String lastMiddle = cellString(row, COL_LAST_MIDDLE);
                String firstName = cellString(row, COL_FIRST_NAME);
                String studentCode = cellString(row, COL_STUDENT_CODE);
                String email = cellString(row, COL_EMAIL);

                if (isBlank(email) && isBlank(lastMiddle) && isBlank(firstName) && isBlank(studentCode)) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    throw new IllegalArgumentException("File vượt quá " + MAX_ROWS + " dòng dữ liệu!");
                }

                String fullName = joinFullName(lastMiddle, firstName);
                rows.add(new ParsedRow(i + 1, email, fullName, studentCode, lastMiddle, firstName));
            }

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("File không có dòng dữ liệu sinh viên!");
            }
            return rows;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đọc được file Excel: " + e.getMessage());
        }
    }

    private static String joinFullName(String lastMiddle, String firstName) {
        String left = lastMiddle != null ? lastMiddle.trim() : "";
        String right = firstName != null ? firstName.trim() : "";
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + " " + right;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String cellString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double n = cell.getNumericCellValue();
            if (n == Math.floor(n) && !Double.isInfinite(n)) {
                return String.valueOf((long) n);
            }
        }
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    public record ParsedRow(
            int rowNumber,
            String email,
            String fullName,
            String studentCode,
            String lastMiddle,
            String firstName) {
    }
}
