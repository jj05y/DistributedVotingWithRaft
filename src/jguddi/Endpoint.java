package jguddi;

import java.io.Serializable;

/**
 * Created by gary on 11/11/16.
 */
public class Endpoint implements Serializable{
    private String publishedTo;
    private String url;
    private String qName;

    public Endpoint() {
    }

    public Endpoint(String publishedTo, String url, String qName) {

        this.publishedTo = publishedTo;
        this.url = url;
        this.qName = qName;
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
        return qName;
    }

    public void setqName(String qName) {
        this.qName = qName;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
            "publishedTo='" + publishedTo + '\'' +
            ", url='" + url + '\'' +
            ", qName='" + qName + '\'' +
            '}';
    }
}
