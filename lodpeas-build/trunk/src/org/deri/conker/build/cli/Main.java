package org.deri.conker.build.cli;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class Main
{
	static transient Logger		_log	= Logger.getLogger(Main.class.getName());

	private static final String	PACKAGE	= Main.class.getPackage().getName() + ".";
	private static final String	USAGE	= "usage: " + Main.class.getName() + " <utility> [options...]";

	public static void main(String[] args)
	{
		try
		{
			if (args.length < 1)
			{
				StringBuffer sb = new StringBuffer();
				sb.append("where <utility> one of");
				sb.append("\n\t" + Consolidate.class.getSimpleName() + "\t\tConsolidate input file");
				sb.append("\n\t" + CleanRedirects.class.getSimpleName() + "\t\tForward and clean-up redirects");
				sb.append("\n\t" + ExtractDocumentGraph.class.getSimpleName() + "\t\tExtract document-level graph");
				sb.append("\n\t" + RankGraph.class.getSimpleName() + "\t\tPageRank an extracted Nx graph");
				sb.append("\n\t" + RankTerms.class.getSimpleName()
						+ "\t\tCompute ranks for terms based on sum of doc. ranks");
				sb.append("\n\t" + RunConcurrence.class.getSimpleName() + "\t\tCompute similarities");
				_log.severe(USAGE);
				_log.severe(sb.toString());
				System.exit(-1);
			}

			Class<?> cls = Class.forName(PACKAGE + args[0]);

			Method mainMethod = cls.getMethod("main", new Class[] { String[].class });

			String[] mainArgs = new String[args.length - 1];
			System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

			long time = System.currentTimeMillis();

			mainMethod.invoke(null, new Object[] { mainArgs });

			long time1 = System.currentTimeMillis();

			_log.info("time elapsed " + (time1 - time) + " ms");
		} catch (Throwable e)
		{
			e.printStackTrace();
			Throwable cause = e.getCause();
			cause.printStackTrace();
			_log.severe(USAGE);
			_log.severe(e.toString());
			System.exit(-1);
		}
	}
}
