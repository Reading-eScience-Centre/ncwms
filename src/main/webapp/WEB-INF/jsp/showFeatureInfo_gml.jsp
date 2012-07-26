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
	<${varName}_layer>
		<${varName}_feature>
			<gml:boundedBy>
				<gml:Box srsName="${crs}">
					<gml:coordinates>
                        ${coords.x},${coords.y} ${coords.x},${coords.y}
					</gml:coordinates>
				</gml:Box>
			</gml:boundedBy>
			<longitude>${longitude}</longitude>
			<latitude>${latitude}</latitude>
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
        </${varName}_feature>
    </${varName}_layer>
</msGMLOutput>