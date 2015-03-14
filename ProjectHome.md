# Welcome to LODPeas #

LODPeas is a system for browsing entities that are found to share many things in common in an RDF dataset. The system first offers standard keyword search to locate a focus entity. Once a focus entity has been found, other entities that share a lot in common with it are displayed in a graph-based visualisation. The degree to which two entities have a lot in common—their level of concurrence—is scored by looking at attributes (property–value pairs) that they share: attributes that are shared by few other entities are given higher weight, and additional shared attributes imply a stronger score. LODPeas is designed to scale for billions of triples and is built in an (almost) entirely domain-agnostic fashion, built on top of the RDF standards themselves and not requiring any domain-specific input.

The LODPeas source code is separated into two projects on googlecode.  LODPeas-build can be considered the back end and is used to genertate the inputs for LODPeas-gui. LODPeas-gui is essentially a web application that allows the user to interact with the data.

Instructions for building the LODPeas-gui can be found [here](http://code.google.com/p/lodpeas/wiki/Usage).
LODPeas-build jar file and LODPeas-gui war file downloads can be found [here](http://code.google.com/p/lodpeas/downloads/list).

At the moment LODPeas only works with quads.  If you are interested in using LODPeas with n-triples or have any other comments please raise an issue within the project.

LODPeas in action over the Billion Triple Challenge 2012 dataset [here](http://lodpeas.org/).

Developed by:

Aidan Hogan

Emir Muñoz

Patrick O'Byrne

Digital Enterprise Research Institute

National University of Ireland, Galway

Galway, Ireland

2012