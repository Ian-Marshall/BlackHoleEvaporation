package ianmarshall;

import java.lang.Thread.UncaughtExceptionHandler;

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
		}

	}

//private static final double DBL_LARGE_DIVISION_RESULT = 1.0e12;
	private static final double h = 6.626_070_15e-34;    // The Planck constant in Js
	private static final double c = 299_792_458;    // The speed of light in ms^-1
	private static final double c2 = c * c;
	private static final double G = 6.674_30e-11;        // The gravitational constant in m^3kg^-1s^-2
	private static final double pi = Math.PI;
	private static final double K = (h * c2 * c2) / (10240 * pi * pi * G * G);

	// The mass of the Universe at the Big Bang in kg (taken to be the same as its current mass)
	private static final double DBL_UNIVERSAL_MASS = 1.5e53;

	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
	private static final int N_LOG_SKIP_RATIO = 1_000_000;

	private static int m_nProcessors = 0;

	private StartParameters m_spStartParameters = null;
	private int m_nRun = 0;
	private ResultData m_rdResultData = null;
//private boolean m_bFirstRun = true;    // This will also be true when resuming running after a pause
	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
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

		m_spStartParameters = spStartParameters;
		m_nRun = nRun;
		m_rdResultData = rdResultData;
		m_wuehExceptionHandler = new WorkerUncaughtExceptionHandler();
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
		double dblStartRadiusRatio = m_spStartParameters.getStartRadiusRatio();
		long loTimeIncrementSeconds = m_spStartParameters.getTimeIncrementSeconds();

		long loTime;
		double dblMass;
		double dblRadius;

		if (m_rdResultData != null)
		{
			loTime = m_rdResultData.getTime();
			dblMass = m_rdResultData.getMass();
			dblRadius = m_rdResultData.getRadius();
		}
		else
		{
			loTime = 0L;
			dblMass = DBL_UNIVERSAL_MASS;
			dblRadius = dblStartRadiusRatio * (2.0 * G * dblMass) / c2;
			m_rdResultData = new ResultData(dblRadius, loTime, dblMass);
		}

		if (m_nRun == 0)
			s_logger.info("Run number, time, mass, time speed-up factor");

		while ((!m_bStopping) && (dblMass > 0.0))
		{
			m_nRun++;
			double dblTimeSpeedUpFactor = 1.0 / (1.0 - ((2 * G * dblMass) / (c2 * dblRadius)));
			double dMdt = -K * dblTimeSpeedUpFactor / (3.0 * dblMass * dblMass);
			dblMass += dMdt * loTimeIncrementSeconds;
			loTime += loTimeIncrementSeconds;
			m_rdResultData.setMass(dblMass);
			m_rdResultData.setTime(loTime);

			if (m_nRun % N_LOG_SKIP_RATIO == 0)
				s_logger.info(String.format("%s,%s,%s,%s",
				 BlackHoleEvaporation.formatInteger(m_nRun),
				 BlackHoleEvaporation.formatLong(loTime),
				 BlackHoleEvaporation.formatDouble(dblMass),
				 BlackHoleEvaporation.formatDouble(dblTimeSpeedUpFactor)));
		}

		if (dblMass <= 0.0)
		{
			m_bProcessingCompleted = true;
			s_logger.info("All processing has been completed.");
		}
		else
			s_logger.info("Stopped before all processing completed.");

		m_WorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_rdResultData);
		m_bStopped = true;
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
