import java.io.Serializable;

public class CertificateInfo implements Serializable {
    private String thumbPrint;
    private String certValue;

    public String getThumbPrint() {

        return thumbPrint;
    }

    public void setThumbPrint(String thumbPrint) {

        this.thumbPrint = thumbPrint;
    }

    public String getCertValue() {

        return certValue;
    }

    public void setCertValue(String certValue) {

        this.certValue = certValue;
    }
}
