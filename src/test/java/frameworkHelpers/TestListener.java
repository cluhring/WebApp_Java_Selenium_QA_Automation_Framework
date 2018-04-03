package frameworkHelpers;

import java.util.Set;

import org.apache.commons.lang.reflect.FieldUtils;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;

/**
 * Test to allow for remove duplication of pass/fail results after re-execution of failed tests
 *
 * Initial code obtained from:
 * https://www.seleniumeasy.com/testng-tutorials/retry-listener-failed-tests-count-update
 *
 * @author Chris Nye *
 *
 */
public class TestListener implements ITestListener {
	@Override
	public void onFinish(ITestContext context) {

		/*TODO leave skips in report?	*/
		Set<ITestResult> skippedTests = context.getSkippedTests().getAllResults();
		for (ITestResult temp : skippedTests) {
			ITestNGMethod method = temp.getMethod();
			// Removed skipped result if method has more than 1 skip (other skip exists), or if a pass/fail exists for method
			if (context.getSkippedTests().getResults(method).size() > 1) {
				skippedTests.remove(temp);
			} else if (context.getPassedTests().getResults(method).size() > 0) {
					skippedTests.remove(temp);
			} else if (context.getFailedTests().getResults(method).size() > 0) {
				skippedTests.remove(temp);
			}
		}
/**/

		Set<ITestResult> failedTests = context.getFailedTests().getAllResults();
		// Removed fail result if method has more than 1 fail (other failure exists), or if a pass exists for method
		for (ITestResult temp : failedTests) {
			ITestNGMethod method = temp.getMethod();
			if (context.getFailedTests().getResults(method).size() > 1) {
				failedTests.remove(temp);
			} else {
				if (context.getPassedTests().getResults(method).size() > 0) {
					failedTests.remove(temp);
				}
			}
		}
	}

	public void onTestStart(ITestResult result) {   }

	public void onTestSuccess(ITestResult result) {   }

	@Override
  public void onTestFailure(ITestResult result) {
      if (result.getMethod().getRetryAnalyzer() != null) {
          RetryAnalyzer retryAnalyzer = (RetryAnalyzer)result.getMethod().getRetryAnalyzer();

          if(retryAnalyzer.isRetryAvailable(result)) {
          	// When retries are available, update result to "SKIP" and
          	// change message to include that test will be retried, but include
          	// original failure message.
          	result.setStatus(ITestResult.SKIP);
            Throwable cause = result.getThrowable();
            if ( cause != null ) {
            	String origMessage = cause.getMessage();
            	String newMessage = "SKIPPING TO RETRY TEST: " + result.getName() + "\n" + origMessage;
            	try {
            		FieldUtils.writeField(cause, "detailMessage", newMessage, true);
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }


          } else {
              result.setStatus(ITestResult.FAILURE);
          }
          Reporter.setCurrentTestResult(result);
      }
  }

	public void onTestSkipped(ITestResult result) {   }

	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {   }

	public void onStart(ITestContext context) {   }
}
