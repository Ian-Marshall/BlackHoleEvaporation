package ianmarshall;

import cern.colt.matrix.DoubleMatrix2D;

import ianmarshall.MetricComponents.MetricComponent;
import ianmarshall.MetricComponents.MetricPosition;

import static ianmarshall.MetricAndDerivatives.DerivativeLevel.None;
import static ianmarshall.MetricComponents.MetricComponent.A;
import static ianmarshall.MetricComponents.MetricPosition.R;
import static ianmarshall.MetricComponents.MetricPosition.T;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.IntStream;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In order to apply the simulated annealing method to a specific problem, one must specify the following parameters:
 *   -  the state space:                     s
 *   -  the energy (goal) function:          E(s)
 *   -  the candidate generator procedure:   neighbour(s)
 *   -  the acceptance probability function: P(E, Enew, T)
 *   -  the annealing schedule:              temperature(iteration №).
 */
public class SimulatedAnnealing
{
	private static final double DBL_EQUALITY_TOLERANCE = 1.0e-12;
	private static final Logger m_logger = LoggerFactory.getLogger(SimulatedAnnealing.class);
	private static final Random m_random = new Random();    // Remove "static" for multi-instance use
	private static final MetricPosition[] m_ampMetricPositions = MetricPosition.values();
	private static final MetricComponent[] m_amcMetricComponents = MetricComponent.values();

	private static int m_nProcessors = 0;

	private double m_dblNeighbourPeakScalingFactor = 0.0;
	private double m_dblAcceptanceProbabilityScalingFactor = 0.0;
	private double m_dblTemperatureScalingFactor = 0.0;
	private double m_dblTemperatureDivisor = 0.0;
//private String m_sLogMessage = null;    // Refactor this for multi-instance use

	public SimulatedAnnealing(StartParameters spStartParameters)
	{
		if (m_nProcessors == 0)
			m_nProcessors = Runtime.getRuntime().availableProcessors();

		m_dblNeighbourPeakScalingFactor = spStartParameters.getNeighbourPeakScalingFactor();
		m_dblAcceptanceProbabilityScalingFactor = spStartParameters.getAcceptanceProbabilityScalingFactor();
		m_dblTemperatureScalingFactor = spStartParameters.getTemperatureScalingFactor();
		m_dblTemperatureDivisor = spStartParameters.getTemperatureDivisor();
	}

	/**
	 * The energy (goal) function.
	 * <br/>
	 * Calculate the energy of the state space, which is represented by the supplied metric tensor components
	 * and their derivatives.
	 * @param madG
	 *   The metric tensor components and their derivatives.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means that no run has yet been executed.
	 * @return
	 *   The simulated annealing energy of the entire space-time.
	 */
	public double energy(MetricAndDerivatives madG, int nRun)
	{
		int nRadiusElements = madG.getNRadiusElements();
		int nTimeElements   = madG.getNTimeElements();
		int nChunkSize = 1 + ((nRadiusElements - 1) / m_nProcessors);

		double dblSumOfSquaresOfRicciTensorsOverAllRAndT = IntStream.range(0, m_nProcessors).parallel().mapToDouble(
		 new IntToDoubleFunction()
		{
			@Override
			public double applyAsDouble(int nChunk)
			{
				double dblSum = 0.0;
				int nRIndexStart = nChunk * nChunkSize;
				int nRIndexFinish = Math.min(nRIndexStart + nChunkSize - 1, nRadiusElements - 1);

				for (int nRIndex = nRIndexStart; nRIndex <= nRIndexFinish; nRIndex++)
					dblSum += calculateContributionForRadius(madG, nRun, nRadiusElements, nTimeElements, nRIndex);

				return dblSum;
			}
		}
		 ).sum();

		return dblSumOfSquaresOfRicciTensorsOverAllRAndT;
	}

