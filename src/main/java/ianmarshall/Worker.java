package ianmarshall;

import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstRadius;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstRadiusFirstTime;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstTime;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.None;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.SecondRadius;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.SecondTime;
import static ianmarshall.MetricComponents.MetricComponent.A;
import static ianmarshall.MetricComponents.MetricComponent.B;
import static ianmarshall.MetricComponents.MetricComponent.C;
import static ianmarshall.MetricComponents.MetricComponent.D;
import static ianmarshall.MetricComponents.MetricPosition.R;
import static ianmarshall.MetricComponents.MetricPosition.T;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import ianmarshall.MetricAndDerivatives.DerivativeLevel;
import ianmarshall.MetricComponents.MetricComponent;
import ianmarshall.MetricComponents.MetricPosition;

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
			m_WorkerResult = new WorkerResult(m_bProcessingCompleted, th, m_nRun, m_madG);
			m_bStopped = true;
		}

	}

	private static final double DBL_LARGE_DIVISION_RESULT = 1.0e12;
	private static final double c = 3.0e8;    // The speed of light in m/s
	private static final double c2 = c * c;
//private static final double DBL_SUCCESS_LOG_PROBABILITY = 0.001;
	private static final Logger s_logger = LoggerFactory.getLogger(Worker.class);
	private static final StringBuilder s_sbMoveLog = new StringBuilder();    // Refactor this for multi-instance use

	private static int m_nProcessors = 0;

	private int m_nRun = 0;
	private int m_nRuns = 0;

	/**
	 * This holds the values of the metric tensor, and optionally their derivatives,
	 * each of which stores metric components for each value of radius and time.
	 */
	private MetricAndDerivatives m_madG = null;

	private boolean m_bFirstRun = true;    // This will also be true when resuming running after a pause
	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
	private WorkerResult m_WorkerResult = null;

	private SimulatedAnnealing m_saSimulatedAnnealing = null;
	private double m_dblEnergyCurrent = -1.0;

	/**
	 * The constructor.
	 * @param spStartParameters
	 *   The the application's start parameters.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means no run has yet been executed.
	 * @param madG
	 *   If not <code>null</code> then use this to set the metric tensor values, otherwise calculate the initial values.
	 */
	public Worker(StartParameters spStartParameters, int nRun, MetricAndDerivatives madG)
	{
		if (m_nProcessors == 0)
			m_nProcessors = Runtime.getRuntime().availableProcessors();

		m_nRun = nRun;
		m_nRuns = spStartParameters.getNumberOfRuns();
		m_madG = madG;
		m_wuehExceptionHandler = new WorkerUncaughtExceptionHandler();
		m_saSimulatedAnnealing = new SimulatedAnnealing(spStartParameters);
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

		while ((!m_bStopping) && (m_nRun < m_nRuns))
		{
			m_nRun++;
			s_logger.info(String.format("Started run number %s.", BlackHoleEvaporation.formatInteger(m_nRun)));

			if (m_bFirstRun)
			{
				if (m_madG == null)
					m_madG = initialiseMetricTensors();

				calculateAllDifferentialsForAllValues(m_madG);

				// The current energy has not been calculated yet
				m_dblEnergyCurrent = m_saSimulatedAnnealing.energy(m_madG, m_nRun);

				m_bFirstRun = false;
			}

			MetricAndDerivatives madNew = m_saSimulatedAnnealing.neighbour(m_madG);
			calculateAllDifferentialsForAllValues(madNew);
			double dblEnergyNew = m_saSimulatedAnnealing.energy(madNew, m_nRun);
			double dblTemperature = m_saSimulatedAnnealing.temperature(m_nRun, m_nRuns);
			double dblProbability = m_saSimulatedAnnealing.acceptanceProbability(m_dblEnergyCurrent, dblEnergyNew,
			 dblTemperature);
			boolean bAcceptMove = Math.random() < dblProbability;
			String sLogEntry = null;
	 // bAcceptMove = false;    // Delete this line

			String sHighlightStart = bAcceptMove ? "    ***  ": " ";
			String sHighlightFinish = bAcceptMove ? "  ***": "";
			String sAction = bAcceptMove ? "accepted" : "rejected";

			if (bAcceptMove || (dblProbability >= 0.5))
				sLogEntry = String.format(
					"Run number %s:%s%s move from energy %f to %f at temperature %f with probability %.5f.%s",
					BlackHoleEvaporation.formatInteger(m_nRun), sHighlightStart, sAction, m_dblEnergyCurrent, dblEnergyNew,
					dblTemperature, dblProbability, sHighlightFinish);

			if (bAcceptMove)
			{
		 // if (Math.random() < DBL_SUCCESS_LOG_PROBABILITY)
		 // {
					String sFormat = s_sbMoveLog.length() > 0 ? "%n  run %d: %s" : "  run %d: %s";
					s_sbMoveLog.append(String.format(sFormat, m_nRun, sLogEntry));
		 // }

				m_madG = madNew;
				m_dblEnergyCurrent = dblEnergyNew;

		 // String sLogMessage = m_saSimulatedAnnealing.popLatestLogMessage();
		 // logger.info(sLogMessage);
			}

			if (sLogEntry == null)
				sLogEntry = String.format("Run number %s: (pre-move) energy = %f.",
				 BlackHoleEvaporation.formatInteger(m_nRun), m_dblEnergyCurrent);

			s_logger.info(sLogEntry);
	 // logger.info(String.format("Completed run number %s with current energy %f.",
	 //  BlackHoleEvaporation.formatInteger(m_nRun), m_dblEnergyCurrent));
		}

		s_logger.info(String.format("Move log is:%n%s", s_sbMoveLog));
		reportFinalTensorValues();

		if (m_nRun >= m_nRuns)
		{
			s_sbMoveLog.setLength(0);
			m_bProcessingCompleted = true;
			s_logger.info("All processing has been completed.");
		}
		else
			s_logger.info("Stopped before all processing completed.");

		m_WorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_madG);
		m_bStopped = true;
	}

	/**
	 * Initialise the metric tensor, and various derivatives with respect to radius and time,
	 * with start values for graduated radius and time values.
	 */
	private MetricAndDerivatives initialiseMetricTensors()
	{
		List<Double> liRadii = new ArrayList<>();
		List<Double> liTimes = new ArrayList<>();

		StringBuilder sbLog = new StringBuilder("Initialising the metric components (a selection is shown)...");
		String sIndent = " ".repeat(72);

		sbLog.append(String.format(
		   "%n%1$sTIndex  RIndex                   T                   R                   A                   B                   C                   D"
		 + "%n%1$s------  ------  ------------------  ------------------  ------------------  ------------------  ------------------  ------------------",
		 sIndent));

		String sFormat = "%n" + sIndent + "%6d  %6d  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f";

		final double DBL_R_MIN = 1.01;
		final double DBL_R_MAX = 100.0;
		final double DBL_STEP_FACTOR_RADIUS = 1.014;

		final double DBL_T_MIN = 0.0;
		final double DBL_STEP_TIME = 1.0;

		double dblR = DBL_R_MIN;
		double dblT = DBL_T_MIN;
		boolean bLoop = true;
		boolean bOneMoreLoop = false;

		while (bLoop)
		{
			if (bOneMoreLoop)
				bLoop = false;

			liRadii.add(dblR);
			liTimes.add(dblT);
			double dblRNew = ((dblR  - 1.0) * DBL_STEP_FACTOR_RADIUS) + 1.0;

			if (dblRNew < DBL_R_MAX)
				dblR = dblRNew;
			else if (dblR < DBL_R_MAX)
			{
				// We shall loop once more only, and then not rely on comparison precision
				dblR = DBL_R_MAX;
				bOneMoreLoop = true;
			}
			else
				bLoop = false;

			dblT += DBL_STEP_TIME;
		}

		Double[] adblRadii = liRadii.toArray(new Double[0]);
		Double[] adblTimes = liTimes.toArray(new Double[0]);
		MetricAndDerivatives madResult = buildMetricAndDerivatives(adblRadii, adblTimes);
		int nRadiusElements = madResult.getNRadiusElements();
		int nTimeElements = madResult.getNTimeElements();
		int nTimeElementMidPoint = nTimeElements / 2;

		double dblA =  1.0;
		double dblB =  -1.0;
		double dblC =  -1.0;
		double dblD =  1.0;    // Let us try first having D positive (it could turn out to be negative instead)

		for (int nRIndex = 0; nRIndex < nRadiusElements; nRIndex++)
			for (int nTIndex = 0; nTIndex < nTimeElements; nTIndex++)
			{
				setMetricComponent(madResult, None, nRIndex, nTIndex, A, dblA);
				setMetricComponent(madResult, None, nRIndex, nTIndex, B, dblB);
				setMetricComponent(madResult, None, nRIndex, nTIndex, C, dblC);
				setMetricComponent(madResult, None, nRIndex, nTIndex, D, dblD);

				if ((nTIndex == nTimeElementMidPoint) && (((nRIndex % 100) == 0) || (nRIndex == nRadiusElements - 1)))
				{
					dblT = getMetricComponent(madResult, None, nRIndex, nTIndex, T, A).getKey().doubleValue();
					dblR = getMetricComponent(madResult, None, nRIndex, nTIndex, R, A).getKey().doubleValue();
					sbLog.append(String.format(sFormat, nTIndex, nRIndex, dblT, dblR, dblA, dblB, dblC, dblD));
				}
			}

		s_logger.info(sbLog.toString());
		s_logger.info("The metric components have been initialised.");
		return madResult;
	}

	/**
	 * Build an initialised metric and its derivatives from the supplied radius and time values.
	 * @param adblRadii
	 *   The array of radius values to be used for the metric tensor components.
	 * @param adblTimes
	 *   The array of time values to be used for the metric tensor components.
	 * @return
	 *   A <code>MetricAndDerivatives</code> object holding the initialised metric and its derivatives.
	 */
	private MetricAndDerivatives buildMetricAndDerivatives(Double[] adblRadii, Double[] adblTimes)
	{
		MetricAndDerivatives madResult = new MetricAndDerivatives(adblRadii, adblTimes);
		return madResult;
	}

	/**
	 * Calculate the first- and second-order differentials of all the metric tensor components
	 * with respect to radius and/or time.
	 * <br>
	 * All of the parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius and time values. This number of elements must be at least 5.
	 * @param madG
	 *   The metric tensor components and its derivatives.
	 */
	private void calculateAllDifferentialsForAllValues(MetricAndDerivatives madG)
	{
		final MetricComponent[] amcMetricComponents = MetricComponent.values();
		int nRadiusElements = madG.getNRadiusElements();
		int nTimeElements   = madG.getNTimeElements();
		int nChunkSize = 1 + ((nRadiusElements - 1) / m_nProcessors);

		for (DerivativeLevel dlDerivativeLevel: DerivativeLevel.values())
			if (dlDerivativeLevel != None)
			{
				MetricPosition mpVarying;
				switch (dlDerivativeLevel)
				{
					case FirstRadius:
					case SecondRadius:
						mpVarying = R;
						break;
					case FirstTime:
					case SecondTime:

					// It does not matter whether we use R or T in this case, but we must be consistent with the code lower down
					case FirstRadiusFirstTime:
						mpVarying = T;
						break;
					default:
						throw new RuntimeException(String.format("Invalid derivative level \"%s\".", dlDerivativeLevel.toString()));
				}

				DerivativeLevel dlGetting = gettingDerivativeLevel(dlDerivativeLevel, mpVarying);

				for (MetricComponent mcMetricComponent: amcMetricComponents)
					IntStream.range(0, m_nProcessors).parallel().forEach(new IntConsumer()
					{
						@Override
						public void accept(int nChunk)
						{
							int nRIndexStart = nChunk * nChunkSize;
							int nRIndexFinish = Math.min(nRIndexStart + nChunkSize - 1, nRadiusElements - 1);

							for (int nRIndex = nRIndexStart; nRIndex <= nRIndexFinish; nRIndex++)
								for (int nTIndex = 0; nTIndex < nTimeElements; nTIndex++)
									calculateDifferentialOfMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpVarying,
									 mcMetricComponent, dlGetting);
						}
					});
			}
	}

	/**
	 * Calculate the specified level of differential of the specified metric component and store it
	 * in the <code>MetricAndDerivatives</code> object supplied.
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain at least 3 elements
	 * for each space-time dimension (time and radius here).
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be calculated.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component, the differential of which is to be calculated.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component, the differential of which is to be calculated.
	 * @param mpVarying
	 *   The metric position, the varying of the value at which is to be calculated.
	 * @param mcMetricComponent
	 *   The metric component, the differential of which is to be calculated.
	 * @param dlGetting
	 * 	The <code>DerivativeLevel</code> to be used to get the metric components for calculating the differential.
	 */
	private void calculateDifferentialOfMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex, MetricPosition mpVarying, MetricComponent mcMetricComponent, DerivativeLevel dlGetting)
	{
		double dblValue = differentialOfMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpVarying,
		 mcMetricComponent, dlGetting);
		setMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mcMetricComponent, dblValue);
	}

	/**
	 * Calculate the specified level of differential of the specified metric component.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The <code>DerivativeLevel</code> to be calculated.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component, the differential of which is to be calculated.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component, the differential of which is to be calculated.
	 * @param mpVarying
	 *   The <code>MetricPosition</code>, the varying of the value at which is to be calculated.
	 * @param mcMetricComponent
	 *   The <code>MetricComponent</code>, the differential of which is to be calculated.
	 * @param dlGetting
	 * 	The <code>DerivativeLevel</code> to be used to get the metric components for calculating the differential.
	 * @return
	 *   The specified level of differential of the specified metric component.
	 */
	private double differentialOfMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex, MetricPosition mpVarying, MetricComponent mcMetricComponent, DerivativeLevel dlGetting)
	{
		double dblResult = 0.0;

		int nVaryingIndex;
		int nVaryingMaxIndex;
		switch (mpVarying)
		{
			case R:
				nVaryingIndex = nRIndex;
				nVaryingMaxIndex = madG.getNRadiusElements() - 1;
				break;
			case T:
				nVaryingIndex = nTIndex;
				nVaryingMaxIndex = madG.getNTimeElements() - 1;
				break;
			default:
				throw new RuntimeException(String.format("Invalid metric position \"%s\".", mpVarying.toString()));
		}

		// The middle elements ("index" 1) are those of the point, the derivatives of which are to be calculated.
		// This may be different from the "index" supplied if it is the first or last point.
		// In these cases, we shall use forward and backward differences, respectively, instead.
		double dblPos0 = 0.0;
		double dblPos1 = 0.0;
		double dblPos2 = 0.0;
		double dblComponent0 = 0.0;
		double dblComponent1 = 0.0;
		double dblComponent2 = 0.0;

		final int nVaryingStart;
		if (nVaryingIndex == 0)
			nVaryingStart = nVaryingIndex;        // Forward difference for the first point
		else if (nVaryingIndex < nVaryingMaxIndex)
			nVaryingStart = nVaryingIndex - 1;    // Central difference for an internal point
		else
			nVaryingStart = nVaryingIndex - 2;    // Backward difference for the last point

		final int nVaryingFinish = nVaryingStart + 2;
		int n = 0;

		for (int i = nVaryingStart; i <= nVaryingFinish; i++)
		{
			switch (mpVarying)
			{
				case R:
					nRIndex = i;
					break;
				case T:
					nTIndex = i;
					break;
				default:
					throw new RuntimeException(String.format("Invalid metric position \"%s\".", mpVarying.toString()));
			}

			SimpleImmutableEntry<Double, Double> entry = getMetricComponent(madG, dlGetting, nRIndex, nTIndex, mpVarying,
			 mcMetricComponent);

			switch (n)
			{
				case 0:
					dblPos0       = entry.getKey().doubleValue();
					dblComponent0 = entry.getValue().doubleValue();
					break;
				case 1:
					dblPos1       = entry.getKey().doubleValue();
					dblComponent1 = entry.getValue().doubleValue();
					break;
				case 2:
					dblPos2       = entry.getKey().doubleValue();
					dblComponent2 = entry.getValue().doubleValue();
					break;
			}

			n++;
		}

		double dblDifferentialPrev = safeDivide((dblComponent1 - dblComponent0), (dblPos1 - dblPos0));
		double dblDifferentialNext = safeDivide((dblComponent2 - dblComponent1), (dblPos2 - dblPos1));

		switch (dlDerivativeLevel)
		{
			case FirstRadius:
			case FirstTime:
			case FirstRadiusFirstTime:
				dblResult = 0.5 * (dblDifferentialNext + dblDifferentialPrev);
				break;
			case SecondRadius:
			case SecondTime:
				dblResult = 2.0 * safeDivide(dblDifferentialNext - dblDifferentialPrev, dblPos2 - dblPos0);
				break;
			default:    // For example: None
				throw new RuntimeException(String.format("Invalid differentiation request for:"
				 + "%n  dlDerivativeLevel = %s,"
				 + "%n  nRIndex           = %d,"
				 + "%n  nTIndex           = %d,"
				 + "%n  mpVarying         = %s,"
				 + "%n  mcMetricComponent = %s,"
				 + "%n  dlGetting         = %s,"
				 + "%n  dblPos0           = %f,"
				 + "%n  dblPos1           = %f,"
				 + "%n  dblPos2           = %f,"
				 + "%n  dblComponent0     = %f,"
				 + "%n  dblComponent1     = %f,"
				 + "%n  dblComponent2     = %f.",
				 dlDerivativeLevel.toString(), nRIndex, nTIndex, mpVarying.toString(), mcMetricComponent.toString(),
				 dlGetting.toString(), dblPos0, dblPos1, dblPos2, dblComponent0, dblComponent1, dblComponent2));
		}

		return dblResult;
	}

	/**
	 * Get the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position from the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be found.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be found.
	 * @param mpMetricPosition
	 *   The metric position to be found.
	 * @param mcMetricComponent
	 *   The metric component to be found.
	 * @return
	 *   An <code>Entry</code> with:
	 *   <ul>
	 *     <li>key: the value of the specified metric position</li>
	 *     <li>value: the value of the specified component of the specified level of differential of the specified index.
	 *     </li>
	 *   </ul>
	 */
	public static SimpleImmutableEntry<Double, Double> getMetricComponent(MetricAndDerivatives madG,
	 DerivativeLevel dlDerivativeLevel, int nRIndex, int nTIndex, MetricPosition mpMetricPosition,
	 MetricComponent mcMetricComponent)
	{
 // return getMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpMetricPosition,
 //  mcMetricComponent, false);
		MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
		return mcMetricComponents.getComponent(mpMetricPosition, mcMetricComponent);
	}

	/*
	 * Get the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position from the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be found.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be found.
	 * @param mpMetricPosition
	 *   The metric position to be found.
	 * @param mcMetricComponent
	 *   The metric component to be found.
	 * @param bLock
	 *   Whether or not to lock the processing in this method in order to synchronise it.
	 * @return
	 *   An <code>Entry</code> with:
	 *   <ul>
	 *     <li>key: the value of the specified metric position</li>
	 *     <li>value: the value of the specified component of the specified level of differential of the specified index.
	 *     </li>
	 *   </ul>
	 */
	/*
	private static SimpleImmutableEntry<Double, Double> getMetricComponent(MetricAndDerivatives madG,
	 DerivativeLevel dlDerivativeLevel, int nRIndex, int nTIndex, MetricPosition mpMetricPosition,
	 MetricComponent mcMetricComponent, boolean bLock)
	{
		SimpleImmutableEntry<Double, Double> entryResult;

		if (bLock)
			synchronized (madG)
			{
				MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
				entryResult = mcMetricComponents.getComponent(mpMetricPosition, mcMetricComponent);
			}
		else
		{
			MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
			entryResult = mcMetricComponents.getComponent(mpMetricPosition, mcMetricComponent);
		}

		return entryResult;
	}
	*/

	/**
	 * Set the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position in the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level of the value to be set.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be set.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be set.
	 * @param mcMetricComponent
	 *   The metric component of the value to be set.
	 * @param dblValue
	 *   The value to be set.
	 */
	private static void setMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel, int nRIndex,
	 int nTIndex, MetricComponent mcMetricComponent, double dblValue)
	{
 // setMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mcMetricComponent, dblValue, false);
		MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
		mcMetricComponents.setComponent(mcMetricComponent, dblValue);
	}

	/*
	 * Set the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position in the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level of the value to be set.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be set.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be set.
	 * @param mcMetricComponent
	 *   The metric component of the value to be set.
	 * @param dblValue
	 *   The value to be set.
	 * @param bLock
	 *   Whether or not to lock the processing in this method in order to synchronise it.
	 */
	/*
	private static void setMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel, int nRIndex,
	 int nTIndex, MetricComponent mcMetricComponent, double dblValue, boolean bLock)
	{
		if (bLock)
			synchronized (madG)
			{
				MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
				mcMetricComponents.setComponent(mcMetricComponent, dblValue);
			}
		else
		{
			MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
			mcMetricComponents.setComponent(mcMetricComponent, dblValue);
		}
	}
	*/

	/**
	 * Obtain the <code>MetricComponents</code> for the given parameters.
	 *
	 * Get the <code>MetricComponents</code> of the specified level of differential of the specified index
	 * from the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be found.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be found.
	 * @return
	 *   The <code>MetricComponents</code>.
	 */
	public static MetricComponents getMetricComponents(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex)
	{
		Metric mMetric = madG.getMetric(dlDerivativeLevel);
		return mMetric.getMetricComponents(nRIndex, nTIndex);
	}

	/**
	 * Determine the <code>DerivativeLevel</code> to be used to get the metric components
	 * for calculating the specified <code>DerivativeLevel</code>.
	 * @param dlDerivativeLevel
	 *   The <code>DerivativeLevel</code> to be calculated.
	 * @param mpVarying
	 *   The <code>MetricPosition</code>, the varying of the value at which is to be calculated.
	 * @return
	 *   The <code>DerivativeLevel</code> to be used.
	 */
	private DerivativeLevel gettingDerivativeLevel(DerivativeLevel dlDerivativeLevel, MetricPosition mpVarying)
	{
		DerivativeLevel dlResult;

		switch (dlDerivativeLevel)
		{
			case FirstRadius:
			case SecondRadius:
			case FirstTime:
			case SecondTime:
				dlResult = None;
				break;
			case FirstRadiusFirstTime:
				// We must be consistent with whether we vary R or T in this case. The statement below does this automatically.
				dlResult = mpVarying == T ? FirstRadius : FirstTime;
				break;
			default:
				throw new RuntimeException(String.format("Invalid derivative level \"%s\".", dlDerivativeLevel.toString()));
		}

		return dlResult;
	}

	/**
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

	/**
	 * Calculate the Ricci tensor values at the given point in space-time.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the point in space-time to be used.
	 * @param nTIndex
	 *   The zero-based index value of the time of the point in space-time to be used.
	 * @return
	 *   The Ricci tensor values at the given point in space-time (the radius and time) as the vector (1-D column matrix):
	 *   <code>(R00, R01, R11, R22)T</code>.
	 */
	public static DoubleMatrix2D calculateRicciTensorValues(MetricAndDerivatives madG, int nRIndex, int nTIndex)
	{
 // double dblT = getMetricComponent(madG, None, nRIndex, nTIndex, T, A).getKey().doubleValue();
		SimpleImmutableEntry<Double, Double> entry = getMetricComponent(madG, None, nRIndex, nTIndex, R, A);
		double dblR = entry.getKey().doubleValue();
		double dblA = entry.getValue().doubleValue();

		double dblB = getMetricComponent(madG, None, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double dblC = getMetricComponent(madG, None, nRIndex, nTIndex, R, C).getValue().doubleValue();
		double dblD = getMetricComponent(madG, None, nRIndex, nTIndex, R, D).getValue().doubleValue();

		double dAdR = getMetricComponent(madG, FirstRadius, nRIndex, nTIndex, R, A).getValue().doubleValue();
		double dBdR = getMetricComponent(madG, FirstRadius, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double dCdR = getMetricComponent(madG, FirstRadius, nRIndex, nTIndex, R, C).getValue().doubleValue();
		double dDdR = getMetricComponent(madG, FirstRadius, nRIndex, nTIndex, R, D).getValue().doubleValue();

		double dAdT = getMetricComponent(madG, FirstTime, nRIndex, nTIndex, R, A).getValue().doubleValue();
		double dBdT = getMetricComponent(madG, FirstTime, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double dCdT = getMetricComponent(madG, FirstTime, nRIndex, nTIndex, R, C).getValue().doubleValue();
		double dDdT = getMetricComponent(madG, FirstTime, nRIndex, nTIndex, R, D).getValue().doubleValue();

		double d2AdR2 = getMetricComponent(madG, SecondRadius, nRIndex, nTIndex, R, A).getValue().doubleValue();
 // double d2BdR2 = getMetricComponent(madG, SecondRadius, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double d2CdR2 = getMetricComponent(madG, SecondRadius, nRIndex, nTIndex, R, C).getValue().doubleValue();
 // double d2DdR2 = getMetricComponent(madG, SecondRadius, nRIndex, nTIndex, R, D).getValue().doubleValue();

 // double d2AdT2 = getMetricComponent(madG, SecondTime, nRIndex, nTIndex, R, A).getValue().doubleValue();
		double d2BdT2 = getMetricComponent(madG, SecondTime, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double d2CdT2 = getMetricComponent(madG, SecondTime, nRIndex, nTIndex, R, C).getValue().doubleValue();
 // double d2DdT2 = getMetricComponent(madG, SecondTime, nRIndex, nTIndex, R, D).getValue().doubleValue();

 // double d2AdRdT = getMetricComponent(madG, FirstRadiusFirstTime, nRIndex, nTIndex, R, A).getValue().doubleValue();
 // double d2BdRdT = getMetricComponent(madG, FirstRadiusFirstTime, nRIndex, nTIndex, R, B).getValue().doubleValue();
		double d2CdRdT = getMetricComponent(madG, FirstRadiusFirstTime, nRIndex, nTIndex, R, C).getValue().doubleValue();
		double d2DdRdT = getMetricComponent(madG, FirstRadiusFirstTime, nRIndex, nTIndex, R, D).getValue().doubleValue();

		double dblFactor1 = (2.0 / (dblD * dblD)) - (1.0 / (4.0 * dblA * dblB));
		double dblFactor2 = (1.0 / (dblD * dblD)) - (1.0 / (4.0 * dblA * dblB));

		double dblR00 = -((2.0 / (dblD * c * dblR)) * dAdT)
		 + ((1.0 / (dblB * dblR)) * dAdR)
		 - ((1.0 / (dblB * c * dblR)) * dDdT)
		 + ((dblFactor1 / c2) * dAdT * dBdT)
		 - ((1.0 / (2.0 * dblB * dblD * c)) * dAdT * dBdR)
		 - ((1.0 / (2.0 * dblA * dblC * c2)) * dAdT * dCdT)
		 - ((1.0 / (2.0 * dblB * dblD * c)) * dAdT * dBdR)
		 - ((1.0 / (dblC * dblD * c)) * dAdT * dCdR)
		 + (dblFactor1 * dAdR * dAdR)
		 - ((1.0 / (2.0 * dblB * dblD * c)) * dAdR * dBdT)
		 - ((1.0 / (4.0 * dblB * dblB)) * dAdR * dBdR)
		 - ((1.0 / (dblC * dblD * c)) * dAdR * dCdT)
		 - ((1.0 / (2.0 * dblB * dblC)) * dAdR * dCdR)
		 - ((dblFactor1 / c) * dAdR * dDdT)
		 + ((1.0 / (2.0 * dblB * dblD)) * dAdR * dDdR)
		 + ((1.0 / (2.0 * dblB * dblD * c2)) * dBdT * dDdT)
		 - ((1.0 / (4.0 * dblB * dblB * c2)) * dBdT * dBdT)
		 + ((1.0 / (4.0 * dblB * dblB * c)) * dBdR * dDdT)
		 - ((1.0 / (2.0 * dblC * dblC * c2)) * dCdT * dCdT)
		 - ((1.0 / (dblC * dblD * c2)) * dCdT * dDdT)
		 - ((1.0 / (2.0 * dblB * dblC * c)) * dCdR * dDdT)
		 - ((1.0 / (2.0 * dblB * dblD * c)) * dDdT * dDdR)
		 + ((1.0 / (2.0 * dblB)) * d2AdR2)
		 + ((1.0 / (2.0 * dblB * c2)) * d2BdT2)
		 + ((1.0 / (dblC * c2)) * d2CdT2)
		 - ((1.0 / (2.0 * dblB * c)) * d2DdRdT);

		double dblR01 = -((2.0 / (dblD * dblR)) * dAdR)
		 - ((1.0 / (dblB * dblR * c)) * dBdT)
		 + ((1.0 / (dblC * dblR * c)) * dCdT)
		 - ((1.0 / (2.0 * dblA * dblD)) * dAdR * dAdR)
		 - ((1.0 / (2.0 * dblB * dblD * c2)) * dBdT * dBdT)
		 - ((1.0 / (2.0 * dblA * dblD * c2)) * dAdT * dBdT)
		 + ((1.0 / (dblD * dblD * c)) * dAdT * dBdR)
		 - ((1.0 / (2.0 * dblA * dblD * c)) * dAdT * dDdR)
		 - ((1.0 / (dblD * dblD * c)) * dAdR * dBdT)
		 - ((1.0 / (2.0 * dblB * dblD)) * dAdR * dBdR)
		 - ((1.0 / (2.0 * dblA * dblC * c)) * dAdR * dCdT)
		 - ((1.0 / (dblC * dblD)) * dAdR * dCdR)
		 + (dblFactor2 * dAdR * dDdR)
		 - ((1.0 / (dblC * dblD * c2)) * dBdT * dCdT)
		 - ((1.0 / (2.0 * dblB * dblC * c)) * dBdT * dCdR)
		 + ((dblFactor2 / c2) * dBdT * dDdT)
		 + ((1.0 / (2.0 * dblB * dblD * c)) * dBdR * dDdT)
		 - ((1.0 / (2.0 * dblC * dblC * c)) * dCdT * dCdR)
		 - ((dblFactor2 / c) * dDdT * dDdR)
		 - ((1.0 / dblD) * d2AdR2)
		 - ((1.0 / (dblD * c2)) * d2BdT2)
		 + ((1.0 / (dblC * c)) * d2CdRdT)
		 + ((1.0 / (dblD * c)) * d2DdRdT);

		double dblR11 = ((2.0 / (dblD * c * dblR)) * dBdT)
		 - ((1.0 / (dblB * dblR)) * dBdR)
		 + ((2.0 / (dblC * dblR)) * dCdR)
		 - ((2.0 / (dblD * dblR)) * dDdR)
		 - ((1.0 / (4.0 * dblA * dblA * c2)) * dAdT * dBdT)
		 - ((1.0 / (2.0 * dblA * dblD * c)) * dAdT * dBdR)
		 + ((1.0 / (4.0 * dblA * dblA * c)) * dAdT * dDdR)
		 + ((1.0 / (2.0 * dblA * dblD * c)) * dAdR * dBdT)
		 + (dblFactor1 * dAdR * dBdR)
		 + ((1.0 / (2.0 * dblA * dblD)) * dAdR * dDdR)
		 + ((1.0 / (2.0 * dblA * dblC * c2)) * dBdT * dCdT)
		 + ((1.0 / (dblC * dblD * c)) * dBdT * dCdR)
		 + ((1.0 / (2.0 * dblA * dblD * c2)) * dBdT * dDdT)
		 - ((dblFactor1 / c) * dBdT * dDdR)
		 - ((1.0 / (dblC * dblD * c)) * dBdR * dCdT)
		 - ((1.0 / (2.0 * dblB * dblC)) * dBdR * dCdR)
		 - ((1.0 / (2.0 * dblA * dblC * c)) * dCdT * dDdR)
		 - ((1.0 / (dblC * dblD)) * dCdR * dDdR)
		 - ((1.0 / (2.0 * dblA * dblD * c)) * dDdT * dDdR)
		 - ((1.0 / (4.0 * dblA * dblA)) * dAdR * dAdR)
		 + ((dblFactor1 / c2) * dBdT * dBdT)
		 - ((1.0 / (2.0 * dblC * dblC)) * dCdR * dCdR)
		 + ((1.0 / (2.0 * dblA)) * d2AdR2)
		 + ((1.0 / (2.0 * dblA * c2)) * d2BdT2)
		 + ((1.0 / dblC) * d2CdR2)
		 - ((1.0 / (2.0 * dblA * c)) * d2DdRdT);

		double dblR22 = -1.0 + (dblC / dblB)
		 + (((dblR * dblC) / (dblA * dblD * c)) * dAdT)
		 + (((dblR * dblC) / (2.0 * dblA * dblB)) * dAdR)
		 + (((dblR * dblC) / (dblB * dblD * c)) * dBdT)
		 - (((dblR * dblC) / (2.0 * dblB * dblB)) * dBdR)
		 + (((4.0 * dblR) / (dblD * c)) * dCdT)
		 + (((2.0 * dblR) / dblB) * dCdR)
		 + (((dblR * dblC) / (dblB * dblD)) * dDdR)
		 - (((dblR * dblR) / (4.0 * dblA * dblA * c2)) * dAdT * dCdT)
		 + (((dblR * dblR) / (2.0 * dblA * dblD * c)) * ((dAdT * dCdR) + (dAdR * dCdT)))
		 + (((dblR * dblR) / (4.0 * dblA * dblB)) * dAdR * dCdR)
		 + (((dblR * dblR) / (4.0 * dblA * dblB * c2)) * dBdT * dCdT)
		 + (((dblR * dblR) / (2.0 * dblB * dblD * c)) * ((dBdT * dCdR) + (dBdR * dCdT)))
		 - (((dblR * dblR) / (4.0 * dblB * dblB)) * dBdR * dCdR)
		 + (((dblR * dblR) / (2.0 * dblA * dblD * c2)) * dCdT * dDdT)
		 + (((dblR * dblR) / (2.0 * dblB * dblD)) * dCdR * dDdR)
		 + (((dblR * dblR) / (2.0 * dblA * c2)) * d2CdT2)
		 + (((2.0 * dblR * dblR) / (dblD * c)) * d2CdRdT)
		 + (((dblR * dblR) / (2.0 * dblB)) * d2CdR2);

		DoubleMatrix2D dvResult = DoubleFactory2D.dense.make(4, 1);
		dvResult.set(0, 0, dblR00);
		dvResult.set(1, 0, dblR01);
		dvResult.set(2, 0, dblR11);
		dvResult.set(3, 0, dblR22);
		return dvResult;
	}

	private void reportFinalTensorValues()
	{
		final boolean B_REPORT_FINAL_TENSOR_VALUES_IN_CSV_FORMAT = false;
		final int N_REPORT_VALUE_SKIP_INTERVAL = 10;

		s_logger.info(String.format(
		 "The metric components (in the format \"tIndex, rIndex, t, r, A, B, C, D\") after the final run are:"));

		String sFormatHeader;
		if (B_REPORT_FINAL_TENSOR_VALUES_IN_CSV_FORMAT)
			sFormatHeader =
			   "%n  tIndex, rIndex,                  t,                  r,                  A,                  B,                  C,                  D"
			 + "%n";
		else
			sFormatHeader =
			   "%n  tIndex  rIndex                   t                   r                   A                   B                   C                   D"
			 + "%n  ------  ------  ------------------  ------------------  ------------------  ------------------  ------------------  ------------------";

		String sFormat;
		if (B_REPORT_FINAL_TENSOR_VALUES_IN_CSV_FORMAT)
			sFormat = "%n  %6d, %6d, %,18.12f, %,18.12f, %,18.12f, %,18.12f, %,18.12f, %,18.12f";
		else
			sFormat = "%n  %6d  %6d  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f";

		StringBuilder sbLog = new StringBuilder(String.format(sFormatHeader));
		int nTimeElements   = m_madG.getNTimeElements();
		int nRadiusElements = m_madG.getNRadiusElements();

		for (int t = 0; t < nTimeElements; t++)
			if ((t % N_REPORT_VALUE_SKIP_INTERVAL == 0) || (t == nTimeElements - 1))
				for (int r = 0; r < nRadiusElements; r++)
					if ((r % N_REPORT_VALUE_SKIP_INTERVAL == 0) || (r == nRadiusElements - 1))
					{
						double dblT = getMetricComponent(m_madG, None, r, t, T, A).getKey().doubleValue();
						double dblR = getMetricComponent(m_madG, None, r, t, R, A).getKey().doubleValue();
						double dblA = getMetricComponent(m_madG, None, r, t, R, A).getValue().doubleValue();
						double dblB = getMetricComponent(m_madG, None, r, t, R, B).getValue().doubleValue();
						double dblC = getMetricComponent(m_madG, None, r, t, R, C).getValue().doubleValue();
						double dblD = getMetricComponent(m_madG, None, r, t, R, D).getValue().doubleValue();
						sbLog.append(String.format(sFormat, t, r, dblT, dblR, dblA, dblB, dblC, dblD));
					}

		s_logger.info(sbLog.toString());
	}
}
