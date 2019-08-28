package co.tuzza.pdfmerger;

import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author dylan
 */
public class PdfAdjustBean implements Serializable {

    @NotNull
    @Size(min = 1)
    private String fileName;

    @Digits(fraction = 0, integer = 1000)
    private int order;

    private List<PdfPageAdjustBean> pages;

    public String getFileName() {
        return fileName;
    }

    public int getOrder() {
        return order;
    }

    public List<PdfPageAdjustBean> getPages() {
        return pages;
    }
}
