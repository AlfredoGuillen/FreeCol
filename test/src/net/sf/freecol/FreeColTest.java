package net.sf.freecol;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.freecol.common.model.NationOptions;

/** 
 * This test class checks getters and setters for FreeCol.java after the refactoring of classes in the net.sf.freecol
 *
 */
public class FreeColTest
{
	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	/**
	 * Tests if difficulty was correctly set with a valid string.
	 */
	public void testDifficulty()
	{
		String hardMode = "HARD";
		FreeCol.setDifficulty(hardMode);
		assertEquals(FreeCol.getDifficulty(), hardMode);
		
	}
	
	@Test
	/**
	 * Tests if difficulty does not return a nullable after setDifficulty was passed a null.
	 */
	public void testDifficultyNull()
	{
		String hardMode = null;
		FreeCol.setDifficulty(hardMode);
		assertNotNull(FreeCol.getDifficulty());
		
	}
	
	@Test
	/**
	 * Tests if NONE advantage is correctly set.
	 */
	public void testAdvantagesNONE()
	{
		FreeCol.setAdvantages(NationOptions.Advantages.NONE);
		assertEquals(FreeCol.getAdvantages(), NationOptions.Advantages.NONE);
	}
	
	@Test
	/**
	 * Tests if FIXED advantage is correctly set.
	 */
	public void testAdvantagesFIXED()
	{
		FreeCol.setAdvantages(NationOptions.Advantages.FIXED);
		assertEquals(FreeCol.getAdvantages(), NationOptions.Advantages.FIXED);
	}
	
	@Test
	/**
	 * Tests if SELECTABLE advantage is correctly set.
	 */
	public void testAdvantagesSELECTABLE()
	{
		FreeCol.setAdvantages(NationOptions.Advantages.SELECTABLE);
		assertEquals(FreeCol.getAdvantages(), NationOptions.Advantages.SELECTABLE);
	}
	
	@Test
	/**
	 * Tests if name was set correctly with a valid string.
	 */
	public void testName()
	{
		String name = "bonnie";
		FreeCol.setName(name);
		assertEquals(FreeCol.getName(), name);
	}
	
	@Test
	/**
	 * Tests if name does not return a nullable after setName was passed a null.
	 */
	public void testNameNull()
	{
		String name = null;
		FreeCol.setName(name);
		assertNotNull(FreeCol.getName());
	}
	
	@Test
	/**
	 * Tests if European count was set successfully.
	 */
	public void testEuropeanCount()
	{
		int count = 5;
		FreeCol.setEuropeanCount(count);
		assertEquals(FreeCol.getEuropeanCount(), count);
	}
	
	@Test
	/**
	 * Tests if total conversion was set successfully.
	 */
	public void testTotalConversion()
	{
		String totalConversion = "AlteredBeast";
		FreeCol.setTC(totalConversion);
		assertEquals(FreeCol.getTC(), totalConversion);
	}
	
	@Test
	/**
	 * Tests if timeout was set successfully.
	 */
	public void testTimeout()
	{
		String timeout = "30000";
		int itimeout = 30000;
		FreeCol.setTimeout(timeout);
		assertEquals(FreeCol.getTimeout(true), itimeout);
	}
	
	@Test
	/**
	 * Tests if server port was set successfully.
	 */
	public void testServerPort()
	{
		String port = "5000";
		int iport = 5000;
		FreeCol.setServerPort(port);
		assertEquals(FreeCol.getServerPort(), iport);
	}
}
