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
	private static final int N_PRECISION = 300;
	private static final int N_LOG_PRECISION = 10;
	private static final MathContext MC_MATH_CONTEXT = new MathContext(N_PRECISION, RoundingMode.HALF_EVEN);
	private static final MathContext MC_LOGGING = new MathContext(N_LOG_PRECISION, RoundingMode.HALF_EVEN);
	private static final BigDecimal K = h.multiply(c2).multiply(c2).divide(
	 new BigDecimal(10240).multiply(new BigDecimal(Math.PI)).multiply(new BigDecimal(Math.PI)).multiply(G).multiply(G),
	 MC_MATH_CONTEXT)
	 .stripTrailingZeros();

	private static final BigDecimal BD_START_RADIUS_RATIO = BigDecimal.ONE.add(
	 RunParameters.BD_START_RADIUS_RATIO_FRACTION);
	private static final BigDecimal BD_ONE_YEAR_IN_SECONDS = new BigDecimal("3.1536e7");
	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
//private static final int N_LOG_SKIP_RATIO = 1_000_000;
	private static final int N_LOG_SKIP_RATIO = 10_000;
	private static final File FILE_OUTPUT = new File("logs\\data.csv");

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
	private BigDecimal m_bdTimeIncrementSeconds = null;
	private BigDecimal m_bdTimeSeconds = null;
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
			FileWriter fwFileWriter = new FileWriter(FILE_OUTPUT, m_nRun > 0);
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
					m_bdTimeIncrementTooBigSeconds = m_bdTimeIncrementSeconds;
					m_spStartParameters.setTimeIncrementTooBigSeconds(m_bdTimeIncrementTooBigSeconds);
					m_rdResultData.setTimeIncrementTooBigSeconds(m_bdTimeIncrementTooBigSeconds);
					bTimeIncrementTooBig = true;
				}
				else
				{
					m_bdTimeIncrementTooSmallSeconds = m_bdTimeIncrementSeconds;
					m_spStartParameters.setTimeIncrementTooSmallSeconds(m_bdTimeIncrementTooSmallSeconds);
					m_rdResultData.setTimeIncrementTooSmallSeconds(m_bdTimeIncrementTooSmallSeconds);
				}

				m_bdTimeSeconds = BigDecimal.ZERO;
				m_bdMass = RunParameters.BD_START_UNIVERSAL_MASS_IN_KG;
				m_rdResultData.setTimeSeconds(m_bdTimeSeconds);
				m_rdResultData.setMass(m_bdMass);

				s_logger.info(String.format("The tried time increment = %ss (%s), m_nRun = %d.",
				 m_bdTimeIncrementSeconds.toString(), bTimeIncrementTooBig ? "too big" : "too small", m_nRun));
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
			m_bdTimeSeconds = m_rdResultData.getTimeSeconds();
			m_bdMass = m_rdResultData.getMass();
			m_bdRadius = m_rdResultData.getRadius();
			m_bdTimeIncrementTooBigSeconds = m_rdResultData.getTimeIncrementTooBigSeconds();
			m_bdTimeIncrementTooSmallSeconds = m_rdResultData.getTimeIncrementTooSmallSeconds();
		}
		else
		{
			m_bdTimeSeconds = BigDecimal.ZERO;
			m_bdMass = RunParameters.BD_START_UNIVERSAL_MASS_IN_KG;
			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);
			m_bdRadius = dbStartSchwarzschildRadius.multiply(BD_START_RADIUS_RATIO).stripTrailingZeros();
			m_bdTimeIncrementTooBigSeconds = m_spStartParameters.getTimeIncrementTooBigSeconds();
			m_bdTimeIncrementTooSmallSeconds = m_spStartParameters.getTimeIncrementTooSmallSeconds();
			m_rdResultData = new ResultData(m_bdRadius, m_bdTimeSeconds, m_bdMass, m_bdTimeIncrementTooBigSeconds,
			 m_bdTimeIncrementTooSmallSeconds);
		}

		if (m_nMinRunsForTimeIncrementNotTooBig > 0)
			m_bdTimeIncrementSeconds = m_bdTimeIncrementTooBigSeconds.add(m_bdTimeIncrementTooSmallSeconds)
			 .divide(BigDecimal.TWO, MC_MATH_CONTEXT).stripTrailingZeros();
		else
			m_bdTimeIncrementSeconds = RunParameters.calculateTimeIncrementSeconds(m_nRun);

		s_logger.info(String.format("m_bdTimeIncrementTooBigSeconds = %ss, m_bdTimeIncrementTooSmallSeconds = %ss,"
		 + " m_bdTimeIncrementSeconds = %ss.",
		 m_bdTimeIncrementTooBigSeconds.toString(),
		 m_bdTimeIncrementTooSmallSeconds.toString(),
		 m_bdTimeIncrementSeconds.stripTrailingZeros().toString()));

		if (m_nRun == 0)
		{
			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);

			s_logger.info(String.format("Start values:"
			 + "%n  Universal mass:                                          %skg,"
			 + "%n  start radius ratio:                                      %s + %s,"
			 + "%n  start Schwarzschild radius:                              %s,"
			 + "%n  \"too big\" time increment:                                %ss,"
			 + "%n  \"too small\" time increment:                              %ss,"
			 + "%n  minimum number of runs for time increment not \"too-big\": %s,"
			 + "%n  RunParameters.CSV_FIELD_MODE:                            %s.",
			 m_bdMass.stripTrailingZeros().toString(),
			 BigDecimal.ONE.toString(), RunParameters.BD_START_RADIUS_RATIO_FRACTION.toString(),
			 toScientificFormat(dbStartSchwarzschildRadius, Integer.valueOf(N_LOG_PRECISION)),
			 m_bdTimeIncrementTooBigSeconds.stripTrailingZeros().toString(),
			 m_bdTimeIncrementTooSmallSeconds.stripTrailingZeros().toString(),
			 BlackHoleEvaporation.formatInteger(m_nMinRunsForTimeIncrementNotTooBig),
			RunParameters.CSV_FIELD_MODE.toString()));

			String sCSVHeader;
			switch (RunParameters.CSV_FIELD_MODE)
			{
				case ALL_FIELDS:
					sCSVHeader =
					 "Run number,Time in years,Time slow-down factor,Log time in years,Log time slow-down factor,Mass in kg";
					break;
				case LINEAR_FIELDS_ONLY:
					sCSVHeader = "Time in years,Time slow-down factor";
					break;
				case LOGARITHMIC_FIELDS_ONLY:
					sCSVHeader = "Log time in years,Log time slow-down factor";
					break;
				default:
					throw new RuntimeException(String.format("Invalid CSVFieldMode: %s.",
					 RunParameters.CSV_FIELD_MODE != null ? RunParameters.CSV_FIELD_MODE.toString() : "[null]"));
			}

			writeToFile(sCSVHeader.toString());
		}

		m_nRunForTimeIncrement = 0;

		BigDecimal bdLastTimeSlowDownFactor = null;
		BigDecimal bdLastPreviousMass = null;
		BigDecimal bd_LastDeltaM = null;

		while ((!m_bStopping) && (m_bdMass.compareTo(BigDecimal.ZERO) == 1)
		 && ((m_nMinRunsForTimeIncrementNotTooBig <= 0)
		     || ((m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0)
		         && (m_nRunForTimeIncrement < m_nMinRunsForTimeIncrementNotTooBig))))
		{
			m_nRun++;
			m_nRunForTimeIncrement++;

			if (m_nMinRunsForTimeIncrementNotTooBig <= 0)
				m_bdTimeIncrementSeconds = RunParameters.calculateTimeIncrementSeconds(m_nRun);

			BigDecimal dbSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);
			BigDecimal bdTimeSlowDownFactor =
			 BigDecimal.ONE.subtract(dbSchwarzschildRadius.divide(m_bdRadius, MC_MATH_CONTEXT));
			BigDecimal bd_dMdt = K.negate().divide(THREE.multiply(bdTimeSlowDownFactor).multiply(m_bdMass).multiply(m_bdMass),
			 MC_MATH_CONTEXT);
			BigDecimal bdPreviousMass = m_bdMass;
			BigDecimal bd_DeltaM = bd_dMdt.multiply(m_bdTimeIncrementSeconds);
			m_bdMass = m_bdMass.add(bd_DeltaM).stripTrailingZeros();
			m_bdTimeSeconds = m_bdTimeSeconds.add(m_bdTimeIncrementSeconds).stripTrailingZeros();
			m_rdResultData.setMass(m_bdMass);
			m_rdResultData.setTimeSeconds(m_bdTimeSeconds);

			bdLastTimeSlowDownFactor = bdTimeSlowDownFactor;
			bdLastPreviousMass = bdPreviousMass;
			bd_LastDeltaM = bd_DeltaM;

			if (shouldReportRun(m_nRun))
				reportRun(bdLastTimeSlowDownFactor, bdLastPreviousMass, bd_LastDeltaM);
		}

		if (!shouldReportRun(m_nRun))
			reportRun(bdLastTimeSlowDownFactor, bdLastPreviousMass, bd_LastDeltaM);
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
		 + "%n  bd.stripTrailingZeros().toString() = %s,"
		 + "%n  toScientificFormat(bd)             = %s.",
		 bd.stripTrailingZeros().toString(), toScientificFormat(bd)));
	}
	*/

	private String toScientificFormat(BigDecimal bd)
	{
		return toScientificFormat(bd, null);
	}

	private String toScientificFormat(BigDecimal bd, Integer iPrecision)
	{
		StringBuilder sbResult = new StringBuilder();
		bd = bd.stripTrailingZeros();
		int nPrecision = bd.precision();
		int nScale = bd.scale();
		int nExponent = nPrecision - nScale - 1;
		String sDigits = bd.unscaledValue().abs().toString();

		if (bd.signum() < 0)
			sbResult.append("-");

		int nPrecisionToUse = iPrecision != null ? Math.min(Math.max(iPrecision.intValue(), 1), nPrecision) : nPrecision;
		int nDigitsToExtract = Math.min(nPrecisionToUse, sDigits.length());
		sbResult.append(sDigits.substring(0, 1));

		if (nDigitsToExtract > 1)
		{
			sbResult.append(".");
			sbResult.append(sDigits.substring(1, nDigitsToExtract));
		}

		sbResult.append("E");

		if (nExponent > 0)
			sbResult.append("+");

		sbResult.append(nExponent);
		return sbResult.toString();
	}

	private boolean shouldReportRun(int nRun)
	{
 // return (nRun % N_LOG_SKIP_RATIO == 0) || (nRun <= 184) || (nRun == 100_000);
		return (nRun % N_LOG_SKIP_RATIO == 0) || (nRun <= 184) || (nRun == 1_000);
	}

	private void reportRun(BigDecimal bdTimeSlowDownFactor, BigDecimal bdPreviousMass, BigDecimal bd_deltaM)
	{
		final Integer I_LOG_PRECISION = Integer.valueOf(N_LOG_PRECISION);
		String sNRun = BlackHoleEvaporation.formatInteger(m_nRun);
		BigDecimal bdTimeYears = m_bdTimeSeconds.divide(BD_ONE_YEAR_IN_SECONDS, MC_MATH_CONTEXT).stripTrailingZeros();
		String sTimeSlowDownFactor = toScientificFormat(bdTimeSlowDownFactor, I_LOG_PRECISION);
		String sLogTimeSlowDownFactor = BigDecimal.valueOf(Math.log10(bdTimeSlowDownFactor.doubleValue()))
		 .round(MC_LOGGING).stripTrailingZeros().toString();
		String sMassInKg = toScientificFormat(m_bdMass);
 // s_logger.info(String.format("Worker.reportRun(...): %s, %s, %s", sNRun, sTimeSlowDownFactor, sMassInKg));
		String sTimeYears = toScientificFormat(bdTimeYears, I_LOG_PRECISION);
		String sLogTimeYears = BigDecimal.valueOf(Math.log10(bdTimeYears.doubleValue())).round(MC_LOGGING)
		 .stripTrailingZeros().toString();

		s_logger.info(String.format("Worker.reportRun(...):"
		 + "%n  m_nRun                   = %s,"
		 + "%n  m_bdTimeIncrementSeconds = %s,"
		 + "%n  bdPreviousMass           = %s,"
		 + "%n  bd_deltaM                = %s,"
		 + "%n  m_bdMass                 = %s,"
		 + "%n  sTimeYears               = %s,"
		 + "%n  bdTimeSlowDownFactor     = %s,"
		 + "%n  sLogTimeYears            = %s,"
		 + "%n  sLogTimeSlowDownFactor   = %s.",
		 sNRun,
		 toScientificFormat(m_bdTimeIncrementSeconds, I_LOG_PRECISION),
		 toScientificFormat(bdPreviousMass,           I_LOG_PRECISION),
		 toScientificFormat(bd_deltaM,                I_LOG_PRECISION),
		 toScientificFormat(m_bdMass,                 I_LOG_PRECISION),
		 sTimeYears,
		 toScientificFormat(bdTimeSlowDownFactor,     I_LOG_PRECISION),
		 sLogTimeYears,
		 sLogTimeSlowDownFactor));

		String sCSVLine;
		switch (RunParameters.CSV_FIELD_MODE)
		{
			case ALL_FIELDS:
				sCSVLine = String.format("%d,%s,%s,%s,%s,%s", m_nRun, sTimeYears, sTimeSlowDownFactor, sLogTimeYears,
				 sLogTimeSlowDownFactor, sMassInKg);
				break;
			case LINEAR_FIELDS_ONLY:
				sCSVLine = String.format("%s,%s", sTimeYears, sTimeSlowDownFactor);
				break;
			case LOGARITHMIC_FIELDS_ONLY:
				sCSVLine = String.format("%s,%s", sLogTimeYears, sLogTimeSlowDownFactor);
				break;
			default:
				throw new RuntimeException(String.format("Invalid CSVFieldMode: %s.",
				 RunParameters.CSV_FIELD_MODE != null ? RunParameters.CSV_FIELD_MODE.toString() : "[null]"));
		}

		writeToFile(sCSVLine);
	}
}
