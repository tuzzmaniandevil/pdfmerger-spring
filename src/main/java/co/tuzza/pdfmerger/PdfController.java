package co.tuzza.pdfmerger;

import com.drew.imaging.ImageProcessingException;
import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author dylan
 */
@Controller
public class PdfController {

    @Autowired
    private PdfMergingService pdfMergingService;

    @PostMapping("/mergePdf")
    public ResponseEntity<byte[]> mergePdf(
            @RequestParam(name = "outputName", required = false) String outputName,
            @RequestPart(name = "files", required = true) MultipartFile[] files) throws IOException, ServletException, ImageProcessingException {

        if (StringUtils.isEmpty(outputName)) {
            outputName = "output.pdf";
        } else {
            outputName = StringUtils.trim(outputName);

            if (!StringUtils.endsWithIgnoreCase(outputName, ".pdf")) {
                outputName += ".pdf";
            }
        }

        ByteArrayOutputStream pdfos = pdfMergingService.mergeFilesToPdf(files);
        byte[] pdfBytes = pdfos.toByteArray();

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + outputName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    @PostMapping("/splitPdf")
    public ResponseEntity<byte[]> splitPdf(@RequestPart(name = "file", required = true) MultipartFile file) throws IOException {
        ByteArrayOutputStream pdfOs = pdfMergingService.splitPdf(file);
        byte[] pdfBytes = pdfOs.toByteArray();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"splitPdf.zip\"")
                .contentType(MediaType.valueOf("application/zip"))
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

}
