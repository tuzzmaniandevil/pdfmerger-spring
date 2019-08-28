package co.tuzza.pdfmerger;

import java.io.Serializable;

/**
 *
 * @author dylan
 */
public class JsonResult implements Serializable {

    private final boolean status;
    private final String message;
    private final Object data;

    public JsonResult(boolean status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public JsonResult(boolean status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

}
