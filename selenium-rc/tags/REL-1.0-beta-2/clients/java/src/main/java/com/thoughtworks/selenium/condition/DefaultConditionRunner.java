/*
 * Copyright 2008 ThoughtWorks, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.thoughtworks.selenium.condition;

import com.thoughtworks.selenium.Selenium;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * {@inheritDoc}
 * <p/>
 * <p> This implementation throws a simple {@link RuntimeException} when the
 * condition is not met in the {@link #waitFor(Condition)} method. More specific
 * runners are preferred for different testing frameworks. E.g. JUnit tests
 * would prefer to use {@link JUnitConditionRunner}.
 */
public class DefaultConditionRunner implements ConditionRunner {

    private static final Logger logger =
            Logger.getLogger(DefaultConditionRunner.class.getName());

    private final Monitor monitor;
    private final Selenium selenium;
    private final int initialDelay;
    private final int interval;
    private final int timeout;

    /**
     * @param selenium the selenium to be passed to the Conditions run from within
     *                 this runner.
     * @param initialDelay (in millis) how long to wait before the initial test of the condition
     * @param interval (in millis) when waiting for a condition, how long to wait
     *                 between calls to
     *                 {@link Condition#isTrue(com.google.testing.selenium.condition.ConditionRunner.Context)}
     * @param timeout  (in millis) when waiting for a condition, how long to wait
     *                 until we give up.
     */
    public DefaultConditionRunner(Monitor monitor, Selenium selenium, int initialDelay, int interval, int timeout) {
        this.monitor = monitor;
        this.selenium = selenium;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.timeout = timeout;
    }

    /**
     * @param selenium the selenium to be passed to the Conditions run from within
     *                 this runner.
     * @param interval (in millis) when waiting for a condition, how long to wait
     *                 between calls to
     *                 {@link Condition#isTrue(com.google.testing.selenium.condition.ConditionRunner.Context)}
     * @param timeout  (in millis) when waiting for a condition, how long to wait
     *                 until we give up.
     */
    public DefaultConditionRunner(Monitor monitor, Selenium selenium, int interval, int timeout) {
        this(new NoOpMonitor(), selenium, interval, interval, timeout);
    }

    /**
     * Constructs an instance of this class with a {@link NoOpMonitor}.
     *
     * @see DefaultConditionRunner#DefaultConditionRunner(Monitor, Selenium, int, int)
     */
    public DefaultConditionRunner(Selenium selenium, int initialDelay, int interval, int timeout) {
        this(new NoOpMonitor(), selenium, initialDelay, interval, timeout);
    }

    /**
     * Constructs an instance of this class with a {@link NoOpMonitor}.
     *
     * @see DefaultConditionRunner#DefaultConditionRunner(Monitor, Selenium, int, int)
     */
    public DefaultConditionRunner(Selenium selenium, int interval, int timeout) {
        this(new NoOpMonitor(), selenium, interval, timeout);
    }

    /**
     * Constructs an instance of this class with reasonable defaults.
     *
     * @see DefaultConditionRunner#DefaultConditionRunner(Monitor, Selenium, int, int)
     */
    public DefaultConditionRunner(Selenium selenium) {
        this(new NoOpMonitor(), selenium, 500, 45 * 1000);
    }

    /**
     * A {@link Monitor} can be installed in {@link DefaultConditionRunner} as an
     * open ended way of being notified of certain events.
     */
    public interface Monitor {

        /**
         * Called whenever a {@link DefaultConditionRunner#waitFor(Condition)} has
         * begun, and is being tracked with the given {@code condition}.
         */
        void waitHasBegun(ConditionRunner.Context context, Condition condition);

        /**
         * Called whenever a {@link DefaultConditionRunner#waitFor(Condition)} is
         * successful (i.e.
         * {@link Condition#isTrue(com.google.testing.selenium.condition.ConditionRunner.Context)}
         * returned true within the timeout}.
         */
        void conditionWasReached(ConditionRunner.Context context, Condition condition);
    }

    /**
     * A no-op implementation of {@link Monitor}.
     * <p/>
     * {@inheritDoc}
     */
    public static final class NoOpMonitor implements Monitor {
        public void conditionWasReached(ConditionRunner.Context context, Condition condition) {
        }

        public void waitHasBegun(ConditionRunner.Context context, Condition condition) {
        }
    }


    public void waitFor(Condition condition) {
        waitFor("", condition);
    }

    public void waitFor(String narrative, Condition condition) {
        ContextImpl context = new ContextImpl();
        try {
            monitor.waitHasBegun(context, condition);
            context.say("Waiting for");
            threadSleep(initialDelay);
            while (context.elapsed() < context.timeout()) {
                if (condition.isTrue(context)) {
                    context.reached(condition);
                    monitor.conditionWasReached(context, condition);
                    return;
                }
                threadSleep(interval);
            }
        } catch (RuntimeException e) {
            throwAssertionException("Exception while waiting for '" + condition.toString() + "'", e);
        }
        // Note that AssertionFailedError will pass right through
        context.fail(narrative, condition);
    }

    private void threadSleep(int interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException ignore) {
        }
    }

    protected void throwAssertionException(String message) {
        throw new RuntimeException(message);
    }

    protected void throwAssertionException(String message, Throwable throwable) {
        throw new RuntimeException(message, throwable);
    }

    private final class ContextImpl implements ConditionRunner.Context {

        private final long start;
        private List<String> info = new ArrayList<String>();
        private String lastInfo;

        public ContextImpl() {
            this.start = now();
        }

        private long now() {
            return System.currentTimeMillis();
        }

        public void info(String info) {
            if (!info.equals(lastInfo)) {
                this.info.add(info);
            }
            lastInfo = info;
        }

        public long elapsed() {
            return now() - start;
        }

        public Selenium getSelenium() {
            return selenium;
        }

        public ConditionRunner getConditionRunner() {
            return DefaultConditionRunner.this;
        }

        private void fail(Condition condition) {
            fail("", condition);
        }

        private void fail(String narrative, Condition condition) {
            String message = condition.toString() +
                    " failed to become true within " + timeout() + " msec";
            message += narrative.equals("") ? "" : "; " + narrative;
            say("Failed");
            if (!info.isEmpty()) {
                message += "; " + info;
            }
            throwAssertionException(message);
        }

        private void say(String msg) {
            logger.info(new Date() + " - " + msg + " " + this);
        }

        private int timeout() {
            return timeout;
        }

        private void reached(Condition condition) {
            say("Reached");
        }
    }
}
