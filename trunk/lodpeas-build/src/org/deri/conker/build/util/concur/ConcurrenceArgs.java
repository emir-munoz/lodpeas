package org.deri.conker.build.util.concur;

/**
 * Arguments for running ConcurrenceEngine over RDF data
 * 
 * @author aidhog
 */
public class ConcurrenceArgs {
	/**
	 * Input files should be considered GZipped by default
	 */
	public static final boolean DEFAULT_GZ_IN = true;
	
	/**
	 * Output files should be GZipped by default
	 */
	public static final boolean DEFAULT_GZ_OUT = true;

	/**
	 * Default temporary directory (sub-)path
	 */
	public static final String DEFAULT_TMP_DIR = "tmp/";
	
	/**
	 * Default name for output non-GZipped results file
	 */
	public static final String DEFAULT_OUT_FINAL_FILENAME_NGZ = "data.final.nx";
	
	/**
	 * Default name for output GZipped results file
	 */
	public static final String DEFAULT_OUT_FINAL_FILENAME_GZ = DEFAULT_OUT_FINAL_FILENAME_NGZ+".gz";

	/**
	 * Default name for output non-GZipped unsorted results file
	 */
	public static final String DEFAULT_OUT_UNSORTED_FILENAME_NGZ = "data.e1e2c.nx";
	
	/**
	 * Default name for output GZipped unsorted results file
	 */
	public static final String DEFAULT_OUT_UNSORTED_FILENAME_GZ = DEFAULT_OUT_UNSORTED_FILENAME_NGZ+".gz";
	
	
	/**
	 * Default name for output non-GZipped raw unaggregated concurrence scores
	 */
	public static final String DEFAULT_OUT_RAW_FILENAME_NGZ = "sim.sp.nx";
	
	/**
	 * Default name for output GZipped raw unaggregated concurrence scores
	 */
	public static final String DEFAULT_OUT_RAW_FILENAME_GZ = DEFAULT_OUT_RAW_FILENAME_NGZ+".gz";
	
	/**
	 * Default name for output non-GZipped results file
	 */
	public static final String DEFAULT_OUT_RAW_SORTED_FILENAME_NGZ = "sim.sp.sorted.nx";
	
	/**
	 * Default name for output GZipped results file
	 */
	public static final String DEFAULT_OUT_RAW_SORTED_FILENAME_GZ = DEFAULT_OUT_RAW_SORTED_FILENAME_NGZ+".gz";
	
	
	/**
	 * Default limit for groupings which produce concurrence scores (note that number of scores
	 * produced are quadratic with respect to this limit... this is effectively a limit to
	 * stop pair-wise comparison in large-scale scenarios).
	 */
	public static final int DEFAULT_LIMIT = 50;
	
	/**
	 * Default setting for producing (spartan) explanations for results
	 */
	public static final boolean DEFAULT_VERBOSE = true;
	
	/**
	 * Default setting for labelling results
	 */
	public static final boolean DEFAULT_LABEL = true;
	
	/**
	 * Default setting for filtering non-subjects
	 */
	public static final boolean DEFAULT_NON_SUBJ_FILTER = DEFAULT_LABEL;
	
	/**
	 * Default setting for filtering multiple relations to common value
	 */
	public static final boolean DEFAULT_RELATION_FILTER = false;
	
	/**
	 * Default setting for writing symmetric pairs
	 */
	public static final boolean DEFAULT_SYM_INDEX = false;
	
	/**
	 * Input s-p-o sorted data
	 */
	private String _inSpo;
	
	/**
	 * Input s-p-o sorted data is GZipped
	 */
	private boolean _gzInSpo = DEFAULT_GZ_IN;
	
	/**
	 * Input o-p-s sorted data
	 */
	private String _inOps;
	
	/**
	 * Input o-p-s sorted data is GZipped
	 */
	private boolean _gzInOps = DEFAULT_GZ_IN;
	
	/**
	 * Output (sorted) results file 
	 */
	private String _outData;
	
	/**
	 * Output (sorted) results file should be GZipped
	 */
	private boolean _gzOutData = DEFAULT_GZ_OUT;
	
	/**
	 * Output (unsorted) results 
	 */
	private String _outUnsorted;
	
	/**
	 * Output (unsorted) results should be GZipped
	 */
	private boolean _gzOutUnsorted = DEFAULT_GZ_OUT;
	
