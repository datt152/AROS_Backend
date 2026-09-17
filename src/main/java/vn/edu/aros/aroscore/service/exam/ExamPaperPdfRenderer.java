package vn.edu.aros.aroscore.service.exam;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.aros.aroscore.dto.print.ExamPaperPrintModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service
@RequiredArgsConstructor
public class ExamPaperPdfRenderer {

    private final TemplateEngine templateEngine;

    public byte[] render(ExamPaperPrintModel paper) {
        Context ctx = new Context();
        ctx.setVariable("paper", paper);
        String html = templateEngine.process("exam/exam-paper", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.useFont(() -> openFont("fonts/TimesNewRoman-Regular.ttf"), "TimesNewRoman");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được file PDF đề thi: " + e.getMessage(), e);
        }
    }

    private InputStream openFont(String classpath) {
        try {
            return new ClassPathResource(classpath).getInputStream();
        } catch (Exception e) {
            throw new UncheckedIOException(
                    new java.io.IOException("Không đọc được font: " + classpath, e));
        }
    }
}
