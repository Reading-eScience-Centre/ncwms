<%@tag description="Displays a dataset as a set of layers in the menu. The dataset must be hosted on this server." pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%@attribute name="metadata" required="true" type="uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata" description="The range metadata object to display"%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.wms.Dataset" description="The dataset object to display"%>
<%@attribute name="featureId" required="true" type="java.lang.String" description="The ID of the feature"%>

<c:forEach items="${metadata.memberNames}" var="member">
	<c:choose>
		<c:when test="${utils:memberIsScalar(metadata, member)}">
            <menu:layer dataset="${dataset}" name="${dataset.id}/${featureId}/${member}" label="${member}" plottable="${true}"/>
		</c:when>
		<c:otherwise>
            <c:set var="thisMetadata" value="${utils:getChildMetadata(metadata, member)}"/>
            <c:set var="plottable" value="${false}"/>
            <c:if test="${not empty thisMetadata.representativeChildren}">
                <c:set var="plottable" value="${true}"/>
            </c:if>
		    <menu:folder label="${member}" id="${dataset.id}/${featureId}/${member}" plottable="${plottable}">
		        <c:forEach items="${metadata.memberNames}" var="childMember">
		            <menu:rangemetadata metadata="${utils:getChildMetadata(metadata, childMember)}" dataset="${dataset}" featureId="${featureId}"/>
		        </c:forEach>
            </menu:folder>
		</c:otherwise>
	</c:choose>
</c:forEach>
