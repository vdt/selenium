/*
 * Copyright 2004 ThoughtWorks, Inc
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
 */

SELENIUM_PROCESS_WAIT = "wait";

starting_up  = true;
TEST_FINISHED = true;
TEST_CONTINUE = false;

function TestLoop(commandFactory, executionContext) {
    this.commandFactory = commandFactory;

    var self = this;

    this.start = function() {
        this.continueCurrentTest();
    }

    this.continueCurrentTest = function() {
        var testStatus = this.kickoffNextCommandExecution();

        if (testStatus == TEST_FINISHED) {
            this.testComplete();
        }
    }

    this.kickoffOneCommandExecution = function() {

        var command;
        if (starting_up == true) {
            command = this.firstCommand();
            starting_up = false;
        } else {
            command = this.nextCommand();
        }

        if (!command) return TEST_FINISHED;

        // Make the current row blue
        this.commandStarted(command);

        var result;
        try {
            var handler = this.commandFactory.getCommandHandler(command.command);
            if(handler == null) {
                throw new Error("Unknown command");
            }

            result = handler.execute(selenium, command);
        } catch (e) {
            this.commandError(e.message);
            return TEST_FINISHED;
        }

        // Record the result so that we can continue the execution using window.setTimeout()
        this.lastCommandResult = result;
        if (result.processState == SELENIUM_PROCESS_WAIT) {
           
            //executionContext.waitForPageLoad(this,selenium);
            
            alert("wait"); // with this line commented in, it all works fine and dandy - just click through and it goes on its way
                           // run the test a few times, though, and then it goes bad.  ????
            
            //delay(2000); //    I tried commenting this line in, but it seems to go ballistic
        } else {
            // Continue processing
            //this.continueCommandExecutionWithDelay();
            //alert("continue");
        }

        // Test is not finished.
        return TEST_CONTINUE;
    }

 function delay(millis) {
   startMillis = new Date();
   while (true) {
     milli = new Date();
     if (milli-startMillis > millis) {
       break;
     }
   }
 }


    this.kickoffNextCommandExecution = function() {

        var command;
        if (starting_up == true) {
            command = this.firstCommand();
            starting_up = false;
        } else {
            command = this.nextCommand();
        }

        if (!command) return TEST_FINISHED;

        // Make the current row blue
        this.commandStarted(command);

        var result;
        try {
            var handler = this.commandFactory.getCommandHandler(command.command);
            if(handler == null) {
                throw new Error("Unknown command");
            }

            result = handler.execute(selenium, command);
        } catch (e) {
            this.commandError(e.message);
            return TEST_FINISHED;
        }

        // Record the result so that we can continue the execution using window.setTimeout()
        this.lastCommandResult = result;
        if (result.processState == SELENIUM_PROCESS_WAIT) {
           
            executionContext.waitForPageLoad(this,selenium);

        } else {
            // Continue processing
            this.continueCommandExecutionWithDelay();
        }

        // Test is not finished.
        return TEST_CONTINUE;
    }

    /**
     * Continues the command execution, after waiting for the specified delay.
     */
    this.continueCommandExecutionWithDelay = function() {
        // Get the interval to use for this command execution, using the pauseInterval is
        // specified. Reset the pause interval, since it's a one-off thing.
        var interval = this.pauseInterval || this.getCommandInterval();
        this.pauseInterval = undefined;

        // Continue processing
        if (interval >= 0) {
            window.setTimeout("testLoop.finishCommandExecution()", interval);
        }
    }

    /**
     * Finishes the execution of the previous command, and continues the test
     */
    this.finishCommandExecution = function() {
        this.commandComplete(this.lastCommandResult);
        this.continueCurrentTest();
    }
}

/** The default is not to have any interval between commands. */
TestLoop.prototype.getCommandInterval = function() {
   return 0;
}

TestLoop.prototype.firstCommand = noop;

TestLoop.prototype.nextCommand = noop;

TestLoop.prototype.commandStarted = noop;

TestLoop.prototype.commandError = noop;

TestLoop.prototype.commandComplete = noop;

TestLoop.prototype.testComplete = noop;

function noop() {};
