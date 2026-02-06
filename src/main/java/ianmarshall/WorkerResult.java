package ianmarshall;

import java.math.BigDecimal;

public class WorkerResult
{
	public static class ResultData
	{
		private BigDecimal m_bdRadius = null;
		private BigDecimal m_bdTime = null;
		private BigDecimal m_bdMass = null;

		public ResultData(BigDecimal bdRadius, BigDecimal bdTime, BigDecimal bdMass)
		{
			m_bdRadius = bdRadius;
			m_bdTime = bdTime;
			m_bdMass = bdMass;
		}

		public BigDecimal getRadius()
		{
			return m_bdRadius;
		}

		public BigDecimal getTime()
		{
			return m_bdTime;
		}

		public void setTime(BigDecimal bdTime)
		{
			m_bdTime = bdTime;
		}

		public BigDecimal getMass()
		{
			return m_bdMass;
		}

		public void setMass(BigDecimal bdMass)
		{
			m_bdMass = bdMass;
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
