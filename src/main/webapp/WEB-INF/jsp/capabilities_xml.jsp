<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%>
<%-- tag library for useful utility functions --%>
<%
    response.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
    response.setHeader("Pragma", "no-cache"); //HTTP 1.0
    response.setDateHeader("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the Capabilities document in XML.
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.wms.ServerConfig)
         datasets   = collection of datasets to display in this Capabilities document (Collection<uk.ac.rdg.resc.ncwms.wms.Dataset>)
         lastUpdate = Last update time of the dataset(s) displayed in this document (org.joda.time.DateTime)
         wmsBaseUrl = Base URL of this server (java.lang.String)
         supportedCrsCodes = List of Strings of supported Coordinate Reference System codes
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
         layerLimit = Maximum number of layers that can be requested simultaneously from this server (int)
         featureInfoFormats = Array of Strings representing MIME types of supported feature info formats
         legendWidth, legendHeight = size of the legend that will be returned from GetLegendGraphic
         paletteNames = Names of colour palettes that are supported by this server (Set<String>)
         verboseTime = boolean flag to indicate whether we should use a verbose or concise version of the TIME value string
     --%>
<WMS_Capabilities version="1.3.0"
	updateSequence="${utils:dateTimeToISO8601(lastUpdate)}"
	xmlns="http://www.opengis.net/wms"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd">

<Service>
<Name>WMS</Name>
<Title><c:out value="${config.title}" /></Title>
<Abstract>
<c:out value="${config.serverAbstract}" />
</Abstract>
<KeywordList>
<%-- forEach recognizes that keywords is a comma-delimited String --%>
<c:forEach var="keyword" items="${config.keywords}">
	<Keyword>${keyword}</Keyword>
</c:forEach>
</KeywordList>
<OnlineResource xlink:type="simple"
	xlink:href="<c:out value="${config.serviceProviderUrl}"/>" />
<ContactInformation>
<ContactPersonPrimary>
<ContactPerson>
<c:out value="${config.contactName}" />
</ContactPerson>
<ContactOrganization>
<c:out value="${config.contactOrganization}" />
</ContactOrganization>
</ContactPersonPrimary>
<ContactVoiceTelephone>
<c:out value="${config.contactTelephone}" />
</ContactVoiceTelephone>
<ContactElectronicMailAddress>
<c:out value="${config.contactEmail}" />
</ContactElectronicMailAddress>
</ContactInformation>
<Fees>none</Fees>
<AccessConstraints>none</AccessConstraints>
<LayerLimit>${layerLimit}</LayerLimit>
<MaxWidth>${config.maxImageWidth}</MaxWidth>
<MaxHeight>${config.maxImageHeight}</MaxHeight>
</Service>
<Capability>
<Request>
<GetCapabilities>
<Format>text/xml</Format>
<DCPType>
<HTTP>
<Get>
<OnlineResource xlink:type="simple"
	xlink:href="<c:out value="${wmsBaseUrl}"/>" />
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
<OnlineResource xlink:type="simple"
	xlink:href="<c:out value="${wmsBaseUrl}"/>" />
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
<OnlineResource xlink:type="simple"
	xlink:href="<c:out value="${wmsBaseUrl}"/>" />
</Get>
</HTTP>
</DCPType>
</GetFeatureInfo>
</Request>
<Exception>
<Format>XML</Format>
</Exception>
<Layer>
<Title><c:out value="${config.title}" /></Title>
<%-- Use of c:out escapes XML --%>
<c:forEach var="crsCode" items="${supportedCrsCodes}">
	<CRS>${crsCode}</CRS>
</c:forEach>
<c:forEach var="dataset" items="${datasets}">
	<c:if test="${dataset.ready}">
		<Layer>
		<Title><c:out value="${dataset.title}" /></Title>
		<c:forEach var="feature" items="${dataset.featureCollection.features}">
			<c:forEach var="metadata" items="${utils:getPlottableLayers(feature)}">
				<Layer <c:if test="${dataset.queryable}"> queryable="1"</c:if>>
				<Name>${dataset.id}/${feature.id}/${metadata.name}</Name>
				<Title><c:out value="${metadata.description}" /></Title>
				<Abstract>
				<c:out value="${feature.description}" />
				</Abstract>
				<c:set var="bbox" value="${utils:getWmsBoundingBox(feature)}" />
				<EX_GeographicBoundingBox>
				<westBoundLongitude>${bbox.minX}</westBoundLongitude>
				<eastBoundLongitude>${bbox.maxX}</eastBoundLongitude>
				<southBoundLatitude>${bbox.minY}</southBoundLatitude>
				<northBoundLatitude>${bbox.maxY}</northBoundLatitude>
				</EX_GeographicBoundingBox>
				<BoundingBox CRS="CRS:84" minx="${bbox.minX}" maxx="${bbox.maxX}"
					miny="${bbox.minY}" maxy="${bbox.maxY}" />
			    <c:set var="vAxis" value="${utils:getVerticalAxis(feature)}"/>
				<c:if test="${not empty vAxis}">
					<Dimension name="elevation"
						units="${vAxis.verticalCrs.units.unitString}"
						default="${utils:getDefaultElevation(feature)}">
					<%-- Print out the dimension values, comma separated, making sure
		                             that there is no comma at the start or end of the list.  Note that
		                             we can't use ${fn:join} because the z values are an array of doubles,
		                             not strings. --%>
					<c:forEach var="zval"
						items="${vAxis.coordinateValues}"
						varStatus="status">
						<c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
					</Dimension>
				</c:if>
			    <c:set var="tAxis" value="${utils:getTimeAxis(feature)}"/>
				<c:if test="${not empty tAxis}">
					<c:set var="tvalues"
						value="${tAxis.coordinateValues}" />
					<Dimension name="time"
						units="${utils:getTimeAxisUnits(tAxis.calendarSystem)}"
						multipleValues="true" current="true"
						default="${utils:dateTimeToISO8601(utils:getDefaultTime(tAxis))}">
					<c:choose>
						<c:when test="${verboseTimes}">
							<%-- Use the verbose version of the time string --%>
							<c:forEach var="tval" items="${tvalues}" varStatus="status">
								<c:if test="${status.index > 0}">,</c:if>${utils:dateTimeToISO8601(tval)}</c:forEach>
						</c:when>
						<c:otherwise>
							<%-- Use the most concise version of the time string --%>
							<c:out value="${utils:getTimeStringForCapabilities(tvalues)}" />
						</c:otherwise>
					</c:choose>
					</Dimension>
				</c:if>

				<c:forEach var="style" items="${utils:getFullStyles(feature, metadata.name, paletteNames)}">
					<Style>
                         <Name>${style}</Name>
                         <Title>${style}</Title>
                         <Abstract>${style.stylename} style<c:if test="${not empty style.palettename}">, using the ${style.palettename} palette</c:if></Abstract>
                         <c:if test="${not empty style.palettename}">
	                         <LegendURL width="${legendWidth}" height="${legendHeight}">
	                             <Format>image/png</Format>
	                             <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}?REQUEST=GetLegendGraphic&amp;PALETTE=${style.palettename}&amp;COLORBARONLY=true&amp;WIDTH=${legendWidth}&amp;HEIGHT=${legendHeight}"/>
	                         </LegendURL>
	                     </c:if>
                    </Style>
				</c:forEach>

				</Layer>
			</c:forEach>
			<%-- End loop through members --%>
		</c:forEach>
		<%-- End loop through features --%>
		</Layer>
	</c:if>
	<%-- End if dataset is ready --%>
</c:forEach>
<%-- End loop through datasets --%>
</Layer>
</Capability>

</WMS_Capabilities>