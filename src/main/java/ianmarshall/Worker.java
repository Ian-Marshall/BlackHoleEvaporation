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
	private static final BigDecimal BD_START_RADIUS_RATIO = BigDecimal.ONE.add(BD_START_RADIUS_RATIO_FRACTION);
	private static final BigDecimal BD_ONE_YEAR_IN_SECONDS = new BigDecimal("3.1536e7");
	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
	private static final int N_LOG_SKIP_RATIO = 1_000_000;
	private static final File S_FILE_OUTPUT = new File("logs\\Output.csv");
	private static final boolean B_MODE_HOMING_IN_ON_TIME_INCREMENT = true;

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

		do
		{
			execute();

			if ((!m_bStopping) && B_MODE_HOMING_IN_ON_TIME_INCREMENT && (m_nMinRunsForTimeIncrementNotTooBig > 0)
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
		while ((!m_bStopping) && (m_bdMass.compareTo(BigDecimal.ZERO) == 1)
		 && ((!B_MODE_HOMING_IN_ON_TIME_INCREMENT)
		     || ((m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0))));

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
		else if (B_MODE_HOMING_IN_ON_TIME_INCREMENT
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

		m_bdTimeIncrement = m_bdTimeIncrementTooBigSeconds.add(m_bdTimeIncrementTooSmallSeconds)
		 .divide(BigDecimal.TWO, MC_MATH_CONTEXT).stripTrailingZeros();

		s_logger.info(String.format("m_bdTimeIncrementTooBigSeconds = %ss, m_bdTimeIncrementTooSmallSeconds = %ss,"
		 + " m_bdTimeIncrement = %ss.",
		 m_bdTimeIncrementTooBigSeconds.toString(),
		 m_bdTimeIncrementTooSmallSeconds.toString(),
		 m_bdTimeIncrement.stripTrailingZeros().toString()));

		if (m_nRun == 0)
		{
			s_logger.info(String.format("Universal mass: %skg, start radius ratio: %s + %s,"
			 + " homing-in-on-time-increment mode: %b, \"too big\" time increment: %ss, \"too small\" time increment: %ss,"
			 + " minimum number of runs for not-\"too-big\"-a-time increment: %s.",
			 m_bdMass.stripTrailingZeros().toString(),
			 BigDecimal.ONE.toString(),
			 BD_START_RADIUS_RATIO_FRACTION.toString(),
			 B_MODE_HOMING_IN_ON_TIME_INCREMENT,
			 m_bdTimeIncrementTooBigSeconds.stripTrailingZeros().toString(),
			 m_bdTimeIncrementTooSmallSeconds.stripTrailingZeros().toString(),
			 BlackHoleEvaporation.formatInteger(m_nMinRunsForTimeIncrementNotTooBig)));

			String sHeader = "Run number, time in years, time slow-down factor, mass in kg";
			s_logger.info(sHeader);
			writeToFile(sHeader);
		}

		m_nRun = 0;
		m_nRunForTimeIncrement = 0;

		while ((!m_bStopping) && (m_bdMass.compareTo(BigDecimal.ZERO) == 1)
		 && ((!B_MODE_HOMING_IN_ON_TIME_INCREMENT)
		     || ((m_bdTimeIncrementTooBigSeconds.subtract(m_bdTimeIncrementTooSmallSeconds).compareTo(BigDecimal.ONE) >= 0)
		         && ((m_nMinRunsForTimeIncrementNotTooBig <= 0)
						     || (m_nRunForTimeIncrement < m_nMinRunsForTimeIncrementNotTooBig)))))
		{
			m_nRun++;
			m_nRunForTimeIncrement++;
			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(m_bdMass).divide(c2, MC_MATH_CONTEXT);
			BigDecimal bdTimeSlowDownFactor =
			 BigDecimal.ONE.subtract(dbStartSchwarzschildRadius.divide(m_bdRadius, MC_MATH_CONTEXT));
			BigDecimal bd_dMdt = K.negate().divide(THREE.multiply(bdTimeSlowDownFactor).multiply(m_bdMass).multiply(m_bdMass),
			 MC_MATH_CONTEXT);
			m_bdMass = m_bdMass.add(bd_dMdt.multiply(m_bdTimeIncrement)).stripTrailingZeros();
			m_bdTime = m_bdTime.add(m_bdTimeIncrement).stripTrailingZeros();
			m_rdResultData.setMass(m_bdMass);
			m_rdResultData.setTime(m_bdTime);

			if (m_nRun % N_LOG_SKIP_RATIO == 0)
			{
				final int N_MINIMUM_PRECISION = 5;
				final int N_MINIMUM_LENGTH_TIME_IN_YEARS = 10;

				String sNRun = BlackHoleEvaporation.formatInteger(m_nRun);
				int nSpacesToPrefix = N_MINIMUM_LENGTH_TIME_IN_YEARS - sNRun.length();
				if (nSpacesToPrefix > 0)
					sNRun = " ".repeat(nSpacesToPrefix) + sNRun;

				BigDecimal bdTimeInYears = m_bdTime.divide(BD_ONE_YEAR_IN_SECONDS, MC_MATH_CONTEXT).stripTrailingZeros();
				int nZeroesToAdd = N_MINIMUM_PRECISION - bdTimeInYears.precision();
				if (nZeroesToAdd > 0)
					bdTimeInYears = bdTimeInYears.setScale(bdTimeInYears.scale() + nZeroesToAdd);

				String sTimeInYears = bdTimeInYears.toString();
				String sTimeSlowDownFactor = bdTimeSlowDownFactor.toString();
				String sMassInKg = m_bdMass.toString();

				String sLogLine = String.format("%s,%s,%s,%s", sNRun, sTimeInYears, sTimeSlowDownFactor, sMassInKg);
				s_logger.info(sLogLine);

				String sCSVLine = String.format("%d,%s,%s,%s", m_nRun, sTimeInYears, sTimeSlowDownFactor, sMassInKg);
				writeToFile(sCSVLine);
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
}
