package ianmarshall;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartParameters
{
	private static final Logger s_logger = LoggerFactory.getLogger(StartParameters.class);
	private static int N_NUMBER_OF_ARGS = 2;


	// The parameters' argument names and data types

	public  static final String      S_ARG_NAME_START_RADIUS_RATIO = "startRadiusRatio";
	private static final String S_ARG_DATA_TYPE_START_RADIUS_RATIO = "decimal number";

	public  static final String      S_ARG_NAME_TIME_INCREMENT_SECONDS = "timeIncrementSeconds";
	private static final String S_ARG_DATA_TYPE_TIME_INCREMENT_SECONDS = "integer";


	// The parameters' fields
	private double m_dblStartRadiusRatio = 1.0;
	private long m_loTimeIncrementSeconds = 1L;

	public StartParameters()
	{
	}

	public double getStartRadiusRatio()
	{
		return m_dblStartRadiusRatio;
	}

	public long getTimeIncrementSeconds()
	{
		return m_loTimeIncrementSeconds;
	}

	public void showUsage()
	{
		String sMsg = String.format(
		   "%nUsage"
		 + "%n-----"
		 + "%n  %s %s [%s] %s [%s]%n"
		 + "%n[%2$s] is the initial ratio of the distance of the observer from the centre of black hole to its Schwarzschild radius."
		 + " This must be greater than one."
		 + "%n[%4$s] is the increase in the elapsed time as experienced by the observer, in seconds, for each iteration."
		 + " This must be greater than zero."
		 + "%n",
		 BlackHoleEvaporation.class.getSimpleName(),
		 S_ARG_NAME_START_RADIUS_RATIO,     S_ARG_DATA_TYPE_START_RADIUS_RATIO,
		 S_ARG_NAME_TIME_INCREMENT_SECONDS, S_ARG_DATA_TYPE_TIME_INCREMENT_SECONDS);

		s_logger.info(sMsg);
	}

	public String parseArguments(String[] asArgs)
	{
		StringBuilder sbError = new StringBuilder();
		s_logger.info(String.format("About to parse the command line arguments \"%s\".", Arrays.asList(asArgs)));

		if (asArgs.length == 2 * N_NUMBER_OF_ARGS)
		{
			int nIndexArgStartRadiusRatio = -1;
			int nIndexArgTimeIncrementSeconds = -1;

			for (int i = 0; i < N_NUMBER_OF_ARGS; i++)
			{
				int nIndexArgName = 2 * i;

				if (S_ARG_NAME_START_RADIUS_RATIO.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgStartRadiusRatio = nIndexArgName + 1;
				else if (S_ARG_NAME_TIME_INCREMENT_SECONDS.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgTimeIncrementSeconds = nIndexArgName + 1;

			}

			if ((nIndexArgStartRadiusRatio > -1) && (nIndexArgTimeIncrementSeconds > -1))
				try
				{
					m_dblStartRadiusRatio = Double.parseDouble(asArgs[nIndexArgStartRadiusRatio]);
					m_loTimeIncrementSeconds = Long.parseLong(asArgs[nIndexArgTimeIncrementSeconds]);

					s_logger.info(String.format(
					 "asArgs[nIndexArgStartRadiusRatio] = \"%s\", m_dblStartRadiusRatio = %g, m_dblStartRadiusRatio - 1.0 = %g .",
					 asArgs[nIndexArgStartRadiusRatio], m_dblStartRadiusRatio, m_dblStartRadiusRatio - 1.0));

					if (m_dblStartRadiusRatio <= 1.0)
						sbError.append(String.format("The parameter \"%s\" of value %f must be greater than 1.0.",
						 S_ARG_NAME_START_RADIUS_RATIO, m_dblStartRadiusRatio));

					if (m_loTimeIncrementSeconds <= 0)
					{
						if (sbError.length() > 0)
							sbError.append(" ");

						sbError.append(String.format("The parameter \"%s\" of value %d must be greater than 0.",
						 S_ARG_NAME_TIME_INCREMENT_SECONDS, m_loTimeIncrementSeconds));
					}
				}
				catch (NumberFormatException e)
				{
					sbError.append("At least one of the parameters has an incorrect data type.");
				}
			else
				sbError.append(String.format(
				 "At least one of the parameters \"%s\" and \"%s\" is missing.",
				 S_ARG_NAME_START_RADIUS_RATIO, S_ARG_NAME_TIME_INCREMENT_SECONDS));
		}
		else
			sbError.append(String.format("Please specify exactly %d parameters, each with one value.", N_NUMBER_OF_ARGS));

		if (sbError.length() > 0)
			sbError.append(" Please see the program's usage for details.");

		return sbError.toString();
	}
}
