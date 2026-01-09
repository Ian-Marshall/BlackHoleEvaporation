package ianmarshall;

import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * This class represents an element of the metric or fundamental tensor at a point in space-time.
 * For the Big Bang approximation, this point in space-time is given by the radius and time.
 */
public class MetricComponents
{
	public enum MetricPosition
	{
		R, T
	}

	public enum MetricComponent
	{
		A, B, C, D
	}

	private double m_R = 0.0;    // The radius co-ordinate of the metric
	private double m_T = 0.0;    // The time co-ordinate of the metric
	private double m_A = 0.0;    // }
	private double m_B = 0.0;    // } The component values
	private double m_C = 0.0;    // } of the metric
	private double m_D = 0.0;    // }

	public MetricComponents(double r, double t, double a, double b, double c, double d)
	{
		m_R = r;
		m_T = t;
		m_A = a;
		m_B = b;
		m_C = c;
		m_D = d;
	}

	public double getR()
	{
		return m_R;
	}

	public void setR(double r)
	{
		m_R = r;
	}

	public double getT()
	{
		return m_T;
	}

	public void setT(double t)
	{
		m_T = t;
	}

	public double getA()
	{
		return m_A;
	}

	public void setA(double a)
	{
		m_A = a;
	}

	public double getB()
	{
		return m_B;
	}

	public void setB(double b)
	{
		m_B = b;
	}

	public double getC()
	{
		return m_C;
	}

	public void setC(double c)
	{
		m_C = c;
	}

	public double getD()
	{
		return m_D;
	}

	public void setD(double d)
	{
		m_D = d;
	}

	public SimpleImmutableEntry<Double, Double> getComponent(MetricPosition mp, MetricComponent mc)
	{
		double dblPosition;
		switch (mp)
		{
			case R:
				dblPosition = getR();
				break;
			case T:
				dblPosition = getT();
				break;
			default:
				String sMP = mp != null ? mp.toString() : "[null]";
				throw new IllegalArgumentException(String.format("Metric position \"%s\" not found.", sMP));
		}

		double dblComponent;
		switch (mc)
		{
			case A:
				dblComponent = getA();
				break;
			case B:
				dblComponent = getB();
				break;
			case C:
				dblComponent = getC();
				break;
			case D:
				dblComponent = getD();
				break;
			default:
				String sMC = mc != null ? mc.toString() : "[null]";
				throw new IllegalArgumentException(String.format("Metric component \"%s\" not found.", sMC));
		}

		return new SimpleImmutableEntry<>(Double.valueOf(dblPosition), Double.valueOf(dblComponent));
	}

	public void setComponent(MetricComponent mc, double dbl)
	{
		switch (mc)
		{
			case A:
				setA(dbl);
				break;
			case B:
				setB(dbl);
				break;
			case C:
				setC(dbl);
				break;
			case D:
				setD(dbl);
				break;
			default:
				throw new IllegalArgumentException(String.format("Metric component \"%s\" not found.", mc.toString()));
		}
	}

	public MetricComponents copy()
	{
		return new MetricComponents(getR(), getT(), getA(), getB(), getC(), getD());
	}

	/*
	 * Make a deep copy of a list of <code>MetricComponents</code>.
	 * @param liG
	 *   The list of <code>MetricComponents</code> to be copied.
	 *   If this is <code>null</code> then an empty list will be returned.
	 * @return
	 *   A deep copy of the list supplied.
	 */
	/*
	public static List<MetricComponents> deepCopyMetricComponents(List<MetricComponents> liG)
	{
		int nSize = liG != null ? liG.size() : 0;
		List<MetricComponents> liResult = new ArrayList<>(nSize);

		if (nSize > 0)
			for (MetricComponents mc: liG)
			{
				MetricComponents mcCopy = mc.copy();
				liResult.add(mcCopy);
			}

		return liResult;
	}
	*/
}
