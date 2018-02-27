package co.tuzza.pdfmerger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;

/**
 *
 * @author dylan
 */
@SpringBootApplication
public class PdfMerger {

    public static void main(String[] args) {
        SpringApplication.run(PdfMerger.class, args);
    }
}
