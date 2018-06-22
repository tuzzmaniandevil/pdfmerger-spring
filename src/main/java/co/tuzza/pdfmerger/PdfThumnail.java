package co.tuzza.pdfmerger;

import java.io.Serializable;

/**
 *
 * @author dylan
 */
public class PdfThumnail implements Serializable {

    private final int pageNum;
    private final String encodedImg;
    private final String format;

    public PdfThumnail(int pageNum, String encodedImg, String format) {
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

}
