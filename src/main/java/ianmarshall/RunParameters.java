package ianmarshall;

import java.math.BigDecimal;

public class RunParameters
{
	public enum CSVFieldMode
	{
		ALL_FIELDS,
		LINEAR_FIELDS_ONLY,
		LOGARITHMIC_FIELDS_ONLY
	}

	// The mass of the Universe at the Big Bang in kg (taken to be the same as its current mass)
	public static final BigDecimal BD_START_UNIVERSAL_MASS_IN_KG = new BigDecimal("1.5e53");

//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-90");
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-10");
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-30");
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-60");
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-120");
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-150");    // Too small
//public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-140");
	public static final BigDecimal BD_START_RADIUS_RATIO_FRACTION = new BigDecimal("1e-145");

//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("3e+112");
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = BigDecimal.ONE;
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("1e-10");
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("1e-41");
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("1e-151");
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("1e-51");
	private static final BigDecimal BD_TIME_INCREMENT_SECONDS_RUN_0 = new BigDecimal("1e-26");
	private static final BigDecimal BD_TIME_INCREMENT_SECONDS_INCREASE_FACTOR = BigDecimal.TEN;
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_FINAL = new BigDecimal("1.5e+136");
//private static final BigDecimal BD_TIME_INCREMENT_SECONDS_FINAL = new BigDecimal("1.5e+140");
	private static final BigDecimal BD_TIME_INCREMENT_SECONDS_FINAL = new BigDecimal("1.5e+138");

	public static final CSVFieldMode CSV_FIELD_MODE = CSVFieldMode.ALL_FIELDS;

	private static boolean s_bTimeIncrementSecondsFinalReached = false;

	/*
	public static BigDecimal calculateTimeIncrementSeconds(int nRun)
	{
		BigDecimal bdResult;

		if (s_bTimeIncrementFinalReached)
			bdResult = BD_TIME_INCREMENT_FINAL;
		else
		{
			bdResult = BD_TIME_INCREMENT_RUN_0.multiply(BD_TIME_INCREMENT_INCREASE_FACTOR.pow(nRun));

			if (bdResult.compareTo(BD_TIME_INCREMENT_FINAL) >= 0)
			{
				bdResult = BD_TIME_INCREMENT_FINAL;
				s_bTimeIncrementFinalReached = true;
			}
		}

		return bdResult;
	}
	*/

	public static BigDecimal calculateTimeIncrementSeconds(int nRun)
	{
		BigDecimal bdResult;

		if (s_bTimeIncrementSecondsFinalReached)
			bdResult = BD_TIME_INCREMENT_SECONDS_FINAL;
		else
		{
			bdResult = BD_TIME_INCREMENT_SECONDS_RUN_0.multiply(BD_TIME_INCREMENT_SECONDS_INCREASE_FACTOR.pow(nRun));

			if (bdResult.compareTo(BD_TIME_INCREMENT_SECONDS_FINAL) >= 0)
			{
				bdResult = BD_TIME_INCREMENT_SECONDS_FINAL;
				s_bTimeIncrementSecondsFinalReached = true;
			}
		}

		return bdResult;
	}
}
