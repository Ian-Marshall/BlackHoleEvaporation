package ianmarshall;

import java.math.BigDecimal;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartParameters
{
	private static final Logger s_logger = LoggerFactory.getLogger(StartParameters.class);
	private static int N_NUMBER_OF_ARGS = 3;


	// The parameters' argument names and data types

	private static final String      S_ARG_NAME_TIME_INCREMENT_TOO_BIG_SECONDS = "timeIncrementTooBigSeconds";
	private static final String S_ARG_DATA_TYPE_TIME_INCREMENT_TOO_BIG_SECONDS = "BigDecimal string";

	private static final String      S_ARG_NAME_TIME_INCREMENT_TOO_SMALL_SECONDS = "timeIncrementTooSmallSeconds";
	private static final String S_ARG_DATA_TYPE_TIME_INCREMENT_TOO_SMALL_SECONDS = "BigDecimal string";

	private static final String      S_ARG_NAME_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG =
	 "minimumNumberOfRunsForTimeIncrementNotTooBig";
	private static final String S_ARG_DATA_TYPE_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG = "integer";


	// The parameters' fields
	private BigDecimal m_bdTimeIncrementTooBigSeconds = BigDecimal.ZERO;
	private BigDecimal m_bdTimeIncrementTooSmallSeconds = BigDecimal.ZERO;
	private int m_nMinRunsForTimeIncrementNotTooBig = 0;

	public StartParameters()
	{
	}

	public BigDecimal getTimeIncrementTooBigSeconds()
	{
		return m_bdTimeIncrementTooBigSeconds;
	}

	public void setTimeIncrementTooBigSeconds(BigDecimal bdTimeIncrementTooBigSeconds)
	{
		m_bdTimeIncrementTooBigSeconds = bdTimeIncrementTooBigSeconds;
	}

	public BigDecimal getTimeIncrementTooSmallSeconds()
	{
		return m_bdTimeIncrementTooSmallSeconds;
	}

	public void setTimeIncrementTooSmallSeconds(BigDecimal bdTimeIncrementTooSmallSeconds)
	{
		m_bdTimeIncrementTooSmallSeconds = bdTimeIncrementTooSmallSeconds;
	}

	public int getNMinRunsForTimeIncrementNotTooBig()
	{
		return m_nMinRunsForTimeIncrementNotTooBig;
	}

	public void showUsage()
	{
		String sMsg = String.format(
		   "%nUsage"
		 + "%n-----"
		 + "%n  %s %s [%s] %s [%s] %s [%s]%n"
		 + "%n[%2$s] is the increase in the elapsed time as experienced by the observer for each iteration, in seconds,"
		 + " that is too big to be suitable for iterating."
		 + " This must be greater than zero."
		 + "%n[%4$s] is the increase in the elapsed time as experienced by the observer for each iteration, in seconds,"
		 + " that is too small to be suitable for iterating."
		 + " This must be greater than zero."
		 + "%n[%6$s] is the number of runs which, if reached, means that the current time increment is not too big"
		 + " to be suitable for iterating. If this less than or equal to zero then this argument will be ignored."
		 + "%n",
		 BlackHoleEvaporation.class.getSimpleName(),
		 S_ARG_NAME_TIME_INCREMENT_TOO_BIG_SECONDS,                S_ARG_DATA_TYPE_TIME_INCREMENT_TOO_BIG_SECONDS,
		 S_ARG_NAME_TIME_INCREMENT_TOO_SMALL_SECONDS,              S_ARG_DATA_TYPE_TIME_INCREMENT_TOO_SMALL_SECONDS,
		 S_ARG_NAME_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG, S_ARG_DATA_TYPE_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG);

		s_logger.info(sMsg);
	}

	public String parseArguments(String[] asArgs)
	{
		StringBuilder sbError = new StringBuilder();
		s_logger.info(String.format("About to parse the command line arguments \"%s\".", Arrays.asList(asArgs)));

		if (asArgs.length == 2 * N_NUMBER_OF_ARGS)
		{
			int nIndexArgTimeIncrementTooBigSeconds = -1;
			int nIndexArgTimeIncrementTooSmallSeconds = -1;
			int nIndexArgNMinRunsForTimeIncrementNotTooBig = -1;

			for (int i = 0; i < N_NUMBER_OF_ARGS; i++)
			{
				int nIndexArgName = 2 * i;

				if      (asArgs[nIndexArgName].equalsIgnoreCase(S_ARG_NAME_TIME_INCREMENT_TOO_BIG_SECONDS))
					nIndexArgTimeIncrementTooBigSeconds = nIndexArgName + 1;
				else if (asArgs[nIndexArgName].equalsIgnoreCase(S_ARG_NAME_TIME_INCREMENT_TOO_SMALL_SECONDS))
					nIndexArgTimeIncrementTooSmallSeconds = nIndexArgName + 1;
				else if (asArgs[nIndexArgName].equalsIgnoreCase(S_ARG_NAME_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG))
					nIndexArgNMinRunsForTimeIncrementNotTooBig = nIndexArgName + 1;
			}

			if ((nIndexArgTimeIncrementTooBigSeconds > -1) && (nIndexArgTimeIncrementTooSmallSeconds > -1)
			 && (nIndexArgNMinRunsForTimeIncrementNotTooBig > -1))
				try
				{
					m_bdTimeIncrementTooBigSeconds = new BigDecimal(asArgs[nIndexArgTimeIncrementTooBigSeconds]);
					m_bdTimeIncrementTooSmallSeconds = new BigDecimal(asArgs[nIndexArgTimeIncrementTooSmallSeconds]);
					m_nMinRunsForTimeIncrementNotTooBig = Integer.parseInt(asArgs[nIndexArgNMinRunsForTimeIncrementNotTooBig]);

					if (m_bdTimeIncrementTooBigSeconds.compareTo(BigDecimal.ZERO) < 1)
						sbError.append(String.format("The parameter %s of value \"%s\" must be greater than zero.",
						 S_ARG_NAME_TIME_INCREMENT_TOO_BIG_SECONDS, asArgs[nIndexArgTimeIncrementTooBigSeconds]));

					if (m_bdTimeIncrementTooSmallSeconds.compareTo(BigDecimal.ZERO) < 1)
						sbError.append(String.format("The parameter %s of value \"%s\" must be greater than zero.",
						 S_ARG_NAME_TIME_INCREMENT_TOO_SMALL_SECONDS, asArgs[nIndexArgTimeIncrementTooSmallSeconds]));
				}
				catch (NumberFormatException e)
				{
					if (sbError.length() > 0)
						sbError.append(" ");

					sbError.append("At least one of the parameters has an incorrect data type.");
				}
			else
				sbError.append(String.format("At least one of the parameters %s, %s and %s is missing.",
				 S_ARG_NAME_TIME_INCREMENT_TOO_BIG_SECONDS,
				 S_ARG_NAME_TIME_INCREMENT_TOO_SMALL_SECONDS,
				 S_ARG_NAME_N_MINIMUM_RUNS_FOR_TIME_INCREMENT_NOT_TOO_BIG));
		}
		else
			sbError.append(String.format("Please specify exactly %d parameters, each with one value.", N_NUMBER_OF_ARGS));

		if (sbError.length() > 0)
			sbError.append(" Please see the program's usage for details.");

		return sbError.toString();
	}
}