	/**
	 * Output raw concurrence score tuples
	 */
	private String _outRaw;
	
	/**
	 * Output raw concurrence score tuples should be GZipped
	 */
	private boolean _gzOutRaw = DEFAULT_GZ_OUT;
	
	/**
	 * Output raw sorted concurrence score tuples
	 */
	private String _outRawSorted;
	
	/**
	 * Output raw concurrence score tuples should be GZipped
	 */
	private boolean _gzOutRawSorted = DEFAULT_GZ_OUT;
	
	/**
	 * Temporary directory for intermediate sorted files
	 */
	private String _tmpdir;
	
	/**
	 * Output directory for results files
	 */
	private String _outdir = null;
	
	/**
	 * Produce (spartan) explanations for results
	 */
	private boolean _verbose = DEFAULT_VERBOSE;
	
	/**
	 * Provide label/rank/internal info for terms from input
	 */
	private boolean _label = DEFAULT_LABEL;
	
	/**
	 * Write symmetric pairs for indexing output
	 */
	private boolean _symindex = DEFAULT_SYM_INDEX;
	
	/**
	 * Filter terms not appearing as subject
	 */
	private boolean _filterNonSubjects = DEFAULT_NON_SUBJ_FILTER;
	
	/**
	 * Filter multiple relations to same value
	 */
	private boolean _filterRelations = DEFAULT_NON_SUBJ_FILTER;
	
	/**
	 * Limit for groupings which produce concurrence scores (note that number of scores
	 * produced are quadratic with respect to this limit... this is effectively a limit to
	 * stop pair-wise comparison in large-scale scenarios).
	 */
	private int _limit = DEFAULT_LIMIT; 
	
	/**
	 * Constructor
	 * 
	 * @param inSpo	input s-p-o sorted data
	 * @param inOps	input o-p-s sorted data
	 * @param outdir output directory
	 */
	public ConcurrenceArgs(String inSpo, String inOps, String outdir){
		_inSpo = inSpo;
		_inOps = inOps;
		_outdir = outdir;
		initDefaults(outdir);
	}
	
	/**
	 * 
	 * @return the output directory
	 */
	public String getOutDir(){
		return _outdir;
	}
	
	/**
	 * 
	 * @return the output filename for sorted results
	 */
	public String getOutData(){
		return _outData;
	}
	
	/**
	 * 
	 * @return the input filename for o-p-s sorted input
	 */
	public String getInOps(){
		return _inOps;
	}
	
	/**
	 * Set if o-p-s input is GZipped
	 * @param gzin	whether o-p-s input is GZipped
	 */
	public void setGzInOps(boolean gzin){
		_gzInOps = gzin;
	}
	
	/**
	 * Get the limit for groupings which produce concurrence scores (note that number of scores
	 * produced are quadratic with respect to this limit... this is effectively a limit to
	 * stop pair-wise comparison in large-scale scenarios).
	 * 
	 * @return the limit
	 */
	public int getLimit(){
		return _limit;
	}
	
	/**
	 * Set the limit for groupings which produce concurrence scores (note that number of scores
	 * produced are quadratic with respect to this limit... this is effectively a limit to
	 * stop pair-wise comparison in large-scale scenarios).
	 * 
	 * @param limit	the limit
	 */
	public void setLimit(int limit){
		_limit = limit;
	}
	
	/**
	 * 
	 * @return if results should contain raw explanations
	 */
	public boolean getVerbose(){
		return _verbose;
	}
	
	/**
	 * 
	 * @param verbose	results should (not) contain raw explanations
	 */
	public void setVerbose(boolean verbose){
		_verbose = verbose;
	}
	
	/**
	 * 
	 * @return if results should contain labels/ranks/internal for terms
	 */
	public boolean getLabel(){
		return _label;
	}
	
	/**
	 * 
	 * @param verbose	results should (not) contain labels/ranks/internal for terms
	 */
	public void setLabel(boolean label){
		_label = label;
	}
	
	/**
	 * 
	 * @return write symmetric pairs for indexing
	 */
	public boolean getSymIndex(){
		return _symindex;
	}
	
	/**
	 * 
	 * @param verbose	results should (not) contain labels/ranks/internal for terms
	 */
	public void setSymIndex(boolean symindex){
		_symindex = symindex;
	}
	
