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
			m_WorkerResult = new WorkerResult(m_bProcessingCompleted, th, m_nRun, m_rdResultData);
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
//private static final BigDecimal BD_UNIVERSAL_MASS = new BigDecimal("1.5e13");    // Test, reducd mass

	/*
	private static final BigDecimal BD_START_RADIUS_RATIO =
	//                 0         1        2         3         4         5         6         7         8         9
	//                 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
	 new BigDecimal("1.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001");
	*/
	private static final BigDecimal BD_START_RADIUS_RATIO = BigDecimal.ONE.add(new BigDecimal("1e-90"));

	private static final BigDecimal BD_TIME_ONE_YEAR_IN_SECONDS = new BigDecimal("3.1536e7");

	// 1^12 years
	private static final BigDecimal BD_TIME_INCREMENT_SECONDS = new BigDecimal("1e12")
	 .multiply(BD_TIME_ONE_YEAR_IN_SECONDS).stripTrailingZeros();

	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
	private static final int N_LOG_SKIP_RATIO = 1_000_000;
	private static final File S_FILE_OUTPUT = new File("logs\\Output.csv");

	private static int m_nProcessors = 0;

//private StartParameters m_spStartParameters = null;
	private int m_nRun = 0;
	private ResultData m_rdResultData = null;
	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
	private BufferedWriter m_bwBufferedWriter = null;
	private WorkerResult m_WorkerResult = null;

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

 // m_spStartParameters = spStartParameters;
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
		return m_WorkerResult;
	}

	@Override
	public void run()
	{
		m_bStopping = false;
		m_bStopped = false;

		BigDecimal bdTime;
		BigDecimal bdMass;
		BigDecimal bdRadius;

		if (m_rdResultData != null)
		{
			bdTime = m_rdResultData.getTime();
			bdMass = m_rdResultData.getMass();
			bdRadius = m_rdResultData.getRadius();
		}
		else
		{
			bdTime = BigDecimal.ZERO;
			bdMass = BD_UNIVERSAL_MASS;
			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(bdMass).divide(c2, MC_MATH_CONTEXT);
			bdRadius = dbStartSchwarzschildRadius.multiply(BD_START_RADIUS_RATIO).stripTrailingZeros();
			m_rdResultData = new ResultData(bdRadius, bdTime, bdMass);
		}

		if (m_nRun == 0)
		{
			String sHeader = "Run number, time, time slow-down factor, mass";
			s_logger.info(sHeader);
			writeToFile(sHeader);
		}

		while ((!m_bStopping) && (bdMass.compareTo(BigDecimal.ZERO) == 1))
		{
			m_nRun++;

			BigDecimal dbStartSchwarzschildRadius = BigDecimal.TWO.multiply(G).multiply(bdMass).divide(c2, MC_MATH_CONTEXT);
			BigDecimal bdTimeSlowDownFactor =
			 BigDecimal.ONE.subtract(dbStartSchwarzschildRadius.divide(bdRadius, MC_MATH_CONTEXT));
			BigDecimal bd_dMdt = K.negate().divide(THREE.multiply(bdTimeSlowDownFactor).multiply(bdMass).multiply(bdMass),
			 MC_MATH_CONTEXT);
			bdMass = bdMass.add(bd_dMdt.multiply(BD_TIME_INCREMENT_SECONDS)).stripTrailingZeros();
			bdTime = bdTime.add(BD_TIME_INCREMENT_SECONDS).stripTrailingZeros();
			m_rdResultData.setMass(bdMass);
			m_rdResultData.setTime(bdTime);

			if (m_nRun % N_LOG_SKIP_RATIO == 0)
			{
				String sTime = bdTime.toString();
				String sTimeSlowDownFactor = bdTimeSlowDownFactor.toString();
				String sMass = bdMass.toString();


				String sLogLine = String.format("%s,%s,%s,%s", BlackHoleEvaporation.formatInteger(m_nRun), sTime,
				 sTimeSlowDownFactor, sMass);
				s_logger.info(sLogLine);

				String sCSVLine = String.format("%d,%s,%s,%s", m_nRun, sTime, sTimeSlowDownFactor, sMass);
				writeToFile(sCSVLine);
			}
		}

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

		if (bdMass.compareTo(BigDecimal.ZERO) <= 0)
		{
			m_bProcessingCompleted = true;
			s_logger.info("All processing has been completed.");
		}
		else
			s_logger.info("Stopped before all processing completed.");

		m_WorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_rdResultData);
		m_bStopped = true;
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
			e.printStackTrace();
		}
	}

	/*
	 * Divide two numbers safely without raising an exception or returning NaN or infinity.
	 * @param dblDividend
	 *   The number to be divided.
	 * @param dblDivisor
	 *   The number to divide by.
	 * @return
	 *   If the dividend is zero then (if the divisor is also zero then <code>1.0</code> else <code>0.0</code>)
	 *   else if the divisor is zero then the signum of the dividend multiplied by <code>DBL_LARGE_DIVISION_RESULT</code>
	 *   otherwise the result of the division.
	 */
	/*
	private double safeDivide(double dblDividend, double dblDivisor)
	{
 // final double DBL_LARGE_DIVISION_RESULT = 1.0e12;
		double dblResult;

		if (dblDividend == 0.0)
			if (dblDivisor == 0.0)
				dblResult = 1.0;
			else
				dblResult = 0.0;
		else if (dblDivisor == 0.0)
			dblResult = Math.signum(dblDividend) * DBL_LARGE_DIVISION_RESULT;
		else
			dblResult = dblDividend / dblDivisor;

		return dblResult;
	}
	*/
}
