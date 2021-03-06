<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!-- needed -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java" pageEncoding="UTF-8"%>

<html>
<head>
<title>LODPeas - Linked Data Browser</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta content="IE=edge,chrome=1" http-equiv="X-UA-Compatible">
<meta content="width=device-width,initial-scale=1" name="viewport">

<!-- CSS Files -->
<link type="text/css" href="css/base.css" rel="stylesheet" />
<!-- For main page -->
<link type="text/css" href="css/ForceDirected.css" rel="stylesheet" />
<!-- For graph -->
<link type="text/css" href="css/style.css" rel="stylesheet" />
<!-- For buttons -->
<link rel="stylesheet"
	href="css/themes/blitzer/jquery-ui-1.9.0.custom.css">
<!-- For popup -->
<link rel="stylesheet" href="css/reveal.css">
<!-- For image slider -->
<link rel="stylesheet" type="text/css" media="screen"
	href="css/themes/slider/css/global.css">

<!-- Favicon -->
<link rel="shortcut icon" href="favicon.ico" />

<!--[if IE]><script language="javascript" type="text/javascript" src="js/Extras/excanvas.js"></script><![endif]-->

<!-- JIT Library File -->
<script type="text/javascript" src="js/jit.js"></script>
<!-- Example File -->
<script type="text/javascript" src="js/lodpeas_graph.js"></script>
<!-- jQuery Library Files -->
<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
<script type="text/javascript"
	src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.18/jquery-ui.min.js"></script>
<script
	src="http://gsgd.co.uk/sandbox/jquery/easing/jquery.easing.1.3.js"></script>
<!-- Popups -->
<script type="text/javascript" src="js/jquery.blockUI.js"></script>
<script type="text/javascript" src="js/jquery.reveal.js"></script>
<!-- Slider -->
<script src="css/themes/slider/js/slides.min.jquery.js"></script>

<script type="text/javascript">
	$(document).ready(function() {
		
		// Search function that call the service
		$("#searchBtn").click(function(e) {
			var terms = $("#searchTxt").val();
			if (terms) {
				loading();
				$.getJSON("search.html", {
					type : "normal",
					term : escape(terms)
				}, function(data) {
					//alert(data);
					var json = data;
					Log.write('data downloaded');
					$('#infovis').empty(); // clean the div

					if (json[0].disambiguation) {
						// if disambiguation is required
						divDisambiguation(json[0]);
						$.unblockUI();
						init(json);
					} else {
						// else show graph normally
						init(json); // call to jit
					}
				}, "json");
			} else {
				// error message
			}
		});

		// Assign effects and actions to the disambiguation options
		function disambiguation() {
			// == first
			// set the click function to the dislink class			
			$(".dislink").live("click",function() {
				id1 =  $(this).attr("title");
				if (id1) {
					loading();
					$.getJSON("search.html", {
						type : "uri",
						term : escape(id1)
					}, function(data) {
						//alert(data);
						var json = data;
						Log.write('data downloaded');
						$('#infovis').empty(); // clean the div

						if (json[0].disambiguation) {
							// if disambiguation is required
							divDisambiguation(json[0]);
							$.unblockUI();
							init(json);
						} else {
							// else show graph normally
							init(json); // call to jit
						}
					}, "json");
				} else {
					// error message
				}
			});
			// == second
			// set the slide effect to the option
			// Set starting slide to 1
			var startSlide = 1;
			// Get slide number if it exists
			if (window.location.hash) {
				startSlide = window.location.hash.replace('#', '');
			}
			// Initialize Slides
			$('#disambiguation').slides({
				container : 'slides_disambiguation',
				paginationClass : 'dispagination',
				preload : false,
				preloadImage : 'css/themes/slider/img/loading.gif',
				generatePagination : true,
				autoHeight : true,
				play : 5000,
				pause : 2500,
				hoverPause : true,
				next : 'next-dis',
				prev : 'prev-dis',
				// Get the starting slide
				start : startSlide,
				animationComplete : function(current) {
					// Set the slide number as a hash
					//window.location.hash = '#' + current;
				}
			});
			// show the disambiguation dialog
			$("#dialog-disambiguation").dialog('open');
		}
		
		// Build the HTML with the top-10 disambiguation options
		function divDisambiguation(json) {
			var pages = "<div id=\"disambiguation\"><div class=\"slides_disambiguation\">";
			for (i = 0; i < json.disambiguation.length; i++) {
				var uri = json.disambiguation[i].id;
				if (json.disambiguation[i].name)
				{
					pages = pages + "<div class=\"slide\"><h2><a title=\""+ uri + "\" style=\"cursor:pointer\" class=\"dislink\">" + json.disambiguation[i].name + "</a></h2>";
				} else {
					pages = pages + "<div class=\"slide\"><h2><a title=\"" + uri + "\" style=\"cursor:pointer\" class=\"dislink\">" + uri.replace("<", "").replace(">", "") + "</a></h2>";
				}
				if (json.disambiguation[i].data.comment) {
					pages = pages + "<p>" + json.disambiguation[i].data.comment.substring(0, 180) + " ...</p></div>";
				} else {
					pages = pages + "<p><i>No comment available</i></p></div>";
				}
			}
			pages = pages + "</div><a href=\"#\" class=\"prev-dis\"><img src=\"css/themes/slider/img/arrow-prev.png\" width=\"24\" height=\"43\" alt=\"Arrow Prev\"></a>" +
					"<a href=\"#\" class=\"next-dis\"><img src=\"css/themes/slider/img/arrow-next.png\" width=\"24\" height=\"43\" alt=\"Arrow Next\"></a></div>";
			$("#disambigua").html(pages);
			
			disambiguation();
		}

		// Loading effect
		function loading() {
			$.blockUI({
				css : {
					border : 'none',
					padding : '15px',
					backgroundColor : '#000',
					'-webkit-border-radius' : '10px',
					'-moz-border-radius' : '10px',
					opacity : .5,
					color : '#fff',
					top : '20%'
				},
				//http://loadinggif.com/images/image-selection/36.gif
				message : '<h1><div class="circle"></div><div class="circle1"></div> Just a moment...</h1>',
				theme : true,
				draggable : false, // draggable option requires jquery UI 
				// z-index for the blocking overlay 
				baseZ : 2000
			});
		}

		// Enter on the input
		$("#searchTxt").keypress(function(e) {
			if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
				$('#searchBtn').click();
				return false;
			} else {
				return true;
			}
		});

		// Options dialog
		$("#dialog").dialog({
			resizable : false,
			position : [ 1500, 520 ], //['left','top'],
			title : 'Options',
			width : 180,
			height : 'auto',
			minHeight : 40,
			open : function(event, ui) {
				$(".ui-dialog-titlebar-close").hide();
			},
			closeOnEscape : false,
			close : function(event, ui) {
			}
		});

		// Dialog for disambiguation
		$("#dialog-disambiguation").dialog({
			autoOpen: false,
			resizable : false,
			position : [ 20, 20 ], //['left','top'],
			title : 'Disambiguation',
			width : 250,
			height : 'auto',
			minHeight : 100,
			open : function(event, ui) {
			},
			closeOnEscape : false,
			close : function(event, ui) {
			}
		});

		// Help button
		$('#helpBtn').click(function(e) {
			e.preventDefault();
			$('#helpModal').reveal();
		});
		
		$.reject({  
	        reject: {
	            safari: false, // Apple Safari  
	            chrome: false, // Google Chrome  
	            firefox: false, // Mozilla Firefox  
	            msie: true, // Microsoft Internet Explorer  
	            opera: false, // Opera  
	            konqueror: true, // Konqueror (Linux)  
	            unknown: true // Everything else  
	        },
	        display: ['firefox','chrome','safari','opera'], // Displays only firefox, chrome, safari, and opera  
	        close: true, // Prevent closing of window
	        header: 'Your browser is not supported for LODPeas', // Header Text
	        paragraph1: 'You are currently using an unsupported browser', // Paragraph 1
	        paragraph2: 'If you want to use LODPeas, please <br/> (1) open this site in other supported browser (e.g., Firefox, Chrome, Safari); <br/> (2) install one of the many optional browsers below to proceed',  
	        // if close : true, uncomment next row
	        closeMessage: 'Close this window at your own demise!', // Message below close window link
	        closeCookie: false // Set cookie to remmember close for this session
	    }); // Customized Browsers
	});