	/**
	 * 
	 * @return if results should contain only terms appearing as subject (requires labelled input)
	 */
	public boolean getFilterNonSubjects(){
		return _filterNonSubjects;
	}
	
	/**
	 * 
	 * @param filter	results should (not) contain only terms appearing as subject (requires labelled input)
	 */
	public void setFilterNonSubjects(boolean filter){
		_filterNonSubjects = filter;
	}
	
	/**
	 * 
	 * @return if only one relation should be considered per each value
	 */
	public boolean getFilterRelations(){
		return _filterRelations;
	}
	
	/**
	 * 
	 * @param filter only one relation should be considered per each value
	 */
	public void setFilterRelations(boolean filter){
		_filterRelations = filter;
	}
	
	/**
	 * 
	 * @return whether o-p-s sorted input data is GZipped
	 */
	public boolean getGzInOps(){
		return _gzInOps;
	}
	
	/**
	 * 
	 * @return s-p-o input data filename
	 */
	public String getInSpo(){
		return _inSpo;
	}
	
	/**
	 * 
	 * @param gzin	s-p-o input data is GZipped
	 */
	public void setGzInSpo(boolean gzin){
		_gzInSpo = gzin;
	}
	
	/**
	 * 
	 * @return whether s-p-o sorted input data is GZipped
	 */
	public boolean getGzInSpo(){
		return _gzInSpo;
	}
	
	/**
	 * 
	 * @param tmpDir	set the temporary directory for intermediate sorted batches
	 */
	public void setTmpDir(String tmpDir){
		_tmpdir = tmpDir;
	}
	
	/**
	 * 
	 * @return the temporary directory for intermediate sorted batches
	 */
	public String getTmpDir(){
		return _tmpdir;
	}
	
	/**
	 * 
	 * @return output filename for raw concurrence score tuples
	 */
	public String getRawOut(){
		return _outRaw;
	}
	
	/**
	 * 
	 * @param gzro	set whether raw concurrence scores should be GZipped
	 */
	public void setGzRawOut(boolean gzro){
		_gzOutRaw = gzro;
	}
	
	/**
	 * 
	 * @return output filename for raw sorted concurrence score tuples
	 */
	public String getRawSortedOut(){
		return _outRawSorted;
	}
	
	/**
	 * 
	 * @param gzro	set whether raw sorted concurrence scores should be GZipped
	 */
	public void setGzRawSortedOut(boolean gzro){
		_gzOutRawSorted = gzro;
	}
	
	/**
	 * 
	 * @return whether raw concurrence scores should be GZipped
	 */
	public boolean getGzRawOut(){
		return _gzOutRaw;
	}
	
	/**
	 * 
	 * @return whether raw sorted concurrence scores should be GZipped
	 */
	public boolean getGzRawSortedOut(){
		return _gzOutRawSorted;
	}
	
	/**
	 * 
	 * @return the output filename for unsorted results
	 */
	public String getUnsortedOut(){
		return _outUnsorted;
	}
	
	/**
	 * 
	 * @param gzuso	set whether unsorted results should be GZipped
	 */
	public void setGzUnsortedOut(boolean gzuso){
		_gzOutUnsorted = gzuso;
	}
	
	/**
	 * 
	 * @return whether unsorted results should be GZipped
	 */
	public boolean getGzUnsortedOut(){
		return _gzOutUnsorted;
	}
	
	/**
	 * Set whether sorted results should be GZipped
	 * 
	 * @param gzdata	whether sorted results should be GZipped
	 */
	public void setGzData(boolean gzdata){
		_gzOutData = gzdata;
	}
	
	/**
	 * 
	 * @return	whether sorted results should be GZipped
	 */
	public boolean getGzData(){
		return _gzOutData;
	}

	/**
	 * Initialise default output filenames for the given output
	 * directory
	 * 
	 * @param outdir the output directory
	 */
	public void initDefaults(String outdir){
		_tmpdir = getDefaultTmpDir(outdir);
		_outData = getDefaultOutFinalFilename(outdir, _gzOutData);
		_outRaw = getDefaultOutRaw(outdir, _gzOutRaw);
		_outRawSorted = getDefaultOutRawSorted(outdir, _gzOutRawSorted);
		_outUnsorted = getDefaultOutUnsorted(outdir, _gzOutUnsorted);
	}
	
