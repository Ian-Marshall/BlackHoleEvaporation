package ianmarshall;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlackHoleEvaporation
{
	private static final Logger logger = LoggerFactory.getLogger(BlackHoleEvaporation.class);
	private static final DecimalFormat m_dfInteger;
	private static final DecimalFormat m_dfLong;
	private static final DecimalFormat m_dfFloat;

	static
	{
		DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols();
		dfSymbols.setDecimalSeparator('.');
		dfSymbols.setGroupingSeparator(' ');
		m_dfInteger = new DecimalFormat("###,###",     dfSymbols);
		m_dfLong    = new DecimalFormat("###,###",     dfSymbols);
		m_dfFloat   = new DecimalFormat("###,###.###", dfSymbols);
		m_dfFloat.setMinimumFractionDigits(1);
	}

	public BlackHoleEvaporation()
	{
	}

	public static void main(String[] asArgs) throws IOException
	{
		BlackHoleEvaporation bbsa = new BlackHoleEvaporation();
		bbsa.execute(asArgs);
	}

	private void execute(String[] asArgs) throws IOException
	{
		StartParameters spStartParams = new StartParameters();
		spStartParams.showUsage();

		String sError = spStartParams.parseArguments(asArgs);
		if (sError.isEmpty())
		{
			double dblStartRadiusRatio = spStartParams.getStartRadiusRatio();
			long loTimeIncrementSeconds = spStartParams.getTimeIncrementSeconds();

			int nMaxWidthParams = Collections.max(Arrays.asList(
			 StartParameters.S_ARG_NAME_START_RADIUS_RATIO.length(),
			 StartParameters.S_ARG_NAME_TIME_INCREMENT_SECONDS.length()));

	 // String sRuns                 = formatInteger(nRuns) + "  ";
			String sStartRadiusRatio     = formatDouble(dblStartRadiusRatio);
			String sTimeIncrementSeconds = formatLong(loTimeIncrementSeconds);

			int nMaxWidthValues = Collections.max(Arrays.asList(
			 sStartRadiusRatio.length(),
			 sTimeIncrementSeconds.length()));

			String sFormat = String.format("Parameter values:"
			 + "%%n  %%%1$ss = %%%2$ss,"
			 + "%%n  %%%1$ss = %%%2$ss."
			 + "%%n%%nTo pause execution enter \"P\"."
			 + "%%nFrom a paused execution, enter \"S\" to stop execution and anything else to resume execution.",
			 nMaxWidthParams, nMaxWidthValues);

			logger.info(String.format(sFormat,
			 StartParameters.S_ARG_NAME_START_RADIUS_RATIO,     sStartRadiusRatio,
			 StartParameters.S_ARG_NAME_TIME_INCREMENT_SECONDS, sTimeIncrementSeconds));

			final int N_DELAY_BEFORE_START_S = 30;
			logger.info(String.format("Waiting %ds before starting processing...", N_DELAY_BEFORE_START_S));

			try
			{
				Thread.sleep(N_DELAY_BEFORE_START_S * 1000);
			}
			catch (InterruptedException e)
			{
				// Do nothing
			}

			logger.info(String.format("Finished waiting %ds.", N_DELAY_BEFORE_START_S));

			Supervisor supervisor = new Supervisor(spStartParams);
			WorkerResult wrResult = supervisor.execute();
			boolean bProcessingCompleted = wrResult.getProcessingCompleted();
			int nRun = wrResult.getRun();
			Throwable th = wrResult.getThrowable();

			if (bProcessingCompleted)
				sFormat = "Processing has completed.";
			else
				sFormat = "Processing was stopped before it completed.";

			sFormat += " The latest run number executed was %s.";
			String sRun = formatInteger(nRun);
			logger.info(String.format(sFormat, sRun));

			StringBuilder sb = new StringBuilder();

			while (th != null)
			{
				if (sb.length() == 0)
					sb.append(String.format("An exception or error was thrown: "));
				else
					sb.append(String.format("caused by: "));

				sb.append(String.format("\"%s\"%n with stack trace:%n", th.toString()));
				StackTraceElement[] asteStackTraceElements = th.getStackTrace();

				for (StackTraceElement steStackTraceElement: asteStackTraceElements)
					sb.append(String.format("  %s%n", steStackTraceElement.toString()));

				th = th.getCause();
			}

			sError = sb.toString();
		}

		if (!sError.isEmpty())
			logger.error(sError);
	}

	public static String formatInteger(int n)
	{
		return m_dfInteger.format(n);
	}

	public static String formatLong(long lo)
	{
		return m_dfLong.format(lo);
	}

	public static String formatDouble(double dbl)
	{
		return m_dfFloat.format(dbl);
	}
}
