<%@tag description="Creates a folder (container) in the menu structure" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="label" required="true" description="Label for this folder"%>
<%@attribute name="id" required="false" description="Optional ID for this folder"%>
<%@attribute name="plottable" type="java.lang.Boolean" required="true" description="Whether this represents plottable data"%>
<json:object>
    <json:property name="label" value="${label}"/>
    <json:property name="id" value="${id}"/>
    <json:property name="plottable" value="${plottable}"/>
    <json:array name="children">
        <jsp:doBody/>
    </json:array>
</json:object>