	/**
	 * @return a string representing the argument settings
	 */
	public String toString(){
		return "inS:"+_inSpo+" gzInS:"+_gzInSpo+"inO:"+_inOps+" gzInO:"+_gzInOps+" tmpdir:"+_tmpdir+" outdir:"+_outdir+" outRaw:"+_outRaw+" rawgz:"+_gzOutRaw+" outdata:"+_outData+" outdatagz:"+_gzOutData;
	}
	
	/**
	 * Generate a default output final results filename for the given output
	 * directory and default GZip setting
	 * 
	 * @param outdir the output directory
	 * @return the default filename for final results
	 */
	public static final String getDefaultOutFinalFilename(String outdir){
		return getDefaultOutFinalFilename(outdir, DEFAULT_GZ_OUT);
	}
	
	/**
	 * Generate a default output final results filename for the given output
	 * directory and the given GZip setting
	 * 
	 * @param outdir the output directory
	 * @param gz	the output should be GZipped
	 * @return the default filename for final results
	 */
	public static final String getDefaultOutFinalFilename(String outdir, boolean gz){
		if(gz)
			return outdir+"/"+DEFAULT_OUT_FINAL_FILENAME_GZ;
		return outdir+"/"+DEFAULT_OUT_FINAL_FILENAME_NGZ;
	}
	
	/**
	 * Generate a default output raw concurrence filename for the given output
	 * directory and default GZip setting
	 * 
	 * @param outdir the output directory
	 * @return the default filename for raw concurrence tuples
	 */
	public static final String getDefaultOutRaw(String outdir){
		return getDefaultOutRaw(outdir, DEFAULT_GZ_OUT);
	}
	
	/**
	 * Generate a default output raw concurrence filename for the given output
	 * directory and the given GZip setting
	 * 
	 * @param outdir the output directory
	 * @param gz	the output should be GZipped
	 * @return the default filename for raw concurrence tuples
	 */
	public static final String getDefaultOutRaw(String outdir, boolean gz){
		if(gz)
			return outdir+"/"+DEFAULT_OUT_RAW_FILENAME_GZ;
		return outdir+"/"+DEFAULT_OUT_RAW_FILENAME_NGZ;
	}
	
	/**
	 * Generate a default output raw concurrence filename for the given output
	 * directory and default GZip setting
	 * 
	 * @param outdir the output directory
	 * @return the default filename for raw concurrence tuples
	 */
	public static final String getDefaultOutRawSorted(String outdir){
		return getDefaultOutRawSorted(outdir, DEFAULT_GZ_OUT);
	}
	
	/**
	 * Generate a default output raw concurrence filename for the given output
	 * directory and the given GZip setting
	 * 
	 * @param outdir the output directory
	 * @param gz	the output should be GZipped
	 * @return the default filename for raw concurrence tuples
	 */
	public static final String getDefaultOutRawSorted(String outdir, boolean gz){
		if(gz)
			return outdir+"/"+DEFAULT_OUT_RAW_SORTED_FILENAME_GZ;
		return outdir+"/"+DEFAULT_OUT_RAW_SORTED_FILENAME_NGZ;
	}
	
	/**
	 * Generate a default output raw results filename for the given output
	 * directory and default GZip setting
	 * 
	 * @param outdir the output directory
	 * @return the default filename for raw results
	 */
	public static final String getDefaultOutUnsorted(String outdir){
		return getDefaultOutUnsorted(outdir, DEFAULT_GZ_OUT);
	}
	
	/**
	 * Generate a default output raw results filename for the given output
	 * directory and the given GZip setting
	 * 
	 * @param outdir the output directory
	 * @param gz	the output should be GZipped
	 * @return the default filename for raw results
	 */
	public static final String getDefaultOutUnsorted(String outdir, boolean gz){
		if(gz)
			return outdir+"/"+DEFAULT_OUT_UNSORTED_FILENAME_GZ;
		return outdir+"/"+DEFAULT_OUT_UNSORTED_FILENAME_NGZ;
	}
	
	/**
	 * Get the default output directory for temporary files
	 * @param outdir	output directory
	 * @return default temporary directory
	 */
	public static final String getDefaultTmpDir(String outdir){
		return outdir+"/"+DEFAULT_TMP_DIR;
	}
}
