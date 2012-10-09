<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Shows data from a GetFeatureInfo request as GML
     
     Data (models) passed in to this page:
          longitude = longitude of the point of interest (float)
          latitude = latitude of the point of interest (float)
          varName = the name of the variable being plotted
          coords = the position of the point of interest, in the requested CRS
          crs = the code for the CRS
          data = Map of joda-time DateTime objects to data values (Map<DateTime, Float>) --%>
<msGMLOutput xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">          
    <c:forEach var="featureInfo" items="${data}">
        <${featureInfo.featureId}_feature>
            <${featureInfo.memberId}_layer>
                <c:if test="${not empty featureInfo.actualPos}">
                    <c:set value="${utils:getLatLon(featureInfo.actualPos)}" var="actualLatLon"/>
                    <longitude>${actualLatLon.longitude}</longitude>
                    <latitude>${actualLatLon.latitude}</latitude>
					<gml:boundedBy>
						<gml:Box srsName="${crs}">
							<gml:coordinates>
		                        ${featureInfo.actualPos.x},${featureInfo.actualPos.y},${featureInfo.actualPos.x},${featureInfo.actualPos.y}
							</gml:coordinates>
						</gml:Box>
					</gml:boundedBy>
                </c:if>
	            <c:forEach var="datapoint" items="${featureInfo.timesAndValues}">
	                <values>
	                    <c:if test="${not empty datapoint.key}">
				            <time>${utils:dateTimeToISO8601(datapoint.key)}</time>
	                    </c:if>
	                    <c:choose>
	                        <c:when test="${empty datapoint.value}">
	                            <value>none</value>
	                        </c:when>
	                        <c:otherwise>
	                            <value>${datapoint.value}</value>
	                        </c:otherwise>
	                    </c:choose>
	                </values>
	            </c:forEach>
            </${featureInfo.memberId}_layer>
        </${featureInfo.featureId}_feature>
    </c:forEach>
</msGMLOutput>
