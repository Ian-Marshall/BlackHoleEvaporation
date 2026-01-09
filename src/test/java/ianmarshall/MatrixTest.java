package ianmarshall;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatrixTest
{
	private static final double DBL_DELTA = 1E-10;

	private static final double[][] M_ADBL_3_X_3 =
	{
		{1.0, 2.0, 3.0},
		{4.0, 5.0, 6.0},
		{7.0, 8.0, 9.0}
	};

	private static final double[][] M_ADBL_2_X_3 =
	{
		{1.0, 2.0, 3.0},
		{4.0, 5.0, 6.0}
	};

	private static final double[][] M_ADBL_2_X_2_A =
	{
		{4.107636E-17, 2.767838E-10},
		{2.767838E-10, 0.73358}
	};

	private static final double[][] M_ADBL_2_X_3_B =
	{
		{-0.533891, -1.562272E+8, 9.159612},
		{-1.167551,  0.058945,   -0.000924}
	};

	private static final Logger logger = Logger.getLogger(MatrixTest.class.getName());
	private static Algebra s_algebra = null;
	private static boolean s_bFirstLog = true;
	private DoubleMatrix2D m_dm3x3 = null;
	private DoubleMatrix2D m_dm2x3 = null;
	private DoubleMatrix2D m_dm2x2A = null;
	private DoubleMatrix2D m_dm2x3B = null;

	public MatrixTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
	{
		s_algebra = new Algebra();
	}

	@AfterClass
	public static void tearDownClass()
	{
	}

	@Before
	public void setUp()
	{
		m_dm3x3 = DoubleFactory2D.dense.make(M_ADBL_3_X_3);
		m_dm2x3 = DoubleFactory2D.dense.make(M_ADBL_2_X_3);
		m_dm2x2A = DoubleFactory2D.dense.make(M_ADBL_2_X_2_A);
		m_dm2x3B = DoubleFactory2D.dense.make(M_ADBL_2_X_3_B);

		if (s_bFirstLog)
		{
			logger.log(Level.INFO, String.format("The source 3 × 3 matrix is: %s%n", m_dm3x3.toString()));
			logger.log(Level.INFO, String.format("The source 2 × 3 matrix is: %s%n", m_dm2x3.toString()));
			s_bFirstLog = false;
		}
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testCernColt3x3MatrixNoOp()
	{
		DoubleMatrix2D dm3x3Actual = m_dm3x3.copy();    // Do nothing
		logger.log(Level.INFO, String.format("The no-op 3 × 3 matrix is: %s%n", dm3x3Actual.toString()));

		double[][] adblExpected =
		{
			{1.0, 2.0, 3.0},
			{4.0, 5.0, 6.0},
			{7.0, 8.0, 9.0}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm3x3Actual.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	@Test
	public void testCernColt2x3MatrixNoOp()
	{
		DoubleMatrix2D dm2x3Actual = m_dm2x3.copy();    // Do nothing
		logger.log(Level.INFO, String.format("The no-op 2 × 3 matrix is: %s%n", dm2x3Actual.toString()));

		double[][] adblExpected =
		{
			{1.0, 2.0, 3.0},
			{4.0, 5.0, 6.0}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm2x3Actual.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	@Test
	public void testCernColt2x2AMatrixNoOp()
	{
		DoubleMatrix2D dm2x2Actual = m_dm2x2A.copy();    // Do nothing
		logger.log(Level.INFO, String.format("The no-op 2 × 2 matrix is: %s%n", dm2x2Actual.toString()));

		double[][] adblExpected =
		{
			{4.107636E-17, 2.767838E-10},
			{2.767838E-10, 0.73358}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm2x2Actual.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, 1E-18);
	}

	@Test
		public void testCernColt2x3BMatrixNoOp()
	{
		DoubleMatrix2D dm2x3Actual = m_dm2x3B.copy();    // Do nothing
		logger.log(Level.INFO, String.format("The no-op 2 × 3 matrix is: %s%n", dm2x3Actual.toString()));

		double[][] adblExpected =
		{
			{-0.533891, -1.562272E+8, 9.159612},
			{-1.167551,  0.058945,   -0.000924}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm2x3Actual.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	@Test
	public void testCernColt3x3MatrixTranspose()
	{
		DoubleMatrix2D dm3x3T = s_algebra.transpose(m_dm3x3).copy();
		logger.log(Level.INFO, String.format("The transposed 3 × 3 matrix is: %s%n", dm3x3T.toString()));

		double[][] adblExpected =
		{
			{1.0, 4.0, 7.0},
			{2.0, 5.0, 8.0},
			{3.0, 6.0, 9.0}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm3x3T.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	@Test
	public void testCernColt2x3MatrixTranspose()
	{
		DoubleMatrix2D dm2x3T = s_algebra.transpose(m_dm2x3).copy();
		logger.log(Level.INFO, String.format("The transposed 2 × 3 matrix is: %s%n", dm2x3T.toString()));

		double[][] adblExpected =
		{
			{1.0, 4.0},
			{2.0, 5.0},
			{3.0, 6.0}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm2x3T.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	@Test
	public void testCernColt2x2By2x3MatrixMultiply()
	{
		DoubleMatrix2D dm2x3Mult = s_algebra.mult(m_dm2x2A, m_dm2x3B);
		logger.log(Level.INFO, String.format("The multiplied 2 × 3 matrix is: %s%n", dm2x3Mult.toString()));

		double[][] adblExpected =
		{
			{-3.231592E-10, -6.400930E-9, -2.553720E-13},
			{-0.8564920627, -2.849794E-7, -0.0006778254}
		};

		double[] vecdblExpected = convert2DArrayTo1DArray(adblExpected);
		double[] vecdblActual = convert2DArrayTo1DArray(dm2x3Mult.toArray());
		assertArrayEquals(vecdblExpected, vecdblActual, DBL_DELTA);
	}

	private double[] convert2DArrayTo1DArray(double[][] adbl2D)
	{
		int nTotalLength = 0;
		for (int i = 0; i < adbl2D.length; i++)
		{
			double[] adblRow = adbl2D[i];
			nTotalLength += adblRow.length;
		}

		double[] adblResult = new double[nTotalLength];
		int nIndex = 0;

		for (int i = 0; i < adbl2D.length; i++)
		{
			double[] adblRow = adbl2D[i];

			for (int j = 0; j < adblRow.length; j++)
				adblResult[nIndex++] = adblRow[j];
		}

		return adblResult;
	}
}
