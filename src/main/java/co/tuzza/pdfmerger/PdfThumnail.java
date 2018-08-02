package co.tuzza.pdfmerger;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Base64;

/**
 *
 * @author dylan
 */
public class PdfThumnail extends ByteArrayInputStream implements Serializable {

    private final int pageNum;
    private final String encodedImg;
    private final String format;

    public PdfThumnail(int pageNum, String encodedImg, String format) {
        super(Base64.getDecoder().decode(encodedImg));
        this.pageNum = pageNum;
        this.encodedImg = encodedImg;
        this.format = format;
    }

    public int getPageNum() {
        return pageNum;
    }

    public String getEncodedImg() {
        return encodedImg;
    }

    public String getFormat() {
        return format;
    }

    public byte[] imageBytes() {
        return Base64.getDecoder().decode(encodedImg);
    }

}
