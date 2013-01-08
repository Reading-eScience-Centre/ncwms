<%@tag description="Displays a layer in the format expected by getCapabilities v1.3.0.  By defining it in a taglib, nested layers are easy." pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/Capabilities" prefix="cap"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%@attribute name="metadata" required="true" type="uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata" description="The range metadata object to display"%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.wms.Dataset" description="The dataset object to display"%>
<%@attribute name="feature" required="true" type="uk.ac.rdg.resc.edal.feature.Feature" description="The feature containing the layer" %>

<Layer <c:if test="${utils:isPlottable(metadata)}"> queryable="1"</c:if>>
    <Name>${dataset.id}/${feature.id}/${metadata.name}</Name>
    <Title><c:out value="${metadata.title}" /></Title>
    <c:if test="${utils:isPlottable(metadata)}">
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
	    <BoundingBox CRS="CRS:84" minx="${bbox.minX}" maxx="${bbox.maxX}" miny="${bbox.minY}" maxy="${bbox.maxY}" />
	    <c:set var="vAxis" value="${utils:getVerticalAxis(feature)}"/>
	    <c:if test="${not empty vAxis}">
	        <Dimension name="elevation" units="${vAxis.verticalCrs.units.unitString}" default="${utils:getDefaultElevation(vAxis)}">
	                <%-- Print out the dimension values, comma separated, making sure
	                                 that there is no comma at the start or end of the list.  Note that
	                                 we can't use ${fn:join} because the z values are an array of doubles,
	                                 not strings. --%>
	            <c:forEach var="zval" items="${vAxis.coordinateValues}" varStatus="status">
	                <c:if test="${status.index > 0}">,</c:if>${zval}
	            </c:forEach>
	        </Dimension>
	    </c:if>
	    <c:set var="tAxis" value="${utils:getTimeAxis(feature)}"/>
	    <c:if test="${not empty tAxis}">
	        <c:set var="tvalues" value="${tAxis.coordinateValues}" />
	        <Dimension name="time" units="${utils:getTimeAxisUnits(tAxis.calendarSystem)}" multipleValues="true" current="true" default="${utils:dateTimeToISO8601(utils:getDefaultTime(tAxis))}">
	         <c:choose>
	             <c:when test="${verboseTimes}">
	                 <%-- Use the verbose version of the time string --%>
	                 <c:forEach var="tval" items="${tvalues}" varStatus="status">
	                     <c:if test="${status.index > 0}">,</c:if>
	                     ${utils:dateTimeToISO8601(tval)}
	                 </c:forEach>
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
    </c:if>
    <c:if test="${not empty metadata.memberNames}">
        <c:forEach var="memberName" items="${metadata.memberNames}">
            <c:set var="childMetadata" value="${utils:getChildMetadata(metadata, memberName)}"/>
            <cap:layer metadata="${childMetadata}" dataset="${dataset}" feature="${feature}"/>
        </c:forEach>
    </c:if>
</Layer>