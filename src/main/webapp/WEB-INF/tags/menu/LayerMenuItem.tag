<%@tag description="Displays a dataset as a set of layers in the menu. The dataset must be hosted on this server." pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.wms.Dataset" description="Dataset containing this layer" %>
<%@attribute name="item" required="true" type="uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem" description="The top-level LayerMenuItem"%>

<c:choose>
	<c:when test="${item.leaf}">
           <menu:layer dataset="${dataset}" name="${item.id}" label="${item.title}" plottable="${item.plottable}"/>
	</c:when>
	<c:otherwise>
	    <menu:folder label="${item.title}" id="${item.id}" plottable="${item.plottable}">
	        <c:forEach items="${item.children}" var="childMember">
	            <menu:layerMenuItem dataset="${dataset}" item="${childMember}"/>
	        </c:forEach>
           </menu:folder>
	</c:otherwise>
</c:choose>
