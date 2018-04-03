package umrMobileLite_AgentApp.umrMobileLiteSmoke;

import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;
import org.testng.Assert;

import umrMobileLite_AgentApp.umrMobileLiteBaseTest;

public class MobileLite_Login extends umrMobileLiteBaseTest {

	@Test
	public void tc005_Login_to_Mobile_Lite() {
		/**
		 * US: Login to Mobile Lite Web Application
		 *
		 * Acceptance Criteria:
		 * 		1. User can log into application
		 *
		 * @author Chris Luhring
		 */

		log.info("Starting test - tc005_Login_to_Mobile_Lite");

		RemoteWebDriver driver = getWebDriver();
		// [Step 1]: Navigate to Mobile Lite desktop. Login displays
		driver.get(mLUrl);
		// [Step 2]: Login with test Agent account. Dashboard displays.
		userLogin("Cluhring", "Test5day");
	}
}
