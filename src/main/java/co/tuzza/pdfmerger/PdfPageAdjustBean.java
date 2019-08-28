package co.tuzza.pdfmerger;

import java.io.Serializable;
import javax.validation.constraints.Digits;

/**
 *
 * @author dylan
 */
public class PdfPageAdjustBean implements Serializable {

    @Digits(fraction = 0, integer = 1000)
    private int originalPageNum;

    @Digits(fraction = 0, integer = 1000)
    private int newPageNum;

    @Digits(fraction = 0, integer = 360)
    private int rotate;

    public int getOriginalPageNum() {
        return originalPageNum;
    }

    public int getNewPageNum() {
        return newPageNum;
    }

    public int getRotate() {
        return rotate;
    }

}
