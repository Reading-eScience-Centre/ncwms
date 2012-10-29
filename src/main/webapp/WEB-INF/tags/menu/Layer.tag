<%@tag description="Displays a single Layer in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.wms.Dataset" description="Dataset containing this layer"%>
<%@attribute name="name" required="true" description="ID of layer within the Capabilities document"%>
<%@attribute name="label" required="true" description="Specifies the title for this layer"%>
<%@attribute name="plottable" type="java.lang.Boolean" required="true" description="Specifies whether this layer is plottable"%>
<%-- A layer on this server.  Has the capability to add information about the
     readiness and error state of the layer's dataset --%>
<json:object>
    <c:if test="${empty dataset}">
        <json:property name="label" value="Dataset does not exist"/>
    </c:if>
    <c:if test="${dataset.ready}">
        <json:property name="id" value="${name}"/>
        <json:property name="label" value="${label}"/>
        <json:property name="plottable" value="${plottable}"/>
    </c:if>
    <c:if test="${dataset.loading}">
        <json:property name="label" value="${label} (loading)"/>
        <json:property name="plottable" value="${false}"/>
    </c:if>
    <c:if test="${dataset.error}">
        <json:property name="label" value="${label} (error)"/>
        <json:property name="plottable" value="${false}"/>
    </c:if>
</json:object>