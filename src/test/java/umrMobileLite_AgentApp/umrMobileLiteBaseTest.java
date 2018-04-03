package umrMobileLite_AgentApp;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.testng.SauceOnDemandAuthenticationProvider;
import com.sun.mail.util.BASE64DecoderStream;

import frameworkHelpers.HelperClass;
import frameworkHelpers.ResourcePool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.HttpClient.Factory;
import org.openqa.selenium.remote.internal.ApacheHttpClient;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/*************************************************************************
 * Abstract class providing helper utilities for Optum Market Test Cases
 *************************************************************************/
public abstract class umrMobileLiteBaseTest implements SauceOnDemandSessionIdProvider, SauceOnDemandAuthenticationProvider {

	protected static SecretKey key;
	protected static String mLUrl;
	protected static String chatUrl;
	protected static String fedChatUrl;
	protected static String retentionChatUrl;
	protected static String dstChatUrl;
	protected static String phoneStateUrl;
	protected static String profile;
	protected ThreadLocal<RemoteWebDriver> threadDriver = new ThreadLocal<RemoteWebDriver>();
	protected static ResourcePool<RemoteWebDriver> driverPool = new ResourcePool<RemoteWebDriver>();
	protected static String bc4DbServer;
	protected static String bc4DbName;
	protected static String bc4DbUserName;
	protected static String bc4DbPassword;
	protected static String bc4TunnelHost;
	protected int localPort = 13306;
	protected static int dbServerPort;
	protected int shortTimeout = 15; // in seconds
	protected int longTimeout = 60; // in seconds
	// LogoutWait is the timeout duration in which the app ends the session and navigates to login
	// As of July 11, 2016 it is 25 minutes.
	protected int LogoutWait = 25 * 60000; // in milliseconds
	//LogoutNotifWait is the time in which no activity triggers notification window
	//  Should be LogoutWait minus 2 minutes
	protected int LogoutNotifWait = LogoutWait - 2 * 60000; // in milliseconds
	protected Logger log = LoggerFactory.getLogger(getClass());
	protected static Properties elementMap;

	protected ThreadLocal<String> userAgentName = new ThreadLocal<String>();
	protected ThreadLocal<String> userMedicaidAgent = new ThreadLocal<String>();
	protected ThreadLocal<String> userRetailName = new ThreadLocal<String>();

	// Values to be initialized in setupSuite using profile values
	protected static ResourcePool<String> adminUserPool = null;
	protected static ResourcePool<String> medicaidUserPool = null;
	protected static ResourcePool<String> retailUserPool = null;

	protected static String agentPassword = "";

	protected static Session session = null;
	protected static String dbUrl = "";

	// Saucelab variables
	private ThreadLocal<String> sessionId = new ThreadLocal<String>();
	SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();

