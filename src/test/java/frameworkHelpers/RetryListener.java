package frameworkHelpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import org.testng.IRetryAnalyzer;

/**
 * Retry listener to allow for re-execution of failed tests
 *
 * Initial code obtained from:
 * https://www.seleniumeasy.com/testng-tutorials/execute-only-failed-test-cases-using-iretryanalyzer
 *
 * @author Chris Nye
 *
 */
public class RetryListener implements IAnnotationTransformer {

	@SuppressWarnings("rawtypes")
	@Override
	public void transform(ITestAnnotation testannotation, Class testClass,
			Constructor testConstructor, Method testMethod)	{
		IRetryAnalyzer retry = testannotation.getRetryAnalyzer();

		if (retry == null)	{
			testannotation.setRetryAnalyzer(RetryAnalyzer.class);
		}

	}
}
