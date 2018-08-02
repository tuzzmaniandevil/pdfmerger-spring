package co.tuzza.pdfmerger;

import com.drew.imaging.ImageProcessingException;
import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.EnumUtils;
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

    @PostMapping(path = "/mergePdf")
    public ResponseEntity<byte[]> mergePdf(@RequestParam(name = "outputName", required = false) String outputName,
            @RequestPart(name = "files", required = true) MultipartFile[] files,
            @RequestPart(required = false, name = "settings") PdfSettings settings,
            @RequestParam(name = "compress", required = false, defaultValue = "false") boolean compress)
            throws IOException, ServletException, ImageProcessingException {

        if (StringUtils.isEmpty(outputName)) {
            outputName = "output.pdf";
        } else {
            outputName = StringUtils.trim(outputName);

            if (!StringUtils.endsWithIgnoreCase(outputName, ".pdf")) {
                outputName += ".pdf";
            }
        }

        ByteArrayOutputStream pdfos = pdfMergingService.mergeFilesToPdf(files, settings);
        byte[] pdfBytes = pdfos.toByteArray();

        if (compress) {
            ByteArrayOutputStream compressedPdf = pdfMergingService.compressPdf(pdfBytes);
            pdfBytes = compressedPdf.toByteArray();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputName + "\"")
                .contentType(MediaType.APPLICATION_PDF).contentLength(pdfBytes.length).body(pdfBytes);
    }

    public static enum ExportType {
        SeperatePDF,
        SinglePDF,
        SeperateJPEG
    }

    @PostMapping("/splitPdf")
    public ResponseEntity<byte[]> splitPdf(@RequestPart(name = "file", required = true) MultipartFile file,
            @RequestParam(name = "startPage", required = false) Integer startPage,
            @RequestParam(name = "endPage", required = false) Integer endPage,
            @RequestParam(name = "exportType", required = false) String exportTypeString) throws IOException {
        if (StringUtils.isBlank(exportTypeString)) {
            exportTypeString = "SeperatePDF";
        }

        ExportType exportType = EnumUtils.getEnum(ExportType.class, exportTypeString);

        if (exportType == null) {
            exportType = ExportType.SeperatePDF;
        }

        ByteArrayOutputStream pdfOs = pdfMergingService.splitPdf(file, startPage, endPage, exportType);
        byte[] pdfBytes = pdfOs.toByteArray();

        String fileName;
        MediaType mediaType;

        switch (exportType) {
            case SinglePDF: {
                fileName = "attachment; filename=\"splitPdf.pdf\"";
                mediaType = MediaType.APPLICATION_PDF;
                break;
            }
            case SeperateJPEG: {
                fileName = "attachment; filename=\"splitJpegs.zip\"";
                mediaType = MediaType.valueOf("application/zip");
                break;
            }
            case SeperatePDF:
            default: {
                fileName = "attachment; filename=\"splitPdf.zip\"";
                mediaType = MediaType.valueOf("application/zip");
                break;
            }
        }

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, fileName)
                .contentType(mediaType).contentLength(pdfBytes.length).body(pdfBytes);
    }

}