	@BeforeSuite(alwaysRun=true)
	public void setupSuite(ITestContext testContext) {

		// profile = "dev", "qa", "uat", "stage", or "prod"
		profile = System.getenv("SPRING_PROFILES_BC4");
		if (profile == null)
			profile = "qa";

		log.info("INFO - Using profile: " + profile);

		// Load element Map properties
		elementMap = new java.util.Properties();
		//InputStream in = getClass().getResourceAsStream("/umrMobileLite_ElementMap.properties");
		InputStream in = getClass().getResourceAsStream("/umrMobileLite_ElementMap.properties");
		try {
			elementMap.load(in);
			in.close();
		} catch (IOException e) {
			Assert.fail("FAIL - Unable to load umrMobileLite_ElementMap.properties.properties", e);

		}

		// Load profile properties
		Properties bc4Properties = new java.util.Properties();
		try {
			//InputStream in2 = getClass().getResourceAsStream("/umrMobileLite.properties");
			InputStream in2 = getClass().getResourceAsStream("/umrMobileLite.properties");
			bc4Properties.load(in2);
			in2.close();
		} catch (IOException e1) {
			Assert.fail("FAIL - Unable to load umrMobileLite.properties", e1);
		}

		// Validate that SPRING_PROFILES_BC4 indicates a property block that is available.
		// configure url
		mLUrl = bc4Properties.getProperty("umr.url." + profile);
		Assert.assertNotNull(mLUrl,
				"FAIL - no mLUrl in properties for profile [" + profile + "]");
		log.info("INFO - Using url: " + mLUrl);

		// configure Chat URL
		chatUrl = bc4Properties.getProperty("umrchat." + profile);
		log.info("INFO - Using Chat URL: " + chatUrl);

		// configure DST (Spanish) Chat URL
		dstChatUrl = bc4Properties.getProperty("umr.dstChatUrl." + profile);
		log.info("INFO - Using DST Chat URL: " + dstChatUrl);

		// configure Fed Chat URL
		fedChatUrl = bc4Properties.getProperty("umr.fedChatUrl." + profile);
		log.info("INFO - Using Fed Chat URL: " + fedChatUrl);

		// configure Retention Chat URL
		retentionChatUrl = bc4Properties.getProperty("umr.retentionChatUrl." + profile);
		log.info("INFO - Using retention Chat URL: " + retentionChatUrl);

		// configure Phone State URL
		phoneStateUrl = bc4Properties.getProperty("umr.phoneStateUrl." + profile);
		log.info("INFO - Using Phone State URL: " + phoneStateUrl);


		// If the BC4_KEY is not set, throw failure, won't be able to decrypt usernames/passwords
		String bc4Key = System.getenv("BC4_KEY");
		if (bc4Key == null) {
			log.error("FAIL - The BC4_KEY Environment Variable Is NOT Set.  Exiting test.");
			Assert.fail("FAIL - The BC4_KEY Environment Variable Is NOT Set.  Exiting test.");
		}

		byte[] newKeyByte;
		try {
			newKeyByte = BASE64DecoderStream.decode(bc4Key.getBytes("UTF8"));
			key = new SecretKeySpec(newKeyByte, "DESede");
		} catch (UnsupportedEncodingException e1) {
			Assert.fail("FAIL - Exception setting decryption key", e1);
		}

		// Set password based on profile
		String agentPasswordEncrypted = bc4Properties.getProperty("umr.agentPassword." + profile);
		agentPassword = HelperClass.decrypt(agentPasswordEncrypted, key);

		/*
		 * Set username pools based on environment variables OR properties based on profile
		 *
		 * For each pool, check whether associated environment variable is set. If not, read
		 * list of users from properties file for appropriate value of 'profile'.  If variable,
		 * is set, split that variable on commas and set that to the list of users in the pool.
		 *
		 * @author Chris Nye
		 * Reviewed by: Team
		 */
		if (System.getenv("BC4_AGENTNAME") == null || System.getenv("BC4_AGENTNAME").trim().equals("")) {
			// Ensure bc4.adminUserPool.profile key is defined in bc4Properties
			Assert.assertNotNull(bc4Properties.getProperty("umr.adminUserPool." + profile),
					"ERROR: property [bc4.adminUserPool." + profile +"] does not exist in bc4Properties\n");
			adminUserPool = new ResourcePool<String>(
					Arrays.asList(bc4Properties.getProperty("umr.adminUserPool." + profile).split(",")));
		}else {
			adminUserPool = new ResourcePool<String>(
					Arrays.asList(System.getenv("BC4_AGENTNAME").split(" *, *")));
		}

		if (System.getenv("BC4_MEDICAIDAGENTNAME") == null || System.getenv("BC4_MEDICAIDAGENTNAME").trim().equals("")) {
			// Ensure bc4.medicaidUserPool.profile key is defined in bc4Properties
			Assert.assertNotNull(bc4Properties.getProperty("umr.medicaidUserPool." + profile),
					"ERROR: property [bc4.medicaidUserPool." + profile +"] does not exist in bc4Properties\n");
			medicaidUserPool = new ResourcePool<String>(
					Arrays.asList(bc4Properties.getProperty("umr.medicaidUserPool." + profile).split(",")));
		} else {
			medicaidUserPool = new ResourcePool<String>(
					Arrays.asList(System.getenv("BC4_MEDICAIDAGENTNAME").split(" *, *")));
		}

		if (System.getenv("BC4_RETAILAGENTNAME") == null || System.getenv("BC4_RETAILAGENTNAME").trim().equals("")) {
			// Ensure bc4.retailUserPool.profile key is defined in bc4Properties
			Assert.assertNotNull(bc4Properties.getProperty("umr.retailUserPool." + profile),
					"ERROR: property [bc4.retailUserPool." + profile +"] does not exist in bc4Properties\n");
			retailUserPool = new ResourcePool<String>(
					Arrays.asList(bc4Properties.getProperty("umr.retailUserPool." + profile).split(",")));
		} else {
			retailUserPool = new ResourcePool<String>(
					Arrays.asList(System.getenv("BC4_RETAILAGENTNAME").split(" *, *"))) ;
		}

		log.info("=============");
		log.info("  DEBUG: adminUserPool has " + adminUserPool.size() + " accounts: " + adminUserPool.toString());
		log.info("  DEBUG: medicaidUserPool has " + medicaidUserPool.size() + " accounts: " + medicaidUserPool.toString());
		log.info("  DEBUG: retailUserPool has " + retailUserPool.size() + " accounts: " + retailUserPool.toString());
		log.info("=============");

		// Verify that the count of users in the pools is >= the number of threads
		// used for parallel execution
		int suiteThreadCount = testContext.getSuite().getXmlSuite().getThreadCount();
		String suiteParallel = testContext.getSuite().getXmlSuite().getParallel();

		log.info("  DEBUG: suiteThreadCount: " + suiteThreadCount + "  ;  suiteParallel: " + suiteParallel);
		// If running single-threaded, treat threadCount as 1
		if ( suiteParallel.equals("false") ) {
			suiteThreadCount = 1;
		}
		Assert.assertTrue(adminUserPool.size() >= suiteThreadCount, "ERROR: Insufficient admin user count [" + adminUserPool.size()
		+ "] for suite thread count [" + suiteThreadCount + "].\n  AdminUserPool: "
		+ adminUserPool.toString() + "\n");
		Assert.assertTrue(medicaidUserPool.size() >= suiteThreadCount, "ERROR: Insufficient medicaid user count [" + medicaidUserPool.size()
		+ "] for suite thread count [" + suiteThreadCount + "].\n  MedicaidUserPool: "
		+ medicaidUserPool.toString() + "\n");
		Assert.assertTrue(retailUserPool.size() >= suiteThreadCount, "ERROR: Insufficient retail user count [" + retailUserPool.size()
		+ "] for suite thread count [" + suiteThreadCount + "].\n  RetailUserPool: "
		+ retailUserPool.toString() + "\n");

		// configure database
		bc4DbServer = bc4Properties.getProperty("umr.db_server." + profile);
		if (bc4DbServer == null) {
			log.info("MySQL DB Server NOT Setup in optumbc4.properties.  Defaulting to null");

		} else {
			// If the bc4DbServer is NOT null, proceed and attempt to set db properties

			log.info("INFO - Using MySQL DB Server: " + bc4DbServer);

			// Set the database properties
			String bc4DbServerPort = bc4Properties.getProperty("umr.db_server_port." + profile);
			try {
				dbServerPort = Integer.parseInt(bc4DbServerPort);
				log.info("INFO - Using MySQL DB Port: " + dbServerPort);
			} catch (Exception e) {
				Assert.fail("dbServerPort is not set in the optumbc4.properties file !!!");
			}
			bc4DbName = bc4Properties.getProperty("umr.db_name." + profile);
			log.info("INFO - Using MySQL DB Name: " + bc4DbName);

			// Get the encrypted username and password
			String encryptedDbUserName = bc4Properties.getProperty("umr.db_username." + profile);
			String encryptedDbPassword = bc4Properties.getProperty("umr.db_password." + profile);

			bc4DbUserName = HelperClass.decrypt(encryptedDbUserName, key);
			bc4DbPassword = HelperClass.decrypt(encryptedDbPassword, key);

		}
	}

