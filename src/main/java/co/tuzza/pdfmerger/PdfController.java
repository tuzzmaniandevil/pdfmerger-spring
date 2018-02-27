package co.tuzza.pdfmerger;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

/**
 *
 * @author dylan
 */
@Controller
public class PdfController {

    @PostMapping("/upload")
    public ResponseEntity processFile(HttpServletRequest request) throws IOException, ServletException {
        System.out.println("Hahahahaaha");
        

        return null;
    }
}
