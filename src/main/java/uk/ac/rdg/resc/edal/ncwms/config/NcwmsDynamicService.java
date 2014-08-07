package uk.ac.rdg.resc.edal.ncwms.config;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * A dynamic dataset object in the ncWMS configuration system: This object links
 * a dynamic location (local or remote) to the ncWMS system. Once a dynamic
 * dataset is added to the configuration ncWMS will attempt to service any
 * incoming WMS request whose URL resolves to the alias of the service plus a
 * matching (via regex) path, using the dynamically-generated dataset as the
 * source of (meta)data.
 *
 * @author Nathan D Potter
 * @author Guy Griffiths
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsDynamicService {
    /* Alias for this service */
    @XmlAttribute(name = "alias", required = true)
    private String alias;

    /*
     * Either a URL or a local path representing the entry point for a dynamic
     * service
     */
    @XmlAttribute(name = "servicePath", required = true)
    private String path;

    /*
     * A regular expression used to match requested datasets with datasets on
     * the dynamic service
     */
    @XmlAttribute(name = "datasetIdMatch", required = true)
    private String datasetIdMatch;

    /*
     * We'll use a default data readerunless this is overridden in the config
     * file
     */
    @XmlAttribute(name = "dataReaderClass", required = false)
    private String dataReaderClass = "";

    @XmlAttribute(name = "copyrightStatement", required = false)
    private String copyrightStatement = "";

    @XmlAttribute(name = "moreInfoUrl", required = false)
    private String moreInfo = "";

    /* Set true to disable the dataset without removing it completely */
    @XmlAttribute(name = "disabled", required = false)
    private boolean disabled = false;

    @XmlAttribute(name = "queryable", required = false)
    private boolean queryable;

    @XmlTransient
    private Pattern idMatchPattern;


    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getServicePath() {
        return path;
    }

    public void setServicePath(String servicePath) {
        this.path = servicePath.trim();
    }

    public String getDatasetIdMatch() {
        return datasetIdMatch;
    }

    public void setDatasetIdMatch(String datasetIdMatch) {
        this.datasetIdMatch = datasetIdMatch.trim();

        idMatchPattern = Pattern.compile(datasetIdMatch);
    }

    public Pattern getIdMatchPattern() {
        if (idMatchPattern == null)
            idMatchPattern = Pattern.compile(datasetIdMatch);

        return idMatchPattern;
    }

    public String getDataReaderClass() {
        return dataReaderClass;
    }

    public void setDataReaderClass(String dataReaderClass) {
        this.dataReaderClass = dataReaderClass.trim();
    }

    public String getCopyrightStatement() {
        return copyrightStatement;
    }

    public void setCopyrightStatement(String copyrightStatement) {
        this.copyrightStatement = copyrightStatement.trim();
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo.trim();
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isQueryable() {
        return queryable;
    }
    
    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }
}