	@BeforeMethod(alwaysRun=true)
	@Parameters({ "browser", "sauceLabExecution" })
	public void beforeMethod(@Optional("firefox") String browser, @Optional("false") Boolean sauceLabExecution, Method context) {

		userAgentName.set(adminUserPool.getResource());
		userMedicaidAgent.set(medicaidUserPool.getResource());
		userRetailName.set(retailUserPool.getResource());

		log.info("INFO - Testing with browser " + browser);

		if (sauceLabExecution) {
			threadDriver.set(createWebDriverSauce(browser, context));
		} else {
			threadDriver.set(createWebDriverLocal(browser));
		}
		log.info("PASS - Created driver for browser " + browser);

		sessionId.set(((RemoteWebDriver) getWebDriver()).getSessionId().toString());

		//threadDriver.get().manage().window().maximize();
		// Enforce 1024 x 768 browser size to match requirements.
		threadDriver.get().manage().window().setSize(new Dimension(1280,800));
		threadDriver.get().manage().window().setPosition(new Point(0,0));
		threadDriver.get().manage().deleteAllCookies();

	}

	private RemoteWebDriver createWebDriverSauce(String browser, Method context) {

		log.info("INFO - Creating SauceLabs driver");

//		// Configure saucelabs authentication
//		String sauceUsername = System.getenv("SAUCE_USERNAME");
//		String sauceAccessKey = System.getenv("SAUCE_ACCESS_KEY");
//
//		String errorMsg = "";
//		if (sauceUsername == null) {
//			errorMsg += "   Missing value for SAUCE_USERNAME environment variable\n";
//		} else {
//			authentication.setUsername(sauceUsername);
//			// For purposes of generating the SauceLab URL, need to encode special characters.
//			// This should **not** be done in the authentication object, that requires exact match
//			// to populate results in sauceLabs dashboard.
//			if ( sauceUsername.contains("@")) {
//				sauceUsername = sauceUsername.replace("@", "%40");
//			}
//
//		}
//		if (sauceAccessKey == null) {
//			errorMsg += "   Missing value for SAUCE_ACCESS_KEY environment variable\n";
//			log.info(errorMsg);
//		} else {
//			authentication.setAccessKey(sauceAccessKey);
//		}
//
//		if (!errorMsg.contentEquals("")) {
//			Assert.fail("FAIL - Missing saucelabs authentication information:\n" + errorMsg);
//			log.info(errorMsg);
//		}

		DesiredCapabilities capabilities = new DesiredCapabilities();

		switch (browser) {
		case "firefox":
			capabilities = DesiredCapabilities.firefox();
			//TODO: newer versions have some behavior differences, to be investigated
			capabilities.setCapability("version", "54.0");
			capabilities.setCapability(CapabilityType.PLATFORM, "Windows 7");
			break;
		case "ie":
			capabilities = DesiredCapabilities.internetExplorer();
			capabilities.setCapability(CapabilityType.VERSION, "11");
			capabilities.setCapability(CapabilityType.PLATFORM, "Windows 7");
			break;
		case "chrome":
			capabilities = DesiredCapabilities.chrome();
			capabilities.setCapability(CapabilityType.PLATFORM, "Windows 7");
			break;
		case "safari":
			capabilities = DesiredCapabilities.safari();
			capabilities.setCapability(CapabilityType.PLATFORM, "OSX 10.8");
			capabilities.setCapability("screenResolution", "1280x1024");
			break;
		default:
			Assert.fail("FAIL - Invalid browser parameter passed");
		}

		capabilities.setCapability("name", profile + " " + browser + " " + context.getName() + ": "
				+ this.getClass().getSimpleName());

		// NOTE: Need to know what devices / resolutions we are testing
		// 1280x1024 is more forgiving.
		capabilities.setCapability("screenResolution", "1280x1024");

		RemoteWebDriver sauceDriver = null;
		
		sauceDriver = connectViaProxy(capabilities);
//		try {
//			sauceDriver = new RemoteWebDriver(new URL("http://" + sauceUsername + ":" + sauceAccessKey
//					+ "@ondemand.saucelabs.com:80/wd/hub"), capabilities);
//
//		} catch (MalformedURLException e) {
//			Assert.fail("FAIL - MalformedURLException when creating sauceLab driver", e);
//		}

		return sauceDriver;

	}