</script>

</head>

<!-- <body onload="init();"> -->
<body onload="$('#helpBtn').click();">
	<div class="box">
		<div class="form-search">
			Search:<input type="text" id="searchTxt" size="30" maxlength="50">
			<a class="button small blue" id="searchBtn" type="button">Find</a>
		</div>
		<div id="help">
			<a href="" id="helpBtn" type="button"><img border="0" width="40"
				src="css/images/question1.png"></a>
		</div>
		<div id="container">
			<!-- <div id="left-container">
			<div id="id-list"></div>
		    </div> -->
			<div id="id-list"></div>

			<div id="center-container">
				<div id="infovis"></div>
			</div>

			<div id="right-container">
				<div id="inner-details">
					<div id="slides">
						<div class="slides_container"></div>
					</div>
				</div>
			</div>
			<div id="log"></div>
		</div>
	</div>
	<div id="dialog" title="Zoom">
		<a id="zoomIn" type="button" style="cursor: pointer;"><img
			class="img" alt="" src="css/images/zoom_in_new.png" /></a> 
			<a 	id="zoomOut" type="button" style="cursor: pointer;"><img
			class="img" alt="" src="css/images/zoom_out_new.png" /></a>
		<a id="zoomReset" type="button" style="cursor: pointer;"><img
			class="img" alt="" src="css/images/zoom_one.png" /></a>
	</div>
	<div id="dialog-disambiguation" title="Disambiguation">
		<div id="disambigua"></div>
	</div>
	<!-- HELP WINDOW -->
	<div id="helpModal" class="reveal-modal" style="overflow: auto;">
		<h1>LODPeas - Linked Data Browser</h1>
		<p>LODPeas is a system for browsing entities (peas) that are found
			to share many (rare) things in common. Use standard keyword search to
			locate a focus entity. Once a focus entity has been found, other
			entities that share a lot in common with it are displayed in a
			graph-based visualisation.</p>
		<h2>How to use LODPeas?</h2>
		<p>
			You can <b>search</b> for entities like people, papers, proteins or
			anything else for that matter; <b>hover</b> over the peas for brief
			info; <b>click</b> the peas &amp; connections for even more info; <b>right-click</b>
			the peas to make them the focus; and <b>zoom-in &amp; zoom-out</b> on
			the graph.
		</p>
		<h2>Which sources does LODPeas contain?</h2>
		<p>
			The current index for the system was built over the <a
				href="http://challenge.semanticweb.org/">Billion Triple
				Challenge 2012</a> dataset. LODPeas is designed to scale for billions of
			triples of diverse data and uses generic RDF processing methods.
		</p>

		<a class="close-reveal-modal">&#215;</a>
		<p align="center">
			<img width="130" src="css/images/DERI_brandmark_rgb.jpg">
		</p>
	</div>
</body>
</html>
