<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
Clicked longitude: ${longitude}
Clicked latitude: ${latitude}
<c:forEach var="featureInfo" items="${data}">
Feature ID: ${featureInfo.featureCollectionId}/${featureInfo.featureId}/${featureInfo.memberId}
<c:if test="${not empty featureInfo.actualPos}">
Feature x: ${featureInfo.actualPos.x}
Feature y: ${featureInfo.actualPos.y}</c:if>
<c:forEach var="datapoint" items="${featureInfo.timesAndValues}"><c:if test="${not empty datapoint.key}">
Time: ${utils:formatNiceDateTime(datapoint.key)}</c:if><c:choose><c:when test="${empty datapoint.value}">
Value: none</c:when><c:otherwise>
Value: ${datapoint.value}</c:otherwise></c:choose></c:forEach>
</c:forEach>
<c:if test="${not empty timeseriesUrl}">
Timeseries plot: ${timeseriesUrl}</c:if>
<c:if test="${not empty profileUrl}">
Profile plot: ${profileUrl} 
</c:if>