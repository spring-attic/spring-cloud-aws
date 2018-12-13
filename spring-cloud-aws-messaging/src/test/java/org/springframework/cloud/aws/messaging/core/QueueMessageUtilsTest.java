package org.springframework.cloud.aws.messaging.core;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Kevin Hwang
 * @since 2.1.0
 */
public class QueueMessageUtilsTest {

	@Test(expected = IllegalArgumentException.class)
	public void getNumberValue_invalidAttributeType_1_throwsIllegalArgumentException() {
		QueueMessageUtils.getNumberValue("abc", "123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getNumberValue_invalidAttributeType_2_throwsIllegalArgumentException() {
		QueueMessageUtils.getNumberValue("int", "123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getNumberValue_invalidAttributeType_3_throwsIllegalArgumentException() {
		QueueMessageUtils.getNumberValue("Integer", "123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getNumberValue_invalidAttributeType_4_throwsIllegalArgumentException() {
		QueueMessageUtils.getNumberValue("Numberint", "123");
	}

	@Test
	public void getNumberValue_Number_withStringOfInteger_returnsBigDecimalOfInteger() {
		assertEquals(BigDecimal.valueOf(123), QueueMessageUtils.getNumberValue("Number", "123"));
	}

	@Test
	public void getNumberValue_byte() {
		assertEquals((byte) 127, QueueMessageUtils.getNumberValue("Number.byte", "127"));
		assertEquals((byte) -128, QueueMessageUtils.getNumberValue("Number.Byte", "-128"));
	}

	@Test
	public void getNumberValue_short() {
		assertEquals((short) 32767, QueueMessageUtils.getNumberValue("Number.short", "32767"));
		assertEquals((short) -32768, QueueMessageUtils.getNumberValue("Number.Short", "-32768"));
	}

	@Test
	public void getNumberValue_integer() {
		assertEquals(2147483647, QueueMessageUtils.getNumberValue("Number.int", "2147483647"));
		assertEquals(-2147483648, QueueMessageUtils.getNumberValue("Number.Integer", "-2147483648"));
	}

	@Test
	public void getNumberValue_long() {
		assertEquals(9223372036854775807L, QueueMessageUtils.getNumberValue("Number.long", "9223372036854775807"));
		assertEquals(-9223372036854775808L, QueueMessageUtils.getNumberValue("Number.Long", "-9223372036854775808"));
	}

	@Test
	public void getNumberValue_float() {
		assertEquals(123.456f, QueueMessageUtils.getNumberValue("Number.float", "123.456"));
		assertEquals(-123.456f, QueueMessageUtils.getNumberValue("Number.Float", "-123.456"));
	}

	@Test
	public void getNumberValue_double() {
		assertEquals(123456.789012d, QueueMessageUtils.getNumberValue("Number.double", "123456.789012"));
		assertEquals(123456.789012d, QueueMessageUtils.getNumberValue("Number.Double", "123456.789012"));
	}

	@Test
	public void getNumberValue_BigInteger() {
		assertEquals(new BigInteger("9223372036854775808"), QueueMessageUtils.getNumberValue("Number.BigInteger", "9223372036854775808"));
	}

	@Test
	public void getNumberValue_BigDecimal() {
		assertEquals(new BigDecimal("9223372036854775808.000218239021809329"), QueueMessageUtils.getNumberValue("Number.BigDecimal", "9223372036854775808.000218239021809329"));
	}
}
