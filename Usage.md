# Usage #

LODPeas requires two inputs in order run i.e. a quad dataset and a redirects file.  For example inputs see data collected form the [Dynamic Linked Data Observatory](http://swse.deri.org/dyldo/) project.  Specifically see [here](http://swse.deri.org/dyldo/data/2012-05-06/data.nq.gz) for example dataset and [here](http://swse.deri.org/dyldo/data/2012-05-06/redirects.nx.gz) for example redirects file.


## LODPeas-build ##

LODPeas-build uses the latest [jar](http://code.google.com/p/lodpeas/downloads/list) (using Java 6) to build the required files for LODPeas-gui.  There are nineteen commands in total.  We have created a bash script called [runLODPeas.sh](http://code.google.com/p/lodpeas/downloads/list) for convenience.  The following is an example command for runLODPeas.sh where 2G is the memory to the jvm.
```
# sh runLODPeas.sh lodpeas-build-0.1-dev.jar quad_dataset.nq.gz redirects.nx.gz 2G
```
runLODPeas.sh assumes that both inputs are gzipped.

LODPeas-build produces the following outputs (amongst others), which are required by LODPeas-gui:
KW, QUAD\_NI, QUAD\_SP, CONCUR\_NI, CONCUR\_SP


## LODPeas-gui ##

Within the LODPeas-gui web application you will need to set the aforementioned properties in the config.props file, located at WebContent/WEB-INF/config.props.  Below is an example:

```
KW=/home/patrick/LODPeas_jar/kWIndex/
QUAD_NI=/home/patrick/LODPeas_jar/output/spoc.ni
QUAD_SP=/home/patrick/LODPeas_jar/output/spoc.sp
CONCUR_NI=/home/patrick/LODPeas_jar/output/concur.ni
CONCUR_SP=/home/patrick/LODPeas_jar/output/concur.sp
```

An complete LODPeas-gui war file can be found [here](http://code.google.com/p/lodpeas/downloads/list).