package ianmarshall;

import java.math.BigDecimal;

public class WorkerResult
{
	public static class ResultData
	{
		private BigDecimal m_bdRadius = null;
		private BigDecimal m_bdTimeSeconds = null;
		private BigDecimal m_bdMass = null;
		private BigDecimal m_bdTimeIncrementTooBigSeconds = null;
		private BigDecimal m_bdTimeIncrementTooSmallSeconds = null;

		public ResultData(BigDecimal bdRadius, BigDecimal bdTimeSeconds, BigDecimal bdMass,
		 BigDecimal bdTimeIncrementTooBigSeconds, BigDecimal bdTimeIncrementTooSmallSeconds)
		{
			m_bdRadius = bdRadius;
			m_bdTimeSeconds = bdTimeSeconds;
			m_bdMass = bdMass;
			m_bdTimeIncrementTooBigSeconds = bdTimeIncrementTooBigSeconds;
			m_bdTimeIncrementTooSmallSeconds = bdTimeIncrementTooSmallSeconds;
		}

		public BigDecimal getRadius()
		{
			return m_bdRadius;
		}

		public BigDecimal getTimeSeconds()
		{
			return m_bdTimeSeconds;
		}

		public void setTimeSeconds(BigDecimal bdTimeSeconds)
		{
			m_bdTimeSeconds = bdTimeSeconds;
		}

		public BigDecimal getMass()
		{
			return m_bdMass;
		}

		public void setMass(BigDecimal bdMass)
		{
			m_bdMass = bdMass;
		}

		public BigDecimal getTimeIncrementTooBigSeconds() {
			return m_bdTimeIncrementTooBigSeconds;
		}

		public void setTimeIncrementTooBigSeconds(BigDecimal bdTimeIncrementTooBigSeconds) {
			m_bdTimeIncrementTooBigSeconds = bdTimeIncrementTooBigSeconds;
		}

		public BigDecimal getTimeIncrementTooSmallSeconds() {
			return m_bdTimeIncrementTooSmallSeconds;
		}

		public void setTimeIncrementTooSmallSeconds(BigDecimal bdTimeIncrementTooSmallSeconds) {
			m_bdTimeIncrementTooSmallSeconds = bdTimeIncrementTooSmallSeconds;
		}
	}

	private boolean m_bProcessingCompleted = false;
	private Throwable m_thThrowable = null;
	private int m_nRun = 0;
	private ResultData m_rdResultData = null;

	public WorkerResult(boolean bProcessingCompleted, Throwable thThrowable, int nRun, ResultData rdResultData)
	{
		m_bProcessingCompleted = bProcessingCompleted;
		m_thThrowable = thThrowable;
		m_nRun = nRun;
		m_rdResultData = rdResultData;
	}

	public boolean getProcessingCompleted()
	{
		return m_bProcessingCompleted;
	}

	public Throwable getThrowable()
	{
		return m_thThrowable;
	}

	public int getRun()
	{
		return m_nRun;
	}

	public ResultData getResultData()
	{
		return m_rdResultData;
	}
}
