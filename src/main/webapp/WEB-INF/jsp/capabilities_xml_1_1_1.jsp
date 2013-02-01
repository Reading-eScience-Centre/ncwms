<%@page contentType="application/vnd.ogc.wms_xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8" standalone="no"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/Capabilities" prefix="cap"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the Capabilities document in XML for WMS 1.1.1
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config)
         datasets   = collection of datasets to display in this Capabilities document (Collection<Dataset>)
         wmsBaseUrl = Base URL of this server (java.lang.String)
         supportedCrsCodes = List of Strings of supported Coordinate Reference System codes
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
         layerLimit = Maximum number of layers that can be requested simultaneously from this server (int)
         featureInfoFormats = Array of Strings representing MIME types of supported feature info formats
         legendWidth, legendHeight = size of the legend that will be returned from GetLegendGraphic
         paletteNames = Names of colour palettes that are supported by this server (Set<String>)
         verboseTime = boolean flag to indicate whether we should use a verbose or concise version of the TIME value string
     --%>

<!DOCTYPE WMT_MS_Capabilities SYSTEM "http://schemas.opengis.net/wms/1.1.1/capabilities_1_1_1.dtd">
<WMT_MS_Capabilities
        version="1.1.1"
        updateSequence="${utils:dateTimeToISO8601(lastUpdate)}"
        xmlns:xlink="http://www.w3.org/1999/xlink">
    <!-- Service Metadata -->
    <Service>
        <!-- The WMT-defined name for this type of service -->
        <Name>OGC:WMS</Name>
        <!-- Human-readable title for pick lists -->
        <Title><c:out value="${config.title}"/></Title>
        <!-- Narrative description providing additional information -->
        <Abstract><c:out value="${config.serverAbstract}"/></Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <!-- Top-level web address of service or service provider. See also OnlineResource
        elements under <DCPType>. -->
        <OnlineResource xlink:type="simple" xlink:href="<c:out value="${config.serviceProviderUrl}"/>"/>
        <!-- Contact information -->
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson><c:out value="${config.contactName}"/></ContactPerson>
                <ContactOrganization><c:out value="${config.contactOrganization}"/></ContactOrganization>
            </ContactPersonPrimary>
            <ContactVoiceTelephone><c:out value="${config.contactTelephone}"/></ContactVoiceTelephone>
            <ContactElectronicMailAddress><c:out value="${config.contactEmail}"/></ContactElectronicMailAddress>
        </ContactInformation>
        <!-- Fees or access constraints imposed. -->
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
    </Service>
    <Capability>
        <Request>
            <GetCapabilities>
                <Format>application/vnd.ogc.wms_xml</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetCapabilities>
            <GetMap>
                <c:forEach var="mimeType" items="${supportedImageFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetMap>
            <GetFeatureInfo>
                <c:forEach var="mimeType" items="${featureInfoFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetFeatureInfo>
        </Request>
        <Exception>
            <Format>application/vnd.ogc.se_xml</Format>
            <!--<Format>application/vnd.ogc.se_inimage</Format>
            <Format>application/vnd.ogc.se_blank</Format>-->
        </Exception>
        
        <Layer>
            <Title><c:out value="${config.title}"/></Title><%-- Use of c:out escapes XML --%>
            <c:forEach var="crsCode" items="${supportedCrsCodes}">
                <SRS>${crsCode}</SRS>
            </c:forEach>
			<c:forEach var="parentLayer" items="${layers}">
			    <c:if test="${parentLayer.ready}">
			        <cap:layer111 layer="${parentLayer}"/>
			    </c:if>
			    <%-- End if dataset is ready --%>
			</c:forEach>
        </Layer>
    </Capability>
</WMT_MS_Capabilities>