	public RemoteWebDriver connectViaProxy(DesiredCapabilities caps) {
		// connectViaProxy added by Chris L on 3/29/18 because of this issue:
		// https://stackoverflow.com/questions/33263006/using-selenium-remotewebdriver-behind-corporate-proxy-java
		// Solution found here:
		// https://stackoverflow.com/questions/34846014/using-selenium-remotewebdriver-behind-corporate-proxy/34908953#34908953
		String proxyHost = "usden-s690-02";
		int proxyPort = 8080;
		String proxyUserDomain = "teletech.com";
		String proxyUser = System.getenv("PROX_USER");
		String proxyPassword = System.getenv("PROX_PASS");

		// Configure saucelabs authentication
		String sauceUsername = System.getenv("SAUCE_USERNAME");
		String sauceAccessKey = System.getenv("SAUCE_ACCESS_KEY");

		String errorMessage = "";
		
		if (sauceUsername == null) {
			errorMessage += "   Missing value for SAUCE_USERNAME environment variable\n";
		} else {
			authentication.setUsername(sauceUsername);
			// For purposes of generating the SauceLab URL, need to encode special characters.
			// This should **not** be done in the authentication object, that requires exact match
			// to populate results in sauceLabs dashboard.
			if ( sauceUsername.contains("@")) {
				sauceUsername = sauceUsername.replace("@", "%40");
			}

		}

		if (sauceAccessKey == null) {
			errorMessage += "   Missing value for SAUCE_ACCESS_KEY environment variable\n";
		} else {
			authentication.setAccessKey(sauceAccessKey);
		}

		if (proxyUser == null || proxyPassword == null ) {
			errorMessage += "   Missing value for PROX_USER or PROX_PASS environment variable\n";
		}

		if (!errorMessage.contentEquals("")) {
			Assert.fail("FAIL - Missing saucelabs authentication information:\n" + errorMessage);
		}

		URL url;

		try {
			url = new URL("http://" + sauceUsername +":" + sauceAccessKey + "@ondemand.saucelabs.com:80/wd/hub");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		HttpClientBuilder builder = HttpClientBuilder.create();

		HttpHost proxy = new HttpHost(proxyHost, proxyPort);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();

		credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new NTCredentials(proxyUser, proxyPassword, getWorkstation(), proxyUserDomain));

		if (url.getUserInfo() != null && !url.getUserInfo().isEmpty()) {
			credsProvider.setCredentials(new AuthScope(url.getHost(), (url.getPort() > 0 ? url.getPort() : url.getDefaultPort())), new UsernamePasswordCredentials(url.getUserInfo()));
		}

		builder.setProxy(proxy);
		builder.setDefaultCredentialsProvider(credsProvider);

		Factory factory = new MyHttpClientFactory(builder);

		HttpCommandExecutor executor = new HttpCommandExecutor(new HashMap<String, CommandInfo>(), url, factory);

		return new RemoteWebDriver(executor, caps);
	}

