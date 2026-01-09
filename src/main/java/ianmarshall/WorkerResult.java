package ianmarshall;

public class WorkerResult
{
	private boolean m_bProcessingCompleted = false;
	private Throwable m_thThrowable = null;
	private int m_nRun = 0;
	private MetricAndDerivatives m_madG = null;

	public WorkerResult(boolean bProcessingCompleted, Throwable thThrowable, int nRun, MetricAndDerivatives madG)
	{
		m_bProcessingCompleted = bProcessingCompleted;
		m_thThrowable = thThrowable;
		m_nRun = nRun;
		m_madG = madG.copy();
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

	public MetricAndDerivatives getMetricAndDerivatives()
	{
		return m_madG;
	}
}
