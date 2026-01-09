package ianmarshall;

import java.util.EnumMap;

import static ianmarshall.MetricAndDerivatives.DerivativeLevel.None;

public class MetricAndDerivatives
{
	/**
	 * The ordering of these elements matters.
	 * The <code>FirstRadiusFirstTime</code> derivative must be placed after the relevant pure first derivative.
	 * (See the code to find out this relevant pure first derivative.)
	 */
	public enum DerivativeLevel
	{
		None, FirstRadius, FirstTime, SecondRadius, SecondTime, FirstRadiusFirstTime;
	}

	/**
	 * These are the Ricci tensor components which can be non-zero.
	 * R33 is excluded since this is simply R22 * ((sin theta)^2).
	 */
	public enum RicciTensor
	{
		R00, R01, R11, R22
	}

	private final int m_nRadiusElements;
	private final int m_nTimeElements;
	private final EnumMap<DerivativeLevel, Metric> m_mapMetricByDerivativeLevel;

	/**
	 * Build an initialised metric and its derivatives from the supplied radius and time values.
	 * "Initialised" here means that the metric tensor components are set to zero.
	 * @param adblRadii
	 *   The array of radius values to be used for the metric tensor components.
	 * @param adblTimes
	 *   The array of time values to be used for the metric tensor components.
	 * @return
	 *   A <code>MetricAndDerivatives</code> object holding the initialised metric and its derivatives.
	 */
	public MetricAndDerivatives(Double[] adblRadii, Double[] adblTimes)
	{
		this(adblRadii, adblTimes, true);
	}

	/**
	 * Build a metric and its derivatives from the supplied radius and time values.
	 * If specified then initialise the metrics and its derivatives.
	 * "Initialised" here means that the metric tensor components are set to zero.
	 * @param adblRadii
	 *   The array of radius values to be used for the metric tensor components.
	 * @param adblTimes
	 *   The array of time values to be used for the metric tensor components.
	 * @param bInitialise
	 *   If <code>true</code> then initialise the <code>Metric</code> tensor components.
	 * @return
	 *   A <code>MetricAndDerivatives</code> object holding the initialised metric and its derivatives.
	 */
	private MetricAndDerivatives(Double[] adblRadii, Double[] adblTimes, boolean bInitialise)
	{
		m_nRadiusElements = adblRadii.length;
		m_nTimeElements = adblTimes.length;
		m_mapMetricByDerivativeLevel = new EnumMap<>(DerivativeLevel.class);

		if (bInitialise)
		{
			Metric metricNonDerivative = new Metric(adblRadii, adblTimes);
			m_mapMetricByDerivativeLevel.put(None, metricNonDerivative);

			for (DerivativeLevel level: DerivativeLevel.values())
				if (level != None)
				{
					Metric metric = metricNonDerivative.copy();
					m_mapMetricByDerivativeLevel.put(level, metric);
				}
		}
	}

	public int getNRadiusElements()
	{
		return m_nRadiusElements;
	}

	public int getNTimeElements()
	{
		return m_nTimeElements;
	}

	public Metric getMetric(DerivativeLevel level)
	{
		return m_mapMetricByDerivativeLevel.get(level);
	}

	public void setMetric(DerivativeLevel level, Metric metric)
	{
		m_mapMetricByDerivativeLevel.put(level, metric);
	}

	public MetricAndDerivatives copy()
	{
		Double[] adblRadii = new Double[m_nRadiusElements];
		Double[] adblTimes = new Double[m_nTimeElements];
		Metric metric = getMetric(None);

		for (int r = 0; r < m_nRadiusElements; r++)
		{
			MetricComponents mc = metric.getMetricComponents(r, 0);
			adblRadii[r] = Double.valueOf(mc.getR());
		}

		for (int t = 0; t < m_nTimeElements; t++)
		{
			MetricComponents mc = metric.getMetricComponents(0, t);
			adblTimes[t] = Double.valueOf(mc.getT());
		}

		// We do not initialise the copy since we shall use our own <code>Metric</code> objects instead
		MetricAndDerivatives madCopy = new MetricAndDerivatives(adblRadii, adblTimes, false);

		for (DerivativeLevel level: DerivativeLevel.values())
		{
			metric = getMetric(level);
			madCopy.setMetric(level, metric.copy());
		}

		return madCopy;
	}
}