	private String getWorkstation() {
		Map<String, String> env = System.getenv();

		if (env.containsKey("COMPUTERNAME")) {
			// Windows
			return env.get("COMPUTERNAME");         
		} else if (env.containsKey("HOSTNAME")) {
			// Unix/Linux/MacOS
			return env.get("HOSTNAME");
		} else {
			// From DNS
			try
			{
				return InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException ex)
			{
				return "Unknown";
			}
		}
	}

	public class MyHttpClientFactory implements org.openqa.selenium.remote.http.HttpClient.Factory {
		final HttpClientBuilder builder; 

		public MyHttpClientFactory(HttpClientBuilder builder) {
			this.builder = builder;
		}

		@Override
		public org.openqa.selenium.remote.http.HttpClient createClient(URL url) {
			return new ApacheHttpClient(builder.build(), url);
		}
	}

	/**
	 * 
	 * @return the Sauce Job id for the current thread
	 */
	public String getSessionId() {
		return sessionId.get();
	}

	/**
	 * 
	 * @return the {@link SauceOnDemandAuthentication} instance containing the
	 *         Sauce username/access key
	 */
	@Override
	public SauceOnDemandAuthentication getAuthentication() {
		return authentication;
	}

	protected RemoteWebDriver createWebDriverLocal(String browser) {

		log.info("Creating local driver");
		RemoteWebDriver thisDriver = driverPool.getResource();
		if ( thisDriver != null ) {
			// Chris L added 1/24/2018 as gecko driver cannot find existing driver - closing open driver seems to work.
			thisDriver.close();
			//log.info("DEBUG: re-using existing driver");
			//return thisDriver;
		}

		switch (browser) {
		case "firefox":
			// updated 1/3/18 by Chris Luhring - so tests can run w/ selenium 3.0+ and mozilla firefox 49+
			// involves downloading geckodriver and updating PATH, as detailed in this blog post:
			// http://www.automationtestinghub.com/selenium-3-0-launch-firefox-with-geckodriver/
			FirefoxOptions ffoptions = new FirefoxOptions();
			System.setProperty("webdriver.gecko.driver", "C:\\Program Files\\GeckoDriver.FireFox\\geckodriver.exe");
			return (RemoteWebDriver) new FirefoxDriver(ffoptions);

		case "chrome":
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				// Assumes web driver executable is in 'Drivers' directory under project.
				System.setProperty("webdriver.chrome.driver", "Drivers" + File.separator + "chromedriver.exe");
				ChromeDriverService service = ChromeDriverService.createDefaultService();
				ChromeOptions options = new ChromeOptions();
				options.addArguments("test-type");
				options.addArguments("--start-maximized");
				options.addArguments("--disable-extensions");
				return new ChromeDriver(service, options);
			}
			else {
				// Assumes web driver executable is in 'Drivers' directory under project.
				System.setProperty("webdriver.chrome.driver", "Drivers" + File.separator + "chromedriver");
				return (RemoteWebDriver) new ChromeDriver();
			}

		case "ie":
			// Assumes web driver executable is in 'Drivers' directory under project.
			// Important for IE to be set at 100% Zoom because:
			// IE generally does everything by coordinates,
			// Selenium asks for the element position to click
			// IE then gives it some coordinates of where it thinks the element is.
			// if the capabilities are set incorrectly or if you are using a different screen resolution
			// then 9 times out of 10 it sends the wrong coordinates back the Selenium, 
			// so Selenium clicks the coordinates (hence your test doesn't fail and carries on).

			InternetExplorerOptions options = new InternetExplorerOptions();
			options.setCapability(CapabilityType.BROWSER_NAME, "internet explorer");
			options.setCapability(CapabilityType.VERSION, "11");
			options.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
			options.setCapability("nativeEvents", false);
			options.setCapability("unexpectedAlertBehaviour", "accept");
			options.setCapability("ignoreProtectedModeSettings", true);
			options.setCapability("disable-popup-blocking", true);
			options.setCapability("enablePersistentHover", true);
			options.setCapability("ignoreZoomSetting", true);
			System.setProperty("webdriver.ie.driver", "Drivers" + File.separator + "IEDriverServer.exe");

			return (RemoteWebDriver) new InternetExplorerDriver (options);

		case "safari":
			return (RemoteWebDriver) new SafariDriver();

		default:
			Assert.fail("FAIL - Invalid browser parameter passed");

		}

