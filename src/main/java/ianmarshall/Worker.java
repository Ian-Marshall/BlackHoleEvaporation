package ianmarshall;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ianmarshall.WorkerResult.ResultData;

public class Worker implements Runnable
{
	public class WorkerUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		public WorkerUncaughtExceptionHandler()
		{
		}

		@Override
		public void uncaughtException(Thread t, Throwable th)
		{
			m_wrWorkerResult = new WorkerResult(m_bProcessingCompleted, th, m_nRun, m_rdResultData);
			m_bStopped = true;

			if (m_bwBufferedWriter != null)
				try
				{
					m_bwBufferedWriter.close();
					m_bwBufferedWriter = null;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
		}

	}

	private static final BigDecimal h = new BigDecimal("6.62607015e-34");    // The Planck constant in Js
	private static final BigDecimal c = new BigDecimal(299792458);           // The speed of light in m s^-1
	private static final BigDecimal c2 = c.multiply(c).stripTrailingZeros();
	private static final BigDecimal G = new BigDecimal("6.6743e-11");    // The gravitational constant in m^3 kg^-1 s^-2
	private static final BigDecimal THREE = new BigDecimal(3);
	private static final int N_PRECISION = 100;
	private static final MathContext MC_MATH_CONTEXT = new MathContext(N_PRECISION, RoundingMode.HALF_EVEN);
	private static final BigDecimal K = h.multiply(c2).multiply(c2).divide(
	 new BigDecimal(10240).multiply(new BigDecimal(Math.PI)).multiply(new BigDecimal(Math.PI)).multiply(G).multiply(G),
	 MC_MATH_CONTEXT)
	 .stripTrailingZeros();

	// The mass of the Universe at the Big Bang in kg (taken to be the same as its current mass)
	private static final BigDecimal BD_UNIVERSAL_MASS = new BigDecimal("1.5e53");
//private static final BigDecimal BD_UNIVERSAL_MASS = new BigDecimal("1.5e13");    // Test, reduced mass

	private static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-90");
	private static final BigDecimal BD_BASE_TIME_INCREMENT = new BigDecimal("3e52");
	private static final BigDecimal BD_START_RADIUS_RATIO = BigDecimal.ONE.add(BD_START_RADIUS_RATIO_FRACTION);
	private static final BigDecimal BD_ONE_YEAR_IN_SECONDS = new BigDecimal("3.1536e7");
	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
	private static final int N_LOG_SKIP_RATIO = 1_000_000;
	private static final File S_FILE_OUTPUT = new File("logs\\Output.csv");
	private static final BigDecimal BD_TEN_TO_THE_2 = BigDecimal.TEN.pow(2);
	private static final BigDecimal BD_TEN_TO_THE_3 = BigDecimal.TEN.pow(3);
	private static final BigDecimal BD_TEN_TO_THE_4 = BigDecimal.TEN.pow(4);
	private static final BigDecimal BD_TEN_TO_THE_5 = BigDecimal.TEN.pow(5);
	private static final BigDecimal BD_TEN_TO_THE_6 = BigDecimal.TEN.pow(6);

	private static int m_nProcessors = 0;

	private StartParameters m_spStartParameters = null;
	private int m_nRun = 0;
	private ResultData m_rdResultData = null;
	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
	private WorkerResult m_wrWorkerResult = null;
	private BufferedWriter m_bwBufferedWriter = null;
	private BigDecimal m_bdTimeIncrementTooBigSeconds = BigDecimal.ONE;
	private BigDecimal m_bdTimeIncrementTooSmallSeconds = BigDecimal.ONE;
	private int m_nMinRunsForTimeIncrementNotTooBig = 0;
	private int m_nRunForTimeIncrement = 0;
	private BigDecimal m_bdTimeIncrement = null;
	private BigDecimal m_bdTime = null;
	private BigDecimal m_bdMass = null;
	private BigDecimal m_bdRadius = null;