	/**
	 * Calculate the energy of the state space, which is represented by the supplied metric tensor components
	 * and their derivatives.
	 * @param madG
	 *   The metric tensor components and their derivatives.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means that no run has yet been executed.
	 * @param nRadiusElements
	 *   The number of radius elements.
	 * @param nTimeElements
	 *   The number of time elements.
	 * @param nRIndex
	 *   The radius index to process.
	 * @return
	 *   The contribution to the simulated annealing energy of the space-time over all time and the given radius index.
	 */
	private double calculateContributionForRadius(MetricAndDerivatives madG, int nRun, int nRadiusElements,
	 int nTimeElements, int nRIndex)
	{
		double dblSumOfSquaresOfRicciTensorsOverAllT = 0.0;

		final int nRIndexStart;
		final int nRIndexFinish;
		if (nRIndex == 0)
		{
			nRIndexStart = nRIndex;        // Forward difference for the first point
			nRIndexFinish = nRIndexStart + 1;
		}
		else if (nRIndex < nRadiusElements - 1)
		{
			nRIndexStart = nRIndex - 1;    // Central difference for an internal point
			nRIndexFinish = nRIndexStart + 2;
		}
		else
		{
			nRIndexStart = nRIndex - 1;    // Backward difference for the last point
			nRIndexFinish = nRIndexStart + 1;
		}

		for (int nTIndex = 0; nTIndex < nTimeElements; nTIndex++)
		{
			DoubleMatrix2D dmRicci = Worker.calculateRicciTensorValues(madG, nRIndex, nTIndex);    // 4 rows by 1 column

	 // boolean bLog = (nRun <= 3) && ((i == 0) || (i == 3));
			boolean bLog = false;

			if (bLog)
			{
				String sMsg = String.format("%n  nRun = %d, nRIndex = %d, nTIndex = %d: dmRicci has elements:%n%s .%n",
				nRun, nRIndex, nTIndex, dmRicci.toString());
				m_logger.info(sMsg);
			}

			double dblSumOfSquaresOfRicciTensors = 0.0;

			for (int j = 0; j < dmRicci.rows(); j++)
			{
				double dblRicciTensor = dmRicci.get(j, 0);
				dblSumOfSquaresOfRicciTensors += dblRicciTensor * dblRicciTensor;
			}

			final int nTIndexStart;
			final int nTIndexFinish;
			if (nTIndex == 0)
			{
				nTIndexStart = nTIndex;        // Forward difference for the first point
				nTIndexFinish = nTIndexStart + 1;
			}
			else if (nTIndex < nTimeElements - 1)
			{
				nTIndexStart = nTIndex - 1;    // Central difference for an internal point
				nTIndexFinish = nTIndexStart + 2;
			}
			else
			{
				nTIndexStart = nTIndex - 1;    // Backward difference for the last point
				nTIndexFinish = nTIndexStart + 1;
			}

			double dblRStart  = Worker.getMetricComponent(madG, None, nRIndexStart, nTIndexStart,   R, A).getKey()
			 .doubleValue();
			double dblTStart  = Worker.getMetricComponent(madG, None, nRIndexStart, nTIndexStart,   T, A).getKey()
			 .doubleValue();
			double dblRFinish = Worker.getMetricComponent(madG, None, nRIndexFinish, nTIndexFinish, R, A).getKey()
			 .doubleValue();
			double dblTFinish = Worker.getMetricComponent(madG, None, nRIndexFinish, nTIndexFinish, T, A).getKey()
			 .doubleValue();

			double dblDivisorFactor;
			if (((nRIndexFinish - nRIndexStart) == 2) && ((nTIndexFinish - nTIndexStart) == 2))
				dblDivisorFactor = 0.25;    // This is, by far, the most likely case
			else if (((nRIndexFinish - nRIndexStart) == 2) || ((nTIndexFinish - nTIndexStart) == 2))
				dblDivisorFactor = 0.5;
			else
				dblDivisorFactor = 1.0;

			// Add to the all-squares total with an appropriate weighting
			dblSumOfSquaresOfRicciTensorsOverAllT += dblSumOfSquaresOfRicciTensors
			 * (dblRFinish - dblRStart) * (dblTFinish - dblTStart) * dblDivisorFactor;
		}

		return dblSumOfSquaresOfRicciTensorsOverAllT;
	}

