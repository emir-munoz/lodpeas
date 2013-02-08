/*******************************************************************************
 * Forced Graph
 ******************************************************************************/
var labelType, useGradients, nativeTextSupport, animate;

(function() {
	var ua = navigator.userAgent, iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i), typeOfCanvas = typeof HTMLCanvasElement, nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'), textSupport = nativeCanvasSupport
			&& (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
	// I'm setting this based on the fact that ExCanvas provides text support
	// for IE
	// and that as of today iPhone/iPad current text support is lame
	labelType = (!nativeCanvasSupport || (textSupport && !iStuff)) ? 'Native' : 'HTML';
	nativeTextSupport = labelType == 'Native';
	useGradients = nativeCanvasSupport;
	animate = !(iStuff || !nativeCanvasSupport);
})();

var Log = {
	elem : false,
	write : function(text) {
		if (!this.elem)
			this.elem = document.getElementById('log');

		// to close the loading message we need to call $.unblockUI()
		if (text == "done") {
			$.unblockUI();
		}

		this.elem.innerHTML = text;
		this.elem.style.left = (500 - this.elem.offsetWidth / 2) + 'px';
	}
};

function imgError(img, altimg) {
	img.src = altimg;
	img.onerror = "this.style.display='none';";
}

// Here start the loading of the json to build the graph
function init(json) {
	// clean canvas
	infov = document.getElementById('infovis');
	infov.innerHTML = "";
	// clean the right column
	$jit.id('inner-details').innerHTML = "";
	// init ForceDirected
	var fd = new $jit.ForceDirected(
			{
				// id of the visualization container
				injectInto : 'infovis',
				// Enable zooming and panning
				// by scrolling and DnD
				Navigation : {
					enable : true,
					// Enable panning events only if we're dragging the empty
					// canvas (and not a node).
					panning : 'avoid nodes', // avoid nodes, false, true
					zooming : 30
				// zoom speed. higher is more sensible
				},
				// Change node and edge styles such as
				// color and width.
				// These properties are also set per node
				// with dollar prefixed data-properties in the
				// JSON structure.
				Node : {
					overridable : true,
					width : 20,
					type : 'circle', // rectangle
					lineWidth : 3,
				// autoWidth: true,
				// autoHeight: true
				},
				Edge : {
					overridable : true,
					type : 'line', // line, hyperline, arrow
					color : '#0000FF',// '#23A4FF',
					CanvasStyles : {
						shadowColor : '#447744', // '#F62217',// '#1495C1',
						shadowBlur : 5
					},
					// lineWidth : 1.5 // given by meta-data
					// This dimension is used to create an area for the line
					// where the
					// contains method for the edge returns true.
					epsilon : 20
				},
				// Native canvas text styling
				Label : {
					type : labelType, // Native or HTML
					textBaseline : 'alphabetic',
					size : 10,
					style : 'bold',
					family : 'sans-serif',
					textAlign : 'center',
					color : '#446644'// '#0088CC'
				},
				// Add Tips
				Tips : {
					enable : true,
					onShow : function(tip, node) {
						// count connections
						var count = 0;
						node.eachAdjacency(function() {
							count++;
						});
						// display node info in tooltip
						var html = "<div class=\"tip-title\">" + node.name + "</div>";
						// + "<div class=\"tip-text\"><b>connections:</b> " +
						// count + "</div>";
						var data = node.data;
						// display node image
						if (data.image) {
							if (data.image.length > 1) {
								html += "<div class=\"tip-img\"><img src=\"" + data.image[0]
										+ "\" class=\"mid\" onerror=\"imgError(this,'" + data.image[1]
										+ "')\" /></div>";
							} else {
								html += "<div class=\"tip-img\"><img src=\"" + data.image[0]
										+ "\" class=\"mid\" onerror=\"this.style.display='none';\" /></div>";
							}
						} else {
                 html += "<div class=\"tip-img\"><img src=\"css/images/no_image.png\""
                    +" class=\"mid\" /></div>";
            }
						if (data.comment) {
							html += "<div class=\"tip-comment\">" + data.comment + "</div>";
						}
						tip.innerHTML = html;
					}
				},
				// Node style on hover
				NodeStyles : {
					enable : true,
					type : 'Native',
					stylesHover : {
						color : '#fcc' // hover color
					},
					duration : 100
				// stylesClick : function(node) {
				// // nothing
				// }
				},
				// Add node events
				Events : {
					enable : true,
					enableForEdges : true,
					type : 'Native', // HTML, SVG, Native
					// Change cursor style when hovering a node
					onMouseEnter : function() {
						fd.canvas.getElement().style.cursor = 'pointer'; // move
					},
					onMouseLeave : function() {
						fd.canvas.getElement().style.cursor = '';
					},
					// Update node positions when dragged
					onDragMove : function(node, eventInfo, e) {
						var pos = eventInfo.getPos();
						node.pos.setc(pos.x, pos.y);
						fd.plot();
					},
					// Implement the same handler for touchscreens
					onTouchMove : function(node, eventInfo, e) {
						$jit.util.event.stop(e); // stop default touchmove
						// event
						this.onDragMove(node, eventInfo, e);
					},
					// Add also a click handler to nodes
					onClick : function(node) {
						// Information of edges
						if (node.nodeFrom) {
							// Build the right column relations list.
							// This is done by traversing the clicked node
							// connections.
							var html = "<h4><b>Between:</b> " + node.nodeTo.name + "<br/><b>and:</b> "
									+ node.nodeFrom.name + "</h4><div class=\"score\"><b>score:</b> " + node.data.score
									+ "</div>";

									html += "<b> values shared in-common (<span class=\"inward\">inward</span> | <span class=\"outward\">outward</outward>):</b><ul><li>",
									list = [];
							if (node.data.common) {
								for (i = 0; i < node.data.common.length; i++) {
									var plabel = node.data.common[i].plabel;
									var vlabel = node.data.common[i].vlabel;

									if (typeof (plabel) == 'undefined' || plabel == null || plabel === "") {
										plabel = node.data.common[i].pnode;
									}
									if (typeof (vlabel) == 'undefined' || vlabel == null || vlabel === "") {
										vlabel = node.data.common[i].vnode;
									}

									plabel = "<span class=\"common-pred\">" + plabel + "</span>";

									if (node.data.common[i].dir === "s") {

										list.push("<span class=\"outward\">" + plabel + " : " + vlabel + " [shared by "
												+ node.data.common[i].n + "]</span>");
									} else {
										list.push("<span class=\"inward\">" + plabel + " : " + vlabel + " [shared by "
												+ node.data.common[i].n + "]</span>");
									}
								}  
							}
							// append connections information

							$jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";

							fd.canvas.getElement().style.cursor = 'pointer';
							return;
						}

						if (!node) {
							// alert(node.id);
							return;
						}

						// Build the right column relations list.
						// This is done by traversing the clicked node
						// connections.
						var html = "<h4>" + node.name + "</h4>";

						// here put all the pictures for the pea
						var data2 = node.data;
						if (data2.image) {
							html = html + "<div id=\"slides\"><div class=\"slides_container\">";
							// ---------------------
							// For each image
							// here we need an array of images
							// for testing I'm using the same image twice
							for (i = 0; i < data2.image.length; i++) {
								html = html + "<div class=\"tip-img\"><img src=\"" + data2.image[i]
										+ "\" alt=\"Cougar\" onerror=\"this.style.display='none';\" /></div>";
							}
							// end loop for each
							// close divs
							html = html + "</div></div>";
						}
						html += "<br/><div id=\"conecctions\"><b> connections:</b><ul><li>", list = [];
						node.eachAdjacency(function(adj) {
							if (adj.nodeTo.name)
								list.push(adj.nodeTo.name);
						});
						// append connections information
						$jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul></div>";
						loadSlides(); // load script por slider
					},
					// A right click handler for nodes
					onRightClick : function(node) {
						// alert(node.name);
						// don't make a query to the server
						if (node.id && node.id != "node0") {
							// test
							loading();
							$.getJSON("search.html", {
								type : "uri",
								term : escape(node.id)
							}, function(data) {
								var json = data;
								Log.write('data downloaded');
								$('#infovis').empty(); // clean the div
								init(json); // call to jit
							}, "json");
						}

					}
				},
				// Number of iterations for the FD algorithm
				iterations : 100, // 200
				orientation : 'top',
				align : 'center',
				// set node and edge styles
				// set overridable=true for styling individual
				// nodes or edges
				offsetX : 0,
				offsetY : 110,

				// Edge length
				levelDistance : 130,
				// Add text to the labels. This method is only triggered
				// on label creation and only for DOM labels (not native canvas
				// ones).
				onCreateLabel : function(domElement, node) {
					domElement.innerHTML = node.name;
					var style = domElement.style;
					style.border = '1px solid transparent';
					style.fontSize = "0.8em";
					style.color = "#ddd";
					// domElement.ondblclick = function() {
					// alert(node.name);
					// };
					// domElement.onmouseover = function() {
					// style.border = '1px solid #333333';
					// };
					// domElement.onmouseout = function() {
					// style.border = '1px solid transparent';
					// };
				},
				// Change node styles when DOM labels are placed
				// or moved.
				onPlaceLabel : function(domElement, node) {
					var style = domElement.style;
					var left = parseInt(style.left);
					var top = parseInt(style.top);
					var w = domElement.offsetWidth;
					style.left = (left - w / 2) + 'px';
					style.top = (top + 5) + 'px';
					style.display = '';
				},

				// This method is called right before plotting
				// an edge. It's useful for changing an individual edge
				// style properties before plotting it.
				// Edge data properties prefixed with a dollar sign will
				// override the Edge global style properties.
				onBeforePlotLine : function(adj) {

					// if (1) { // adj.nodeFrom.selected && adj.nodeTo.selected
					adj.data.$color = "#eed";
					// the width of the edge is the score of the similarity
					var data = adj.data;
					adj.data.$lineWidth = Math.pow(data.score, 2.4) * 10;
					// add the label
					// var data = adj.data;
					// now adjust the label placement
					// var radius = this.viz.canvas.getSize();
					// var x = parseInt((pos.x + posChild.x + radius.width) /
					// 2);
					// var y = parseInt((pos.y + posChild.y + radius.height) /
					// 2);
					// canvas.getCtx().fillText(data.score, x, y);
					// } else {
					// delete adj.data.$color;
					// delete adj.data.$lineWidth;
					// }
				}

			});
	// load JSON data.
	fd.loadJSON(json, 0); // 0 is the indexed node as root for the

	// visualization.
	// compute positions incrementally and animate.
	fd.computeIncremental({
		iter : 20,
		property : 'end',
		onStep : function(perc) {
			Log.write(perc + '% loaded...');
		},
		onComplete : function() {
			Log.write('done');
			// just plot the graph or animate
			// fd.plot();
			fd.animate({
				modes : [ 'linear' ],
				transition : $jit.Trans.Elastic.easeOut,
				duration : 1000
			// 2000
			});
		}
	});
	// end

	// // Custom node
	// $jit.ForceDirected.Plot.NodeTypes.implement({
	// // this node type is used for plotting resource types (web)
	// 'custom' : {
	// 'render' : function(node, canvas) {
	// var offset = 1;
	// var ctx = canvas.getCtx();
	// var img = new Image();
	// var pos = node.getPos();
	// img.src = 'css/rdf2.png';
	// img.style.width = '50px';
	// img.style.height = '50px';
	//
	// if (!status) {
	// // only for first loading
	// img.onload = function() {
	// ctx.drawImage(img, pos.x - 16, pos.y - 16);
	// };
	// } else {
	//
	// ctx.drawImage(img, pos.x - 16, pos.y - 16);
	// }
	// // img.onload = function() {
	// // ctx.drawImage(img, pos.x - offset, pos.y - offset);
	// // };
	// },
	// 'contains' : function(node, pos) {
	// var npos = node.pos.getc(true), dim = node.getData('dim');
	// return this.nodeHelper.square.contains(npos, pos, dim);
	// }
	// }
	// });

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
			// http://loadinggif.com/images/image-selection/36.gif
			message : '<h1><div class="circle"></div><div class="circle1"></div> Just a moment...</h1>',
			theme : true,
			draggable : false, // draggable option requires jquery UI
			// z-index for the blocking overlay
			baseZ : 2000
		});
		// to close we need to call $.unblockUI()
		/* setTimeout($.unblockUI, 2000); */
	}

	// Variables and functions for zoom
	// SNAC panZoomControl
	var scaleFactor = 1.1;
	var panSize = 25;
	initialZoom = 1.331;
	fd.canvas.scale(initialZoom, initialZoom);

	$("#zoomIn").click(function() {
		fd.canvas.scale(scaleFactor, scaleFactor);
	});

	$('#zoomReset').click(function() {
		fd.canvas.scale(initialZoom / fd.canvas.scaleOffsetX, initialZoom / fd.canvas.scaleOffsetY);
	});

	$('#zoomOut').click(function() {
		fd.canvas.scale(1 / scaleFactor, 1 / scaleFactor);
	});

	function loadSlides() {
		$('#slides').slides({
			container : 'slides_container',
			preload : false,
			preloadImage : 'css/themes/slider/img/loading.gif',
			play : 4000,
			pause : 1000,
			autoHeight : true,
			hoverPause : true
		});
	}
}
