package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

public interface AviExportHandler {
    public String getAviUrl(String times, String fps);
    public void animationStarted(String times, String fps);
    public void animationStopped();
}
