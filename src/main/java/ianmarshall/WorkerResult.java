package ianmarshall;

public class WorkerResult
{
	public static class ResultData
	{
		private double m_dblRadius = 0.0;
		private long m_loTime = 0L;
		private double m_dblMass = 0.0;

		public ResultData(double dblRadius, long loTime, double dblMass)
		{
			m_dblRadius = dblRadius;
			m_loTime = loTime;
			m_dblMass = dblMass;
		}

		public double getRadius()
		{
			return m_dblRadius;
		}

 // public void setRadius(double dblRadius)
 // {
 // 	m_dblRadius = dblRadius;
 // }

		public long getTime()
		{
			return m_loTime;
		}

		public void setTime(long loTime)
		{
			m_loTime = loTime;
		}

		public double getMass()
		{
			return m_dblMass;
		}

		public void setMass(double dblMass)
		{
			m_dblMass = dblMass;
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
