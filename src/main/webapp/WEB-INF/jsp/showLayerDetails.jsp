<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%--
     Displays the details of a variable as a JSON object
     See MetadataController.showLayerDetails().
     
     Data (models) passed in to this page:
         bbox = the bounding box of the layer
         vaxis = the vertical axis of the layer
         multiFeature = true if we have a multiple features in a dataset
            startTime = the start of the time axis for a multiFeature dataset
            endTime = the end of the time axis for a multiFeature dataset
            tAxisUnits = the units of the time axis
            startZ = the start of the vertical axis for a multiFeature dataset
            endZ = the end of the vertical axis for a multiFeature dataset
            verticalCrs = the vertical CRS for a multiFeature dataset
         styles = a Set of strings representing the supported styles
         featureMetadata = the plotting metadata
         memberName = the member to be plotted
         dataset = the dataset which the feature belongs to
         datesWithData = Map<Integer, Map<Integer, List<Integer>>>.  Contains
                information about which days contain data for the Layer.  Maps
                years to a map of months to an array of day numbers.
         units = the units
         nearestTimeIso = ISO8601 String representing the point along the time
                axis that is closest to the required date (as passed to the server)
         paletteNames = the available palettes
--%>

<json:object>
    <json:property name="units" value="${units}"/>

    <c:if test="${not empty bbox}">
	    <json:array name="bbox">
	        <json:property>${bbox.minX}</json:property>
	        <json:property>${bbox.minY}</json:property>
	        <json:property>${bbox.maxX}</json:property>
	        <json:property>${bbox.maxY}</json:property>
	    </json:array>
    </c:if>

    <json:array name="scaleRange">
        <json:property>${featureMetadata.colorScaleRange.low}</json:property>
        <json:property>${featureMetadata.colorScaleRange.high}</json:property>
    </json:array>

    <json:property name="numColorBands" value="${featureMetadata.numColorBands}"/>

    <json:array name="supportedStyles" items="${styles}"/>

    <c:choose>
	    <c:when test="${multiFeature}">
	        <json:property name="startZ" value="${startZ}"/>
	        <json:property name="endZ" value="${endZ}"/>
	        <json:property name="zUnits" value="${verticalCrs.units.unitString}"/>
	        <json:property name="zPositive" value="${verticalCrs.positiveDirection.positive}"/>
	    </c:when>
	    <c:otherwise>
		    <c:if test="${not empty vaxis}">
		        <json:object name="zaxis">
		            <json:property name="units" value="${vaxis.verticalCrs.units.unitString}"/>
		            <json:property name="positive" value="${vaxis.verticalCrs.positiveDirection.positive}"/>
		            <json:array name="values" items="${vaxis.coordinateValues}"/>
		        </json:object>
		    </c:if>
	    </c:otherwise>
    </c:choose>

    <json:property name="multiFeature" value="${multiFeature}"/>
    <c:choose>
        <c:when test="${multiFeature}">
            <json:property name="startTime" value="${startTime}"/>
            <json:property name="endTime" value="${endTime}"/>
	        <json:property name="timeAxisUnits" value="${tAxisUnits}"/>
        </c:when>
        <c:otherwise>
		    <c:if test="${not empty datesWithData}">
		        <json:object name="datesWithData">
		            <c:forEach var="year" items="${datesWithData}">
		                <json:object name="${year.key}">
		                    <c:forEach var="month" items="${year.value}">
		                        <json:array name="${month.key}" items="${month.value}"/>
		                    </c:forEach>
		                </json:object>
		            </c:forEach>
		        </json:object>
		        <%-- The time axis units: "ISO8601" for "normal" axes, "360_day" for
		             axes that use the 360-day calendar, etc. --%>
		        <json:property name="timeAxisUnits" value="${tAxisUnits}"/>
		    </c:if>
        </c:otherwise>
    </c:choose>
    
    <%-- The nearest time on the time axis to the time that's currently
         selected on the web interface, in ISO8601 format --%>
    <json:property name="nearestTimeIso" value="${nearestTimeIso}"/>
    <json:property name="moreInfo" value="${dataset.moreInfoUrl}"/>
    <json:property name="copyright" value="${dataset.copyrightStatement}"/>
    <json:array name="palettes" items="${paletteNames}"/>
    <json:property name="defaultPalette" value="${featureMetadata.paletteName}"/>
    <json:property name="logScaling" value="${featureMetadata.logScaling}"/>
</json:object>
