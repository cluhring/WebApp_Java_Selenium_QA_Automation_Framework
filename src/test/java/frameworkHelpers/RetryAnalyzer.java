package frameworkHelpers;

import org.apache.commons.lang.reflect.FieldUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retry Analyzer to allow for re-execution of failed tests
 *
 * Initial code obtained from:
 * https://www.seleniumeasy.com/testng-tutorials/execute-only-failed-test-cases-using-iretryanalyzer
 *
 * @author Chris Nye *
 *
 */
public class RetryAnalyzer implements IRetryAnalyzer {
	private int retryCount = 0;
	private int maxRetryCount = 0;	// Default to no retries

	// Below method returns 'true' if the test method has to be retried else
	// 'false'
	// and it takes the 'Result' as parameter of the test method that just ran
	public boolean retry(ITestResult result) {

		if (isRetryAvailable(result)) {
			retryCount++;
			System.out.println("Retrying test " + result.getName() + " with status "
					+ getResultStatusName(result.getStatus()) + " for the " + retryCount + " of " + maxRetryCount + " time(s).");

			result.setStatus(ITestResult.SKIP);
      Throwable cause = result.getThrowable();
      if ( cause != null ) {
      	String origMessage = cause.getMessage();
      	String newMessage = "RETRY " + retryCount + " of " + maxRetryCount + "\n" + origMessage;
      	try {
      		FieldUtils.writeField(cause, "detailMessage", newMessage, true);
      	} catch (Exception e) {
      		e.printStackTrace();
      	}
      }
			return true;
		}
		return false;
	}

	public boolean isRetryAvailable(ITestResult result) {

		String retryParam = result.getMethod().getXmlTest().getSuite().getParameter("retryCount");

		if ( ! (retryParam == null ) ) {
			try {
				maxRetryCount = Integer.parseInt(retryParam);
			} catch (NumberFormatException e) {
				maxRetryCount = 0;
			}
		}
		if (retryCount < maxRetryCount) {
			return true;
		}
		return false;
	}

	public String getResultStatusName(int status) {
		String resultName = null;
		if (status == 1)
			resultName = "SUCCESS";
		if (status == 2)
			resultName = "FAILURE";
		if (status == 3)
			resultName = "SKIP";
		return resultName;
	}
}
