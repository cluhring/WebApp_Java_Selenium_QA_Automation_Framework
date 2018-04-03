package frameworkHelpers;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;

/**
 *
 * @author RKrahl
 *
 */
public class HelperClass {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@Test
	public void decryptUsingEnvVariableKey() throws Exception {

		String encryptedString = "H/2Y+VuB52pC32S3ZmqXwQ==";
		log.info("**> original encrypted string = \"" + encryptedString + "\"");

		// If the BC4_KEY is not set, log warning and skip setting db properties
		String bc4Key = System.getenv("BC4_KEY");
		if (bc4Key == null)
			Assert.fail("The BC4_KEY Environment Variable is not set, cannot decrypt.");

		byte[] newKeyByte = BASE64DecoderStream.decode(bc4Key.getBytes("UTF8"));
		SecretKey newkey = new SecretKeySpec(newKeyByte, "DESede");
		String decryptedString = decrypt(encryptedString, newkey);
		log.info("**> Decrypted String: \"" + decryptedString + "\"");
	}

	@Test
	public void encryptUsingEnvVariableKey() throws Exception {
		String inTheClear = "in the clear";

		log.info("**> original in the clear text = \"" + inTheClear + "\"");

		// If the BC4_KEY is not set, log warning and skip setting db properties
		String bc4Key = System.getenv("BC4_KEY");
		if (bc4Key == null)
			Assert.fail("The bc4Key BC4_KEY Environment Variable is not set !!!");

		byte[] newKeyByte = BASE64DecoderStream.decode(bc4Key.getBytes("UTF8"));
		SecretKey newkey = new SecretKeySpec(newKeyByte, "DESede");
		String encryptedString = encrypt(inTheClear, newkey);
		log.info("The inTheClear String \"" + inTheClear + "\" Encrypted is \"" + encryptedString + "\"");
	}

	@Test
	public void generateNewEncryptionKey() throws Exception {
		/***
		 * WARNING !!! Only Use this IF you plan to RE-KEY all your
		 * encryptions or you're building a new environment
		 ***/
		String newEncryptionKey = keyBuilder();
		log.info("**> the key is = \"" + newEncryptionKey + "\"");
		log.info("***** add this new key to your system environment variable, use to encrypt / decrypt sensitive properties *****");
	}

	/**
	   * Returns current date/time in yyyy-MM-dd-HH-mm-ss format
	   * @return
	   */
	  public static String timeStamp() {
		  return timeStamp("yyyy-MM-dd-HH-mm-ss");
	  }

	/**
	   * Returns current date/time in provided format
	   *
	   * Java date format string documentation:
	   * https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	   *
	   * @param format
	   * @return
	   */
	  public static String timeStamp(String format) {
		  String timeStampNow = new SimpleDateFormat(format).format(new Date());
		  return timeStampNow;
	  }

	/**
	 * Encrypted data is arbitrary binary data which contains "invalid"
	 * characters and is not valid UTF-8 data and cannot be converted
	 * conventionally. Therefore, encrypted data must first be converted to
	 * Base64.
	 */
	public static String decrypt(String encryptedString, SecretKey key) {

		// If error occurs during decrypt, return input.
		String decryptedString = encryptedString;
		Cipher dcipher;

		try {
			dcipher = Cipher.getInstance("DESede");
			dcipher.init(Cipher.DECRYPT_MODE, key);

			// decode with base64 to get bytes
			byte[] dec = BASE64DecoderStream.decode(encryptedString.getBytes("UTF8"));
			byte[] utf8 = dcipher.doFinal(dec);

			// create new string based on the specified charset
			decryptedString = new String(utf8, "UTF8");
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException
				| IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			Assert.fail("FAIL - Failed to encrypt input", e);
		}

		return decryptedString;
	}

	/**
	 * Encrypted data is arbitrary binary data which contains "invalid"
	 * characters and is not valid UTF-8 data and cannot be converted
	 * conventionally. Therefore, encrypted data must first be converted to
	 * Base64.
	 */
	public static String encrypt(String inTheClearText, SecretKey key) {

		String encryptedString = inTheClearText;
		Cipher ecipher;

		try {
			ecipher = Cipher.getInstance("DESede");
			ecipher.init(Cipher.ENCRYPT_MODE, key);

			// encode the string into a sequence of bytes using the named
			// charset
			// storing the result into a new byte array.
			byte[] utf8 = inTheClearText.getBytes("UTF8");
			byte[] enc = ecipher.doFinal(utf8);

			// encode to base64
			enc = BASE64EncoderStream.encode(enc);
			encryptedString = new String(enc, "UTF8");
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException
				| IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			Assert.fail("FAIL - Failed to decrypt input", e);
		}

		return encryptedString;
	}

	/**
	 * @return - Helper function returns an encrypted base64 String "base64Key"
	 *         to be stored as an environment variable to be used for encrypting
	 *         and decrypting. This "base64Key" key uses DESede with a key size
	 *         of 168 bits. DES (The Data Encryption Standard) is now out of
	 *         date. DESede is a triple variant of DES and increases the key
	 *         space which helps prevent brute force attacks. This value must be
	 *         converted into the actual SecretKey as follows:
	 *
	 *         byte[] newKeyByte = BASE64DecoderStream.decode(base64Key.getBytes("UTF8"));
	 *         key = new SecretKeySpec(newKeyByte, "DESede");
	 *
	 */
	public static String keyBuilder() {
		String newKey = "";
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
			keyGenerator.init(168);
			SecretKey secretKey = keyGenerator.generateKey();
			byte[] encodedKeyByteArray = secretKey.getEncoded();
			byte[] encodedKeyB64 = BASE64EncoderStream.encode(encodedKeyByteArray);
			newKey = new String(encodedKeyB64, "UTF8");
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			Assert.fail("ERROR: Exception building key", e);
		}

		return newKey;
	}

}
