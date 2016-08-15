function getLayerNames()
{
	var theHtml = "";
	var theUrl = "wms?request=GetMetadata&item=menu";
	var JSONObject = JSON.parse(httpGet(theUrl));
	var dataSets = JSONObject.children;
	if (dataSets != null) {
		for (var i = 0; i < dataSets.length; i++) {
			theHtml = theHtml + "<strong>" + dataSets[i].label + "</strong><br />";
			theHtml = theHtml + "<div style=\"margin-left: 1em;\">";
			var variables = dataSets[i].children;
			if (variables != null) {
				for (var j = 0; j < variables.length; j++){
					theHtml = theHtml + variables[j].label + " (" + variables[j].id + ")<br />";
					var subvariables = variables[j].children;
					if (subvariables != null) { 
						for (var k = 0; k < subvariables.length; k++){
							theHtml = theHtml + "<div style=\"margin-left: 1em;\">" + subvariables[k].label;
							theHtml = theHtml + " (" + subvariables[k].id + ")<br /></div>";
						}
					}			
				}
			}
			theHtml = theHtml + "</div>";
		}
	} else {
		theHtml = theHtml + "<div style=\"margin-left: 0.5em;\">No available datasets.</div>";
	}
	return theHtml;
}

/*
 * Read response from HTTP GET request.
 * This was taken from the web at:
 * http://stackoverflow.com/questions/247483/http-get-request-in-javascript
 */
function httpGet(theUrl)
{
	var xmlHttp = null;

	xmlHttp = new XMLHttpRequest();
	xmlHttp.open( "GET", theUrl, false );
	xmlHttp.send( null );
	return xmlHttp.responseText;
}

function loadNewImage()
{
	var xml = document.getElementById("sldtext").value;
	var newURL = "wms?FORMAT=image%2Fpng&TRANSPARENT=TRUE&" +
		"SLD_BODY=" + encodeURIComponent(xml) + "&VERSION=1.3.0&EXCEPTIONS=INIMAGE&" +
		"SERVICE=WMS&REQUEST=GetMap&CRS=CRS%3A84&BBOX=-180,-90,180,90&WIDTH=1024&HEIGHT=512";
	document.getElementById("image").src = newURL;
}

function loadSampleSLD(file)
{
	$.get(file, function(data) {
			document.getElementById("sldtext").value = data;
		}, dataType = "text");
}