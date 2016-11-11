package jguddi;

import java.io.Serializable;

/**
 * Created by gary on 11/11/16.
 */
public class Endpoint implements Serializable{
    private String publishedTo;
    private String url;
    private String qname;

    public String getQname2() {
        return qname2;
    }

    public void setQname2(String qname2) {
        this.qname2 = qname2;
    }

    private String qname2;

    public Endpoint() {
    }

    public Endpoint(String publishedTo, String url, String qname, String qname2) {

        this.publishedTo = publishedTo;
        this.url = url;
        this.qname = qname;
        this.qname2 = qname2;
    }

    public String getPublishedTo() {
        return publishedTo;
    }

    public void setPublishedTo(String publishedTo) {
        this.publishedTo = publishedTo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getqName() {
        return qname;
    }

    public void setqName(String qName) {
        this.qname = qName;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
            "publishedTo='" + publishedTo + '\'' +
            ", url='" + url + '\'' +
            ", qName='" + qname + '\'' +
            '}';
    }
}
