package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public abstract class LayerRequestCallback implements RequestCallback {

    private static final NumberFormat format2Digits = NumberFormat.getFormat("00");
    private static final NumberFormat format4Digits = NumberFormat.getFormat("0000");

    private ErrorHandler err;

    private String units;
    private String extents;
    private String scaleRange;
    private int nColorBands;
    private boolean logScale;
    private List<String> supportedStyles;
    private String zUnits;
    private boolean zPositive;
    private List<String> availableZs;
    private String moreInfo;
    private String copyright;
    private List<String> availablePalettes;
    private String selectedPalette;
    private List<String> availableDates;
    private String nearestTime;
    private String nearestDate;

    public String getUnits() {
        return units;
    }

    public String getExtents() {
        return extents;
    }

    public String getScaleRange() {
        return scaleRange;
    }

    public int getNumColorBands() {
        return nColorBands;
    }

    public boolean isLogScale() {
        return logScale;
    }

    public List<String> getSupportedStyles() {
        return supportedStyles;
    }

    public String getZUnits() {
        return zUnits;
    }

    public boolean isZPositive() {
        return zPositive;
    }

    public List<String> getAvailableZs() {
        return availableZs;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public String getCopyright() {
        return copyright;
    }

    public List<String> getAvailablePalettes() {
        return availablePalettes;
    }

    public String getSelectedPalette() {
        return selectedPalette;
    }

    public List<String> getAvailableDates() {
        return availableDates;
    }

    public String getNearestTime() {
        return nearestTime;
    }

    public String getNearestDate() {
        return nearestDate;
    }

    public LayerRequestCallback(ErrorHandler err) {
        this.err = err;
    }

    public void onError(Request request, Throwable exception){
        err.handleError(exception);
    }

    public void onResponseReceived(Request request, Response response) {
        JSONValue jsonMap = JSONParser.parseLenient(response.getText());
        JSONObject parentObj = jsonMap.isObject();

        JSONValue unitsJson = parentObj.get("units");
        // We don't care if no units are specified
        if (unitsJson != null) {
            units = unitsJson.isString().stringValue();
        }

        JSONValue bboxJson = parentObj.get("bbox");
        if (bboxJson != null) {
            JSONArray bboxArr = bboxJson.isArray();
            if (bboxArr.size() != 4) {
                extents = null;
                err.handleError(new IndexOutOfBoundsException("Wrong number of elements for bounding box: "
                        + bboxArr.size()));
            } else {
                extents = bboxArr.get(0).isString().stringValue() + "," + bboxArr.get(1).isString().stringValue() + ","
                        + bboxArr.get(2).isString().stringValue() + "," + bboxArr.get(3).isString().stringValue();
            }
        } else {
            extents = "-180,-90,180,90";
        }

        JSONValue scaleRangeJson = parentObj.get("scaleRange");
        scaleRange = null;
        if (scaleRangeJson != null) {
            JSONArray scaleRangeArr = scaleRangeJson.isArray();
            if (scaleRangeArr.size() != 2) {
                err.handleError(new IndexOutOfBoundsException("Wrong number of elements for scale range: "
                        + scaleRangeArr.size()));
            } else {
                scaleRange = scaleRangeArr.get(0).isString().stringValue() + ","
                        + scaleRangeArr.get(1).isString().stringValue();
            }
        }

        JSONValue nColorBandsJson = parentObj.get("numColorBands");
        // Set a default value
        nColorBands = 50;
        if (nColorBandsJson != null) {
            JSONNumber nColorBandsNum = nColorBandsJson.isNumber();
            nColorBands = (int) nColorBandsNum.doubleValue();
        }

        JSONValue logScalingJson = parentObj.get("logScaling");
        logScale = false;
        if (logScalingJson != null) {
            logScale = logScalingJson.isBoolean().booleanValue();
        }

        JSONValue supportedStylesJson = parentObj.get("supportedStyles");
        if (supportedStylesJson != null) {
            JSONArray supportedStylesArr = supportedStylesJson.isArray();
            supportedStyles = new ArrayList<String>();
            for (int i = 0; i < supportedStylesArr.size(); i++) {
                supportedStyles.add(supportedStylesArr.get(i).isString().stringValue());
            }
        } else {
            err.handleError(new NullPointerException("No styles listed"));
        }

        JSONValue zvalsJson = parentObj.get("zaxis");
        if (zvalsJson != null) {
            JSONObject zvalsObj = zvalsJson.isObject();
            zUnits = zvalsObj.get("units").isString().stringValue();
            zPositive = zvalsObj.get("positive").isBoolean().booleanValue();
            availableZs = new ArrayList<String>();
            JSONArray zvalsArr = zvalsObj.get("values").isArray();
            for (int i = 0; i < zvalsArr.size(); i++) {
                availableZs.add(zvalsArr.get(i).isNumber().toString());
            }
        } else {
            availableZs = null;
            zUnits = null;
            zPositive = true;
        }

        JSONValue moreInfoJson = parentObj.get("moreInfo");
        if (moreInfoJson != null) {
            moreInfo = moreInfoJson.isString().stringValue();
        }

        JSONValue copyrightJson = parentObj.get("copyright");
        if (copyrightJson != null) {
            copyright = copyrightJson.isString().stringValue();
        }

        JSONValue palettesJson = parentObj.get("palettes");
        if (palettesJson != null) {
            JSONArray palettesArr = palettesJson.isArray();
            availablePalettes = new ArrayList<String>();
            for (int i = 0; i < palettesArr.size(); i++) {
                availablePalettes.add(palettesArr.get(i).isString().stringValue());
            }
            // TODO Don't select default palette if a Cookie for this
            // layer exists - get palette from Cookie
            JSONValue defaultPaletteJson = parentObj.get("defaultPalette");
            if (defaultPaletteJson != null) {
                selectedPalette = defaultPaletteJson.isString().stringValue();
            }
        }

        JSONValue datesJson = parentObj.get("datesWithData");
        if (datesJson != null) {
            // TODO deal with timeAxisUnits (currently assume ISO8601)
            JSONObject datesObj = datesJson.isObject();
            availableDates = new ArrayList<String>();
            for (String yearString : datesObj.keySet()) {
                int year = Integer.parseInt(yearString);
                JSONObject yearObj = datesObj.get(yearString).isObject();
                for (String monthString : yearObj.keySet()) {
                    // Months start from zero
                    int month = Integer.parseInt(monthString);
                    JSONArray daysArr = yearObj.get(monthString).isArray();
                    for (int iDay = 0; iDay < daysArr.size(); iDay++) {
                        int day = (int) daysArr.get(iDay).isNumber().doubleValue();
                        availableDates.add(format4Digits.format(year) + "-"
                                + format2Digits.format(month+1) + "-" + format2Digits.format(day));
                    }
                }
            }

            // If we have different times, we may (will?) have a nearest
            // time string.
            JSONValue nearestTimeJson = parentObj.get("nearestTimeIso");
            if (nearestTimeJson != null) {
                nearestTime = nearestTimeJson.isString().stringValue();
                nearestDate = nearestTimeJson.isString().stringValue().substring(0, 10);
            }
        }
    }

}