	/**
	 * The constructor.
	 * @param spStartParameters
	 *   The the application's start parameters.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means no run has yet been executed.
	 * @param rdResultData
	 *   If not <code>null</code> then use this to set the current result of the run(s), otherwise use initial values.
	 */
	public Worker(StartParameters spStartParameters, int nRun, ResultData rdResultData)
	{
		if (m_nProcessors == 0)
			m_nProcessors = Runtime.getRuntime().availableProcessors();

		m_spStartParameters = spStartParameters;
		m_nRun = nRun;
		m_rdResultData = rdResultData;
		m_wuehExceptionHandler = new WorkerUncaughtExceptionHandler();

		try
		{
			FileWriter fwFileWriter = new FileWriter(S_FILE_OUTPUT, m_nRun > 0);
			m_bwBufferedWriter = new BufferedWriter(fwFileWriter);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void stopExecution()
	{
		m_bStopping = true;
		s_logger.info(String.format("Stopping run number %s...", BlackHoleEvaporation.formatInteger(m_nRun)));
	}

	/**
	 * @return
	 *   Whether working has stopped, whether all processing has been completed or not.
	 */
	public boolean getStopped()
	{
		return m_bStopped;
	}

	public WorkerUncaughtExceptionHandler getWorkerUncaughtExceptionHandler()
	{
		return m_wuehExceptionHandler;
	}

	public WorkerResult getWorkerResult()
	{
		return m_wrWorkerResult;
	}

	@Override
	public void run()
	{
		m_bStopped = false;
		m_bStopping = false;

		/*
		testToScientificFormat(new BigDecimal(
		 "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
		 .negate());

		testToScientificFormat(new BigDecimal(
		 "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"));

		testToScientificFormat(new BigDecimal("-0.001"));
		testToScientificFormat(new BigDecimal("0.001"));

		testToScientificFormat(BigDecimal.TEN.negate());
		testToScientificFormat(BigDecimal.TWO.negate());
		testToScientificFormat(BigDecimal.ONE.negate());
		testToScientificFormat(BigDecimal.ZERO);
		testToScientificFormat(BigDecimal.ONE);
		testToScientificFormat(BigDecimal.TWO);
		testToScientificFormat(BigDecimal.TEN);
		*/

		do
		{
			execute();

			if ((!m_bStopping) && (m_nMinRunsForTimeIncrementNotTooBig > 0)
			 && (m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0))
			{
				boolean bTimeIncrementTooBig = false;

				if (m_nRunForTimeIncrement < m_nMinRunsForTimeIncrementNotTooBig)
				{
					m_bdTimeIncrementTooBigSeconds = m_bdTimeIncrement;
					m_spStartParameters.setTimeIncrementTooBigSeconds(m_bdTimeIncrementTooBigSeconds);
					m_rdResultData.setTimeIncrementTooBigSeconds(m_bdTimeIncrementTooBigSeconds);
					bTimeIncrementTooBig = true;
				}
				else
				{
					m_bdTimeIncrementTooSmallSeconds = m_bdTimeIncrement;
					m_spStartParameters.setTimeIncrementTooSmallSeconds(m_bdTimeIncrementTooSmallSeconds);
					m_rdResultData.setTimeIncrementTooSmallSeconds(m_bdTimeIncrementTooSmallSeconds);
				}

				m_bdTime = BigDecimal.ZERO;
				m_bdMass = BD_UNIVERSAL_MASS;
				m_rdResultData.setTime(m_bdTime);
				m_rdResultData.setMass(m_bdMass);

				s_logger.info(String.format("The tried time increment = %ss (%s), m_nRun = %d.", m_bdTimeIncrement.toString(),
				 bTimeIncrementTooBig ? "too big" : "too small", m_nRun));
			}
		}
		while ((!m_bStopping) && (m_bdMass.compareTo(BigDecimal.ZERO) == 1) && ((m_nMinRunsForTimeIncrementNotTooBig <= 0)
		 || (m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0)));

		if (m_bwBufferedWriter != null)
			try
			{
				m_bwBufferedWriter.close();
				m_bwBufferedWriter = null;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		if (m_bdMass.compareTo(BigDecimal.ZERO) <= 0)
		{
			m_bProcessingCompleted = true;
			s_logger.info("All processing has been completed.");
		}
		else if ((m_nMinRunsForTimeIncrementNotTooBig > 0)
		 && (m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) < 0))
		{
			m_bProcessingCompleted = true;
			s_logger.info("All processing has been completed. The time increment has now been bracketed.");
		}
		else
			s_logger.info("Stopped before all processing completed.");

		m_wrWorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_rdResultData);
		m_bStopped = true;
	}

	private void execute()
	{
		m_nMinRunsForTimeIncrementNotTooBig = m_spStartParameters.getNMinRunsForTimeIncrementNotTooBig();

		if (m_rdResultData != null)
		{
			m_bdTime = m_rdResultData.getTime();
			m_bdMass = m_rdResultData.getMass();
			m_bdRadius = m_rdResultData.getRadius();
			m_bdTimeIncrementTooBigSeconds = m_rdResultData.getTimeIncrementTooBigSeconds();
			m_bdTimeIncrementTooSmallSeconds = m_rdResultData.getTimeIncrementTooSmallSeconds();
		}
		else
		{
			m_bdTime = BigDecimal.ZERO;
			m_bdMass = BD_UNIVERSAL_MASS;
			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);
			m_bdRadius = dbStartSchwarzschildRadius.multiply(BD_START_RADIUS_RATIO).stripTrailingZeros();
			m_bdTimeIncrementTooBigSeconds = m_spStartParameters.getTimeIncrementTooBigSeconds();
			m_bdTimeIncrementTooSmallSeconds = m_spStartParameters.getTimeIncrementTooSmallSeconds();
			m_rdResultData = new ResultData(m_bdRadius, m_bdTime, m_bdMass, m_bdTimeIncrementTooBigSeconds,
			 m_bdTimeIncrementTooSmallSeconds);
		}

		if (m_nMinRunsForTimeIncrementNotTooBig > 0)
			m_bdTimeIncrement = m_bdTimeIncrementTooBigSeconds.add(m_bdTimeIncrementTooSmallSeconds)
			 .divide(BigDecimal.TWO, MC_MATH_CONTEXT).stripTrailingZeros();
		else
			m_bdTimeIncrement = calculateTimeIncrement(m_nRun);

		s_logger.info(String.format("m_bdTimeIncrementTooBigSeconds = %ss, m_bdTimeIncrementTooSmallSeconds = %ss,"
		 + " m_bdTimeIncrement = %ss.",
		 m_bdTimeIncrementTooBigSeconds.toString(),
		 m_bdTimeIncrementTooSmallSeconds.toString(),
		 m_bdTimeIncrement.stripTrailingZeros().toString()));

		if (m_nRun == 0)
		{
			s_logger.info(String.format("Universal mass: %skg, start radius ratio: %s + %s,"
			 + " \"too big\" time increment: %ss, \"too small\" time increment: %ss,"
			 + " minimum number of runs for not-\"too-big\"-a-time increment: %s.",
			 m_bdMass.stripTrailingZeros().toString(),
			 BigDecimal.ONE.toString(),
			 BD_START_RADIUS_RATIO_FRACTION.toString(),
			 m_bdTimeIncrementTooBigSeconds.stripTrailingZeros().toString(),
			 m_bdTimeIncrementTooSmallSeconds.stripTrailingZeros().toString(),
			 BlackHoleEvaporation.formatInteger(m_nMinRunsForTimeIncrementNotTooBig)));

			s_logger.info("Run number, time slow-down factor, mass in kg");
			writeToFile("Run number, time in years, time slow-down factor, mass in kg");
		}

		m_nRunForTimeIncrement = 0;

		while ((!m_bStopping) && (m_bdMass.compareTo(BigDecimal.ZERO) == 1)
		 && ((m_nMinRunsForTimeIncrementNotTooBig <= 0)
		     || ((m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0)
		         && (m_nRunForTimeIncrement < m_nMinRunsForTimeIncrementNotTooBig))))
		{
			m_nRun++;
			m_nRunForTimeIncrement++;

			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);
			BigDecimal bdTimeSlowDownFactor =
			 BigDecimal.ONE.subtract(dbStartSchwarzschildRadius.divide(m_bdRadius, MC_MATH_CONTEXT));
			BigDecimal bd_dMdt = K.negate().divide(THREE.multiply(bdTimeSlowDownFactor).multiply(m_bdMass).multiply(m_bdMass),
			 MC_MATH_CONTEXT);
			BigDecimal bdPreviousMass = m_bdMass;
			BigDecimal bd_deltaM = bd_dMdt.multiply(m_bdTimeIncrement);
			m_bdMass = m_bdMass.add(bd_deltaM).stripTrailingZeros();
			m_bdTime = m_bdTime.add(m_bdTimeIncrement).stripTrailingZeros();
			m_rdResultData.setMass(m_bdMass);
			m_rdResultData.setTime(m_bdTime);

			if (m_nMinRunsForTimeIncrementNotTooBig <= 0)
				m_bdTimeIncrement = calculateTimeIncrement(m_nRun);

			if (reportRun(m_nRun))
			{
				if (m_nRun < N_LOG_SKIP_RATIO)
					s_logger.info(String.format("Worker.execute():"
					 + "%n  m_nRun            = %d,"
					 + "%n  m_bdTimeIncrement = \"%s\","
					 + "%n  bdPreviousMass    = \"%s\","
					 + "%n  bd_deltaM         = \"%s\","
					 + "%n  m_bdMass          = \"%s\".",
					 m_nRun, toScientificFormat(m_bdTimeIncrement), toScientificFormat(bdPreviousMass),
					 toScientificFormat(bd_deltaM), toScientificFormat(m_bdMass)));

				final int N_MINIMUM_PRECISION = 5;
				final int N_MINIMUM_LENGTH_TIME_IN_YEARS = 11;

				String sNRun = BlackHoleEvaporation.formatInteger(m_nRun);
				int nSpacesToPrefix = N_MINIMUM_LENGTH_TIME_IN_YEARS - sNRun.length();
				if (nSpacesToPrefix > 0)
					sNRun = " ".repeat(nSpacesToPrefix) + sNRun;

				BigDecimal bdTimeInYears = m_bdTime.divide(BD_ONE_YEAR_IN_SECONDS, MC_MATH_CONTEXT).stripTrailingZeros();
				int nZeroesToAdd = N_MINIMUM_PRECISION - bdTimeInYears.precision();
				if (nZeroesToAdd > 0)
					bdTimeInYears = bdTimeInYears.setScale(bdTimeInYears.scale() + nZeroesToAdd);

				String sTimeSlowDownFactor = toScientificFormat(bdTimeSlowDownFactor);
				String sMassInKg = toScientificFormat(m_bdMass);
				s_logger.info(String.format("%s, %s, %s", sNRun, sTimeSlowDownFactor, sMassInKg));

				String sTimeInYears = bdTimeInYears.toString();
				sTimeSlowDownFactor = bdTimeSlowDownFactor.toString();
				sMassInKg = m_bdMass.toString();
				writeToFile(String.format("%d,%s,%s,%s", m_nRun, sTimeInYears, sTimeSlowDownFactor, sMassInKg));
			}
		}
	}

	private void writeToFile(String sLine)
	{
		try
		{
			m_bwBufferedWriter.write(sLine);
			m_bwBufferedWriter.newLine();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	/*
	private void testToScientificFormat(BigDecimal bd)
	{
		s_logger.info(String.format("Worker.testToScientificFormat(...):"
		 + "%n  bd.stripTrailingZeros().toString() = \"%s\","
		 + "%n  toScientificFormat(bd)             = \"%s\".",
		 bd.stripTrailingZeros().toString(), toScientificFormat(bd)));
	}
	*/

	private String toScientificFormat(BigDecimal bd)
	{
		StringBuilder sbResult = new StringBuilder();
		bd = bd.stripTrailingZeros();
		int nPrecision = bd.precision();
		int nScale = bd.scale();
		int nExponent = nPrecision - nScale - 1;
		String sDigits = bd.unscaledValue().abs().toString();

		if (bd.signum() < 0)
			sbResult.append('-');

		sbResult.append(sDigits.substring(0, 1));

		if (sDigits.length() > 1)
		{
			sbResult.append('.');
			sbResult.append(sDigits.substring(1));
		}

		sbResult.append("E");

		if (nExponent > 0)
			sbResult.append('+');

		sbResult.append(nExponent);
		return sbResult.toString();
	}

	/*
	private BigDecimal calculateTimeIncrement(BigDecimal bdTime)
	{
		BigDecimal bdResult = BD_BASE_TIME_INCREMENT;

		if      (bdTime.compareTo(BigDecimal.TEN)  < 0)
			bdResult = bdResult.divide(BD_TEN_TO_THE_6);
		else if (bdTime.compareTo(BD_TEN_TO_THE_2) < 0)
			bdResult = bdResult.divide(BD_TEN_TO_THE_5);
		else if (bdTime.compareTo(BD_TEN_TO_THE_3) < 0)
			bdResult = bdResult.divide(BD_TEN_TO_THE_4);
		else if (bdTime.compareTo(BD_TEN_TO_THE_4) < 0)
			bdResult = bdResult.divide(BD_TEN_TO_THE_3);
		else if (bdTime.compareTo(BD_TEN_TO_THE_5) < 0)
			bdResult = bdResult.divide(BD_TEN_TO_THE_2);
		else if (bdTime.compareTo(BD_TEN_TO_THE_6) < 0)
			bdResult = bdResult.divide(BigDecimal.TEN);

		return bdResult;
	}
	*/

	private BigDecimal calculateTimeIncrement(int nRun)
	{
		BigDecimal bdResult = BD_BASE_TIME_INCREMENT;

		if      (nRun <        10)
			bdResult = bdResult.divide(BD_TEN_TO_THE_6);
		else if (nRun <       100)
			bdResult = bdResult.divide(BD_TEN_TO_THE_5);
		else if (nRun <     1_000)
			bdResult = bdResult.divide(BD_TEN_TO_THE_4);
		else if (nRun <    10_000)
			bdResult = bdResult.divide(BD_TEN_TO_THE_3);
		else if (nRun <   100_000)
			bdResult = bdResult.divide(BD_TEN_TO_THE_2);
		else if (nRun < 1_000_000)
			bdResult = bdResult.divide(BigDecimal.TEN);

		return bdResult;
	}

	private boolean reportRun(int nRun)
	{
		return (nRun % N_LOG_SKIP_RATIO == 0) || (nRun == 1) || (nRun == 10) || (nRun == 100) || (nRun == 1_000)
		 || (nRun == 10_000) || (nRun == 100_000);
	}
}
