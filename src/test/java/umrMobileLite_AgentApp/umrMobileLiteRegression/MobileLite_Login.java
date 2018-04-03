package umrMobileLite_AgentApp.umrMobileLiteRegression;

import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;
import org.testng.Assert;

import umrMobileLite_AgentApp.umrMobileLiteBaseTest;

public class MobileLite_Login extends umrMobileLiteBaseTest {

	@Test
	public void tc001_Login_to_Mobile_Lite() {
		/**
		 * US: Login to Mobile Lite Web Application
		 *
		 * Acceptance Criteria:
		 * 		1. User can log into application
		 *
		 * @author Chris Luhring
		 */

		log.info("Starting test - tc001_Login_to_Mobile_Lite");

		RemoteWebDriver driver = getWebDriver();
	    // [Step 1]: Navigate to Mobile Lite desktop. Login displays
	    driver.get(mLUrl);
	    // [Step 2]: Login with test Agent account. Dashboard displays.
		userLogin("Cluhring", "Test5day");
	}

	@Test(groups="singleThread")
	public void tc002_Invalid_Login_Capture() {
		/**
		 * US: Invalid Login Capture
		 *
		 * Acceptance Criteria:
		 * 		1. When a user attempts to login with a username or password that is not correct the message
		 * 			that is displayed should read as follows:  "The username or password you have entered does
		 * 			not match our records. Please try again."
		 *
		 * @author Chris Luhring
		 */

		log.info("Starting test - tc002_Invalid_Login_Capture");

		String expectErrorMessage = "The username/password combination you have entered does not match our records. Please try again.";

		// 1. Navigate to Agent Desktop login page
		RemoteWebDriver driver = getWebDriver();

		driver.get(mLUrl);

		// 2. Enter invalid credentials
		driver.findElement(getMobileLiteElement("login.username.input")).clear();
		driver.findElement(getMobileLiteElement("login.username.input")).sendKeys("badusername");
		driver.findElement(getMobileLiteElement("login.password.input")).clear();
		driver.findElement(getMobileLiteElement("login.password.input")).sendKeys("WrongPassword");
		driver.findElement(getMobileLiteElement("login.login.button")).click();

		// Wait for element (shortTimeout)
		Assert.assertTrue(waitForElement(getMobileLiteElement("login.error.message"), shortTimeout),
				"FAIL - Did not find errormessage element id within timeout period");

		// 3. Confirm the following message displays:
		// "The username or password you have entered does not match our records. Please try again."
		Assert.assertEquals(driver.findElement(getMobileLiteElement("login.error.message")).getText(), expectErrorMessage,
				"FAIL - Did not find expected error message when user enters wrong password");
		log.info("PASS - Correct error message is displayed when user enters wrong password");

		log.info("PASSED - tc002_Invalid_Login_Capture");
	}

	@Test
	public void tc003_Login_to_Mobile_Lite() {
		/**
		 * US: Login to Mobile Lite Web Application
		 *
		 * Acceptance Criteria:
		 * 		1. User can log into application
		 *
		 * @author Chris Luhring
		 */

		log.info("Starting test - tc003_Login_to_Mobile_Lite");

		RemoteWebDriver driver = getWebDriver();
	    // [Step 1]: Navigate to Mobile Lite desktop. Login displays
	    driver.get(mLUrl);
	    // [Step 2]: Login with test Agent account. Dashboard displays.
		userLogin("Cluhring", "Test5day");
	}

	@Test
	public void tc004_Login_to_Mobile_Lite() {
		/**
		 * US: Login to Mobile Lite Web Application
		 *
		 * Acceptance Criteria:
		 * 		1. User can log into application
		 *
		 * @author Chris Luhring
		 */

		log.info("Starting test - tc004_Login_to_Mobile_Lite");

		RemoteWebDriver driver = getWebDriver();
	    // [Step 1]: Navigate to Mobile Lite desktop. Login displays
	    driver.get(mLUrl);
	    // [Step 2]: Login with test Agent account. Dashboard displays.
		userLogin("Cluhring", "Test5day");
	}

}
