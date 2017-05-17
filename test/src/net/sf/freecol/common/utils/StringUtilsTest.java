package net.sf.freecol.common.utils;

import static org.junit.Assert.*;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import net.sf.freecol.common.util.StringUtils;

public class StringUtilsTest{
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void joinTest1() {
		String delimiter = ";";
		String a = "Hello";
		String b = "World";
		assertEquals("Hello;World", StringUtils.join(delimiter, a, b));
	}

	@Test
	public void joinTest2(){
		String[] isNull = null;
		assertNull(StringUtils.join(";", isNull));
	}
	
	@Test
	public void joinTest3(){
		String[] isNull = new String[0];
		assertNull(StringUtils.join(";", isNull));
	}
	
	@Test
	public void joinTest4(){
		// populated list of strings
		String delimiter = ";";
		List<String> strings = new ArrayList<>();
		strings.add("Hello");
		strings.add("World");
		assertEquals("Hello;World", StringUtils.join(delimiter, strings));
	}
	
	@Test
	public void joinTest5(){
		// empty list of strings
		String delimiter = ";";
		List<String> isEmpty = new ArrayList<>();
		assertNull(StringUtils.join(delimiter, isEmpty));
	}
	
	@Test
	public void joinTest6(){
		assertEquals("Hello", StringUtils.join(null, "Hello"));
	}

	@Test(expected=NullPointerException.class)
	public void joinTest7(){
		StringUtils.join(null, null, "Hello");
	}
	
	@Test
	public void chopTest1(){
		String longString = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		assertEquals("AAAAA", StringUtils.chop(longString, 5));
	}
	
	@Test
	public void chopTest2(){
		String longString = "HELLO";
		assertEquals("HELLO", StringUtils.chop(longString, 6));
	}
	
	@Test(expected=NullPointerException.class)
	public void chopTest3(){
		StringUtils.chop(null, 6);
	}
	
	@Test
	public void lastPartTest1(){
		String delimiter = ";";
		assertEquals("WORLD", StringUtils.lastPart("Hello;WORLD", delimiter));
	}
	
	@Test
	public void lastPartTest2(){
		String delimiter = ";";
		assertEquals("", StringUtils.lastPart("Hello;", delimiter));
	}
	
	@Test
	public void lastPartTest3(){
		String delimiter = ";";
		assertNull(StringUtils.lastPart(null, delimiter));
	}
	
	@Test(expected=NullPointerException.class)
	public void lastPartTest4(){
		StringUtils.lastPart("Hello", null);
	}
	
	@Test
	public void lastPartTest5(){
		assertNull(StringUtils.lastPart(null, null));
	}
	
	@Test
	public void lastPartTest6(){
		assertEquals("", StringUtils.lastPart("Hello", ""));
	}
	
	@Test
	public void lastPartTest7(){
		assertEquals("Hello", StringUtils.lastPart("Hello", "world"));
	}
	
	private enum Words{
		HELLO_WORLD, WORLD_HELLO, HELLO
	}
	
	@Test
	public void getEnumKeyTest1(){
		assertEquals("helloWorld", StringUtils.getEnumKey(Words.HELLO_WORLD));
	}
	
	@Test
	public void getEnumKeyTest2(){
		assertEquals("worldHello", StringUtils.getEnumKey(Words.WORLD_HELLO));
	}
	
	@Test
	public void getEnumKeyTest3(){
		assertEquals("hello", StringUtils.getEnumKey(Words.HELLO));
	}
	
	@Test(expected=NullPointerException.class)
	public void getEnumKeyTest4(){
		StringUtils.getEnumKey(null);
	}
	
	@Test
	public void getBreakingPointTest1(){
		assertEquals(6, StringUtils.getBreakingPoint("LONG CAT IS LONG", "A"));
	}
	
	@Test
	public void getBreakingPointTest2(){
		assertEquals(7, StringUtils.getBreakingPoint("Working on Stuff"));
	}
	
	@Test(expected=NullPointerException.class)
	public void getBreakingPointTest3(){
		StringUtils.getBreakingPoint(null);
	}
	