	/**
	 * The candidate generator procedure.
	 * @param madG
	 *   The metric tensor components and their derivatives.
	 * @return
	 *   The metric tensor components of the candidate for each value of radius and time.
	 *   The derivatives have been initialised but not calculated.
	 */
	public MetricAndDerivatives neighbour(MetricAndDerivatives madG)
	{
		MetricAndDerivatives madResult = madG.copy();
		MetricPosition mp = randomMetricPosition();
		MetricComponent mc = randomMetricComponent();

		int nSizeVarying = 0;
		int nSizeFixed = 0;
		switch (mp)
		{
			case R:
				nSizeVarying = madResult.getNRadiusElements();
				nSizeFixed = madResult.getNTimeElements();
				break;
			case T:
				nSizeVarying = madResult.getNTimeElements();
				nSizeFixed = madResult.getNRadiusElements();
				break;
		}

		int nIndexCentre = m_random.nextInt(nSizeVarying);
		int nIndexFixed = m_random.nextInt(nSizeFixed);
		double dblStandardDeviationMax = nSizeVarying / 10.0;
		double dblStandardDeviation = m_random.nextDouble() * dblStandardDeviationMax;

		// Equally likely between -m_dblNeighbourPeakScalingFactor and +m_dblNeighbourPeakScalingFactor inclusive
		double dblDeltaPeak = m_dblNeighbourPeakScalingFactor * m_random.nextDouble(-1.0, 1.0);

 // m_sLogMessage = String.format("SimulatedAnnealing.neighbour(...):"
 //  + "%n  nIndexCentre         = %d,"
 //  + "%n  dblStandardDeviation = %f,"
 //  + "%n  dblDeltaPeak         = %f.",
 //  nIndexCentre, dblStandardDeviation, dblDeltaPeak);

		for (int i = 0; i < nSizeVarying; i++)
		{
			int nRIndex;
			int nTIndex;
			switch (mp)
			{
				case R:
					nRIndex = i;
					nTIndex = nIndexFixed;
					break;
				case T:
					nRIndex = nIndexFixed;
					nTIndex = i;
					break;
				default:
					throw new RuntimeException(String.format("Invalid metric position \"%s\".", mp.toString()));
			}

			double dblExponent = (i - nIndexCentre) / dblStandardDeviation;
			double dblDelta = dblDeltaPeak * Math.exp(-dblExponent * dblExponent);

			if (Math.abs(dblDelta) >= DBL_EQUALITY_TOLERANCE)
			{
				MetricComponents mcMetricComponents = Worker.getMetricComponents(madResult, None, nRIndex, nTIndex);
				SimpleImmutableEntry<Double, Double> entry = mcMetricComponents.getComponent(mp, mc);
				double dblValue = entry.getValue().doubleValue();
				mcMetricComponents.setComponent(mc, dblValue + dblDelta);
			}
			else if (i > nIndexCentre)
				break;    // There are no more significant changes to make
		}

		return madResult;
	}

	/**
	 * The acceptance probability function.
	 * <br/>
	 * Calculate the probability of the jump from the current to the new state.
	 * @param dblEnergyCurrent
	 *   The energy of the current state.
	 * @param dblEnergyNew
	 *   The energy of the new (proposed) state.
	 * @param dblTemperature
	 *   The simulated annealing temperature.
	 * @return
	 *   The probability of the jump from the current to the new state.
	 */
	public double acceptanceProbability(double dblEnergyCurrent, double dblEnergyNew, double dblTemperature)
	{
		double result = 0.0;

		if (dblEnergyNew <= dblEnergyCurrent)
			result = 1.0;
		else if (dblTemperature <= 0.0)
			result = 0.0;
		else    // exp(-k(Enew - E)/T)
			result = Math.exp(-m_dblAcceptanceProbabilityScalingFactor * (dblEnergyNew - dblEnergyCurrent) / dblTemperature);

		return result;
	}

	/**
	 * The annealing schedule.
	 * <br/>
	 * Calculate the simulated annealing temperature.
	 * @param nIteration
	 *   The <code>1</code>-based iteration №.
	 * @param nRuns
	 *   The total № of runs (iterations).
	 * @return
	 * The simulated annealing temperature.
	 */
	public double temperature(int nIteration, int nRuns)
	{
		double result = 0.0;
		double dblFactor = 1.0 - (((double)(nIteration - 1)) / m_dblTemperatureDivisor);

		if (dblFactor > 0.0)
			result = Math.max(m_dblTemperatureScalingFactor * Math.pow(dblFactor, 4.0), 0.0);

 // if (result <= 4.0)
 // 	result = 4.0;

		return result;
	}

	/*
	 * Obtain the latest log message, then clear it to <code>null</code>.
	 * @return
	 *   The latest log message. If there is none then return <code>null</code>.
	 */
	/*
	public String popLatestLogMessage()
	{
		String sResult = m_sLogMessage;
		m_sLogMessage = null;
		return sResult;
	}
	*/

	private static MetricPosition randomMetricPosition()
	{
		int nIndex = m_random.nextInt(m_ampMetricPositions.length);
		return m_ampMetricPositions[nIndex];
	}

	private static MetricComponent randomMetricComponent()
	{
		int nIndex = m_random.nextInt(m_amcMetricComponents.length);
		return m_amcMetricComponents[nIndex];
	}
}