		return null;

	}

	public By getMobileLiteElement(String element_key) {

		By foundObject = null;

		// log.info("DEBUG: select by string: " + element_key);
		// elementMap is a properties object initialized in setupSuite from property
		// file.
		String selectorFull = elementMap.getProperty(element_key);
		// Validate that key is found from property file.
		Assert.assertNotNull(selectorFull, "FAIL - Did not find " + element_key + " in element map");

		// Parse property value to determine the type of selector and selection
		// string
		// for the desired element. The first part of the value is the selector
		// type, then
		// a '!' separator token, followed by the string to be used by that
		// selector.
		// Example properties:
		// DashboardSearchBtn=name!RecordLocator
		// NewOppBtn=id!newopportunitybtn
		// NewOppModalHeader=css!div.panel-heading.panel-modal-newopportunity > h4
		int split = selectorFull.indexOf("!");
		String selectorType = selectorFull.substring(0, split);
		String selectorString = selectorFull.substring(split + 1);

		switch (selectorType) {
		case "css":
			foundObject = By.cssSelector(selectorString);
			break;
		case "id":
			foundObject = By.id(selectorString);
			break;
		case "name":
			foundObject = By.name(selectorString);
			break;
		case "xpath":
			foundObject = By.xpath(selectorString);
			break;
		case "linkText":
			foundObject = By.linkText(selectorString);
			break;
		case "partialLinkText":
			foundObject = By.partialLinkText(selectorString);
			break;
		default:
			Assert.fail("FAIL - Invalid selector type [" + selectorType + "] in map for key [" + element_key + "]");
		}

		return foundObject;
	}

	/**
	 * @return the {@link RemoteWebDriver} for the current thread
	 */
	public RemoteWebDriver getWebDriver() {
		//System.out.println("WebDriver " + threadDriver.get());
		return threadDriver.get();
	}

	/**
	 * helper method to supplement Selenium IDE because WebDriver does not have it
	 */
	protected boolean isElementPresent(By by) {
		RemoteWebDriver driver = getWebDriver();
		try {
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}


	/**
	 * helper method to supplement Selenium IDE because WebDriver does not have it
	 * 
	 * Differs from isElementPresent by checking that element is present AND 
	 * visible on the page.
	 */
	protected boolean isElementDisplayed(By by) {
		RemoteWebDriver driver = getWebDriver();
		Boolean displayed = false;
		try {
			displayed = driver.findElement(by).isDisplayed();
		} catch (NoSuchElementException e) {
			displayed = false;
		} catch (StaleElementReferenceException e1) {
			// try one more time...
			try {
				displayed = driver.findElement(by).isDisplayed();
			} catch (NoSuchElementException | StaleElementReferenceException e) {
				displayed = false;
			}
		}

		return displayed;
	}

	/**
	 * @author Chris Nye
	 * @description Helper method to wait on the UI to display a desired element
	 * @param by
	 *          Selector for element to be searched for
	 * @param seconds
	 *          Number of seconds to wait before timing out
	 * @return true: element was found and is being displayed<br>
	 *         false: element was not found or is not being displayed
	 * 
	 */
	protected boolean waitForElement(By by, int seconds) {

		boolean displayedElement = false;
		RemoteWebDriver driver = getWebDriver();

		// Intialize wait object on current driver, with timeout set to 'seconds'
		// parameter
		WebDriverWait wait = new WebDriverWait(driver, seconds);

		try {
			// Wait condition is to be that element exists, it is displayed and spinner/loading overlay is invisible
			wait.until(ExpectedConditions.and(
					ExpectedConditions.visibilityOfElementLocated(by),
					ExpectedConditions.invisibilityOfElementLocated(By.id("modalSpinner"))
					));

			// If exception is not thrown, element was found, set displayedElement to
			// true
			displayedElement = true;
		} catch (TimeoutException e) {
			//log.info("Did not find a visible element with [" + by.toString() + "]");
			displayedElement = false;
		} catch (Exception e) {
			log.error("FAIL - Generic exception e [" + e.getMessage() + "]");
			displayedElement = false;
		}

		// Check page load status, reset Timeout to use default shortTimeout duration if input time is shorter
		if ( seconds < shortTimeout) {
			wait.withTimeout(shortTimeout, TimeUnit.SECONDS);
		}    
		// Also only wait on this if displayedElement is true, 
		// loadstatus exists with data-xhr other than init.
		if ( displayedElement &&
				isElementPresent(By.cssSelector("div#loadstatus")) &&
				driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") != null &&
				! "init".equals(driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr")) ) {
			try {
				wait.until(ExpectedConditions.attributeToBe(By.cssSelector("div#loadstatus"), "data-xhr", "completed"));
			} catch (TimeoutException e) {
				Assert.fail("FAIL - page loadstatus did not indicate complete after waiting for [" + by.toString() + "], "
						+ "final page status of data-xhr: [" + driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") + "]", 
						e);
			}
		}
		return displayedElement;
	}

	/**
	 * Closes a modal by clicking the provided element, verifies that element is
	 * no longer displayed.  Always waits for modal elements to go away, if clicking
	 * element would display same/new modal, use closeModal(element, false)
	 * 
	 * Common elements to use:
	 *    getMobileLiteElement("modal.close.button"): Generic 'x' button available on most/all modals
	 *    getMobileLiteElement("searchModal.load.button"): Load button on search modal
	 *    By.id("newinboundcallbtn"): Open new inbound call button 
	 * 
	 * @param element Element to click and wait until it goes away

	 * @author Chris Nye
	 * Reviewed on: 2016-03-31
	 * Reviewed by: Team
	 */
	public void closeModal(By element) {
		closeModal(element, true);
	}

	/**
	 * Closes a modal by clicking the provided element, verifies that modal closes
	 * 
	 * Common elements to use:
	 *    getMobileLiteElement("modal.close.button"): Generic 'x' button available on most/all modals
	 *    getMobileLiteElement("searchModal.load.button"): Load button on search modal
	 *    By.id("newinboundcallbtn"): Open new inbound call button 
	 * 
	 * @param element Element to click and wait until it goes away
	 * @param modalWait if 'true', also wait for all modal elements to no longer be displayed
	 * 
	 * @author Chris Nye
	 * Reviewed on: 2016-03-31
	 * Reviewed by: Team
	 */
	public void closeModal(By element, boolean modalWait) {
		RemoteWebDriver driver = getWebDriver();
		WebDriverWait wait = new WebDriverWait(driver, shortTimeout);

		// Verify provided element exists on page and clickable.  
		try {
			wait.until(ExpectedConditions.elementToBeClickable(element));
		} catch (TimeoutException e) {
			Assert.fail("FAIL - element [" + element.toString() + "] did not become clickable within timeout.", e);
		} 

		if ( isElementPresent(By.cssSelector("div#loadstatus")) &&
				driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") != null &&
				! "init".equals(driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr")) ) {
			try {
				wait.until(ExpectedConditions.attributeToBe(By.cssSelector("div#loadstatus"), "data-xhr", "completed"));
			} catch (TimeoutException e) {
				//	    			Assert.fail("FAIL - page loadstatus did not indicate complete after clicking [" + element.toString() + "], "
				//	    			+ "final page status of data-xhr: [" + driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") + "]", 
				//	    			e);
			}
		}

		driver.findElement(element).click();
		try {
			// Wait for element to become invisible / removed
			wait.until(ExpectedConditions.invisibilityOfElementLocated(element));
			if ( modalWait ) {
				wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.modal-backdrop.fade.in")));
			}
		} catch (TimeoutException e) {
			Assert.fail("FAIL - clicking [" + element.toString() + "] did not close modal within timeout.", e);
		}

		// Check page load status
		if ( isElementPresent(By.cssSelector("div#loadstatus")) &&
				driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") != null &&
				! "init".equals(driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr")) ) {
			try {
				wait.until(ExpectedConditions.attributeToBe(By.cssSelector("div#loadstatus"), "data-xhr", "completed"));
			} catch (TimeoutException e) {
				Assert.fail("FAIL - page loadstatus did not indicate complete after clicking [" + element.toString() + "], "
						+ "final page status of data-xhr: [" + driver.findElement(By.cssSelector("div#loadstatus")).getAttribute("data-xhr") + "]", 
						e);
			}
		}

	}

	/**
	 * In case when you need to click element and wait until
	 * other element has loaded using default shortTimeout
	 * 
	 * @param clickElement Element to be clicked
	 * @param waitElement Element to wait on after clicking clickElement
	 */
	public void clickAndWait(By clickElement, By waitElement) {
		clickAndWait(clickElement, waitElement, shortTimeout);
	}

	/**
	 * In case when you need to click element and wait until
	 * other element has loaded
	 * 
	 * @param clickElement Element to be clicked
	 * @param waitElement Element to wait on after clicking clickElement
	 * @param timeout wait time (in seconds) to wait for elements
	 */
	public void clickAndWait(By clickElement, By waitElement, int timeout) {

		RemoteWebDriver driver = getWebDriver();

		Assert.assertTrue(waitForElement(clickElement, timeout),
				"FAIL - Did not find " + clickElement + " on the page within timeout period");
		driver.findElement(clickElement).click();
		Assert.assertTrue(waitForElement(waitElement, timeout),
				"FAIL - Did not find [" + waitElement + "] on the page within timeout period after clicking on " + clickElement);
	}

	public void userLogin(String userName, String userPassword) {

		log.info("DEBUG: logging in with username " + userName);
		RemoteWebDriver driver = getWebDriver();
		WebDriverWait wait = new WebDriverWait(driver, 120); // Dashboard can be slow, use longer timeout

		Assert.assertTrue(waitForElement(getMobileLiteElement("login.username.input"), shortTimeout),
				"FAIL - Did not display login fields");
		driver.findElement(getMobileLiteElement("login.username.input")).clear();
		driver.findElement(getMobileLiteElement("login.username.input")).sendKeys(userName);
		driver.findElement(getMobileLiteElement("login.password.input")).clear();
		driver.findElement(getMobileLiteElement("login.password.input")).sendKeys(userPassword);
		driver.findElement(getMobileLiteElement("login.login.button")).click();

		/**
		 * CNYE: Streamlined search for landing on dashboard.  After login, we have the following
		 * possible states:
		 *
		 *   1. Dashboard: getMobileLiteElement("dashboard.taskGrid.table")
		 *   2. Unresolved calls grid: By.cssSelector("button.btn-open-uc")
		 *   3. Unresolved error dialog: By.id("retryRequestBtn")
		 *   4. Invalid username / password: By.cssSelector("div#errormessage")
		 *
		 * Do a wait.until() on any of those being visible, process as needed.
		 */

		Boolean waitDashboard = true;
		int count = 0;
		while (waitDashboard && count++ < 5 ) { // try to limit looping

			try {
				wait.until(ExpectedConditions.or(
						ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#errormessage")),
						ExpectedConditions.visibilityOfElementLocated(By.id("changePasswordButton")),
						ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button.btn-open-uc")),
						ExpectedConditions.visibilityOfElementLocated(By.id("retryRequestBtn")),
						ExpectedConditions.visibilityOfElementLocated(getMobileLiteElement("dashboard.taskGrid.table"))
						));
			} catch (TimeoutException e) {
				Assert.fail("FAIL - Timed out after logging in as ["+ userName + "], did not find any expected element.  "
						+ "Ended on URL [" + driver.getCurrentUrl() + "]", e);
			}

			// Have found something matching above, now figure out which scenario we're in...
			if ( isElementDisplayed(By.cssSelector("div#errormessage"))) {
				// Error logging in, skip test....
				throw new SkipException("SKIPPING TEST: Login failure for user " + userName);

			} else if ( isElementDisplayed(By.id("changePasswordButton"))) {
				// Account has an expired password, skip test
				throw new SkipException("SKIPPING TEST: Expired password for user " + userName);

			} else if ( isElementDisplayed(getMobileLiteElement("dashboard.taskGrid.table"))) {
				// Landed on dashboard, can continue
				waitDashboard=false;
			} else if ( isElementDisplayed(By.cssSelector("button.btn-open-uc"))) {
				// Have unresolved calls to process
				wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#ucgrid button.btn-open-uc")));
				log.info("INFO - Found Open button on unresolved calls grid.");
				driver.findElement(By.cssSelector("div#ucgrid button.btn-open-uc")).click();
				// wait for the "Resolution" drop-down in the footer to be available and clickable
				waitForElement(getMobileLiteElement("footer.resolution.select"),shortTimeout);

				if ( waitForElement(getMobileLiteElement("licenseWarning.yes.button"), 1) ) {
					closeModal(getMobileLiteElement("licenseWarning.yes.button"));
				}

				wait.until(ExpectedConditions.elementToBeClickable(getMobileLiteElement("footer.resolution.select")));


				// Can close opened call immediately...
				// TODO - Commenting out closeCall
				//closeCall();
				// Close any remaining opportunity / Contacts.
				// TODO - Commenting out closeAllOpenCalls
				//closeAllOpenCalls();

			} else if ( isElementDisplayed(By.id("retryRequestBtn"))) {
				// Error retrieving unresolved calls list, click retry button and try again
				log.info("INFO - Found retry request button");
				closeModal(By.id("retryRequestBtn"));

			} else {
				Assert.fail("FAIL - In unknown state after login");
			}      // end if

		} // end while loop

		// After all that, should now be on dashboard...
		if( isElementDisplayed(getMobileLiteElement("login.login.button")) ) {
			throw new SkipException("SKIPPING TEST - Login button still displayed, login failed / stalled for user " + userName);
		}
		Assert.assertTrue(waitForElement(getMobileLiteElement("dashboard.taskGrid.table"), 5), "FAIL - Did not load dashboard after login");

		// If unresolved calls were processed and in non-default org, org select may be in different state.
		// Reset to ensure test has expected start state
		// TODO - Commenting Out switchOrg
		//switchOrg("Kaiser");

		log.info("PASS - Finished login");
	}
}