	@Test(expected=NullPointerException.class)
	public void getBreakingPointTest4(){
		StringUtils.getBreakingPoint(null, "k");
	}
	
	@Test(expected=NullPointerException.class)
	public void getBreakingPointTest5(){
		StringUtils.getBreakingPoint(null, null);
	}
	
	@Test
	public void getBreakingPointTest6(){
		assertEquals(-1, StringUtils.getBreakingPoint("Working"));
	}
	
	@Test
	public void getBreakingPointTest7(){
		assertEquals(-1, StringUtils.getBreakingPoint(""));
	}
	
	@Test
	public void getBreakingPointTest8(){
		assertEquals(-1, StringUtils.getBreakingPoint("Short"));
	}
	
	@Test(expected=NullPointerException.class)
	public void getBreakingPointTest9(){
		StringUtils.getBreakingPoint("LONG CAT IS LONG", null);
	}
	
	@Test
	public void getBreakingPointTest10(){
		assertEquals(-1, StringUtils.getBreakingPoint(" ", " "));
	}
	
	@Test
	public void getBreakingPointTest11(){
		assertEquals(-1, StringUtils.getBreakingPoint("", ""));
	}
	
	@Test
	public void getBreakingPointTest12(){
		assertEquals(-1, StringUtils.getBreakingPoint("LONG CAT IS LONG", ""));
	}
	
	@Test
	public void getBreakingPointTest13(){
		assertEquals(8, StringUtils.getBreakingPoint("LONG CAT IS LONG"));
	}
	
	@Test
	public void getBreakingPointTest14(){
		assertEquals(5, StringUtils.getBreakingPoint("LONG CAT IS LONG", "COOL"));
	}
	
	@Test
	public void getBreakingPointTest15(){
		assertEquals(4, StringUtils.getBreakingPoint("LONGCATISLONG", "COOL"));
	}
	
	@Test
	public void getBreakingPointTest17(){
		assertEquals(-1, StringUtils.getBreakingPoint("LONGCATISLONG", ""));
	}
	
	@Test
	public void getBreakingPointTest18(){
		assertEquals(-1, StringUtils.getBreakingPoint("LONGCATISLONG"));
	}
	
	@Test
	public void getBreakingPointTest19(){
		assertEquals(6, StringUtils.getBreakingPoint("LONGCATISLONG", "CAT"));
	}
	
	@Test
	public void getBreakingPointTest20(){
		assertEquals(7, StringUtils.getBreakingPoint("LONG CAT IS LONG", "CAT"));
	}
	
	@Test
	public void splitTextTest11(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		List<String> expectedList = new ArrayList<>();
		expectedList.add("ThisIs");
		expectedList.add("VeryLongWord");
		String expected = expectedList.get(0) + expectedList.get(1);
		
		List<String> actualList = StringUtils.splitText("ThisIsAVeryLongWord", "A", fontMetrics, 15);
		String actual = actualList.get(0) + actualList.get(1);
		
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void splitTextTest12(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		List<String> expectedList = new ArrayList<>();
		expectedList.add("ThisIsAVeryLongWord");
		String expected = expectedList.get(0);
		
		List<String> actualList = StringUtils.splitText("ThisIsAVeryLongWord", "", fontMetrics, 15);
		String actual = actualList.get(0);
		
		
		assertEquals(expected, actual);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest13(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		StringUtils.splitText(null, "a", fontMetrics, 15);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest14(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		StringUtils.splitText(null, "", fontMetrics, 15);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest15(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		StringUtils.splitText(null, null, fontMetrics, 15);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest16(){
		StringUtils.splitText("A word", "w", null, 15);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest17(){
		StringUtils.splitText(null, null, null, 15);
	}
	
	@Test(expected=NullPointerException.class)
	public void splitTextTest18(){
		Font font = new Font("Helvetica", Font.PLAIN, 12);
		java.awt.Canvas canvas = new java.awt.Canvas();
		FontMetrics fontMetrics = canvas.getFontMetrics(font);
		
		StringUtils.splitText("Looooooooong cat", null, fontMetrics, 15);
	}
}
