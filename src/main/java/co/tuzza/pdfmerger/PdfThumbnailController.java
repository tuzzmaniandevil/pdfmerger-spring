package co.tuzza.pdfmerger;

import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author dylan
 */
@RestController
public class PdfThumbnailController {

    @Autowired
    private PdfMergingService pdfMergingService;

    @PostMapping("/pdfThumbnail")
    public JsonResult genThumbnails(@RequestPart(name = "file", required = true) MultipartFile file) throws IOException {
        if (pdfMergingService.isPdf(file)) {
            try {
                PDDocument doc = PDDocument.load(file.getInputStream());

                List<PdfThumnail> thumbnails = pdfMergingService.generatePdfThumbnails(doc, 150, "jpg");

                if (thumbnails != null) {
                    return new JsonResult(true, "Success", thumbnails);
                } else {
                    return new JsonResult(false, "Unable to generate thumbnails");
                }
            } catch (IOException ex) {
                return new JsonResult(false, "Error: " + ex.getMessage());
            }
        } else {
            return new JsonResult(false, "File is not a valid PDF");
        }
    }
}
