<%@tag description="Creates a folder (container) in the menu structure" pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="label" required="true" description="Label for this folder"%>
<%@attribute name="id" required="false" description="Optional ID for this folder"%>
<json:object>
    <json:property name="label" value="${label}"/>
    <json:property name="id" value="${id}"/>
    <json:array name="children">
        <jsp:doBody/>
    </json:array>
</json:object>