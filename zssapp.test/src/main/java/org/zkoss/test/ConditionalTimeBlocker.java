/* ConditionalTimeBlocker.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Jan 18, 2012 3:01:29 PM , Created by sam
}}IS_NOTE

Copyright (C) 2012 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.test;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

/**
 * @author sam
 *
 */
public class ConditionalTimeBlocker {

	final WebDriver webDriver;
	
	final Browser browser;
	
	final JavascriptExecutor javascriptExecutor;
	
	@Inject
	public ConditionalTimeBlocker (WebDriver webDriver, Browser browser) {
		this.webDriver = webDriver;
		this.browser = browser;
		javascriptExecutor = (JavascriptExecutor)webDriver;
	}
	
	public void waitResponse () {
		int timeOutInSeconds = browser.isIE6() || browser.isIE7() || browser.isFF36() ? 10 : 5;
		waitUntil(timeOutInSeconds, new Predicate<Void>() {
			public boolean apply(Void input) {
				Boolean ret = null;
				try {
					ret = !(Boolean) javascriptExecutor.executeScript("return !!zAu.processing();");
				} catch (WebDriverException ex) {
					//ignore js error
				}
				return ret != null && ret;
			}
		});
		
		if (browser.isSafari()) {//wait a sec, sometimes chrome finish au processing, but result doesn't come out, SS_045_Test
			waitUntil(1);
		}
	}
	
	/**
	 * Wait least 5 Seconds until {@link Predicate} returns true
	 * 
	 * @param predicate
	 */
	public void waitUntil(final Predicate<Void> predicate) {
    	waitUntil(5, predicate);
    }
	
	public void waitUntil(long timeOutInSeconds, final Predicate<Void> predicate) {
    	new WebDriverWait(webDriver, timeOutInSeconds).until(
        	new ExpectedCondition<Boolean>(){

				public Boolean apply(WebDriver webDriver) {
					return predicate.apply(null);
				}
        });
    }
	
	public void waitUntil(long timeOutInSeconds) {
    	try {
        	waitUntil(timeOutInSeconds, new Predicate<Void>() {

    			public boolean apply(Void input) {
    				return false;
    			}
    		});	
    	} catch (TimeoutException ex) {
    	}
	}
}
